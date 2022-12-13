/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.CfCommandUnitConstants.Downsize;
import static io.harness.pcf.CfCommandUnitConstants.Wrapup;
import static io.harness.pcf.model.CfConstants.CLOUD_FOUNDRY_LOG_PREFIX;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.task.tas.TasNgConfigMapper;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfDeployCommandResult;
import io.harness.delegate.beans.pcf.CfInBuiltVariablesUpdateValues;
import io.harness.delegate.beans.pcf.CfInternalInstanceElement;
import io.harness.delegate.beans.pcf.CfRollbackCommandResult;
import io.harness.delegate.beans.pcf.CfServiceData;
import io.harness.delegate.beans.pcf.TasApplicationInfo;
import io.harness.delegate.task.cf.CfCommandTaskHelperNG;
import io.harness.delegate.task.pcf.TasTaskHelperBase;
import io.harness.delegate.task.pcf.request.CfCommandRequestNG;
import io.harness.delegate.task.pcf.request.CfRollbackCommandRequestNG;
import io.harness.delegate.task.pcf.response.CfCommandResponseNG;
import io.harness.delegate.task.pcf.response.CfDeployCommandResponseNG;
import io.harness.delegate.task.pcf.response.CfRollbackCommandResponseNG;
import io.harness.delegate.task.pcf.response.TasInfraConfig;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.Misc;
import io.harness.pcf.CfCommandUnitConstants;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfRenameRequest;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.CloudFoundryConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;

// Rollback handler for only Canary and Basic Deployment
@NoArgsConstructor
@Singleton
@Slf4j
@OwnedBy(CDP)
public class CfRollbackCommandTaskHandlerNG extends CfCommandTaskNGHandler {
  @Inject TasTaskHelperBase tasTaskHelperBase;
  @Inject TasNgConfigMapper tasNgConfigMapper;
  @Inject CfDeploymentManager cfDeploymentManager;
  @Inject protected CfCommandTaskHelperNG cfCommandTaskHelperNG;
  public static final String INTERIM_DELIMITER = "__INTERIM";
  @Override
  protected CfCommandResponseNG executeTaskInternal(CfCommandRequestNG cfCommandRequestNG,
      ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(cfCommandRequestNG instanceof CfRollbackCommandRequestNG)) {
      throw new InvalidArgumentsException(
          Pair.of("cfCommandRequestNG", "Must be instance of CfRollbackCommandRequestNG"));
    }
    CfInBuiltVariablesUpdateValues updateValues = CfInBuiltVariablesUpdateValues.builder().build();
    LogCallback executionLogCallback = tasTaskHelperBase.getLogCallback(
        iLogStreamingTaskClient, CfCommandUnitConstants.Upsize, true, commandUnitsProgress);
    executionLogCallback.saveExecutionLog(color("--------- Starting Rollback deployment", White, Bold));
    List<CfServiceData> cfServiceDataUpdated = new ArrayList<>();
    CfRollbackCommandResult cfRollbackCommandResult = CfRollbackCommandResult.builder().build();
    CfRollbackCommandResponseNG cfRollbackCommandResponseNG = CfRollbackCommandResponseNG.builder().build();

    CfRollbackCommandRequestNG cfRollbackCommandRequestNG = (CfRollbackCommandRequestNG) cfCommandRequestNG;

