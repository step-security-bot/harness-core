/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.asg;

import static io.harness.exception.WingsException.USER;

import static software.wings.beans.TaskType.AWS_ASG_BLUE_GREEN_SWAP_SERVICE_TASK_NG;

import io.harness.account.services.AccountService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.executables.CdTaskExecutable;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.task.aws.asg.AsgBlueGreenSwapServiceRequest;
import io.harness.delegate.task.aws.asg.AsgBlueGreenSwapServiceResponse;
import io.harness.delegate.task.aws.asg.AsgBlueGreenSwapServiceResult;
import io.harness.delegate.task.aws.asg.AsgCommandResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.SkipTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class AsgBlueGreenSwapServiceStep extends CdTaskExecutable<AsgCommandResponse> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.ASG_BLUE_GREEN_SWAP_SERVICE.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  public static final String ASG_BLUE_GREEN_SWAP_SERVICE_COMMAND_NAME = "AsgBlueGreenSwapService";
  public static final String ASG_BLUE_GREEN_DEPLOY_STEP_MISSING = "Blue Green Deploy step is not configured.";

  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private OutcomeService outcomeService;
  @Inject private AsgStepCommonHelper asgStepCommonHelper;
  @Inject private AccountService accountService;
  @Inject private StepHelper stepHelper;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // Nothing to validate
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<AsgCommandResponse> responseDataSupplier) throws Exception {
    StepResponse stepResponse = null;
    try {
      AsgBlueGreenSwapServiceResponse asgBlueGreenSwapServiceResponse =
          (AsgBlueGreenSwapServiceResponse) responseDataSupplier.get();

      StepResponseBuilder stepResponseBuilder = StepResponse.builder().unitProgressList(
          asgBlueGreenSwapServiceResponse.getUnitProgressData().getUnitProgresses());

      stepResponse = generateStepResponse(ambiance, asgBlueGreenSwapServiceResponse, stepResponseBuilder);
    } catch (Exception e) {
      log.error("Error while processing asg blue green swap service response: {}", ExceptionUtils.getMessage(e), e);
      throw e;
    }
    return stepResponse;
  }

  private StepResponse generateStepResponse(Ambiance ambiance,
      AsgBlueGreenSwapServiceResponse asgBlueGreenSwapServiceResponse, StepResponseBuilder stepResponseBuilder) {
    StepResponse stepResponse;

    if (asgBlueGreenSwapServiceResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      stepResponse =
          stepResponseBuilder.status(Status.FAILED)
              .failureInfo(FailureInfo.newBuilder()
                               .setErrorMessage(asgStepCommonHelper.getErrorMessage(asgBlueGreenSwapServiceResponse))
                               .build())
              .build();
    } else {
      AsgBlueGreenSwapServiceResult asgBlueGreenSwapServiceResult =
          asgBlueGreenSwapServiceResponse.getAsgBlueGreenSwapServiceResult();

      AsgBlueGreenSwapServiceOutcome asgBlueGreenSwapServiceOutcome =
          AsgBlueGreenSwapServiceOutcome.builder()
              .loadBalancer(asgBlueGreenSwapServiceResult.getLoadBalancer())
              .prodListenerArn(asgBlueGreenSwapServiceResult.getProdListenerArn())
              .prodListenerRuleArn(asgBlueGreenSwapServiceResult.getProdListenerRuleArn())
              .prodTargetGroupArnList(asgBlueGreenSwapServiceResult.getProdTargetGroupArnList())
              .stageListenerArn(asgBlueGreenSwapServiceResult.getStageListenerArn())
              .stageListenerRuleArn(asgBlueGreenSwapServiceResult.getStageListenerRuleArn())
              .stageTargetGroupArnList(asgBlueGreenSwapServiceResult.getStageTargetGroupArnList())
              .build();

      executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.ASG_BLUE_GREEN_SWAP_SERVICE_OUTCOME,
          asgBlueGreenSwapServiceOutcome, StepOutcomeGroup.STEP.name());

      stepResponse = stepResponseBuilder.status(Status.SUCCEEDED)
                         .stepOutcome(StepResponse.StepOutcome.builder()
                                          .name(OutcomeExpressionConstants.OUTPUT)
                                          .outcome(asgBlueGreenSwapServiceOutcome)
                                          .build())
                         .build();
    }
    return stepResponse;
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    final String accountId = AmbianceUtils.getAccountId(ambiance);

    AsgBlueGreenSwapServiceStepParameters asgBlueGreenSwapServiceStepParameters =
        (AsgBlueGreenSwapServiceStepParameters) stepParameters.getSpec();

    if (EmptyPredicate.isEmpty(asgBlueGreenSwapServiceStepParameters.asgBlueGreenDeployFqn)) {
      throw new InvalidRequestException(ASG_BLUE_GREEN_DEPLOY_STEP_MISSING, USER);
    }

    OptionalSweepingOutput asgBlueGreenPrepareRollbackDataOptional =
        executionSweepingOutputService.resolveOptional(ambiance,
            RefObjectUtils.getSweepingOutputRefObject(asgBlueGreenSwapServiceStepParameters.getAsgBlueGreenDeployFqn()
                + "." + OutcomeExpressionConstants.ASG_BLUE_GREEN_PREPARE_ROLLBACK_DATA_OUTCOME));

    OptionalSweepingOutput asgBlueGreenDeployDataOptional = executionSweepingOutputService.resolveOptional(ambiance,
        RefObjectUtils.getSweepingOutputRefObject(asgBlueGreenSwapServiceStepParameters.getAsgBlueGreenDeployFqn() + "."
            + OutcomeExpressionConstants.ASG_BLUE_GREEN_DEPLOY_OUTCOME));

    if (!asgBlueGreenPrepareRollbackDataOptional.isFound() || !asgBlueGreenDeployDataOptional.isFound()) {
      throw new InvalidRequestException(ASG_BLUE_GREEN_DEPLOY_STEP_MISSING, USER);
    }
    /*
        AsgBlueGreenPrepareRollbackDataOutcome asgBlueGreenPrepareRollbackDataOutcome =
            (AsgBlueGreenPrepareRollbackDataOutcome) asgBlueGreenPrepareRollbackDataOptional.getOutput();

        AsgBlueGreenDeployDataOutcome asgBlueGreenDeployDataOutcome =
            (AsgBlueGreenDeployDataOutcome) asgBlueGreenDeployDataOptional.getOutput();
    */
    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
    /*
        AsgLoadBalancerConfig asgLoadBalancerConfig =
            AsgLoadBalancerConfig.builder()
                .loadBalancer(asgBlueGreenPrepareRollbackDataOutcome.getLoadBalancer())
                .prodListenerArn(asgBlueGreenPrepareRollbackDataOutcome.getProdListenerArn())
                .prodListenerRuleArn(asgBlueGreenPrepareRollbackDataOutcome.getProdListenerRuleArn())
                .prodTargetGroupArn(asgBlueGreenPrepareRollbackDataOutcome.getProdTargetGroupArn())
                .stageListenerArn(asgBlueGreenPrepareRollbackDataOutcome.getStageListenerArn())
                .stageListenerRuleArn(asgBlueGreenPrepareRollbackDataOutcome.getStageListenerRuleArn())
                .stageTargetGroupArn(asgBlueGreenPrepareRollbackDataOutcome.getStageTargetGroupArn())
                .build();
    */
    AsgBlueGreenSwapServiceRequest asgBlueGreenSwapServiceRequest =
        AsgBlueGreenSwapServiceRequest.builder()
            .accountId(accountId)
            .commandName(ASG_BLUE_GREEN_SWAP_SERVICE_COMMAND_NAME)
            .commandUnitsProgress(CommandUnitsProgress.builder().build())
            .asgInfraConfig(asgStepCommonHelper.getAsgInfraConfig(infrastructureOutcome, ambiance))
            .timeoutIntervalInMin(CDStepHelper.getTimeoutInMin(stepParameters))
            //.asgLoadBalancerConfig(asgLoadBalancerConfig)
            //.oldServiceName(asgBlueGreenPrepareRollbackDataOutcome.getServiceName())
            //.newServiceName(asgBlueGreenDeployDataOutcome.getServiceName())
            .downsizeOldAsg(asgBlueGreenSwapServiceStepParameters.getDownsizeOldAsg().getValue() != null
                && asgBlueGreenSwapServiceStepParameters.getDownsizeOldAsg().getValue())
            .build();

    return asgStepCommonHelper
        .queueAsgTask(stepParameters, asgBlueGreenSwapServiceRequest, ambiance,
            AsgExecutionPassThroughData.builder().infrastructure(infrastructureOutcome).build(), true,
            AWS_ASG_BLUE_GREEN_SWAP_SERVICE_TASK_NG)
        .getTaskRequest();
  }

  private TaskRequest skipTaskRequest(Ambiance ambiance, String message) {
    return TaskRequest.newBuilder()
        .setSkipTaskRequest(SkipTaskRequest.newBuilder().setMessage(message).build())
        .build();
  }
}
