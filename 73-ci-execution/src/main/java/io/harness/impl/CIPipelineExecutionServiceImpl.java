package io.harness.impl;

import com.google.inject.Inject;

import io.harness.beans.CIPipeline;
import io.harness.beans.EmbeddedUser;
import io.harness.engine.ExecutionEngine;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.BasicExecutionPlanGenerator;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.plan.Plan;
import io.harness.plan.input.InputArgs;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CIPipelineExecutionServiceImpl implements CIPipelineExecutionService {
  @Inject private ExecutionEngine engine;
  @Inject private BasicExecutionPlanGenerator planGenerator;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;

  public PlanExecution executePipeline(CIPipeline ciPipeline) {
    InputArgs inputArgs = InputArgs.builder().build();

    Plan plan = executionPlanCreatorService.createPlanForPipeline(ciPipeline, ciPipeline.getAccountId());
    // TODO set user before execution which will be available once we build authentication
    // User user = UserThreadLocal.get()
    return engine.startExecution(plan, inputArgs,
        EmbeddedUser.builder().uuid("harsh").email("harsh.jain@harness.io").name("harsh jain").build());
  }
}
