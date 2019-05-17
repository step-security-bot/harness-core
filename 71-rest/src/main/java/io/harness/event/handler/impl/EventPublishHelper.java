package io.harness.event.handler.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.event.handler.impl.Constants.ACCOUNT_ID;
import static io.harness.event.handler.impl.Constants.EMAIL_ID;
import static io.harness.event.handler.impl.Constants.USER_INVITE_ID;
import static io.harness.event.handler.impl.Constants.USER_NAME;
import static software.wings.beans.security.UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME;
import static software.wings.beans.security.UserGroup.DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME;
import static software.wings.beans.security.UserGroup.DEFAULT_PROD_SUPPORT_USER_GROUP_NAME;
import static software.wings.beans.security.UserGroup.DEFAULT_READ_ONLY_USER_GROUP_NAME;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder;
import io.harness.beans.SortOrder.OrderType;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.event.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.EntityType;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.SamlSettings;
import software.wings.common.VerificationConstants;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WhitelistService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Publishes event if all the criteria is met. MarketoHandler handles the event and converts it into a marketo campaign.
 * @author rktummala on 11/27/18
 */
@Singleton
@Slf4j
public class EventPublishHelper {
  @Inject private EventPublisher eventPublisher;
  @Inject private ExecutorService executorService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private AccountService accountService;
  @Inject private UserService userService;
  @Inject private SSOSettingService ssoSettingService;
  @Inject private DelegateService delegateService;
  @Inject private WhitelistService whitelistService;
  @Inject private UserGroupService userGroupService;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private MarketoConfig marketoConfig;
  @Inject private SegmentConfig segmentConfig;
  @Inject private StateExecutionService stateExecutionService;
  @Inject private ContinuousVerificationService continuousVerificationService;

  private List<StateType> analysisStates = VerificationConstants.getAnalysisStates();

  public void publishLicenseChangeEvent(String accountId, String oldAccountType, String newAccountType) {
    if (!checkIfMarketoOrSegmentIsEnabled()) {
      return;
    }

    executorService.submit(() -> {
      Map<String, String> properties = new HashMap<>();
      properties.put(ACCOUNT_ID, accountId);
      publishEvent(EventType.LICENSE_UPDATE, properties);
    });

    EventType eventType = null;
    if (oldAccountType == null || newAccountType == null) {
      return;
    }

    if (oldAccountType.equals(newAccountType)) {
      return;
    }

    if (AccountType.TRIAL.equals(oldAccountType) && AccountType.PAID.equals(newAccountType)) {
      eventType = EventType.TRIAL_TO_PAID;
    } else if (AccountType.TRIAL.equals(oldAccountType) && AccountType.COMMUNITY.equals(newAccountType)) {
      eventType = EventType.TRIAL_TO_COMMUNITY;
    } else if (AccountType.COMMUNITY.equals(oldAccountType) && AccountType.PAID.equals(newAccountType)) {
      eventType = EventType.COMMUNITY_TO_PAID;
    }

    if (eventType != null) {
      EventType finalEventType = eventType;
      executorService.submit(() -> notifyAllUsersOfAccount(accountId, finalEventType));
    }
  }

  public void publishSSOEvent(String accountId) {
    String userEmail = checkIfMarketoOrSegmentIsEnabledAndGetUserEmail(EventType.SETUP_SSO);

    if (isEmpty(userEmail)) {
      return;
    }

    executorService.submit(() -> {
      if (!shouldPublishEventForAccount(accountId)) {
        return;
      }

      SamlSettings samlSettings = ssoSettingService.getSamlSettingsByAccountId(accountId);
      LdapSettings ldapSettings = ssoSettingService.getLdapSettingsByAccountId(accountId);

      boolean hasSamlSetting = samlSettings != null && samlSettings.getCreatedBy().getEmail().equals(userEmail);
      boolean hasLdapSetting = ldapSettings != null && ldapSettings.getCreatedBy().getEmail().equals(userEmail);

      boolean shouldReport = !(hasSamlSetting && hasLdapSetting) && (hasSamlSetting || hasLdapSetting);

      if (!shouldReport) {
        return;
      }

      Map<String, String> properties = new HashMap<>();
      properties.put(ACCOUNT_ID, accountId);
      properties.put(EMAIL_ID, userEmail);
      publishEvent(EventType.SETUP_SSO, properties);
    });
  }

