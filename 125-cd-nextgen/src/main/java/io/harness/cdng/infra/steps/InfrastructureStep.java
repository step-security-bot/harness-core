/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.infra.steps;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LogCallbackUtils.saveExecutionLogSafely;

import static software.wings.beans.LogColor.Green;
import static software.wings.beans.LogColor.Red;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;

import static java.lang.String.format;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.customdeploymentng.CustomDeploymentInfrastructureHelper;
import io.harness.cdng.execution.ExecutionInfoKey;
import io.harness.cdng.execution.InfraExecutionSummaryDetails;
import io.harness.cdng.execution.InfraExecutionSummaryDetails.InfraExecutionSummaryDetailsBuilder;
import io.harness.cdng.execution.StageExecutionInfoUpdateDTO;
import io.harness.cdng.execution.helper.ExecutionInfoKeyMapper;
import io.harness.cdng.execution.helper.StageExecutionHelper;
import io.harness.cdng.execution.service.StageExecutionInfoService;
import io.harness.cdng.infra.InfrastructureOutcomeProvider;
import io.harness.cdng.infra.InfrastructureValidator;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sAwsInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sAzureInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sRancherInfrastructureOutcome;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.instance.InstanceOutcomeHelper;
import io.harness.cdng.instance.outcome.InstancesOutcome;
import io.harness.cdng.service.steps.ServiceStepOutcome;
import io.harness.cdng.ssh.output.HostsOutput;
import io.harness.cdng.ssh.output.SshInfraDelegateConfigOutput;
import io.harness.cdng.ssh.output.WinRmInfraDelegateConfigOutput;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.spotconnector.SpotConnectorDTO;
import io.harness.delegate.beans.connector.tasconnector.TasConnectorDTO;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.ssh.SshInfraDelegateConfig;
import io.harness.delegate.task.ssh.WinRmInfraDelegateConfig;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.logstreaming.NGLogCallback;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.EntityReferenceExtractorUtils;
import io.harness.steps.OutputExpressionConstants;
import io.harness.steps.StepUtils;
import io.harness.steps.environment.EnvironmentOutcome;
import io.harness.steps.executable.SyncExecutableWithRbac;
import io.harness.steps.shellscript.K8sInfraDelegateConfigOutput;
import io.harness.utils.NGFeatureFlagHelperService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@Slf4j
@OwnedBy(CDC)
public class InfrastructureStep implements SyncExecutableWithRbac<Infrastructure> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.INFRASTRUCTURE.getName())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Inject private InfrastructureStepHelper infrastructureStepHelper;
  @Inject @Named("PRIVILEGED") private AccessControlClient accessControlClient;
  @Inject private EntityReferenceExtractorUtils entityReferenceExtractorUtils;
  @Inject private PipelineRbacHelper pipelineRbacHelper;
  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;
  @Inject private OutcomeService outcomeService;
  @Inject private CDStepHelper cdStepHelper;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private StageExecutionHelper stageExecutionHelper;
  @Inject private InfrastructureValidator infrastructureValidator;
  @Inject private CustomDeploymentInfrastructureHelper customDeploymentInfrastructureHelper;
  @Inject private InstanceOutcomeHelper instanceOutcomeHelper;
  @Inject private InfrastructureOutcomeProvider infrastructureOutcomeProvider;
  @Inject private NGFeatureFlagHelperService ngFeatureFlagHelperService;
  @Inject private StageExecutionInfoService stageExecutionInfoService;

  @Override
  public Class<Infrastructure> getStepParametersClass() {
    return Infrastructure.class;
  }

  InfraMapping createInfraMappingObject(Infrastructure infrastructureSpec) {
    return infrastructureSpec.getInfraMapping();
  }

  @Override
  public StepResponse executeSyncAfterRbac(Ambiance ambiance, Infrastructure infrastructure,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    long startTime = System.currentTimeMillis();

    NGLogCallback logCallback = infrastructureStepHelper.getInfrastructureLogCallback(ambiance, true);
    saveExecutionLogSafely(logCallback, "Starting infrastructure step...");

    validateConnector(infrastructure, ambiance);

    saveExecutionLogSafely(logCallback, "Fetching environment information...");

    if (infrastructure.isDynamicallyProvisioned()) {
      infrastructureValidator.resolveProvisionerExpressions(ambiance, infrastructure);
    }
    NGLogCallback infraValidationLogCallback = infrastructureStepHelper.getInfrastructureLogCallback(ambiance);
    infrastructureValidator.validateInfrastructure(infrastructure, ambiance, infraValidationLogCallback);

    EnvironmentOutcome environmentOutcome = (EnvironmentOutcome) executionSweepingOutputService.resolve(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(OutputExpressionConstants.ENVIRONMENT));
    ServiceStepOutcome serviceOutcome = (ServiceStepOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);

    final InfrastructureOutcome infrastructureOutcome = infrastructureOutcomeProvider.getOutcome(ambiance,
        infrastructure, environmentOutcome, serviceOutcome, ngAccess.getAccountIdentifier(),
        ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier(), new HashMap<>());

    if (environmentOutcome != null) {
      if (isNotEmpty(environmentOutcome.getName())) {
        saveExecutionLogSafely(
            logCallback, color(format("Environment Name: %s", environmentOutcome.getName()), Yellow));
      }

      if (environmentOutcome.getType() != null && isNotEmpty(environmentOutcome.getType().name())) {
        saveExecutionLogSafely(
            logCallback, color(format("Environment Type: %s", environmentOutcome.getType().name()), Yellow));
      }
    }

    if (infrastructureOutcome != null && isNotEmpty(infrastructureOutcome.getKind())) {
      saveExecutionLogSafely(
          logCallback, color(format("Infrastructure Definition Type: %s", infrastructureOutcome.getKind()), Yellow));
    }

    saveExecutionLogSafely(logCallback, color("Environment information fetched", Green));
    boolean skipInstances = infrastructureStepHelper.getSkipInstances(infrastructure);

    Optional<InstancesOutcome> instancesOutcomeOpt = publishInfraDelegateConfigOutput(
        serviceOutcome, environmentOutcome, infrastructureOutcome, skipInstances, ambiance);

    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    instancesOutcomeOpt.ifPresent(instancesOutcome
        -> stepResponseBuilder.stepOutcome(StepOutcome.builder()
                                               .outcome(instancesOutcome)
                                               .name(OutcomeExpressionConstants.INSTANCES)
                                               .group(OutcomeExpressionConstants.INFRASTRUCTURE_GROUP)
                                               .build()));

    String infrastructureKind = infrastructure.getKind();
    ExecutionInfoKey executionInfoKey =
        ExecutionInfoKeyMapper.getExecutionInfoKey(ambiance, environmentOutcome, serviceOutcome, infrastructureOutcome);
    stageExecutionHelper.saveStageExecutionInfo(ambiance, executionInfoKey, infrastructureKind);
    stageExecutionHelper.addRollbackArtifactToStageOutcomeIfPresent(
        ambiance, stepResponseBuilder, executionInfoKey, infrastructureKind);

    if (logCallback != null) {
      logCallback.saveExecutionLog(
          color("Completed infrastructure step", Green), LogLevel.INFO, CommandExecutionStatus.SUCCESS);
    }
    saveInfraExecutionDataToStageInfo(ambiance, infrastructureOutcome);

    return stepResponseBuilder.status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder()
                         .outcome(infrastructureOutcome)
                         .name(OutcomeExpressionConstants.OUTPUT)
                         .group(OutcomeExpressionConstants.INFRASTRUCTURE_GROUP)
                         .build())
        .unitProgressList(Collections.singletonList(UnitProgress.newBuilder()
                                                        .setUnitName("Execute")
                                                        .setStatus(UnitStatus.SUCCESS)
                                                        .setStartTime(startTime)
                                                        .setEndTime(System.currentTimeMillis())
                                                        .build()))
        .build();
  }

  private Optional<InstancesOutcome> publishInfraDelegateConfigOutput(ServiceStepOutcome serviceOutcome,
      EnvironmentOutcome environmentOutcome, InfrastructureOutcome infrastructureOutcome, boolean skipInstances,
      Ambiance ambiance) {
    if (ServiceSpecType.SSH.equals(serviceOutcome.getType())) {
      ExecutionInfoKey executionInfoKey = ExecutionInfoKeyMapper.getExecutionInfoKey(
          ambiance, environmentOutcome, serviceOutcome, infrastructureOutcome);
      return Optional.ofNullable(
          publishSshInfraDelegateConfigOutput(infrastructureOutcome, skipInstances, ambiance, executionInfoKey));
    }

    if (ServiceSpecType.WINRM.equals(serviceOutcome.getType())) {
      ExecutionInfoKey executionInfoKey = ExecutionInfoKeyMapper.getExecutionInfoKey(
          ambiance, environmentOutcome, serviceOutcome, infrastructureOutcome);
      return Optional.ofNullable(
          publishWinRmInfraDelegateConfigOutput(infrastructureOutcome, skipInstances, ambiance, executionInfoKey));
    }

    if (infrastructureOutcome instanceof K8sGcpInfrastructureOutcome
        || infrastructureOutcome instanceof K8sDirectInfrastructureOutcome
        || infrastructureOutcome instanceof K8sAzureInfrastructureOutcome
        || infrastructureOutcome instanceof K8sAwsInfrastructureOutcome
        || infrastructureOutcome instanceof K8sRancherInfrastructureOutcome) {
      K8sInfraDelegateConfig k8sInfraDelegateConfig =
          cdStepHelper.getK8sInfraDelegateConfig(infrastructureOutcome, ambiance);

      K8sInfraDelegateConfigOutput k8sInfraDelegateConfigOutput =
          K8sInfraDelegateConfigOutput.builder().k8sInfraDelegateConfig(k8sInfraDelegateConfig).build();
      executionSweepingOutputService.consume(ambiance, OutputExpressionConstants.K8S_INFRA_DELEGATE_CONFIG_OUTPUT_NAME,
          k8sInfraDelegateConfigOutput, StepCategory.STAGE.name());
    }

    return Optional.empty();
  }

  private InstancesOutcome publishSshInfraDelegateConfigOutput(InfrastructureOutcome infrastructureOutcome,
      boolean skipInstances, Ambiance ambiance, ExecutionInfoKey executionInfoKey) {
    NGLogCallback logCallback = infrastructureStepHelper.getInfrastructureLogCallback(ambiance, false);
    SshInfraDelegateConfig sshInfraDelegateConfig =
        cdStepHelper.getSshInfraDelegateConfig(infrastructureOutcome, ambiance);

    SshInfraDelegateConfigOutput sshInfraDelegateConfigOutput =
        SshInfraDelegateConfigOutput.builder().sshInfraDelegateConfig(sshInfraDelegateConfig).build();
    executionSweepingOutputService.consume(ambiance, OutputExpressionConstants.SSH_INFRA_DELEGATE_CONFIG_OUTPUT_NAME,
        sshInfraDelegateConfigOutput, StepCategory.STAGE.name());
    Set<String> hosts = sshInfraDelegateConfig.getHosts();
    if (EmptyPredicate.isEmpty(hosts)) {
      saveExecutionLogSafely(logCallback,
          color("No host(s) were provided for specified infrastructure or filter did not match any instance(s)", Red));
    } else {
      saveExecutionLogSafely(logCallback, color(format("Successfully fetched %s instance(s)", hosts.size()), Green));
      saveExecutionLogSafely(logCallback, color(format("Fetched following instance(s) %s)", hosts), Green));
    }

    Set<String> filteredHosts = stageExecutionHelper.saveAndExcludeHostsWithSameArtifactDeployedIfNeeded(
        ambiance, executionInfoKey, infrastructureOutcome, hosts, ServiceSpecType.SSH, skipInstances, logCallback);
    executionSweepingOutputService.consume(ambiance, OutputExpressionConstants.OUTPUT,
        HostsOutput.builder().hosts(filteredHosts).build(), StepCategory.STAGE.name());
    return instanceOutcomeHelper.saveAndGetInstancesOutcome(ambiance, infrastructureOutcome, filteredHosts);
  }

  private InstancesOutcome publishWinRmInfraDelegateConfigOutput(InfrastructureOutcome infrastructureOutcome,
      boolean skipInstances, Ambiance ambiance, ExecutionInfoKey executionInfoKey) {
    NGLogCallback logCallback = infrastructureStepHelper.getInfrastructureLogCallback(ambiance, false);
    WinRmInfraDelegateConfig winRmInfraDelegateConfig =
        cdStepHelper.getWinRmInfraDelegateConfig(infrastructureOutcome, ambiance);

    WinRmInfraDelegateConfigOutput winRmInfraDelegateConfigOutput =
        WinRmInfraDelegateConfigOutput.builder().winRmInfraDelegateConfig(winRmInfraDelegateConfig).build();
    executionSweepingOutputService.consume(ambiance, OutputExpressionConstants.WINRM_INFRA_DELEGATE_CONFIG_OUTPUT_NAME,
        winRmInfraDelegateConfigOutput, StepCategory.STAGE.name());
    Set<String> hosts = winRmInfraDelegateConfig.getHosts();
    if (EmptyPredicate.isEmpty(hosts)) {
      saveExecutionLogSafely(logCallback,
          color("No host(s) were provided for specified infrastructure or filter did not match any instance(s)", Red));
    } else {
      saveExecutionLogSafely(logCallback, color(format("Successfully fetched %s instance(s)", hosts.size()), Green));
      saveExecutionLogSafely(logCallback, color(format("Fetched following instance(s) %s)", hosts), Green));
    }

    saveExecutionLogSafely(logCallback, color(format("Successfully fetched %s instance(s)", hosts.size()), Green));
    saveExecutionLogSafely(logCallback, color(format("Fetched following instance(s) [%s])", hosts), Green));

    Set<String> filteredHosts = stageExecutionHelper.saveAndExcludeHostsWithSameArtifactDeployedIfNeeded(
        ambiance, executionInfoKey, infrastructureOutcome, hosts, ServiceSpecType.WINRM, skipInstances, logCallback);
    executionSweepingOutputService.consume(ambiance, OutputExpressionConstants.OUTPUT,
        HostsOutput.builder().hosts(filteredHosts).build(), StepCategory.STAGE.name());
    return instanceOutcomeHelper.saveAndGetInstancesOutcome(ambiance, infrastructureOutcome, filteredHosts);
  }

  @VisibleForTesting
  void validateConnector(Infrastructure infrastructure, Ambiance ambiance) {
    NGLogCallback logCallback = infrastructureStepHelper.getInfrastructureLogCallback(ambiance);

    if (infrastructure == null) {
      return;
    }

    if (InfrastructureKind.PDC.equals(infrastructure.getKind())
        && ParameterField.isNull(infrastructure.getConnectorReference())) {
      return;
    }

    List<ConnectorInfoDTO> connectorInfo = infrastructureStepHelper.validateAndGetConnectors(
        infrastructure.getConnectorReferences(), ambiance, logCallback);
    if (InfrastructureKind.KUBERNETES_GCP.equals(infrastructure.getKind())) {
      if (!(connectorInfo.get(0).getConnectorConfig() instanceof GcpConnectorDTO)) {
        throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
            connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
            ConnectorType.GCP.name()));
      }
    }

    if (InfrastructureKind.SERVERLESS_AWS_LAMBDA.equals(infrastructure.getKind())) {
      if (!(connectorInfo.get(0).getConnectorConfig() instanceof AwsConnectorDTO)) {
        throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
            connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
            ConnectorType.AWS.name()));
      }
    }

    if (InfrastructureKind.KUBERNETES_AZURE.equals(infrastructure.getKind())
        && !(connectorInfo.get(0).getConnectorConfig() instanceof AzureConnectorDTO)) {
      throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
          connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
          ConnectorType.AZURE.name()));
    }

    if (InfrastructureKind.SSH_WINRM_AZURE.equals(infrastructure.getKind())
        && !(connectorInfo.get(0).getConnectorConfig() instanceof AzureConnectorDTO)) {
      throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
          connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
          ConnectorType.AZURE.name()));
    }

    if (InfrastructureKind.AZURE_WEB_APP.equals(infrastructure.getKind())
        && !(connectorInfo.get(0).getConnectorConfig() instanceof AzureConnectorDTO)) {
      throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
          connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
          ConnectorType.AZURE.name()));
    }

    if (InfrastructureKind.ECS.equals(infrastructure.getKind())) {
      if (!(connectorInfo.get(0).getConnectorConfig() instanceof AwsConnectorDTO)) {
        throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
            connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
            ConnectorType.AWS.name()));
      }
    }

    if (InfrastructureKind.GOOGLE_CLOUD_FUNCTIONS.equals(infrastructure.getKind())) {
      if (!(connectorInfo.get(0).getConnectorConfig() instanceof GcpConnectorDTO)) {
        throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
            connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
            ConnectorType.GCP.name()));
      }
    }

    if (InfrastructureKind.ELASTIGROUP.equals(infrastructure.getKind())) {
      if (!(connectorInfo.get(0).getConnectorConfig() instanceof SpotConnectorDTO)) {
        throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
            connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
            ConnectorType.SPOT.name()));
      }
    }

    if (InfrastructureKind.TAS.equals(infrastructure.getKind())) {
      if (!(connectorInfo.get(0).getConnectorConfig() instanceof TasConnectorDTO)) {
        throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
            connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
            ConnectorType.TAS.name()));
      }
    }

    if (InfrastructureKind.ASG.equals(infrastructure.getKind())) {
      if (!(connectorInfo.get(0).getConnectorConfig() instanceof AwsConnectorDTO)) {
        throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
            connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
            ConnectorType.AWS.name()));
      }
    }

    if (InfrastructureKind.AWS_SAM.equals(infrastructure.getKind())) {
      if (!(connectorInfo.get(0).getConnectorConfig() instanceof AwsConnectorDTO)) {
        throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
            connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
            ConnectorType.AWS.name()));
      }
    }

    if (InfrastructureKind.AWS_LAMBDA.equals(infrastructure.getKind())) {
      if (!(connectorInfo.get(0).getConnectorConfig() instanceof AwsConnectorDTO)) {
        throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
            connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
            ConnectorType.AWS.name()));
      }
    }

    if (InfrastructureKind.KUBERNETES_AWS.equals(infrastructure.getKind())) {
      if (!(connectorInfo.get(0).getConnectorConfig() instanceof AwsConnectorDTO)) {
        throw new InvalidRequestException(format("Invalid connector type [%s] for identifier: [%s], expected [%s]",
            connectorInfo.get(0).getConnectorType().name(), infrastructure.getConnectorReference().getValue(),
            ConnectorType.AWS.name()));
      }
    }

    saveExecutionLogSafely(logCallback, color("Connector validated", Green));
  }

  @Override
  public void validateResources(Ambiance ambiance, Infrastructure infrastructure) {
    ExecutionPrincipalInfo executionPrincipalInfo = ambiance.getMetadata().getPrincipalInfo();
    String principal = executionPrincipalInfo.getPrincipal();
    if (isEmpty(principal)) {
      return;
    }
    Set<EntityDetailProtoDTO> entityDetails =
        entityReferenceExtractorUtils.extractReferredEntities(ambiance, infrastructure);
    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetails);
  }

  @Override
  public List<String> getLogKeys(Ambiance ambiance) {
    return StepUtils.generateLogKeys(ambiance, null);
  }

  public void saveInfraExecutionDataToStageInfo(Ambiance ambiance, InfrastructureOutcome infrastructureOutcome) {
    stageExecutionInfoService.updateStageExecutionInfo(ambiance,
        StageExecutionInfoUpdateDTO.builder()
            .infraExecutionSummary(createInfraExecutionSummaryDetailsFromInfraOutcome(infrastructureOutcome))
            .build());
  }

  protected InfraExecutionSummaryDetails createInfraExecutionSummaryDetailsFromInfraOutcome(
      InfrastructureOutcome infrastructureOutcome) {
    InfraExecutionSummaryDetailsBuilder infraExecutionSummaryDetailsBuilder =
        InfraExecutionSummaryDetails.builder()
            .infrastructureIdentifier(infrastructureOutcome.getInfraIdentifier())
            .infrastructureName(infrastructureOutcome.getInfraName())
            .connectorRef(infrastructureOutcome.getConnectorRef());
    if (infrastructureOutcome.getEnvironment() != null) {
      infraExecutionSummaryDetailsBuilder.identifier(infrastructureOutcome.getEnvironment().getIdentifier())
          .name(infrastructureOutcome.getEnvironment().getName())
          .type(infrastructureOutcome.getEnvironment().getType().name())
          .envGroupId(infrastructureOutcome.getEnvironment().getEnvGroupRef())
          .envGroupName(infrastructureOutcome.getEnvironment().getEnvGroupName());
    }
    return infraExecutionSummaryDetailsBuilder.build();
  }
}
