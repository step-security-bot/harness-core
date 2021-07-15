package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.HarnessStringUtils.nullIfEmpty;
import static io.harness.encryption.ScopeHelper.getScope;
import static io.harness.gitsync.common.YamlConstants.HARNESS_FOLDER_EXTENSION;
import static io.harness.gitsync.common.YamlConstants.PATH_DELIMITER;
import static io.harness.gitsync.common.beans.BranchSyncStatus.SYNCED;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitConfig;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HookEventType;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.gitsync.common.helper.GitSyncConnectorHelper;
import io.harness.gitsync.common.remote.YamlGitConfigMapper;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.ng.webhook.UpsertWebhookRequestDTO;
import io.harness.ng.webhook.UpsertWebhookResponseDTO;
import io.harness.ng.webhook.services.api.WebhookEventService;
import io.harness.repositories.repositories.yamlGitConfig.YamlGitConfigRepository;
import io.harness.utils.IdentifierRefHelper;

import software.wings.utils.CryptoUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@Singleton
@Slf4j
@OwnedBy(DX)
public class YamlGitConfigServiceImpl implements YamlGitConfigService {
  private final YamlGitConfigRepository yamlGitConfigRepository;
  private final ConnectorService connectorService;
  private final Producer gitSyncConfigEventProducer;
  private final ExecutorService executorService;
  private final GitBranchService gitBranchService;
  private final GitSyncConnectorHelper gitSyncConnectorHelper;
  private final WebhookEventService webhookEventService;

  @Inject
  public YamlGitConfigServiceImpl(YamlGitConfigRepository yamlGitConfigRepository,
      @Named("connectorDecoratorService") ConnectorService connectorService,
      @Named(EventsFrameworkConstants.GIT_CONFIG_STREAM) Producer gitSyncConfigEventProducer,
      ExecutorService executorService, GitBranchService gitBranchService, GitSyncConnectorHelper gitSyncConnectorHelper,
      WebhookEventService webhookEventService) {
    this.yamlGitConfigRepository = yamlGitConfigRepository;
    this.connectorService = connectorService;
    this.gitSyncConfigEventProducer = gitSyncConfigEventProducer;
    this.executorService = executorService;
    this.gitBranchService = gitBranchService;
    this.gitSyncConnectorHelper = gitSyncConnectorHelper;
    this.webhookEventService = webhookEventService;
  }