    File workingDirectory = null;
    Exception exception;
    try {
      // This will be CF_HOME for any cli related operations
      workingDirectory = cfCommandTaskHelperNG.generateWorkingDirectoryForDeployment();
      TasInfraConfig tasInfraConfig = cfRollbackCommandRequestNG.getTasInfraConfig();
      CloudFoundryConfig cfConfig = tasNgConfigMapper.mapTasConfigWithDecryption(
          tasInfraConfig.getTasConnectorDTO(), tasInfraConfig.getEncryptionDataDetails());
      CfRequestConfig cfRequestConfig = buildCfRequestConfig(cfRollbackCommandRequestNG, workingDirectory, cfConfig);

      // Will be used if app autoscalar is configured
      CfAppAutoscalarRequestData autoscalarRequestData =
          CfAppAutoscalarRequestData.builder()
              .cfRequestConfig(cfRequestConfig)
              .configPathVar(workingDirectory.getAbsolutePath())
              .timeoutInMins(cfRollbackCommandRequestNG.getTimeoutIntervalInMin() != null
                      ? cfRollbackCommandRequestNG.getTimeoutIntervalInMin()
                      : 10)
              .build();
      // get Upsize Instance data
      List<CfInternalInstanceElement> cfInstanceElements = new ArrayList<>();
      // During rollback, always upsize old ones


      updateNewAppName(cfRollbackCommandRequestNG.getNewApplicationDetails(), cfRequestConfig, executionLogCallback);
      renameOldApp(cfRollbackCommandRequestNG.getOldApplicationDetails(), cfRequestConfig, executionLogCallback);
      cfCommandTaskHelperNG.upsizeListOfInstancesAndRestoreRoutes(executionLogCallback, cfDeploymentManager,
              cfRollbackCommandRequestNG.getOldApplicationDetails(), cfRequestConfig, cfRollbackCommandRequestNG);
      // Enable autoscalar for older app, if it was disabled during deploy

      //todo : ask about autoscaler
   //   cfCommandTaskHelperNG.enableAutoscalerIfNeeded(cfRollbackCommandRequestNG.getOldApplicationDetails(), autoscalarRequestData, executionLogCallback);
      executionLogCallback.saveExecutionLog("#---------- Upsize Application Successfully Completed", INFO, SUCCESS);

      executionLogCallback =
          tasTaskHelperBase.getLogCallback(iLogStreamingTaskClient, Downsize, true, commandUnitsProgress);

      // Downsizing
      cfCommandTaskHelperNG.downSizeListOfInstancesAndUnmapRoutes(executionLogCallback, cfServiceDataUpdated,
          cfRequestConfig, cfRollbackCommandRequestNG.getNewApplicationDetails(),
          cfRollbackCommandRequestNG, autoscalarRequestData);
      //todo: different resopnse
      cfRollbackCommandResponseNG.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

      if (isRollbackCompleted(cfRollbackCommandRequestNG, cfRequestConfig)) {
        cfCommandTaskHelperNG.deleteNewApp(cfRequestConfig, cfRollbackCommandRequestNG, executionLogCallback);
        executionLogCallback.saveExecutionLog("\n\n--------- CF Rollback completed successfully", INFO, SUCCESS);
      } else {
        executionLogCallback.saveExecutionLog("\n\n--------- CF Rollback is not completed", INFO, FAILURE);
      }

    } catch (Exception e) {
      exception = e;
      logExceptionMessage(executionLogCallback, cfRollbackCommandRequestNG, exception);
      cfRollbackCommandResponseNG.setCommandExecutionStatus(FAILURE);
      cfRollbackCommandResult.setInstanceDataUpdated(cfServiceDataUpdated);
      cfRollbackCommandResponseNG.setErrorMessage(ExceptionUtils.getMessage(exception));

    } finally {
      executionLogCallback =
          tasTaskHelperBase.getLogCallback(iLogStreamingTaskClient, Wrapup, true, commandUnitsProgress);
      executionLogCallback.saveExecutionLog("#------- Deleting Temporary Files");
      if (workingDirectory != null) {
        try {
          FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
          executionLogCallback.saveExecutionLog("Temporary Files Successfully deleted", INFO, SUCCESS);
        } catch (IOException e) {
          log.warn("Failed to delete temp cf home folder", e);
        }
      }
    }
    cfRollbackCommandResponseNG.setCfRollbackCommandResult(cfRollbackCommandResult);
    return cfRollbackCommandResponseNG;
  }

  private void renameOldApp(TasApplicationInfo oldApplicationDetails, CfRequestConfig cfRequestConfig, LogCallback executionLogCallback) throws PivotalClientApiException {
    String oldName = oldApplicationDetails.getOldName();
    CfRenameRequest cfRenameRequest = new CfRenameRequest(cfRequestConfig, oldApplicationDetails.getApplicationGuid(),
            oldApplicationDetails.getApplicationName(),
            oldName);
    cfDeploymentManager.renameApplication(cfRenameRequest, executionLogCallback);
    oldApplicationDetails.setApplicationName(oldName);
  }

  private void updateNewAppName(TasApplicationInfo newApplicationDetails, CfRequestConfig cfRequestConfig, LogCallback executionLogCallback) throws PivotalClientApiException {
    String newName = newApplicationDetails.getApplicationName() + INTERIM_DELIMITER;
    CfRenameRequest cfRenameRequest = new CfRenameRequest(cfRequestConfig, newApplicationDetails.getApplicationGuid(),
            newApplicationDetails.getApplicationName(),
            newName);
   cfDeploymentManager.renameApplication(cfRenameRequest, executionLogCallback);
    newApplicationDetails.setApplicationName(newName);
  }

  private CfRequestConfig buildCfRequestConfig(
      CfRollbackCommandRequestNG cfRollbackCommandRequestNG, File workingDirectory, CloudFoundryConfig cfConfig) {
    return CfRequestConfig.builder()
        .userName(String.valueOf(cfConfig.getUserName()))
        .password(String.valueOf(cfConfig.getPassword()))
        .endpointUrl(cfConfig.getEndpointUrl())
        .orgName(cfRollbackCommandRequestNG.getTasInfraConfig().getOrganization())
        .spaceName(cfRollbackCommandRequestNG.getTasInfraConfig().getSpace())
        .timeOutIntervalInMins(cfRollbackCommandRequestNG.getTimeoutIntervalInMin() == null
                ? 10
                : cfRollbackCommandRequestNG.getTimeoutIntervalInMin())
        .cfHomeDirPath(workingDirectory.getAbsolutePath())
        .cfCliPath(cfCommandTaskHelperNG.getCfCliPathOnDelegate(true, cfRollbackCommandRequestNG.getCfCliVersion()))
        .cfCliVersion(cfRollbackCommandRequestNG.getCfCliVersion())
        .build();
  }

  private boolean isRollbackCompleted(CfRollbackCommandRequestNG commandRollbackRequest,
      CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    // app downsized - to be deleted
    TasApplicationInfo newApp = commandRollbackRequest.getNewApplicationDetails();
    boolean rollbackCompleted =
        instanceCountMatches(newApp.getApplicationName(), 0, cfRequestConfig);
    // app upsized - to be renamed
    TasApplicationInfo prevActiveApp = commandRollbackRequest.getOldApplicationDetails();
    if (!isNull(prevActiveApp)) {
      rollbackCompleted = rollbackCompleted
          && instanceCountMatches(prevActiveApp.getApplicationName(), prevActiveApp.getRunningCount(), cfRequestConfig);
    }

    return rollbackCompleted;
  }

  private boolean instanceCountMatches(String applicationName, Integer expectedInstanceCount,
      CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    cfRequestConfig.setApplicationName(applicationName);
    ApplicationDetail application = cfDeploymentManager.getApplicationByName(cfRequestConfig);
    return null != application && application.getInstances().equals(expectedInstanceCount);
  }

  private void logExceptionMessage(
      LogCallback executionLogCallback, CfRollbackCommandRequestNG commandRollbackRequest, Exception exception) {
    log.error(
        CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing CF Rollback task [{}]", commandRollbackRequest, exception);
    executionLogCallback.saveExecutionLog("\n\n--------- CF Rollback failed to complete successfully", ERROR, FAILURE);
    Misc.logAllMessages(exception, executionLogCallback);
  }
}