  public void publishSetupCV247Event(String accountId, String cvConfigId) {
    String userEmail = checkIfMarketoOrSegmentIsEnabledAndGetUserEmail(EventType.SETUP_CV_24X7);

    if (isEmpty(userEmail)) {
      return;
    }

    executorService.submit(() -> {
      if (!shouldPublishEventForAccount(accountId)) {
        return;
      }

      if (!isFirstCV247ConfigInAccount(cvConfigId, accountId, userEmail)) {
        return;
      }

      Map<String, String> properties = new HashMap<>();
      properties.put(ACCOUNT_ID, accountId);
      properties.put(EMAIL_ID, userEmail);
      publishEvent(EventType.SETUP_CV_24X7, properties);
    });
  }

  private boolean isFirstCV247ConfigInAccount(String cvConfigId, String accountId, String userEmail) {
    PageRequest<CVConfiguration> pageRequest = aPageRequest()
                                                   .addFilter("accountId", Operator.EQ, accountId)
                                                   .addFilter("createdBy.email", Operator.EQ, userEmail)
                                                   .addOrder(CVConfiguration.CREATED_AT_KEY, OrderType.ASC)
                                                   .addFieldsIncluded("_id")
                                                   .withLimit("1")
                                                   .build();
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId, pageRequest);

    if (isEmpty(cvConfigurations)) {
      return false;
    }

    if (cvConfigId.equals(cvConfigurations.get(0).getUuid())) {
      return true;
    }

