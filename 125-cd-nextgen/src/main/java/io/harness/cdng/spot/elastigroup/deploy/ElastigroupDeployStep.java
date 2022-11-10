/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.spot.elastigroup.deploy;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.spot.elastigroup.deploy.ElastigroupDeployTaskParameters;
import io.harness.delegate.task.spot.elastigroup.deploy.ElastigroupDeployTaskResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class ElastigroupDeployStep extends TaskExecutableWithRollbackAndRbac<ElastigroupDeployTaskResponse> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.ELASTIGROUP_DEPLOY.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  public static final String UNIT_NAME = "Execute";

  @Inject private ElastigroupDeployStepHelper stepHelper;

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // Noop
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    ElastigroupDeployStepParameters elastigroupDeployStepParameters =
        (ElastigroupDeployStepParameters) stepParameters.getSpec();
    validateStepParameters(elastigroupDeployStepParameters);

    ElastigroupDeployTaskParameters taskParameters =
        stepHelper.getElastigroupDeployTaskParameters(elastigroupDeployStepParameters, ambiance, stepParameters);

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.ELASTIGROUP_DEPLOY.name())
            .parameters(new Object[] {taskParameters})
            .timeout(StepUtils.getTimeoutMillis(stepParameters.getTimeout(), StepUtils.DEFAULT_STEP_TIMEOUT))
            .build();

    return stepHelper.prepareTaskRequest(ambiance, taskData,
        Arrays.asList(UP_SCALE_COMMAND_UNIT, UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT, DOWN_SCALE_COMMAND_UNIT,
            DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT),
        TaskType.ELASTIGROUP_DEPLOY.getDisplayName(),
        TaskSelectorYaml.toTaskSelector(
            emptyIfNull(getParameterFieldValue(elastigroupDeployStepParameters.getDelegateSelectors()))));
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<ElastigroupDeployTaskResponse> responseDataSupplier) throws Exception {
    ElastigroupDeployTaskResponse taskResponse;
    try {
      taskResponse = responseDataSupplier.get();
    } catch (Exception ex) {
      return stepHelper.handleTaskFailure(ambiance, stepParameters, ex);
    }

    if (taskResponse == null) {
      return stepHelper.handleTaskFailure(ambiance, stepParameters,
          new InvalidArgumentsException("Failed to process elastigroup deploy task response"));
    }

    return stepHelper.handleTaskResult(ambiance, stepParameters, taskResponse);
  }

  private void validateStepParameters(ElastigroupDeployStepParameters elastigroupDeployStepParameters) {}
}
