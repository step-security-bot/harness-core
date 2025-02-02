/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitxwebhooks.service;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.HookEventType;
import io.harness.beans.Scope;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InternalServerErrorException;
import io.harness.exception.ScmException;
import io.harness.gitsync.caching.beans.GitFileCacheKey;
import io.harness.gitsync.caching.service.GitFileCacheService;
import io.harness.gitsync.common.helper.GitRepoHelper;
import io.harness.gitsync.common.service.GitSyncConnectorService;
import io.harness.gitsync.gitxwebhooks.dtos.CreateGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.CreateGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.DeleteGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.DeleteGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GetGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GetGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.ListGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.ListGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.UpdateGitXWebhookCriteriaDTO;
import io.harness.gitsync.gitxwebhooks.dtos.UpdateGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.UpdateGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.entity.GitXWebhook;
import io.harness.gitsync.gitxwebhooks.entity.GitXWebhook.GitXWebhookKeys;
import io.harness.gitsync.gitxwebhooks.loggers.GitXWebhookLogContext;
import io.harness.ng.webhook.UpsertWebhookRequestDTO;
import io.harness.ng.webhook.UpsertWebhookResponseDTO;
import io.harness.ng.webhook.services.api.WebhookEventService;
import io.harness.repositories.gitxwebhook.GitXWebhookRepository;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_GITX})
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class GitXWebhookServiceImpl implements GitXWebhookService {
  @Inject GitXWebhookRepository gitXWebhookRepository;
  @Inject GitRepoHelper gitRepoHelper;
  @Inject GitSyncConnectorService gitSyncConnectorService;
  @Inject WebhookEventService webhookEventService;
  @Inject GitFileCacheService gitFileCacheService;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "GitX Webhook with identifier [%s] or repo [%s] already exists in the account [%s].";

  private static final String WEBHOOK_FAILURE_ERROR_MESSAGE =
      "Unexpected error occurred while [%s] git webhook. Please contact Harness Support.";

  private static final String CREATING = "creating";
  private static final String FETCHING = "fetching";
  private static final String UPDATING = "updating";
  private static final String DELETING = "deleting";
  private static final String LISTING_WEBHOOKS = "listing webhooks";

  @Override
  public CreateGitXWebhookResponseDTO createGitXWebhook(CreateGitXWebhookRequestDTO createGitXWebhookRequestDTO) {
    try (GitXWebhookLogContext context = new GitXWebhookLogContext(createGitXWebhookRequestDTO)) {
      try {
        clearCache(
            createGitXWebhookRequestDTO.getScope().getAccountIdentifier(), createGitXWebhookRequestDTO.getRepoName());
        GitXWebhook gitXWebhook = buildGitXWebhooks(createGitXWebhookRequestDTO);
        log.info(String.format("Creating Webhook with identifier %s in account %s",
            createGitXWebhookRequestDTO.getWebhookIdentifier(),
            createGitXWebhookRequestDTO.getScope().getAccountIdentifier()));
        registerWebhookOnGit(createGitXWebhookRequestDTO.getScope(), gitXWebhook.getRepoName(),
            gitXWebhook.getConnectorRef(), gitXWebhook.getIdentifier());
        GitXWebhook createdGitXWebhook = gitXWebhookRepository.create(gitXWebhook);
        if (createdGitXWebhook == null) {
          log.error(String.format("Error while saving webhook [%s] in DB", gitXWebhook.getIdentifier()));
          throw new InternalServerErrorException(
              String.format("Error while saving webhook [%s] in DB", gitXWebhook.getIdentifier()));
        }
        return CreateGitXWebhookResponseDTO.builder().webhookIdentifier(createdGitXWebhook.getIdentifier()).build();
      } catch (DuplicateKeyException ex) {
        log.error(format(DUP_KEY_EXP_FORMAT_STRING, createGitXWebhookRequestDTO.getWebhookIdentifier(),
                      createGitXWebhookRequestDTO.getRepoName(),
                      createGitXWebhookRequestDTO.getScope().getAccountIdentifier()),
            USER_SRE);
        throw new DuplicateFieldException(
            format(DUP_KEY_EXP_FORMAT_STRING, createGitXWebhookRequestDTO.getWebhookIdentifier(),
                createGitXWebhookRequestDTO.getRepoName(),
                createGitXWebhookRequestDTO.getScope().getAccountIdentifier()),
            USER_SRE, ex);
      } catch (Exception exception) {
        log.error(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, CREATING), exception);
        throw new InternalServerErrorException(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, CREATING));
      }
    }
  }

  @Override
  public Optional<GetGitXWebhookResponseDTO> getGitXWebhook(GetGitXWebhookRequestDTO getGitXWebhookRequestDTO) {
    try (GitXWebhookLogContext context = new GitXWebhookLogContext(getGitXWebhookRequestDTO)) {
      try {
        log.info(String.format("Retrieving Webhook with identifier %s in account %s.",
            getGitXWebhookRequestDTO.getWebhookIdentifier(),
            getGitXWebhookRequestDTO.getScope().getAccountIdentifier()));
        Criteria criteria =
            buildCriteria(getGitXWebhookRequestDTO.getScope(), getGitXWebhookRequestDTO.getWebhookIdentifier());
        GitXWebhook gitXWebhook = gitXWebhookRepository.find(new Query(criteria));
        return Optional.of(prepareGitXWebhooks(gitXWebhook));
      } catch (Exception exception) {
        log.error(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, FETCHING), exception);
        throw new InternalServerErrorException(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, FETCHING));
      }
    }
  }

  @Override
  public List<GitXWebhook> getGitXWebhook(String accountIdentifier, String repoName) {
    return gitXWebhookRepository.findByAccountIdentifierAndRepoName(accountIdentifier, repoName);
  }

  @Override
  public List<GitXWebhook> getGitXWebhookForAllScopes(Scope scope, String repoName) {
    Criteria criteria = Criteria.where(GitXWebhookKeys.accountIdentifier)
                            .is(scope.getAccountIdentifier())
                            .and(GitXWebhookKeys.orgIdentifier)
                            .in(null, scope.getOrgIdentifier())
                            .and(GitXWebhookKeys.projectIdentifier)
                            .in(null, scope.getProjectIdentifier())
                            .and(GitXWebhookKeys.repoName)
                            .is(repoName);
    return gitXWebhookRepository.findAll(new Query(criteria));
  }

  @Override
  public Optional<GitXWebhook> getGitXWebhookForGivenScopes(Scope scope, String repoName) {
    List<GitXWebhook> gitXWebhookList =
        gitXWebhookRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndRepoName(
            scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier(), repoName);
    if (isEmpty(gitXWebhookList)) {
      return Optional.empty();
    }
    return Optional.of(gitXWebhookList.get(0));
  }

  @Override
  public UpdateGitXWebhookResponseDTO updateGitXWebhook(UpdateGitXWebhookCriteriaDTO updateGitXWebhookCriteriaDTO,
      UpdateGitXWebhookRequestDTO updateGitXWebhookRequestDTO) {
    try (GitXWebhookLogContext context =
             new GitXWebhookLogContext(updateGitXWebhookCriteriaDTO, updateGitXWebhookRequestDTO)) {
      try {
        log.info(String.format("Updating Webhook with identifier %s in account %s",
            updateGitXWebhookCriteriaDTO.getWebhookIdentifier(),
            updateGitXWebhookCriteriaDTO.getScope().getAccountIdentifier()));
        clearCache(
            updateGitXWebhookCriteriaDTO.getScope().getAccountIdentifier(), updateGitXWebhookRequestDTO.getRepoName());
        Criteria criteria =
            buildCriteria(updateGitXWebhookCriteriaDTO.getScope(), updateGitXWebhookCriteriaDTO.getWebhookIdentifier());
        Query query = new Query(criteria);
        Update update = buildUpdate(updateGitXWebhookRequestDTO);
        if (isNotEmpty(updateGitXWebhookRequestDTO.getRepoName())) {
          String connectorRef = getConnectorRef(updateGitXWebhookCriteriaDTO, updateGitXWebhookRequestDTO);
          registerWebhookOnGit(updateGitXWebhookCriteriaDTO.getScope(), updateGitXWebhookRequestDTO.getRepoName(),
              connectorRef, updateGitXWebhookCriteriaDTO.getWebhookIdentifier());
        }
        GitXWebhook updatedGitXWebhook;
        updatedGitXWebhook = gitXWebhookRepository.update(query, update);
        if (updatedGitXWebhook == null) {
          log.error(String.format(
              "Error while updating webhook [%s] in DB", updateGitXWebhookCriteriaDTO.getWebhookIdentifier()));
          throw new InternalServerErrorException(String.format(
              "Error while updating webhook [%s] in DB", updateGitXWebhookCriteriaDTO.getWebhookIdentifier()));
        }
        return UpdateGitXWebhookResponseDTO.builder().webhookIdentifier(updatedGitXWebhook.getIdentifier()).build();
      } catch (InternalServerErrorException exception) {
        log.error("Unexpected error occurred while updating the GitX webhook {}",
            updateGitXWebhookCriteriaDTO.getWebhookIdentifier(), exception);
        throw exception;
      } catch (Exception exception) {
        log.error(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, UPDATING), exception);
        throw new InternalServerErrorException(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, UPDATING));
      }
    }
  }

  @Override
  public ListGitXWebhookResponseDTO listGitXWebhooks(ListGitXWebhookRequestDTO listGitXWebhookRequestDTO) {
    try (GitXWebhookLogContext context = new GitXWebhookLogContext(listGitXWebhookRequestDTO)) {
      try {
        log.info(String.format(
            "Get List of GitX Webhooks in account %s", listGitXWebhookRequestDTO.getScope().getAccountIdentifier()));
        Criteria criteria = buildListCriteria(listGitXWebhookRequestDTO);
        List<GitXWebhook> gitXWebhookList = gitXWebhookRepository.list(criteria);
        return ListGitXWebhookResponseDTO.builder().gitXWebhooksList(prepareGitXWebhooks(gitXWebhookList)).build();
      } catch (Exception exception) {
        log.error(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, LISTING_WEBHOOKS), exception);
        throw new InternalServerErrorException(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, LISTING_WEBHOOKS));
      }
    }
  }

  @Override
  public DeleteGitXWebhookResponseDTO deleteGitXWebhook(DeleteGitXWebhookRequestDTO deleteGitXWebhookRequestDTO) {
    try (GitXWebhookLogContext context = new GitXWebhookLogContext(deleteGitXWebhookRequestDTO)) {
      try {
        log.info(String.format("Deleting Webhook with identifier %s in account %s",
            deleteGitXWebhookRequestDTO.getWebhookIdentifier(),
            deleteGitXWebhookRequestDTO.getScope().getAccountIdentifier()));
        Criteria criteria =
            buildCriteria(deleteGitXWebhookRequestDTO.getScope(), deleteGitXWebhookRequestDTO.getWebhookIdentifier());
        DeleteResult deleteResult = gitXWebhookRepository.delete(criteria);
        return DeleteGitXWebhookResponseDTO.builder().successfullyDeleted(deleteResult.getDeletedCount() == 1).build();
      } catch (Exception exception) {
        log.error(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, DELETING), exception);
        throw new InternalServerErrorException(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, DELETING));
      }
    }
  }

  private String getConnectorRef(UpdateGitXWebhookCriteriaDTO updateGitXWebhookCriteriaDTO,
      UpdateGitXWebhookRequestDTO updateGitXWebhookRequestDTO) {
    if (isEmpty(updateGitXWebhookRequestDTO.getConnectorRef())) {
      Optional<GetGitXWebhookResponseDTO> optionalGetGitXWebhookResponse =
          getGitXWebhook(GetGitXWebhookRequestDTO.builder()
                             .scope(updateGitXWebhookCriteriaDTO.getScope())
                             .webhookIdentifier(updateGitXWebhookCriteriaDTO.getWebhookIdentifier())
                             .build());
      if (optionalGetGitXWebhookResponse.isEmpty()) {
        throw new InternalServerErrorException(
            String.format("Failed to fetch the connectorRef for webhook with identifier %s in account %s",
                updateGitXWebhookCriteriaDTO.getWebhookIdentifier(),
                updateGitXWebhookCriteriaDTO.getScope().getAccountIdentifier()));
      }
      return optionalGetGitXWebhookResponse.get().getConnectorRef();
    } else {
      return updateGitXWebhookRequestDTO.getConnectorRef();
    }
  }

  private List<GetGitXWebhookResponseDTO> prepareGitXWebhooks(List<GitXWebhook> gitXWebhookList) {
    return emptyIfNull(gitXWebhookList)
        .stream()
        .map(gitXWebhookResponseDTO
            -> GetGitXWebhookResponseDTO.builder()
                   .accountIdentifier(gitXWebhookResponseDTO.getAccountIdentifier())
                   .webhookIdentifier(gitXWebhookResponseDTO.getIdentifier())
                   .webhookName(gitXWebhookResponseDTO.getName())
                   .connectorRef(gitXWebhookResponseDTO.getConnectorRef())
                   .folderPaths(gitXWebhookResponseDTO.getFolderPaths())
                   .isEnabled(gitXWebhookResponseDTO.getIsEnabled())
                   .repoName(gitXWebhookResponseDTO.getRepoName())
                   .eventTriggerTime(gitXWebhookResponseDTO.getLastEventTriggerTime())
                   .build())
        .collect(Collectors.toList());
  }

  private GetGitXWebhookResponseDTO prepareGitXWebhooks(GitXWebhook gitXWebhook) {
    return GetGitXWebhookResponseDTO.builder()
        .accountIdentifier(gitXWebhook.getAccountIdentifier())
        .webhookIdentifier(gitXWebhook.getIdentifier())
        .webhookName(gitXWebhook.getName())
        .connectorRef(gitXWebhook.getConnectorRef())
        .folderPaths(gitXWebhook.getFolderPaths())
        .isEnabled(gitXWebhook.getIsEnabled())
        .repoName(gitXWebhook.getRepoName())
        .eventTriggerTime(gitXWebhook.getLastEventTriggerTime())
        .build();
  }

  private Criteria buildCriteria(Scope scope, String webhookIdentifier) {
    return Criteria.where(GitXWebhookKeys.accountIdentifier)
        .is(scope.getAccountIdentifier())
        .and(GitXWebhookKeys.orgIdentifier)
        .is(scope.getOrgIdentifier())
        .and(GitXWebhookKeys.projectIdentifier)
        .is(scope.getProjectIdentifier())
        .and(GitXWebhookKeys.identifier)
        .is(webhookIdentifier);
  }

  private Criteria buildListCriteria(ListGitXWebhookRequestDTO listGitXWebhookRequestDTO) {
    Criteria criteria = new Criteria();
    criteria.and(GitXWebhookKeys.accountIdentifier).is(listGitXWebhookRequestDTO.getScope().getAccountIdentifier());
    criteria.and(GitXWebhookKeys.orgIdentifier).is(listGitXWebhookRequestDTO.getScope().getOrgIdentifier());
    criteria.and(GitXWebhookKeys.projectIdentifier).is(listGitXWebhookRequestDTO.getScope().getProjectIdentifier());
    if (isNotEmpty(listGitXWebhookRequestDTO.getWebhookIdentifier())) {
      criteria.and(GitXWebhookKeys.identifier).is(listGitXWebhookRequestDTO.getWebhookIdentifier());
    }
    return criteria;
  }

  private Update buildUpdate(UpdateGitXWebhookRequestDTO updateGitXWebhookRequestDTO) {
    long currentTimeInMilliseconds = System.currentTimeMillis();
    Update update = new Update();
    update.set(GitXWebhookKeys.folderPaths, updateGitXWebhookRequestDTO.getFolderPaths());
    if (isNotEmpty(updateGitXWebhookRequestDTO.getRepoName())) {
      update.set(GitXWebhookKeys.repoName, updateGitXWebhookRequestDTO.getRepoName());
    }
    if (isNotEmpty(updateGitXWebhookRequestDTO.getConnectorRef())) {
      update.set(GitXWebhookKeys.connectorRef, updateGitXWebhookRequestDTO.getConnectorRef());
    }
    if (isNotEmpty(updateGitXWebhookRequestDTO.getWebhookName())) {
      update.set(GitXWebhookKeys.name, updateGitXWebhookRequestDTO.getWebhookName());
    }
    if (updateGitXWebhookRequestDTO.getIsEnabled() != null) {
      update.set(GitXWebhookKeys.isEnabled, Boolean.TRUE.equals(updateGitXWebhookRequestDTO.getIsEnabled()));
    }
    if (updateGitXWebhookRequestDTO.getLastEventTriggerTime() != null) {
      update.set(GitXWebhookKeys.lastEventTriggerTime, updateGitXWebhookRequestDTO.getLastEventTriggerTime());
    }
    update.set(GitXWebhookKeys.lastUpdatedAt, currentTimeInMilliseconds);
    return update;
  }

  private GitXWebhook buildGitXWebhooks(CreateGitXWebhookRequestDTO createGitXWebhookRequestDTO) {
    return GitXWebhook.builder()
        .accountIdentifier(createGitXWebhookRequestDTO.getScope().getAccountIdentifier())
        .orgIdentifier(createGitXWebhookRequestDTO.getScope().getOrgIdentifier())
        .projectIdentifier(createGitXWebhookRequestDTO.getScope().getProjectIdentifier())
        .identifier(createGitXWebhookRequestDTO.getWebhookIdentifier())
        .name(createGitXWebhookRequestDTO.getWebhookName())
        .connectorRef(createGitXWebhookRequestDTO.getConnectorRef())
        .folderPaths(createGitXWebhookRequestDTO.getFolderPaths())
        .repoName(createGitXWebhookRequestDTO.getRepoName())
        .isEnabled(true)
        .build();
  }

  private void registerWebhookOnGit(Scope scope, String repoName, String connectorRef, String webhookIdentifier) {
    UpsertWebhookRequestDTO upsertWebhookRequestDTO = buildUpsertWebhookRequestDTO(scope, repoName, connectorRef);
    try {
      final RetryPolicy<Object> retryPolicy = getWebhookRegistrationRetryPolicy(
          "[Retrying] attempt: {} for failure case of save webhook call", "Failed to save webhook after {} attempts");
      Failsafe.with(retryPolicy).get(() -> registerWebhook(upsertWebhookRequestDTO));
    } catch (ExplanationException | HintException | ScmException e) {
      log.error("Error while creating webhook on git." + webhookIdentifier, e);
      throw e;
    }
    log.info(String.format("Successfully created the webhook with identifier %s on git", webhookIdentifier));
  }

  private UpsertWebhookResponseDTO registerWebhook(UpsertWebhookRequestDTO upsertWebhookRequestDTO) {
    UpsertWebhookResponseDTO upsertWebhookResponseDTO = webhookEventService.upsertWebhook(upsertWebhookRequestDTO);
    log.info(String.format(
        "Successfully registered webhook %s on git.", upsertWebhookResponseDTO.getWebhookResponse().getId()));
    return upsertWebhookResponseDTO;
  }

  private RetryPolicy<Object> getWebhookRegistrationRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withMaxAttempts(2)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }

  private UpsertWebhookRequestDTO buildUpsertWebhookRequestDTO(Scope scope, String repoName, String connectorRef) {
    ScmConnector scmConnector = gitSyncConnectorService.getScmConnector(scope, connectorRef);
    String repoUrl = gitRepoHelper.getRepoUrl(scmConnector, repoName);
    return UpsertWebhookRequestDTO.builder()
        .accountIdentifier(scope.getAccountIdentifier())
        .orgIdentifier(scope.getOrgIdentifier())
        .projectIdentifier(scope.getProjectIdentifier())
        .connectorIdentifierRef(connectorRef)
        .hookEventType(HookEventType.TRIGGER_EVENTS)
        .repoURL(repoUrl)
        .build();
  }

  private void clearCache(String accountIdentifier, String repoName) {
    GitFileCacheKey cacheKey =
        GitFileCacheKey.builder().accountIdentifier(accountIdentifier).repoName(repoName).build();
    try {
      gitFileCacheService.invalidateCache(cacheKey);
    } catch (Exception ex) {
      log.error("Exception occurred while clearing the cache for key {}", cacheKey, ex);
      throw new InternalServerErrorException(
          String.format("Error occurred while clearing the cache files in repo %s", repoName), ex);
    }
  }
}