    return false;
  }

  public void publishSetupRbacEvent(String accountId, String entityId, EntityType entityType) {
    String userEmail = checkIfMarketoOrSegmentIsEnabledAndGetUserEmail(EventType.SETUP_RBAC);

    if (isEmpty(userEmail)) {
      return;
    }

    executorService.submit(() -> {
      if (!shouldPublishEventForAccount(accountId)) {
        return;
      }

      if (!isFirstRbacConfigInAccount(accountId, entityId, entityType, userEmail)) {
        return;
      }

      Map<String, String> properties = new HashMap<>();
      properties.put(ACCOUNT_ID, accountId);
      properties.put(EMAIL_ID, userEmail);
      publishEvent(EventType.SETUP_RBAC, properties);
    });
  }

  private boolean isFirstRbacConfigInAccount(
      String accountId, String entityId, EntityType entityType, String userEmail) {
    if (EntityType.USER_GROUP.equals(entityType)) {
      PageRequest<UserGroup> pageRequest = aPageRequest()
                                               .addFilter("accountId", Operator.EQ, accountId)
                                               .addFilter("createdBy.email", Operator.EQ, userEmail)
                                               .addOrder(UserGroup.CREATED_AT_KEY, OrderType.ASC)
                                               .build();
      PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, false);
      List<UserGroup> userGroups = pageResponse.getResponse();
      if (isEmpty(userGroups)) {
        return false;
      }

      Optional<UserGroup> firstUserGroupOptional = userGroups.stream()
                                                       .filter(userGroup -> {
                                                         String userGroupName = userGroup.getName();
                                                         switch (userGroupName) {
                                                           case DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME:
                                                           case DEFAULT_PROD_SUPPORT_USER_GROUP_NAME:
                                                           case DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME:
                                                           case DEFAULT_READ_ONLY_USER_GROUP_NAME:
                                                             return false;
                                                           default:
                                                             return true;
                                                         }
                                                       })
                                                       .findFirst();

      if (!firstUserGroupOptional.isPresent()) {
        return false;
      }

      if (entityId.equals(firstUserGroupOptional.get().getUuid())) {
        return true;
      }
    } else if (EntityType.USER.equals(entityType)) {
      Account account = accountService.getFromCache(accountId);
      PageRequest<User> pageRequest = aPageRequest()
                                          .addFilter("accounts", Operator.IN, account)
                                          .addFilter("createdBy.email", Operator.EQ, userEmail)
                                          .addOrder(User.CREATED_AT_KEY, OrderType.ASC)
                                          .withLimit("10")
                                          .build();

      PageResponse<User> pageResponse = userService.list(pageRequest, false);
      List<User> users = pageResponse.getResponse();
      if (isEmpty(users)) {
        return false;
      }

      Optional<User> firstUserOptional =
          users.stream().filter(user -> !user.getEmail().endsWith("@harness.io")).findFirst();

      if (!firstUserOptional.isPresent()) {
        return false;
      }

      if (entityId.equals(firstUserOptional.get().getUuid())) {
        return true;
      }

      return false;
    }
    return false;
  }

  public void publishSetupIPWhitelistingEvent(String accountId, String whitelistId) {
    String userEmail = checkIfMarketoOrSegmentIsEnabledAndGetUserEmail(EventType.SETUP_IP_WHITELISTING);

    if (isEmpty(userEmail)) {
      return;
    }

    executorService.submit(() -> {
      if (!shouldPublishEventForAccount(accountId)) {
        return;
      }

      if (!isFirstWhitelistConfigInAccount(whitelistId, accountId, userEmail)) {
        return;
      }

      Map<String, String> properties = new HashMap<>();
      properties.put(ACCOUNT_ID, accountId);
      properties.put(EMAIL_ID, userEmail);
      publishEvent(EventType.SETUP_IP_WHITELISTING, properties);
    });
  }

  private boolean isFirstWhitelistConfigInAccount(String whitelistId, String accountId, String userEmail) {
    PageRequest<Whitelist> pageRequest = aPageRequest()
                                             .addFilter("accountId", Operator.EQ, accountId)
                                             .addFilter("createdBy.email", Operator.EQ, userEmail)
                                             .addOrder(Whitelist.CREATED_AT_KEY, OrderType.ASC)
                                             .addFieldsIncluded("_id")
                                             .withLimit("1")
                                             .build();
    PageResponse<Whitelist> pageResponse = whitelistService.list(accountId, pageRequest);
    List<Whitelist> whitelistConfigs = pageResponse.getResponse();
    if (isEmpty(whitelistConfigs)) {
      return false;
    }

    if (whitelistId.equals(whitelistConfigs.get(0).getUuid())) {
      return true;
    }

    return false;
  }

  public void publishSetup2FAEvent(String accountId) {
    String userEmail = checkIfMarketoOrSegmentIsEnabledAndGetUserEmail(EventType.SETUP_2FA);

    if (isEmpty(userEmail)) {
      return;
    }

    executorService.submit(() -> {
      if (!shouldPublishEventForAccount(accountId)) {
        return;
      }

      Map<String, String> properties = new HashMap<>();
      properties.put(ACCOUNT_ID, accountId);
      properties.put(EMAIL_ID, userEmail);
      publishEvent(EventType.SETUP_2FA, properties);
    });
  }

  private void notifyAllUsersOfAccount(String accountId, EventType eventType) {
    Map<String, String> properties = new HashMap<>();
    properties.put(ACCOUNT_ID, accountId);
    publishEvent(eventType, properties);
  }

  public void publishUserInviteFromAccountEvent(String accountId, String email) {
    if (!checkIfMarketoOrSegmentIsEnabled()) {
      return;
    }

    executorService.submit(() -> {
      if (!shouldPublishEventForAccount(accountId)) {
        return;
      }

      Map<String, String> properties = new HashMap<>();
      properties.put(ACCOUNT_ID, accountId);
      properties.put(EMAIL_ID, email);
      publishEvent(EventType.USER_INVITED_FROM_EXISTING_ACCOUNT, properties);
    });
  }

  private void publishEvent(EventType eventType, Map<String, String> properties) {
    eventPublisher.publishEvent(
        Event.builder().eventType(eventType).eventData(EventData.builder().properties(properties).build()).build());
  }

  public void publishInstalledDelegateEvent(String accountId, String delegateId) {
    if (!checkIfMarketoOrSegmentIsEnabled()) {
      return;
    }

    executorService.submit(() -> {
      if (!shouldPublishEventForAccount(accountId)) {
        return;
      }

      if (!isFirstDelegateInAccount(delegateId, accountId)) {
        return;
      }

      Map<String, String> properties = new HashMap<>();
      properties.put(ACCOUNT_ID, accountId);
      publishEvent(EventType.FIRST_DELEGATE_REGISTERED, properties);
    });
  }

  private boolean isFirstDelegateInAccount(String delegateId, String accountId) {
    PageRequest<Delegate> pageRequest = aPageRequest()
                                            .addFilter(DelegateKeys.accountId, Operator.EQ, accountId)
                                            .addOrder(DelegateKeys.createdAt, OrderType.ASC)
                                            .addFieldsIncluded(DelegateKeys.uuid)
                                            .withLimit("1")
                                            .build();
    PageResponse<Delegate> pageResponse = delegateService.list(pageRequest);
    List<Delegate> delegates = pageResponse.getResponse();
    if (isEmpty(delegates)) {
      return false;
    }

    if (delegateId.equals(delegates.get(0).getUuid())) {
      return true;
    }

    return false;
  }

  public void publishWorkflowCreatedEvent(String workflowId, String accountId) {
    String userEmail = checkIfMarketoOrSegmentIsEnabledAndGetUserEmail(EventType.FIRST_WORKFLOW_CREATED);

    if (isEmpty(userEmail)) {
      return;
    }

    executorService.submit(() -> {
      if (!shouldPublishEventForAccount(accountId)) {
        return;
      }

      if (!isFirstWorkflowInAccount(workflowId, accountId, userEmail)) {
        return;
      }

      Map<String, String> properties = new HashMap<>();
      properties.put(ACCOUNT_ID, accountId);
      properties.put(EMAIL_ID, userEmail);
      publishEvent(EventType.FIRST_WORKFLOW_CREATED, properties);
    });
  }

  private boolean isFirstWorkflowInAccount(String workflowId, String accountId, String userEmail) {
    List<String> appIds = appService.getAppIdsByAccountId(accountId);

    PageRequest<Workflow> pageRequest = aPageRequest()
                                            .addFilter("appId", Operator.IN, appIds.toArray())
                                            .addFilter("createdBy.email", Operator.EQ, userEmail)
                                            .addOrder(Workflow.CREATED_AT_KEY, OrderType.ASC)
                                            .addFieldsIncluded("_id")
                                            .withLimit("1")
                                            .build();
    PageResponse<Workflow> pageResponse = workflowService.listWorkflows(pageRequest);
    List<Workflow> workflows = pageResponse.getResponse();
    if (isEmpty(workflows)) {
      return false;
    }

    if (workflowId.equals(workflows.get(0).getUuid())) {
      return true;
    }

    return false;
  }

  public void publishUserRegistrationCompletionEvent(String accountId, User user) {
    if (user == null) {
      return;
    }

    if (!shouldPublishEventForAccount(accountId)) {
      return;
    }

    Map<String, String> properties = new HashMap<>();
    properties.put(ACCOUNT_ID, accountId);
    properties.put(EMAIL_ID, user.getEmail());
    publishEvent(EventType.COMPLETE_USER_REGISTRATION, properties);
  }

  public void publishTrialUserSignupEvent(String email, String userName, String inviteId) {
    if (isEmpty(email)) {
      return;
    }

    publishEvent(EventType.NEW_TRIAL_SIGNUP, getProperties(null, email, userName, inviteId));
  }

  private String checkIfMarketoOrSegmentIsEnabledAndGetUserEmail(EventType eventType) {
    if (!checkIfMarketoOrSegmentIsEnabled()) {
      return null;
    }

    User user = UserThreadLocal.get();
    if (!shouldPublishEventForUser(user)) {
      return null;
    }

    if (isEventAlreadyReportedToUser(user, eventType)) {
      return null;
    }

    return user.getEmail();
  }

  private boolean shouldPublishEventForUser(User user) {
    if (user == null) {
      return false;
    }

    List<Account> accounts = user.getAccounts();
    if (isEmpty(accounts)) {
      return false;
    }

    if (accounts.size() > 1) {
      return false;
    }

    return true;
  }

  private boolean isEventAlreadyReportedToUser(User user, EventType eventType) {
    // only report event if not reported already
    Set<String> reportedMarketoCampaigns = user.getReportedMarketoCampaigns();
    Set<String> reportedSegmentTracks = user.getReportedSegmentTracks();

    if (isEmpty(reportedMarketoCampaigns) || isEmpty(reportedSegmentTracks)) {
      return false;
    }

    return reportedMarketoCampaigns.contains(eventType.name()) && reportedSegmentTracks.contains(eventType.name());
  }

  private boolean shouldPublishEventForAccount(String accountId) {
    Account account = accountService.getFromCache(accountId);

    if (account == null) {
      return false;
    }

    if (account.getLicenseInfo() == null) {
      return false;
    }

    if (!AccountType.TRIAL.equals(account.getLicenseInfo().getAccountType())) {
      return false;
    }

    return true;
  }

  private boolean checkIfMarketoOrSegmentIsEnabled() {
    return marketoConfig.isEnabled() || segmentConfig.isEnabled();
  }

  public boolean isWorkflowRolledBack(String workflowExecutionId, List<String> appIds) {
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .addFilter("appId", SearchFilter.Operator.IN, appIds.toArray())
            .addFilter("executionUuid", SearchFilter.Operator.EQ, workflowExecutionId)
            .addFilter("rollback", SearchFilter.Operator.EQ, true)
            .addFieldsIncluded("_id")
            .addOrder("createdAt", SortOrder.OrderType.ASC)
            .withLimit("1")
            .build();
    PageResponse<StateExecutionInstance> pageResponse = stateExecutionService.list(pageRequest);
    return isNotEmpty(pageResponse.getResponse());
  }

  public boolean publishVerificationWorkflowMetrics(
      String workflowExecutionId, List<String> appIds, String accountId, boolean isVerificationRolledBack) {
    PageRequest<ContinuousVerificationExecutionMetaData> cvPageRequest =
        aPageRequest()
            .addFilter("appId", Operator.IN, appIds.toArray())
            .addFilter("workflowExecutionId", Operator.EQ, workflowExecutionId)
            .addFieldsIncluded("_id")
            .withLimit("1")
            .build();
    List<ContinuousVerificationExecutionMetaData> cvExecutionMetaDataList =
        continuousVerificationService.getCVDeploymentData(cvPageRequest);

    if (!isEmpty(cvExecutionMetaDataList)) {
      Map<String, String> properties = new HashMap<>();
      properties.put("accountId", accountId);
      properties.put("workflowExecutionId", workflowExecutionId);
      properties.put("rollback", String.valueOf(isVerificationRolledBack));
      publishEvent(EventType.DEPLOYMENT_VERIFIED, properties);
      return true;
    }

    return false;
  }

  public void handleDeploymentCompleted(WorkflowExecution workflowExecution) {
    if (workflowExecution == null) {
      return;
    }

    executorService.submit(() -> {
      String appId = workflowExecution.getAppId();
      String workflowExecutionId = workflowExecution.getUuid();

      String accountId = appService.getAccountIdByAppId(appId);

      List<String> appIds = appService.getAppIdsByAccountId(accountId);

      boolean workflowRolledBack = isWorkflowRolledBack(workflowExecutionId, appIds);
      boolean workflowWithVerification =
          publishVerificationWorkflowMetrics(workflowExecutionId, appIds, accountId, workflowRolledBack);

      if (!checkIfMarketoOrSegmentIsEnabled()) {
        return;
      }

      if (!shouldPublishEventForAccount(accountId)) {
        return;
      }

      EmbeddedUser createdBy = workflowExecution.getCreatedBy();

      if (createdBy == null) {
        logger.info("CreatedBy is null for execution id {}", workflowExecutionId);
        return;
      }

      String userEmail = createdBy.getEmail();

      if (isEmpty(userEmail)) {
        logger.info("CreatedBy user email is null for execution id {}", workflowExecutionId);
        return;
      }

      String userId = createdBy.getUuid();
      if (isEmpty(userId)) {
        logger.info("CreatedBy user id is null for execution id {}", workflowExecutionId);
        return;
      }

      User user = userService.getUserFromCacheOrDB(userId);
      if (!shouldPublishEventForUser(user)) {
        logger.info("Skipping publish event for user {} and execution id {}", userEmail, workflowExecutionId);
        return;
      }

      if (!isEventAlreadyReportedToUser(user, EventType.FIRST_DEPLOYMENT_EXECUTED)) {
        publishIfFirstDeployment(workflowExecutionId, appIds, accountId, userEmail);
      }

      if (!isEventAlreadyReportedToUser(user, EventType.FIRST_ROLLED_BACK_DEPLOYMENT) && workflowRolledBack) {
        publishEvent(EventType.FIRST_ROLLED_BACK_DEPLOYMENT, getProperties(accountId, userEmail));
      }

      if (!isEventAlreadyReportedToUser(user, EventType.FIRST_VERIFIED_DEPLOYMENT) && workflowWithVerification) {
        publishEvent(EventType.FIRST_VERIFIED_DEPLOYMENT, getProperties(accountId, userEmail));
      }
    });
  }

  private void publishIfFirstDeployment(
      String workflowExecutionId, List<String> appIds, String accountId, String userEmail) {
    PageRequest<WorkflowExecution> executionPageRequest = aPageRequest()
                                                              .addFilter("appId", Operator.IN, appIds.toArray())
                                                              .addFilter("createdBy.email", Operator.EQ, userEmail)
                                                              .addOrder(WorkflowExecutionKeys.createdAt, OrderType.ASC)
                                                              .withLimit("1")
                                                              .build();

    PageResponse<WorkflowExecution> executionPageResponse =
        workflowExecutionService.listExecutions(executionPageRequest, false);
    List<WorkflowExecution> workflowExecutions = executionPageResponse.getResponse();

    if (isNotEmpty(workflowExecutions)) {
      if (workflowExecutionId.equals(workflowExecutions.get(0).getUuid())) {
        publishEvent(EventType.FIRST_DEPLOYMENT_EXECUTED, getProperties(accountId, userEmail));
      }
    }
  }

  private void publishIfExecutionHasVerificationState(
      String workflowExecutionId, List<String> appIds, String accountId, String userEmail) {
    PageRequest<StateExecutionInstance> pageRequest = PageRequestBuilder.aPageRequest()
                                                          .addFilter("appId", Operator.IN, appIds.toArray())
                                                          .addFilter("executionUuid", Operator.EQ, workflowExecutionId)
                                                          .addFilter("stateType", Operator.IN, analysisStates.toArray())
                                                          .addFilter("status", Operator.EQ, "SUCCESS")
                                                          .addFieldsIncluded("_id")
                                                          .addOrder(StateExecutionInstanceKeys.createdAt, OrderType.ASC)
                                                          .withLimit("1")
                                                          .build();
    PageResponse<StateExecutionInstance> pageResponse = stateExecutionService.list(pageRequest);
    List<StateExecutionInstance> stateExecutionInstances = pageResponse.getResponse();

    if (isNotEmpty(stateExecutionInstances)) {
      publishEvent(EventType.FIRST_VERIFIED_DEPLOYMENT, getProperties(accountId, userEmail));
    }
  }

  private void publishIfExecutionHasRollbackState(
      String workflowExecutionId, List<String> appIds, String accountId, String userEmail) {
    PageRequest<StateExecutionInstance> pageRequest = PageRequestBuilder.aPageRequest()
                                                          .addFilter("appId", Operator.IN, appIds.toArray())
                                                          .addFilter("executionUuid", Operator.EQ, workflowExecutionId)
                                                          .addFilter("rollback", Operator.EQ, true)
                                                          .addFieldsIncluded("_id")
                                                          .addOrder(StateExecutionInstanceKeys.createdAt, OrderType.ASC)
                                                          .withLimit("1")
                                                          .build();
    PageResponse<StateExecutionInstance> pageResponse = stateExecutionService.list(pageRequest);
    List<StateExecutionInstance> stateExecutionInstances = pageResponse.getResponse();

    if (isNotEmpty(stateExecutionInstances)) {
      publishEvent(EventType.FIRST_ROLLED_BACK_DEPLOYMENT, getProperties(accountId, userEmail));
    }
  }

  private Map<String, String> getProperties(String accountId, String userEmail) {
    Map<String, String> properties = new HashMap<>();
    properties.put(ACCOUNT_ID, accountId);
    properties.put(EMAIL_ID, userEmail);
    return properties;
  }

  private Map<String, String> getProperties(String accountId, String userEmail, String userName, String userInviteId) {
    Map<String, String> properties = new HashMap<>();
    properties.put(ACCOUNT_ID, accountId);
    properties.put(EMAIL_ID, userEmail);
    properties.put(USER_NAME, userName);
    properties.put(USER_INVITE_ID, userInviteId);
    return properties;
  }
}
