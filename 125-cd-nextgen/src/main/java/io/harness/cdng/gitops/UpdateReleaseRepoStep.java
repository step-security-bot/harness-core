/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.gitops;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;
import static io.harness.cdng.gitops.constants.GitopsConstants.GITOPS_SWEEPING_OUTPUT;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.ListUtils.trimStrings;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executions.steps.ExecutionNodeType.GITOPS_UPDATE_RELEASE_REPO;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.trim;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.FeatureName;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.expressions.CDExpressionResolver;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.gitops.gitrestraint.services.GitRestraintInstanceService;
import io.harness.cdng.gitops.steps.GitOpsStepHelper;
import io.harness.cdng.gitops.steps.GitopsClustersOutcome;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitOpsTaskType;
import io.harness.delegate.task.git.NGGitOpsResponse;
import io.harness.delegate.task.git.NGGitOpsTaskParams;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.ConsumerId;
import io.harness.distribution.constraint.InvalidPermitsException;
import io.harness.distribution.constraint.PermanentlyBlockedConsumerException;
import io.harness.distribution.constraint.UnableToRegisterConsumerException;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.common.ExpressionMode;
import io.harness.gitopsprovider.entity.GitRestraintInstance.GitRestraintInstanceKeys;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncChainExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.security.PmsSecurityContextEventGuard;
import io.harness.pms.yaml.ParameterField;
import io.harness.serializer.KryoSerializer;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;
import io.harness.steps.TaskRequestsUtils;
import io.harness.steps.executable.AsyncChainExecutableWithRbac;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(GITOPS)
@Slf4j
public class UpdateReleaseRepoStep implements AsyncChainExecutableWithRbac<StepElementParameters>,
                                              TaskExecutable<StepElementParameters, NGGitOpsResponse> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(GITOPS_UPDATE_RELEASE_REPO.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  private static final String CONSTRAINT_OPERATION = "CREATE_PR";

  @Inject private CDExpressionResolver cdExpressionResolver;
  @Inject @Named("referenceFalseKryoSerializer") private KryoSerializer referenceFalseKryoSerializer;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  @Inject protected OutcomeService outcomeService;
  @Inject private GitOpsStepHelper gitOpsStepHelper;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject private GitRestraintInstanceService gitRestraintInstanceService;
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject private StepHelper stepHelper;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {}

  private Map<String, Object> populateConstraintContext(ConstraintUnit constraintUnit, String releaseEntityId) {
    Map<String, Object> constraintContext = new HashMap<>();
    constraintContext.put(GitRestraintInstanceKeys.releaseEntityId, releaseEntityId);
    constraintContext.put(
        GitRestraintInstanceKeys.order, gitRestraintInstanceService.getMaxOrder(constraintUnit.getValue()) + 1);

    return constraintContext;
  }

  private NGLogCallback getLogCallback(Ambiance ambiance, boolean shouldOpenStream) {
    return new NGLogCallback(logStreamingStepClientFactory, ambiance, null, shouldOpenStream);
  }

  @Override
  @SneakyThrows
  public AsyncChainExecutableResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    NGLogCallback logCallback = getLogCallback(ambiance, true);

    UpdateReleaseRepoStepParams gitOpsSpecParams = (UpdateReleaseRepoStepParams) stepParameters.getSpec();
    ManifestOutcome releaseRepoOutcome = gitOpsStepHelper.getReleaseRepoOutcome(ambiance);
    ConnectorInfoDTO connectorInfoDTO =
        cdStepHelper.getConnector(releaseRepoOutcome.getStore().getConnectorReference().getValue(), ambiance);

    if (!cdFeatureFlagHelper.isEnabled(
            AmbianceUtils.getAccountId(ambiance), FeatureName.GITOPS_GITHUB_RESTRAINT_FOR_STEPS)) {
      String taskId =
          queueDelegateTask(ambiance, stepParameters, releaseRepoOutcome, gitOpsSpecParams, connectorInfoDTO);
      return AsyncChainExecutableResponse.newBuilder()
          .addAllLogKeys(getLogKeys(ambiance, gitOpsSpecParams.getCommandUnits()))
          .setCallbackId(taskId)
          .addAllUnits(gitOpsSpecParams.getCommandUnits())
          .setChainEnd(true)
          .build();
    }

    logCallback.saveExecutionLog(
        String.format("Trying to acquire lock on token for %s operation", CONSTRAINT_OPERATION));
    String tokenRefIdentifier = GitOpsStepUtils.extractToken(connectorInfoDTO);
    if (tokenRefIdentifier == null) {
      throw new InvalidRequestException("Failed to get token identifier from connector");
    }
    String constraintUnitIdentifier = CONSTRAINT_OPERATION + AmbianceUtils.getAccountId(ambiance) + tokenRefIdentifier;

    Constraint constraint = gitRestraintInstanceService.createAbstraction(constraintUnitIdentifier);
    String releaseEntityId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    String consumerId = generateUuid();
    ConstraintUnit constraintUnit = new ConstraintUnit(constraintUnitIdentifier);

    Map<String, Object> constraintContext = populateConstraintContext(constraintUnit, releaseEntityId);

    try {
      Consumer.State state = constraint.registerConsumer(
          constraintUnit, new ConsumerId(consumerId), 1, constraintContext, gitRestraintInstanceService);
      switch (state) {
        case BLOCKED:
          logCallback.saveExecutionLog("Running instances were found, step queued.", INFO, SUCCESS);
          return AsyncChainExecutableResponse.newBuilder()
              .addAllLogKeys(getLogKeys(ambiance, gitOpsSpecParams.getCommandUnits()))
              .setCallbackId(consumerId)
              .addAllUnits(gitOpsSpecParams.getCommandUnits())
              .build();
        case ACTIVE:
          try {
            logCallback.saveExecutionLog("Lock acquired, proceeding with delegate task.", INFO, SUCCESS);
            // Fetch files from releaseRepoOutcome and replace expressions if present with cluster name and environment
            String taskId =
                queueDelegateTask(ambiance, stepParameters, releaseRepoOutcome, gitOpsSpecParams, connectorInfoDTO);
            return AsyncChainExecutableResponse.newBuilder()
                .addAllLogKeys(getLogKeys(ambiance, gitOpsSpecParams.getCommandUnits()))
                .setCallbackId(taskId)
                .addAllUnits(gitOpsSpecParams.getCommandUnits())
                .setChainEnd(true)
                .build();

          } catch (Exception e) {
            log.error("Failed to execute Update Release Repo step", e);
            throw new InvalidRequestException(
                String.format("Failed to execute Update Release Repo step. %s", e.getMessage()));
          }
        case REJECTED:
          logCallback.saveExecutionLog(
              "Constraint acquire rejected. Please try again after few minutes.", ERROR, FAILURE);
          throw new GeneralException("Found already running resourceConstrains, marking this execution as failed");
        default:
          throw new IllegalStateException("This should never happen");
      }

    } catch (InvalidPermitsException | UnableToRegisterConsumerException | PermanentlyBlockedConsumerException e) {
      log.error("Exception on UpdateReleaseRepoStep for id [{}]", AmbianceUtils.obtainCurrentRuntimeId(ambiance), e);
      throw e;
    }
  }

  private List<String> getLogKeys(Ambiance ambiance, List<String> units) {
    return new ArrayList<>(StepUtils.generateLogKeys(ambiance, units));
  }

  @Override
  public AsyncChainExecutableResponse executeNextLinkWithSecurityContext(Ambiance ambiance,
      StepElementParameters stepParameters, StepInputPackage inputPackage, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseSupplier) throws Exception {
    try {
      NGLogCallback logCallback = getLogCallback(ambiance, false);
      logCallback.saveExecutionLog("Lock acquired, proceeding with delegate task.", INFO, SUCCESS);
      UpdateReleaseRepoStepParams gitOpsSpecParams = (UpdateReleaseRepoStepParams) stepParameters.getSpec();
      ManifestOutcome releaseRepoOutcome = gitOpsStepHelper.getReleaseRepoOutcome(ambiance);
      ConnectorInfoDTO connectorInfoDTO =
          cdStepHelper.getConnector(releaseRepoOutcome.getStore().getConnectorReference().getValue(), ambiance);
      // Fetch files from releaseRepoOutcome and replace expressions if present with cluster name and environment
      String taskId =
          queueDelegateTask(ambiance, stepParameters, releaseRepoOutcome, gitOpsSpecParams, connectorInfoDTO);
      return AsyncChainExecutableResponse.newBuilder()
          .addAllLogKeys(getLogKeys(ambiance, gitOpsSpecParams.getCommandUnits()))
          .setCallbackId(taskId)
          .setChainEnd(true)
          .addAllUnits(gitOpsSpecParams.getCommandUnits())
          .build();

    } catch (Exception e) {
      log.error("Failed to execute Update Release Repo step", e);
      throw new InvalidRequestException(
          String.format("Failed to execute Update Release Repo step. %s", e.getMessage()));
    }
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    NGGitOpsResponse ngGitOpsResponse = (NGGitOpsResponse) responseDataSupplier.get();
    return calculateStepResponse(ambiance, ngGitOpsResponse);
  }

  private StepResponse calculateStepResponse(Ambiance ambiance, NGGitOpsResponse ngGitOpsResponse) {
    NGLogCallback logCallback = getLogCallback(ambiance, false);

    if (TaskStatus.SUCCESS.equals(ngGitOpsResponse.getTaskStatus())) {
      logCallback.saveExecutionLog("UpdateReleaseRepo step finished.", INFO, SUCCESS);
      UpdateReleaseRepoOutcome updateReleaseRepoOutcome = UpdateReleaseRepoOutcome.builder()
                                                              .prlink(ngGitOpsResponse.getPrLink())
                                                              .prNumber(ngGitOpsResponse.getPrNumber())
                                                              .commitId(ngGitOpsResponse.getCommitId())
                                                              .ref(ngGitOpsResponse.getRef())
                                                              .build();

      executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.UPDATE_RELEASE_REPO_OUTCOME,
          updateReleaseRepoOutcome, StepOutcomeGroup.STAGE.name());

      return StepResponse.builder()
          .unitProgressList(ngGitOpsResponse.getUnitProgressData().getUnitProgresses())
          .status(Status.SUCCEEDED)
          .stepOutcome(StepResponse.StepOutcome.builder()
                           .name(OutcomeExpressionConstants.UPDATE_RELEASE_REPO_OUTCOME)
                           .outcome(updateReleaseRepoOutcome)
                           .build())
          .build();
    }

    return StepResponse.builder()
        .unitProgressList(ngGitOpsResponse.getUnitProgressData().getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder().setErrorMessage(ngGitOpsResponse.getErrorMessage()).build())
        .build();
  }

  private String queueDelegateTask(Ambiance ambiance, StepElementParameters stepParameters,
      ManifestOutcome releaseRepoOutcome, UpdateReleaseRepoStepParams gitOpsSpecParams,
      ConnectorInfoDTO connectorInfoDTO) {
    Map<String, Map<String, String>> filesToVariablesMap =
        buildFilePathsToVariablesMap(releaseRepoOutcome, ambiance, gitOpsSpecParams.getVariables());

    List<GitFetchFilesConfig> gitFetchFilesConfig = new ArrayList<>();
    gitFetchFilesConfig.add(getGitFetchFilesConfig(ambiance, releaseRepoOutcome, filesToVariablesMap.keySet()));

    NGGitOpsTaskParams ngGitOpsTaskParams = NGGitOpsTaskParams.builder()
                                                .gitOpsTaskType(GitOpsTaskType.UPDATE_RELEASE_REPO)
                                                .gitFetchFilesConfig(gitFetchFilesConfig.get(0))
                                                .accountId(AmbianceUtils.getAccountId(ambiance))
                                                .connectorInfoDTO(connectorInfoDTO)
                                                .filesToVariablesMap(filesToVariablesMap)
                                                .prTitle(gitOpsSpecParams.prTitle.getValue())
                                                .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepParameters))
                                  .taskType(TaskType.GITOPS_TASK_NG.name())
                                  .parameters(new Object[] {ngGitOpsTaskParams})
                                  .build();
    TaskRequest taskRequest = TaskRequestsUtils.prepareTaskRequestWithTaskSelector(ambiance, taskData,
        referenceFalseKryoSerializer, TaskCategory.DELEGATE_TASK_V2, gitOpsSpecParams.getCommandUnits(), true,
        taskData.getTaskType(),
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(gitOpsSpecParams.getDelegateSelectors()))));

    DelegateTaskRequest delegateTaskRequest =
        cdStepHelper.mapTaskRequestToDelegateTaskRequest(taskRequest, taskData, emptySet(), "", true);

    return delegateGrpcClientWrapper.submitAsyncTaskV2(delegateTaskRequest, Duration.ZERO);
  }

  @Override
  public void handleAbort(Ambiance ambiance, StepElementParameters stepParameters,
      AsyncChainExecutableResponse executableResponse, boolean userMarked) {
    // TODO: implement this
  }

  @VisibleForTesting
  public Map<String, Map<String, String>> buildFilePathsToVariablesMap(
      ManifestOutcome releaseRepoOutcome, Ambiance ambiance, Map<String, Object> variables) {
    // Get FilePath from release repo
    GitStoreConfig gitStoreConfig = (GitStoreConfig) releaseRepoOutcome.getStore();
    String filePath = gitStoreConfig.getPaths().getValue().get(0);

    // Read environment outcome and iterate over clusterData to replace the cluster and env name
    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(GITOPS_SWEEPING_OUTPUT));

    Map<String, Map<String, String>> filePathsToVariables = new HashMap<>();

    if (optionalSweepingOutput != null && optionalSweepingOutput.isFound()) {
      GitopsClustersOutcome output = (GitopsClustersOutcome) optionalSweepingOutput.getOutput();
      List<GitopsClustersOutcome.ClusterData> clustersData = output.getClustersData();

      String file;

      for (GitopsClustersOutcome.ClusterData cluster : clustersData) {
        file = filePath;
        if (filePath.contains("<+envgroup.name>")) {
          file = filePath.replaceAll(
              "<\\+envgroup.name>/", cluster.getEnvGroupName() == null ? EMPTY : cluster.getEnvGroupName() + "/");
        }
        if (filePath.contains("<+env.name>")) {
          file = file.replaceAll("<\\+env.name>", cluster.getEnvName());
        }
        if (filePath.contains("<+cluster.name>")) {
          file = file.replaceAll("<\\+cluster.name>", cluster.getClusterName());
        }
        List<String> files = new ArrayList<>();
        files.add(file);
        // Resolve any other expressions in the filepaths. eg. service variables
        cdExpressionResolver.updateExpressions(ambiance, files);

        file = files.get(0);

        cdExpressionResolver.updateExpressions(ambiance, cluster.getVariables());

        Map<String, String> flattennedVariables = new HashMap<>();
        // Convert variables map from Map<String, Object> to Map<String, String>
        for (String key : cluster.getVariables().keySet()) {
          Object value = cluster.getVariables().get(key);
          if (value instanceof String && ((String) value).startsWith("${ngSecretManager.obtain")) {
            continue;
          }
          if (value.getClass() == ParameterField.class) {
            ParameterField<Object> p = (ParameterField) value;
            populateVariables(p, flattennedVariables, key, p.getValue().toString());
          } else {
            if (value != null && !"null".equals(String.valueOf(value)) && !String.valueOf(value).isBlank()) {
              flattennedVariables.put(key, value.toString());
            } else {
              log.info(format("Ignoring key %s value %s", key, value.toString()));
            }
          }
        }
        // Convert variables from spec parameters
        for (Map.Entry<String, Object> variableEntry : variables.entrySet()) {
          ParameterField p = (ParameterField) variableEntry.getValue();
          ParameterField copyParameter = ParameterField.builder()
                                             .expression(p.isExpression())
                                             .expressionValue(p.getExpressionValue())
                                             .value(p.getValue())
                                             .build();
          if (copyParameter.isExpression()) {
            if (copyParameter.getExpressionValue().contains("<+envgroup.name>")) {
              copyParameter.setExpressionValue(copyParameter.getExpressionValue().replaceAll(
                  "<\\+envgroup.name>", cluster.getEnvGroupName() == null ? EMPTY : cluster.getEnvGroupName()));
            }
            if (copyParameter.getExpressionValue().contains("<+env.name>")) {
              copyParameter.setExpressionValue(
                  copyParameter.getExpressionValue().replaceAll("<\\+env.name>", cluster.getEnvName()));
            }
            if (copyParameter.getExpressionValue().contains("<+cluster.name>")) {
              copyParameter.setExpressionValue(
                  copyParameter.getExpressionValue().replaceAll("<\\+cluster.name>", cluster.getClusterName()));
            }
          }

          cdExpressionResolver.updateExpressions(ambiance, copyParameter, ExpressionMode.THROW_EXCEPTION_IF_UNRESOLVED);

          if (copyParameter.getValue() != null) {
            populateVariables(
                copyParameter, flattennedVariables, variableEntry.getKey(), copyParameter.getValue().toString());
          }

          for (String key : flattennedVariables.keySet()) {
            String value = flattennedVariables.get(key);
            if (value.matches("[-+]?[0-9]*\\.0")) {
              flattennedVariables.put(key, value.split("\\.")[0]);
            }
          }
        }
        filePathsToVariables.put(file, flattennedVariables);
      }
    } else {
      throw new InvalidRequestException("No outcome published from GitOpsCluster Step");
    }
    return filePathsToVariables;
  }

  private void populateVariables(
      ParameterField parameter, Map<String, String> flattennedVariables, String key, String value) {
    if (isVariableNotEmpty(parameter)) {
      flattennedVariables.put(key, value);
    } else {
      log.info(format("Ignoring key %s value %s", key, value));
    }
  }

  private boolean isVariableNotEmpty(ParameterField parameter) {
    return parameter.getValue() != null && !String.valueOf(parameter.getValue()).isBlank()
        && !"null".equals(String.valueOf(parameter.getValue()));
  }

  private GitFetchFilesConfig getGitFetchFilesConfig(
      Ambiance ambiance, ManifestOutcome manifestOutcome, Set<String> resolvedFilePaths) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) manifestOutcome.getStore();
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = cdStepHelper.getConnector(connectorId, ambiance);

    List<String> gitFilePaths = new ArrayList<>();
    gitFilePaths.addAll(resolvedFilePaths);

    GitStoreDelegateConfig gitStoreDelegateConfig = cdStepHelper.getGitStoreDelegateConfigWithApiAccess(
        gitStoreConfig, connectorDTO, gitFilePaths, ambiance, manifestOutcome);

    ScmConnector scmConnector = (ScmConnector) connectorDTO.getConnectorConfig();

    // Overriding the gitStoreDelegateConfig to set the correct version of scmConnector that allows
    // to retain gitConnector metadata required for updating release repo
    GitStoreDelegateConfig rebuiltGitStoreDelegateConfig =
        GitStoreDelegateConfig.builder()
            .gitConfigDTO(scmConnector)
            .apiAuthEncryptedDataDetails(gitStoreDelegateConfig.getApiAuthEncryptedDataDetails())
            .sshKeySpecDTO(gitStoreDelegateConfig.getSshKeySpecDTO())
            .encryptedDataDetails(gitStoreDelegateConfig.getEncryptedDataDetails())
            .fetchType(gitStoreConfig.getGitFetchType())
            .branch(trim(getParameterFieldValue(gitStoreConfig.getBranch())))
            .commitId(trim(getParameterFieldValue(gitStoreConfig.getCommitId())))
            .paths(trimStrings(gitFilePaths))
            .connectorName(connectorDTO.getName())
            .manifestType(manifestOutcome.getType())
            .manifestId(manifestOutcome.getIdentifier())
            .optimizedFilesFetch(gitStoreDelegateConfig.isOptimizedFilesFetch())
            .build();

    return GitFetchFilesConfig.builder()
        .identifier(manifestOutcome.getIdentifier())
        .manifestType(manifestOutcome.getType())
        .succeedIfFileNotFound(true)
        .gitStoreDelegateConfig(rebuiltGitStoreDelegateConfig)
        .build();
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      validateResources(ambiance, stepParameters);
      UpdateReleaseRepoStepParams gitOpsSpecParams = (UpdateReleaseRepoStepParams) stepParameters.getSpec();
      ManifestOutcome releaseRepoOutcome = gitOpsStepHelper.getReleaseRepoOutcome(ambiance);
      // Fetch files from releaseRepoOutcome and replace expressions if present with cluster name and environment
      Map<String, Map<String, String>> filesToVariablesMap =
          buildFilePathsToVariablesMap(releaseRepoOutcome, ambiance, gitOpsSpecParams.getVariables());

      List<GitFetchFilesConfig> gitFetchFilesConfig = new ArrayList<>();
      gitFetchFilesConfig.add(getGitFetchFilesConfig(ambiance, releaseRepoOutcome, filesToVariablesMap.keySet()));

      NGGitOpsTaskParams ngGitOpsTaskParams =
          NGGitOpsTaskParams.builder()
              .gitOpsTaskType(GitOpsTaskType.UPDATE_RELEASE_REPO)
              .gitFetchFilesConfig(gitFetchFilesConfig.get(0))
              .accountId(AmbianceUtils.getAccountId(ambiance))
              .connectorInfoDTO(
                  cdStepHelper.getConnector(releaseRepoOutcome.getStore().getConnectorReference().getValue(), ambiance))
              .filesToVariablesMap(filesToVariablesMap)
              .prTitle(gitOpsSpecParams.prTitle.getValue())
              .build();

      final TaskData taskData = TaskData.builder()
                                    .async(true)
                                    .timeout(CDStepHelper.getTimeoutInMillis(stepParameters))
                                    .taskType(TaskType.GITOPS_TASK_NG.name())
                                    .parameters(new Object[] {ngGitOpsTaskParams})
                                    .build();

      return TaskRequestsUtils.prepareCDTaskRequest(ambiance, taskData, referenceFalseKryoSerializer,
          gitOpsSpecParams.getCommandUnits(), TaskType.GITOPS_TASK_NG.getDisplayName(),
          TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(gitOpsSpecParams.getDelegateSelectors()))),
          stepHelper.getEnvironmentType(ambiance));

    } catch (Exception e) {
      log.error("Failed to execute Update Release Repo step", e);
      throw new InvalidRequestException(
          String.format("Failed to execute Update Release Repo step. %s", e.getMessage()));
    }
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<NGGitOpsResponse> responseDataSupplier) throws Exception {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      NGGitOpsResponse response = responseDataSupplier.get();
      return calculateStepResponse(ambiance, response);
    }
  }
}
