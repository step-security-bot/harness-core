package software.wings.service.impl.instance;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;
import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.HQuery.excludeValidate;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.ClusterRecord.ClusterRecordKeys;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpBillingAccount.GcpBillingAccountKeys;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.security.EncryptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.api.DeploymentSummary;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.beans.LicenseInfo;
import software.wings.beans.ResourceLookup;
import software.wings.beans.ResourceLookup.ResourceLookupKeys;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata;
import software.wings.service.impl.LicenseUtils;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.settings.SettingVariableTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class CloudToHarnessMappingServiceImpl implements CloudToHarnessMappingService {
  private final HPersistence persistence;
  private final WingsPersistence wingsPersistence;
  private final DeploymentService deploymentService;

  private static final String EXC_MSG_USER_DOESNT_EXIST = "User does not exist";

  @Inject
  public CloudToHarnessMappingServiceImpl(
      HPersistence persistence, WingsPersistence wingsPersistence, DeploymentService deploymentService) {
    this.persistence = persistence;
    this.wingsPersistence = wingsPersistence;
    this.deploymentService = deploymentService;
  }

  @Override
  public Optional<HarnessServiceInfo> getHarnessServiceInfo(DeploymentSummary deploymentSummary) {
    return getHarnessServiceInfo(deploymentService.getWithAccountId(deploymentSummary));
  }

  private Optional<HarnessServiceInfo> getHarnessServiceInfo(Optional<DeploymentSummary> summary) {
    if (summary.isPresent()) {
      DeploymentSummary deploymentSummaryResponse = summary.get();
      InfrastructureMapping infrastructureMapping =
          persistence.createQuery(InfrastructureMapping.class)
              .filter(InfrastructureMappingKeys.uuid, deploymentSummaryResponse.getInfraMappingId())
              .get();

      if (infrastructureMapping != null) {
        return Optional.of(
            new HarnessServiceInfo(infrastructureMapping.getServiceId(), infrastructureMapping.getAppId(),
                infrastructureMapping.getComputeProviderSettingId(), infrastructureMapping.getEnvId(),
                deploymentSummaryResponse.getInfraMappingId(), deploymentSummaryResponse.getUuid()));
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<HarnessServiceInfo> getHarnessServiceInfo(
      String accountId, String computeProviderId, String namespace, String podName) {
    Instance instance = persistence.createQuery(Instance.class, excludeValidate)
                            .filter(InstanceKeys.accountId, accountId)
                            .filter(InstanceKeys.computeProviderId, computeProviderId)
                            .filter(InstanceKeys.instanceInfoNamespace, namespace)
                            .filter(InstanceKeys.instanceInfoPodName, podName)
                            .order(Sort.descending(InstanceKeys.createdAt))
                            .get();

    if (null != instance) {
      return getHarnessServiceInfo(deploymentService.getWithInfraMappingId(accountId, instance.getInfraMappingId()));
    }
    return Optional.empty();
  }

  @Override
  public Optional<SettingAttribute> getSettingAttribute(String id) {
    return Optional.ofNullable(
        persistence.createQuery(SettingAttribute.class).filter(SettingAttributeKeys.uuid, id).get());
  }

  @Override
  public List<HarnessServiceInfo> getHarnessServiceInfoList(List<DeploymentSummary> deploymentSummaryList) {
    List<String> infraMappingIds =
        deploymentSummaryList.stream().map(DeploymentSummary::getInfraMappingId).collect(Collectors.toList());
    List<InfrastructureMapping> infrastructureMappings =
        persistence.createQuery(InfrastructureMapping.class, excludeAuthority)
            .field(InfrastructureMappingKeys.uuid)
            .in(infraMappingIds)
            .asList();
    return infrastructureMappings.stream()
        .map(infrastructureMapping
            -> new HarnessServiceInfo(infrastructureMapping.getServiceId(), infrastructureMapping.getAppId(),
                infrastructureMapping.getComputeProviderSettingId(), infrastructureMapping.getEnvId(),
                infrastructureMapping.getUuid(), null))
        .collect(Collectors.toList());
  }

  @Override
  public List<Account> getCeEnabledAccounts() {
    List<Account> accounts = new ArrayList<>();
    Query<Account> query = persistence.createQuery(Account.class, excludeAuthority);
    query.or(query.criteria(AccountKeys.cloudCostEnabled).equal(Boolean.TRUE),
        query.criteria(AccountKeys.ceAutoCollectK8sEvents).equal(Boolean.TRUE));
    try (HIterator<Account> accountItr = new HIterator<>(query.fetch())) {
      for (Account account : accountItr) {
        accounts.add(account);
      }
    }
    return accounts;
  }

  @Override
  public Account getAccountInfoFromId(String accountId) {
    Account defaultAccount = Account.Builder.anAccount().withAccountName(accountId).build();
    try (HIterator<Account> query = new HIterator<>(persistence.createQuery(Account.class, excludeAuthority)
                                                        .filter(AccountKeys.cloudCostEnabled, Boolean.TRUE)
                                                        .field(AccountKeys.uuid)
                                                        .equal(accountId)
                                                        .fetch())) {
      for (Account account : query) {
        if (account.getUuid().equals(accountId)) {
          return account;
        }
      }
    }
    return defaultAccount;
  }

  @Override
  public List<ResourceLookup> getResourceList(String accountId, List<String> resourceIds) {
    return persistence.createQuery(ResourceLookup.class)
        .filter(ResourceLookupKeys.accountId, accountId)
        .field(ResourceLookupKeys.resourceId)
        .in(resourceIds)
        .asList();
  }

  @Override
  public Map<String, String> getServiceName(String accountId, List<String> serviceIds) {
    List<Service> services = persistence.createQuery(Service.class)
                                 .filter(ServiceKeys.accountId, accountId)
                                 .field(ServiceKeys.uuid)
                                 .in(serviceIds)
                                 .asList();
    return services.stream().collect(Collectors.toMap(Service::getUuid, Service::getName));
  }

  @Override
  public Map<String, String> getEnvName(String accountId, List<String> envIds) {
    List<Environment> environments = persistence.createQuery(Environment.class)
                                         .filter(EnvironmentKeys.accountId, accountId)
                                         .field(EnvironmentKeys.uuid)
                                         .in(envIds)
                                         .asList();
    return environments.stream().collect(Collectors.toMap(Environment::getUuid, Environment::getName));
  }

  @Override
  public List<DeploymentSummary> getDeploymentSummary(
      String accountId, String offset, Instant startTime, Instant endTime) {
    return deploymentService.getDeploymentSummary(accountId, offset, startTime, endTime);
  }

  @Override
  public SettingAttribute getFirstSettingAttributeByCategory(String accountId, SettingCategory category) {
    return persistence.createQuery(SettingAttribute.class, excludeAuthority)
        .filter(SettingAttributeKeys.accountId, accountId)
        .filter(SettingAttributeKeys.category, category)
        .order(Sort.ascending(SettingAttributeKeys.createdAt))
        .get();
  }

  @Override
  public List<SettingAttribute> listSettingAttributesCreatedInDuration(
      String accountId, SettingCategory category, SettingVariableTypes valueType) {
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    try (HIterator<SettingAttribute> query =
             new HIterator<>(persistence.createQuery(SettingAttribute.class, excludeAuthority)
                                 .filter(SettingAttributeKeys.accountId, accountId)
                                 .filter(SettingAttributeKeys.category, category)
                                 .filter(SettingAttributeKeys.valueType, valueType)
                                 .fetch())) {
      for (SettingAttribute settingAttribute : query) {
        settingAttributes.add(settingAttribute);
      }
    }
    return settingAttributes;
  }

  @Override
  public List<SettingAttribute> listSettingAttributesCreatedInDuration(
      String accountId, SettingCategory category, SettingVariableTypes valueType, long startTime, long endTime) {
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    try (HIterator<SettingAttribute> query =
             new HIterator<>(persistence.createQuery(SettingAttribute.class, excludeAuthority)
                                 .filter(SettingAttributeKeys.accountId, accountId)
                                 .filter(SettingAttributeKeys.category, category)
                                 .filter(SettingAttributeKeys.valueType, valueType)
                                 .field(SettingAttributeKeys.createdAt)
                                 .greaterThan(startTime)
                                 .field(SettingAttributeKeys.createdAt)
                                 .lessThan(endTime)
                                 .fetch())) {
      for (SettingAttribute settingAttribute : query) {
        settingAttributes.add(settingAttribute);
      }
    }
    return settingAttributes;
  }

  public List<GcpBillingAccount> listGcpBillingAccountUpdatedInDuration(
      String accountId, long startTime, long endTime) {
    return persistence.createQuery(GcpBillingAccount.class, excludeAuthority)
        .filter(GcpBillingAccountKeys.accountId, accountId)
        .field(GcpBillingAccountKeys.lastUpdatedAt)
        .greaterThanOrEq(startTime)
        .field(GcpBillingAccountKeys.lastUpdatedAt)
        .lessThan(endTime)
        .asList();
  }

  @Override
  public String getEntityName(BillingDataQueryMetadata.BillingDataMetaDataFields field, String entityId) {
    switch (field) {
      case APPID:
      case ENVID:
      case SERVICEID:
      case CLUSTERID:
        return fetchEntityName(field, entityId);
      case CLOUDSERVICENAME:
      case TASKID:
      case WORKLOADNAME:
      case NAMESPACE:
      case CLUSTERNAME:
        return entityId;
      default:
        throw new InvalidRequestException("Invalid EntityType " + field);
    }
  }

  private String fetchEntityName(BillingDataQueryMetadata.BillingDataMetaDataFields field, String entityId) {
    switch (field) {
      case APPID:
        return getApplicationName(entityId);
      case ENVID:
        return getEnvironmentName(entityId);
      case SERVICEID:
        return getServiceName(entityId);
      case CLUSTERID:
        return getClusterName(entityId);
      default:
        throw new InvalidRequestException("Invalid EntityType " + field);
    }
  }

  private String getApplicationName(String entityId) {
    try {
      Application app = persistence.get(Application.class, entityId);
      if (app != null) {
        return app.getName();
      } else {
        return entityId;
      }
    } catch (Exception e) {
      logger.info("Entity Id could not be converted : ", e);
      return entityId;
    }
  }

  private String getServiceName(String entityId) {
    try {
      Service service = persistence.get(Service.class, entityId);
      if (service != null) {
        return service.getName();
      } else {
        return entityId;
      }
    } catch (Exception e) {
      logger.info("Entity Id could not be converted : ", e);
      return entityId;
    }
  }

  private String getEnvironmentName(String entityId) {
    try {
      Environment env = persistence.get(Environment.class, entityId);
      if (env != null) {
        return env.getName();
      } else {
        return entityId;
      }
    } catch (Exception e) {
      logger.info("Entity Id could not be converted : ", e);
      return entityId;
    }
  }

  private String getClusterName(String entityId) {
    try {
      Cluster cluster = getCluster(entityId).getCluster();
      if (cluster != null) {
        if (cluster.getClusterType().equals(AWS_ECS)) {
          EcsCluster ecsCluster = (EcsCluster) cluster;
          if (null != ecsCluster.getClusterName()) {
            return ecsCluster.getClusterName();
          } else {
            return entityId;
          }
        } else if (cluster.getClusterType().equals(DIRECT_KUBERNETES)) {
          DirectKubernetesCluster kubernetesCluster = (DirectKubernetesCluster) cluster;
          String clusterName = kubernetesCluster.getClusterName();
          if (null == clusterName || clusterName.equals("")) {
            SettingAttribute settingAttribute = getSettingAttributeForCluster(kubernetesCluster.getCloudProviderId());
            clusterName = settingAttribute.getName();
          }
          return clusterName;
        } else {
          return entityId;
        }
      } else {
        return entityId;
      }
    } catch (Exception e) {
      logger.info("Entity Id could not be converted : ", e);
      return entityId;
    }
  }

  private ClusterRecord getCluster(String clusterId) {
    Query<ClusterRecord> query =
        persistence.createQuery(ClusterRecord.class).filter(ClusterRecordKeys.uuid, new ObjectId(clusterId));
    return query.get();
  }

  private SettingAttribute getSettingAttributeForCluster(String varId) {
    return persistence.get(SettingAttribute.class, varId);
  }

  @Override
  public UserGroup getUserGroup(String accountId, String userGroupId, boolean loadUsers) {
    UserGroup userGroup = persistence.createQuery(UserGroup.class)
                              .filter(UserGroupKeys.accountId, accountId)
                              .filter(UserGroup.ID_KEY, userGroupId)
                              .get();
    if (userGroup == null) {
      return null;
    }

    if (loadUsers) {
      Account account = getAccountInfoFromId(accountId);
      loadUsers(userGroup, account);
    }
    return userGroup;
  }

  private void loadUsers(UserGroup userGroup, Account account) {
    if (userGroup.getMemberIds() != null) {
      PageRequest<User> req = aPageRequest()
                                  .addFilter(ID_KEY, SearchFilter.Operator.IN, userGroup.getMemberIds().toArray())
                                  .addFilter(UserKeys.accounts, SearchFilter.Operator.IN, account)
                                  .build();

      PageResponse<User> res = wingsPersistence.query(User.class, req);
      List<User> userList = res.getResponse();
      userList.sort((u1, u2) -> StringUtils.compareIgnoreCase(u1.getName(), u2.getName()));
      userGroup.setMembers(userList);
    } else {
      userGroup.setMembers(new ArrayList<>());
    }
  }

  @Override
  public User getUser(String userId) {
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      throw new UnauthorizedException(EXC_MSG_USER_DOESNT_EXIST, USER);
    }

    List<Account> accounts = user.getAccounts();
    if (isNotEmpty(accounts)) {
      accounts.forEach(account -> decryptLicenseInfo(account, false));
    }

    return user;
  }

  public Account decryptLicenseInfo(Account account, boolean setExpiry) {
    if (account == null) {
      return null;
    }

    byte[] encryptedLicenseInfo = account.getEncryptedLicenseInfo();
    if (isNotEmpty(encryptedLicenseInfo)) {
      byte[] decryptedBytes = EncryptionUtils.decrypt(encryptedLicenseInfo, null);
      if (isNotEmpty(decryptedBytes)) {
        LicenseInfo licenseInfo = LicenseUtils.convertToObject(decryptedBytes, setExpiry);
        account.setLicenseInfo(licenseInfo);
      } else {
        logger.error("Error while decrypting license info. Deserialized object is not instance of LicenseInfo");
      }
    }

    return account;
  }
}
