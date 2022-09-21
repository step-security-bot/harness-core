/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.DOWNLOAD_ARTIFACT_NOT_SUPPORTED_FOR_CUSTOM_ARTIFACT;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.DOWNLOAD_ARTIFACT_NOT_SUPPORTED_FOR_CUSTOM_ARTIFACT_HINT;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.NO_DESTINATION_DOWNLOAD_ARTIFACT_PATH_SPECIFIED;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.NO_DESTINATION_DOWNLOAD_ARTIFACT_PATH_SPECIFIED_EXPLANATION;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.NO_DESTINATION_DOWNLOAD_PATH_SPECIFIED_HINT;
import static io.harness.delegate.utils.ArtifactoryUtils.getArtifactFileName;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.shell.ssh.CommandHandler;
import io.harness.delegate.task.ssh.CopyCommandUnit;
import io.harness.delegate.task.ssh.NgCommandUnit;
import io.harness.delegate.task.ssh.NgDownloadArtifactCommandUnit;
import io.harness.delegate.task.ssh.artifact.CustomArtifactDelegateConfig;
import io.harness.delegate.task.ssh.artifact.SkipCopyArtifactDelegateConfig;
import io.harness.delegate.task.ssh.artifact.SshWinRmArtifactDelegateConfig;
import io.harness.delegate.task.ssh.artifact.SshWinRmArtifactType;
import io.harness.delegate.task.winrm.ArtifactDownloadHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.runtime.WinRmCommandExecutionException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.shell.BaseScriptExecutor;
import io.harness.shell.ExecuteCommandResponse;
import io.harness.shell.ScriptType;
import io.harness.ssh.FileSourceType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
@Singleton
public abstract class AbstractDownloadArtifactCommandHandler implements CommandHandler {
  @Inject private Map<SshWinRmArtifactType, ArtifactDownloadHandler> artifactHandlers;

  @Override
  public ExecuteCommandResponse handle(CommandTaskParameters parameters, NgCommandUnit commandUnit,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress,
      Map<String, Object> taskContext) {
    if (!(commandUnit instanceof NgDownloadArtifactCommandUnit) && !(commandUnit instanceof CopyCommandUnit)) {
      throw new InvalidRequestException("Invalid command unit specified for command task.");
    }

    if (commandUnit instanceof NgDownloadArtifactCommandUnit) {
      if (!FileSourceType.ARTIFACT.equals(((NgDownloadArtifactCommandUnit) commandUnit).getSourceType())) {
        throw new InvalidRequestException("Invalid source type specified for command unit.");
      }
    }

    BaseScriptExecutor executor =
        getExecutor(parameters, commandUnit, logStreamingTaskClient, commandUnitsProgress, taskContext);
    LogCallback logCallback = executor.getLogCallback();
    CommandExecutionStatus commandExecutionStatus = downloadArtifact(parameters, logCallback, commandUnit, executor);

    if (FAILURE == commandExecutionStatus) {
      logCallback.saveExecutionLog("Failed to download artifact.", ERROR, commandExecutionStatus);
    }
    logCallback.saveExecutionLog(
        "Command execution finished with status " + commandExecutionStatus, INFO, commandExecutionStatus);

    log.info("Download artifact command execution returned status: {}", commandExecutionStatus);
    return ExecuteCommandResponse.builder().status(commandExecutionStatus).build();
  }

  private CommandExecutionStatus downloadArtifact(CommandTaskParameters commandTaskParameters, LogCallback logCallback,
      NgCommandUnit commandUnit, BaseScriptExecutor executor) {
    log.info("About to download artifact");
    SshWinRmArtifactDelegateConfig artifactDelegateConfig = commandTaskParameters.getArtifactDelegateConfig();
    if (artifactDelegateConfig instanceof SkipCopyArtifactDelegateConfig) {
      log.info("Artifactory docker registry found, skipping download artifact.");
      logCallback.saveExecutionLog("Command execution finished with status " + SUCCESS, INFO, SUCCESS);
      return SUCCESS;
    }

    if (artifactDelegateConfig instanceof CustomArtifactDelegateConfig) {
      throw NestedExceptionUtils.hintWithExplanationException(DOWNLOAD_ARTIFACT_NOT_SUPPORTED_FOR_CUSTOM_ARTIFACT_HINT,
          DOWNLOAD_ARTIFACT_NOT_SUPPORTED_FOR_CUSTOM_ARTIFACT,
          new WinRmCommandExecutionException(DOWNLOAD_ARTIFACT_NOT_SUPPORTED_FOR_CUSTOM_ARTIFACT));
    }

    if (artifactDelegateConfig == null) {
      throw new InvalidRequestException("Artifact delegate config not found.");
    }

    logCallback.saveExecutionLog(format("Begin execution of command: %s", commandUnit.getName()), INFO);
    logCallback.saveExecutionLog("Downloading artifact from " + artifactDelegateConfig.getArtifactType() + " to "
            + commandUnit.getDestinationPath() + "\\" + getArtifactFileName(artifactDelegateConfig.getArtifactPath()),
        INFO);

    if (isEmpty(commandUnit.getDestinationPath())) {
      log.info("Destination path not provided for download command unit");
      throw NestedExceptionUtils.hintWithExplanationException(
          format(NO_DESTINATION_DOWNLOAD_PATH_SPECIFIED_HINT, commandUnit.getName()),
          format(NO_DESTINATION_DOWNLOAD_ARTIFACT_PATH_SPECIFIED_EXPLANATION, commandUnit.getName()),
          new WinRmCommandExecutionException(NO_DESTINATION_DOWNLOAD_ARTIFACT_PATH_SPECIFIED));
    }

    if (isEmpty(artifactDelegateConfig.getArtifactPath())) {
      logCallback.saveExecutionLog("artifactPath or artifactPathFilter is blank", ERROR, FAILURE);
    }

    try {
      String command = getCommandString(commandUnit, artifactDelegateConfig);
      return executor.executeCommandString(command);
    } catch (Exception e) {
      return FAILURE;
    }
  }

  private String getCommandString(NgCommandUnit commandUnit, SshWinRmArtifactDelegateConfig artifactDelegateConfig) {
    String command;
    try {
      ArtifactDownloadHandler artifactHandler = artifactHandlers.get(artifactDelegateConfig.getArtifactType());
      if (artifactHandler == null) {
        log.warn("Wrong artifact delegate config submitted: {}", artifactDelegateConfig.getArtifactType());
        throw new InvalidRequestException("Expecting artifactory or jenkins delegate config");
      }
      command =
          artifactHandler.getCommandString(artifactDelegateConfig, commandUnit.getDestinationPath(), getScriptType());
    } catch (Exception e) {
      log.error("Cannot get command string for download artifact.", e);
      throw new RuntimeException("Cannot get command string for download artifact");
    }
    return command;
  }

  public abstract BaseScriptExecutor getExecutor(CommandTaskParameters commandTaskParameters, NgCommandUnit commandUnit,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress,
      Map<String, Object> taskContext);
  public abstract ScriptType getScriptType();
}
