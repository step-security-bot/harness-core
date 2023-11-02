/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitxwebhooks.runnable;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.gitsync.common.beans.GitXWebhookEventStatus;
import io.harness.gitsync.common.dtos.ScmGetBatchFileRequestIdentifier;
import io.harness.gitsync.common.dtos.ScmGetFileByBranchRequestDTO;
import io.harness.gitsync.common.dtos.ScmUpdateGitCacheRequestDTO;
import io.harness.gitsync.common.service.ScmFacilitatorService;
import io.harness.gitsync.gitxwebhooks.dtos.GitXCacheUpdateRunnableRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GitXEventUpdateRequestDTO;
import io.harness.gitsync.gitxwebhooks.helper.GitXWebhookTriggerHelper;
import io.harness.gitsync.gitxwebhooks.loggers.GitXWebhookCacheUpdateLogContext;
import io.harness.gitsync.gitxwebhooks.service.GitXWebhookEventService;
import io.harness.logging.ResponseTimeRecorder;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_GITX})
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class GitXWebhookCacheUpdateRunnable implements Runnable {
  @Inject private ScmFacilitatorService scmFacilitatorService;
  @Inject private GitXWebhookEventService gitXWebhookEventService;
  @Inject private GitXWebhookTriggerHelper gitXWebhookTriggerHelper;
  private GitXCacheUpdateRunnableRequestDTO gitXCacheUpdateRunnableRequestDTO;
  private String eventIdentifier;

  public GitXWebhookCacheUpdateRunnable(
      String eventIdentifier, GitXCacheUpdateRunnableRequestDTO gitXCacheUpdateRunnableRequestDTO) {
    this.eventIdentifier = eventIdentifier;
    this.gitXCacheUpdateRunnableRequestDTO = gitXCacheUpdateRunnableRequestDTO;
  }

  @Override
  public void run() {
    try (ResponseTimeRecorder ignore2 = new ResponseTimeRecorder("GitXWebhookCacheUpdateRunnable BG Task");
         GitXWebhookCacheUpdateLogContext context =
             new GitXWebhookCacheUpdateLogContext(gitXCacheUpdateRunnableRequestDTO)) {
      String accountIdentifier = gitXCacheUpdateRunnableRequestDTO.getAccountIdentifier();
      log.info(String.format(
          "In the account %s, updating the git cache for the event %s.", accountIdentifier, eventIdentifier));
      scmFacilitatorService.updateGitCache(buildScmUpdateGitCacheRequestDTO(gitXCacheUpdateRunnableRequestDTO));
      updateEventStatusAndStartTriggerExecution(accountIdentifier, eventIdentifier, GitXWebhookEventStatus.SUCCESSFUL,
          gitXCacheUpdateRunnableRequestDTO.getWebhookDTO());
      log.info(String.format("In the account %s, successfully updated the git cache for the event %s",
          accountIdentifier, eventIdentifier));
    } catch (Exception exception) {
      log.error("Faced exception while submitting background task for updating the git cache for event: {} ",
          eventIdentifier, exception);
      updateEventStatusAndStartTriggerExecution(gitXCacheUpdateRunnableRequestDTO.getAccountIdentifier(),
          eventIdentifier, GitXWebhookEventStatus.FAILED, gitXCacheUpdateRunnableRequestDTO.getWebhookDTO());
    }
  }

  private ScmUpdateGitCacheRequestDTO buildScmUpdateGitCacheRequestDTO(
      GitXCacheUpdateRunnableRequestDTO gitXCacheUpdateRunnableRequestDTO) {
    return ScmUpdateGitCacheRequestDTO.builder()
        .accountIdentifier(gitXCacheUpdateRunnableRequestDTO.getAccountIdentifier())
        .scmGetFileByBranchRequestDTOMap(buildScmGetFileByBranchRequestDTOMap(gitXCacheUpdateRunnableRequestDTO))
        .build();
  }

  private Map<ScmGetBatchFileRequestIdentifier, ScmGetFileByBranchRequestDTO> buildScmGetFileByBranchRequestDTOMap(
      GitXCacheUpdateRunnableRequestDTO gitXCacheUpdateRunnableRequestDTO) {
    Map<ScmGetBatchFileRequestIdentifier, ScmGetFileByBranchRequestDTO> scmGetFileByBranchRequestDTOMap =
        new HashMap<>();
    gitXCacheUpdateRunnableRequestDTO.getModifiedFilePaths().forEach(modifiedFilePath -> {
      String uniqueIdentifier = buildUniqueIdentifier(gitXCacheUpdateRunnableRequestDTO, modifiedFilePath);
      ScmGetBatchFileRequestIdentifier scmGetBatchFileRequestIdentifier =
          ScmGetBatchFileRequestIdentifier.builder().identifier(uniqueIdentifier).build();
      ScmGetFileByBranchRequestDTO scmGetFileByBranchRequestDTO =
          ScmGetFileByBranchRequestDTO.builder()
              .scope(Scope.of(gitXCacheUpdateRunnableRequestDTO.getAccountIdentifier()))
              .scmConnector(gitXCacheUpdateRunnableRequestDTO.getScmConnector())
              .repoName(gitXCacheUpdateRunnableRequestDTO.getRepoName())
              .branchName(gitXCacheUpdateRunnableRequestDTO.getBranch())
              .filePath(modifiedFilePath)
              .connectorRef(gitXCacheUpdateRunnableRequestDTO.getConnectorRef())
              .useCache(false)
              .build();
      scmGetFileByBranchRequestDTOMap.put(scmGetBatchFileRequestIdentifier, scmGetFileByBranchRequestDTO);
    });
    return scmGetFileByBranchRequestDTOMap;
  }

  private String buildUniqueIdentifier(
      GitXCacheUpdateRunnableRequestDTO gitXCacheUpdateRunnableRequestDTO, String modifiedFilePath) {
    return gitXCacheUpdateRunnableRequestDTO.getAccountIdentifier() + "/"
        + gitXCacheUpdateRunnableRequestDTO.getEventIdentifier() + "/" + modifiedFilePath;
  }

  private void updateEventStatusAndStartTriggerExecution(String accountIdentifier, String eventIdentifier,
      GitXWebhookEventStatus gitXWebhookEventStatus, WebhookDTO webhookDTO) {
    try {
      gitXWebhookEventService.updateEvent(accountIdentifier, eventIdentifier,
          GitXEventUpdateRequestDTO.builder().gitXWebhookEventStatus(gitXWebhookEventStatus).build());
    } catch (Exception ex) {
      log.error("Exception occurred while changing the state of the event {} to {}", webhookDTO.getEventId(),
          gitXWebhookEventStatus.name(), ex);
    }
    gitXWebhookTriggerHelper.startTriggerExecution(webhookDTO);
  }
}