  @Override
  public YamlGitConfigDTO get(String projectIdentifier, String orgIdentifier, String accountId, String identifier) {
    Optional<YamlGitConfig> yamlGitConfig =
        yamlGitConfigRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountId, orgIdentifier, projectIdentifier, identifier);
    return yamlGitConfig.map(YamlGitConfigMapper::toYamlGitConfigDTO)
        .orElseThrow(()
                         -> new InvalidRequestException(
                             getYamlGitConfigNotFoundMessage(accountId, orgIdentifier, projectIdentifier, identifier)));
  }

  @Override
  public YamlGitConfigDTO getByFolderIdentifierAndIsEnabled(
      String projectIdentifier, String orgIdentifier, String accountId, String folderId) {
    // todo @deepak Implement this method when required
    return null;
  }

  private Optional<YamlGitConfig> getYamlGitConfigEntity(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    return yamlGitConfigRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountId, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public List<YamlGitConfigDTO> list(String projectIdentifier, String orgIdentifier, String accountId) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    List<YamlGitConfig> yamlGitConfigs =
        yamlGitConfigRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndScopeOrderByCreatedAtDesc(
            accountId, orgIdentifier, projectIdentifier, scope);
    return emptyIfNull(yamlGitConfigs)
        .stream()
        .map(YamlGitConfigMapper::toYamlGitConfigDTO)
        .collect(Collectors.toList());
  }

  private String findDefaultIfPresent(YamlGitConfigDTO yamlGitConfigDTO) {
    if (yamlGitConfigDTO.getDefaultRootFolder() != null) {
      return yamlGitConfigDTO.getDefaultRootFolder().getIdentifier();
    }
    return null;
  }

  @Override
  public YamlGitConfigDTO updateDefault(
      String projectIdentifier, String orgIdentifier, String accountId, String identifier, String folderPath) {
    Optional<YamlGitConfig> yamlGitConfigOptional =
        getYamlGitConfigEntity(accountId, orgIdentifier, projectIdentifier, identifier);
    if (!yamlGitConfigOptional.isPresent()) {
      throw new InvalidRequestException(
          getYamlGitConfigNotFoundMessage(accountId, orgIdentifier, projectIdentifier, identifier));
    }
    YamlGitConfig yamlGitConfig = yamlGitConfigOptional.get();
    List<YamlGitConfigDTO.RootFolder> rootFolders = yamlGitConfig.getRootFolders();
    YamlGitConfigDTO.RootFolder newDefaultRootFolder = null;
    for (YamlGitConfigDTO.RootFolder folder : rootFolders) {
      if (folder.getRootFolder().equals(folderPath)) {
        newDefaultRootFolder = folder;
      }
    }
    if (newDefaultRootFolder == null) {
      throw new InvalidRequestException("No folder exists with the path " + folderPath);
    }
    yamlGitConfig.setDefaultRootFolder(newDefaultRootFolder);
    YamlGitConfig updatedYamlGitConfig = yamlGitConfigRepository.save(yamlGitConfig);
    return YamlGitConfigMapper.toYamlGitConfigDTO(updatedYamlGitConfig);
  }

  @Override
  public List<YamlGitConfigDTO> getByConnectorRepoAndBranch(
      String gitConnectorId, String repo, String branchName, String accountId) {
    List<YamlGitConfig> yamlGitConfigs = yamlGitConfigRepository.findByGitConnectorRefAndRepoAndBranchAndAccountId(
        gitConnectorId, repo, branchName, accountId);
    return emptyIfNull(yamlGitConfigs)
        .stream()
        .map(YamlGitConfigMapper::toYamlGitConfigDTO)
        .collect(Collectors.toList());
  }

  @Override
  public YamlGitConfigDTO save(YamlGitConfigDTO ygs) {
    return save(ygs, ygs.getAccountIdentifier(), true);
  }

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public YamlGitConfigDTO update(YamlGitConfigDTO gitSyncConfig) {
    validatePresenceOfRequiredFields(gitSyncConfig.getAccountIdentifier(), gitSyncConfig.getIdentifier());
    return updateInternal(gitSyncConfig);
  }

  private YamlGitConfigDTO updateInternal(YamlGitConfigDTO gitSyncConfigDTO) {
    validateTheGitConfigInput(gitSyncConfigDTO);
    Optional<YamlGitConfig> existingYamlGitConfigDTO =
        yamlGitConfigRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            gitSyncConfigDTO.getAccountIdentifier(), gitSyncConfigDTO.getOrganizationIdentifier(),
            gitSyncConfigDTO.getProjectIdentifier(), gitSyncConfigDTO.getIdentifier());
    if (!existingYamlGitConfigDTO.isPresent()) {
      throw new InvalidRequestException(getYamlGitConfigNotFoundMessage(gitSyncConfigDTO.getAccountIdentifier(),
          gitSyncConfigDTO.getOrganizationIdentifier(), gitSyncConfigDTO.getProjectIdentifier(),
          gitSyncConfigDTO.getIdentifier()));
    }
    validateThatImmutableValuesAreNotChanged(gitSyncConfigDTO, existingYamlGitConfigDTO.get());
    YamlGitConfig yamlGitConfigToBeSaved = toYamlGitConfig(gitSyncConfigDTO, gitSyncConfigDTO.getAccountIdentifier());
    yamlGitConfigToBeSaved.setWebhookToken(existingYamlGitConfigDTO.get().getWebhookToken());
    yamlGitConfigToBeSaved.setUuid(existingYamlGitConfigDTO.get().getUuid());
    yamlGitConfigToBeSaved.setVersion(existingYamlGitConfigDTO.get().getVersion());
    YamlGitConfig yamlGitConfig = yamlGitConfigRepository.save(yamlGitConfigToBeSaved);
    return YamlGitConfigMapper.toYamlGitConfigDTO(yamlGitConfig);
  }

  private void validateThatImmutableValuesAreNotChanged(
      YamlGitConfigDTO gitSyncConfigDTO, YamlGitConfig existingYamlGitConfigDTO) {
    if (!gitSyncConfigDTO.getRepo().equals(existingYamlGitConfigDTO.getRepo())) {
      throw new InvalidRequestException("The repo url of an git config cannot be changed");
    }
    if (!gitSyncConfigDTO.getBranch().equals(existingYamlGitConfigDTO.getBranch())) {
      throw new InvalidRequestException("The default branch of an git config cannot be changed");
    }
  }

  private String getYamlGitConfigNotFoundMessage(
      String accountId, String organizationId, String projectId, String identifier) {
    return String.format("No git sync config exists with the id %s, in account %s, org %s, project %s", identifier,
        accountId, organizationId, projectId);
  }

  public YamlGitConfigDTO save(YamlGitConfigDTO ygs, String accountId, boolean performFullSync) {
    // TODO(abhinav): add full sync logic.
    return saveInternal(ygs, accountId);
  }

  private YamlGitConfigDTO saveInternal(YamlGitConfigDTO gitSyncConfigDTO, String accountId) {
    validateTheGitConfigInput(gitSyncConfigDTO);
    YamlGitConfig yamlGitConfigToBeSaved = toYamlGitConfig(gitSyncConfigDTO, accountId);
    yamlGitConfigToBeSaved.setWebhookToken(CryptoUtils.secureRandAlphaNumString(40));
    registerWebhookAsync(gitSyncConfigDTO);
    YamlGitConfig savedYamlGitConfig = null;
    try {
      savedYamlGitConfig = yamlGitConfigRepository.save(yamlGitConfigToBeSaved);
    } catch (DuplicateKeyException ex) {
      throw new InvalidRequestException(
          String.format("A git sync config with this identifier or repo %s and branch %s already exists",
              gitSyncConfigDTO.getRepo(), gitSyncConfigDTO.getBranch()));
    }
    sendEventForConfigChange(accountId, yamlGitConfigToBeSaved.getOrgIdentifier(),
        yamlGitConfigToBeSaved.getProjectIdentifier(), yamlGitConfigToBeSaved.getIdentifier(), "Save");
    executorService.submit(() -> {
      gitBranchService.createBranches(accountId, gitSyncConfigDTO.getOrganizationIdentifier(),
          gitSyncConfigDTO.getProjectIdentifier(), gitSyncConfigDTO.getGitConnectorRef(), gitSyncConfigDTO.getRepo(),
          gitSyncConfigDTO.getIdentifier());
      gitBranchService.updateBranchSyncStatus(
          accountId, gitSyncConfigDTO.getRepo(), gitSyncConfigDTO.getBranch(), SYNCED);
    });

    return YamlGitConfigMapper.toYamlGitConfigDTO(savedYamlGitConfig);
  }

  private void registerWebhookAsync(YamlGitConfigDTO gitSyncConfigDTO) {
    executorService.submit(() -> saveWebhook(gitSyncConfigDTO));
  }

  private void saveWebhook(YamlGitConfigDTO gitSyncConfigDTO) {
    if (isNewRepoInProject(gitSyncConfigDTO)) {
      UpsertWebhookResponseDTO upsertWebhookResponseDTO = registerWebhook(gitSyncConfigDTO);
      log.info("Response of Upsert Webhook {}", upsertWebhookResponseDTO);
    }
  }

  private UpsertWebhookResponseDTO registerWebhook(YamlGitConfigDTO gitSyncConfigDTO) {
    UpsertWebhookRequestDTO upsertWebhookRequest = getUpsertWebhookRequest(gitSyncConfigDTO);
    return webhookEventService.upsertWebhook(upsertWebhookRequest);
  }

  private UpsertWebhookRequestDTO getUpsertWebhookRequest(YamlGitConfigDTO gitSyncConfigDTO) {
    return UpsertWebhookRequestDTO.builder()
        .accountIdentifier(gitSyncConfigDTO.getAccountIdentifier())
        .orgIdentifier(gitSyncConfigDTO.getOrganizationIdentifier())
        .projectIdentifier(gitSyncConfigDTO.getProjectIdentifier())
        .connectorIdentifierRef(gitSyncConfigDTO.getGitConnectorRef())
        .hookEventType(HookEventType.TRIGGER_EVENTS)
        .repoURL(gitSyncConfigDTO.getRepo())
        .build();
  }

  private boolean isNewRepoInProject(YamlGitConfigDTO gitSyncConfigDTO) {
    final Optional<YamlGitConfig> yamlGitConfig =
        yamlGitConfigRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            gitSyncConfigDTO.getAccountIdentifier(), gitSyncConfigDTO.getOrganizationIdentifier(),
            gitSyncConfigDTO.getProjectIdentifier(), gitSyncConfigDTO.getIdentifier());
    return !yamlGitConfig.isPresent();
  }

  private void sendEventForConfigChange(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, String eventType) {
    EntityScopeInfo entityScopeInfo = EntityScopeInfo.newBuilder()
                                          .setOrgId(StringValue.newBuilder().setValue(orgIdentifier).build())
                                          .setProjectId(StringValue.newBuilder().setValue(projectIdentifier).build())
                                          .setAccountId(accountId)
                                          .build();
    try {
      gitSyncConfigEventProducer.send(Message.newBuilder()
                                          .putAllMetadata(ImmutableMap.of("accountId", accountId))
                                          .setData(entityScopeInfo.toByteString())
                                          .build());
    } catch (Exception e) {
      log.error("Event to send git config update failed for accountId {} during {} event for yamlgitconfig [{}]",
          accountId, eventType, identifier, e);
    }
  }

  private void validateTheGitConfigInput(YamlGitConfigDTO ygs) {
    ensureFolderEndsWithDelimiter(ygs);
    validateFolderFollowsHarnessParadigm(ygs);
    validateFolderPathIsUnique(ygs);
    validateFoldersAreIndependant(ygs);
    validateAPIAccessFieldPresence(ygs);
    validateThatHarnessStringComesOnce(ygs);
  }

  @VisibleForTesting
  void validateThatHarnessStringComesOnce(YamlGitConfigDTO ygs) {
    if (ygs.getRootFolders() == null) {
      return;
    }
    for (YamlGitConfigDTO.RootFolder folder : ygs.getRootFolders()) {
      int harnessStringCount = getHarnessStringCount(folder.getRootFolder());
      if (harnessStringCount > 1) {
        throw new InvalidRequestException("The .harness should come only once in the folder path");
      }
    }
  }

  private int getHarnessStringCount(String folderPath) {
    return folderPath.split(".harness", -1).length - 1;
  }

  private void validateAPIAccessFieldPresence(YamlGitConfigDTO ygs) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(ygs.getGitConnectorRef(),
        ygs.getAccountIdentifier(), ygs.getOrganizationIdentifier(), ygs.getProjectIdentifier());
    Optional<ConnectorInfoDTO> gitConnectorOptional = getGitConnector(identifierRef);
    if (gitConnectorOptional.isPresent()) {
      ConnectorConfigDTO connectorConfig = gitConnectorOptional.get().getConnectorConfig();
      if (connectorConfig instanceof ScmConnector) {
        gitSyncConnectorHelper.validateTheAPIAccessPresence((ScmConnector) connectorConfig);
      } else {
        throw new InvalidRequestException(
            String.format("The connector reference %s is not a git connector", ygs.getGitConnectorRef()));
      }
    } else {
      throw new InvalidRequestException(
          String.format("No connector found for the connector reference %s", ygs.getGitConnectorRef()));
    }
  }

  @Override
  public Optional<ConnectorInfoDTO> getGitConnector(IdentifierRef identifierRef) {
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    return connectorDTO.map(ConnectorResponseDTO::getConnector);
  }

  /**
   * Checks that one folder should not be contained within other
   * for eg portal/.harness, portal/.harness/ci/.harness is an
   * invalid folder pair.
   *
   */
  private void validateFoldersAreIndependant(YamlGitConfigDTO ygs) {
    final List<YamlGitConfigDTO.RootFolder> rootFolders = ygs.getRootFolders();
    for (int i = 0; i < rootFolders.size(); i++) {
      for (int j = 0; j < rootFolders.size() && j != i; j++) {
        String firstFolder = rootFolders.get(i).getRootFolder();
        String secondFolder = rootFolders.get(j).getRootFolder();
        if (firstFolder.startsWith(secondFolder)) {
          throw new InvalidRequestException(
              String.format("The folder %s is already contained in %s", firstFolder, secondFolder));
        }
      }
    }
  }

  private void validateFolderPathIsUnique(YamlGitConfigDTO ygs) {
    if (ygs.getRootFolders() == null) {
      return;
    }
    Set<String> folderPaths = new HashSet();
    for (YamlGitConfigDTO.RootFolder folder : ygs.getRootFolders()) {
      if (folderPaths.contains(folder.getRootFolder())) {
        throw new DuplicateFieldException(
            String.format("A folder with name %s already exists in the list", folder.getRootFolder()));
      }
      folderPaths.add(folder.getRootFolder());
    }
  }

  private void ensureFolderEndsWithDelimiter(YamlGitConfigDTO ygs) {
    if (ygs.getRootFolders() == null) {
      return;
    }
    final Optional<YamlGitConfigDTO.RootFolder> rootFolder =
        ygs.getRootFolders().stream().filter(config -> !config.getRootFolder().endsWith(PATH_DELIMITER)).findFirst();
    if (rootFolder.isPresent()) {
      throw new InvalidRequestException("The folder should end with /");
    }
  }

  private void validateFolderFollowsHarnessParadigm(YamlGitConfigDTO ygs) {
    if (ygs.getRootFolders() == null) {
      return;
    }
    final Optional<YamlGitConfigDTO.RootFolder> rootFolder =
        ygs.getRootFolders()
            .stream()
            .filter(
                config -> !config.getRootFolder().endsWith(PATH_DELIMITER + HARNESS_FOLDER_EXTENSION + PATH_DELIMITER))
            .findFirst();
    if (rootFolder.isPresent()) {
      throw new InvalidRequestException("The folder should end with /.harness/");
    }
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    boolean deleted =
        yamlGitConfigRepository.deleteByAccountIdAndOrgIdentifierAndProjectIdentifierAndScopeAndIdentifier(
            accountId, orgIdentifier, projectIdentifier, scope, identifier)
        != 0;
    if (deleted) {
      sendEventForConfigChange(accountId, orgIdentifier, projectIdentifier, identifier, "delete");
    }
    return deleted;
  }

  @Override
  public Boolean isGitSyncEnabled(String accountIdentifier, String organizationIdentifier, String projectIdentifier) {
    return yamlGitConfigRepository.existsByAccountIdAndOrgIdentifierAndProjectIdentifier(
        accountIdentifier, nullIfEmpty(organizationIdentifier), nullIfEmpty(projectIdentifier));
  }

  @Override
  public Boolean isRepoExists(String repo) {
    return yamlGitConfigRepository.existsByRepo(repo);
  }

  @Override
  public List<YamlGitConfigDTO> getByRepo(String repo) {
    List<YamlGitConfigDTO> yamlGitConfigDTOs = new ArrayList<>();

    List<YamlGitConfig> yamlGitConfigs = yamlGitConfigRepository.findByRepoOrderByCreatedAtDesc(repo);
    yamlGitConfigs.forEach(
        yamlGitConfig -> yamlGitConfigDTOs.add(YamlGitConfigMapper.toYamlGitConfigDTO(yamlGitConfig)));
    return yamlGitConfigDTOs;
  }
}
