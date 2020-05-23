package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdvisingEvent;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.delay.DelayEventHelper;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.advise.AdviseHandlerFactory;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.ExecutableInvokerFactory;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.resume.EngineResumeExecutor;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.engine.services.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.ExecutionInstanceStatus;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;
import io.harness.plan.input.InputSet;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StepRegistry;
import io.harness.resolvers.Resolver;
import io.harness.state.Step;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Slf4j
@Redesign
@OwnedBy(CDC)
public class ExecutionEngine implements Engine {
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Injector injector;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private StepRegistry stepRegistry;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private ResolverRegistry resolverRegistry;
  @Inject private AmbianceHelper ambianceHelper;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private EngineStatusHelper engineStatusHelper;
  @Inject private ExecutableInvokerFactory executableInvokerFactory;
  @Inject private AdviseHandlerFactory adviseHandlerFactory;
  @Inject private DelayEventHelper delayEventHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private EngineExpressionService engineExpressionService;

  public PlanExecution startExecution(@Valid Plan plan, EmbeddedUser createdBy) {
    return startExecution(plan, null, createdBy);
  }

  public PlanExecution startExecution(@Valid Plan plan, InputSet inputSet, EmbeddedUser createdBy) {
    PlanExecution instance = PlanExecution.builder()
                                 .uuid(generateUuid())
                                 .plan(plan)
                                 .inputSet(inputSet)
                                 .status(ExecutionInstanceStatus.RUNNING)
                                 .createdBy(createdBy)
                                 .startTs(System.currentTimeMillis())
                                 .build();
    hPersistence.save(instance);
    ExecutionNode executionNode = plan.fetchStartingNode();
    if (executionNode == null) {
      logger.warn("Cannot Start Execution for empty plan");
      return null;
    }
    Ambiance ambiance =
        Ambiance.builder().setupAbstractions(plan.getSetupAbstractions()).planExecutionId(instance.getUuid()).build();
    triggerExecution(ambiance, plan.fetchStartingNode());
    return instance;
  }

  public void startNodeExecution(Ambiance ambiance) {
    // Update to Running Status
    NodeExecution nodeExecution = hPersistence.createQuery(NodeExecution.class)
                                      .filter(NodeExecutionKeys.uuid, ambiance.obtainCurrentRuntimeId())
                                      .get();
    ExecutionNode node = nodeExecution.getNode();
    // Facilitate and execute
    List<StepTransput> inputs =
        engineObtainmentHelper.obtainInputs(ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
    StepParameters resolvedStepParameters =
        (StepParameters) engineExpressionService.resolve(ambiance, node.getStepParameters());
    nodeExecutionService.updateResolvedStepParameters(nodeExecution.getUuid(), resolvedStepParameters);
    facilitateExecution(ambiance, nodeExecution, inputs);
  }

  public void triggerExecution(Ambiance ambiance, ExecutionNode node) {
    String uuid = generateUuid();
    NodeExecution previousNodeExecution = null;
    if (ambiance.obtainCurrentRuntimeId() != null) {
      previousNodeExecution = engineStatusHelper.updateNodeInstance(
          ambiance.obtainCurrentRuntimeId(), ops -> ops.set(NodeExecutionKeys.nextId, uuid));
    }

    Ambiance cloned = reBuildAmbiance(ambiance, node, uuid);

    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(uuid)
            .ambiance(cloned)
            .node(node)
            .startTs(System.currentTimeMillis())
            .status(NodeExecutionStatus.QUEUED)
            .notifyId(previousNodeExecution == null ? null : previousNodeExecution.getNotifyId())
            .parentId(previousNodeExecution == null ? null : previousNodeExecution.getParentId())
            .previousId(previousNodeExecution == null ? null : previousNodeExecution.getUuid())
            .build();
    hPersistence.save(nodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(cloned).executionEngine(this).build());
  }

  private Ambiance reBuildAmbiance(Ambiance ambiance, ExecutionNode node, String uuid) {
    Ambiance cloned = ambiance.obtainCurrentRuntimeId() == null ? ambiance : ambiance.cloneForFinish();

    cloned.addLevel(Level.builder().setupId(node.getUuid()).runtimeId(uuid).identifier(node.getIdentifier()).build());
    return cloned;
  }

  private void facilitateExecution(Ambiance ambiance, NodeExecution nodeExecution, List<StepTransput> inputs) {
    ExecutionNode node = nodeExecution.getNode();
    FacilitatorResponse facilitatorResponse = null;
    for (FacilitatorObtainment obtainment : node.getFacilitatorObtainments()) {
      Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
      injector.injectMembers(facilitator);
      facilitatorResponse = facilitator.facilitate(
          ambiance, nodeExecution.getResolvedStepParameters(), obtainment.getParameters(), inputs);
      if (facilitatorResponse != null) {
        break;
      }
    }
    Preconditions.checkNotNull(facilitatorResponse,
        "No execution mode detected for State. Name: " + node.getName() + "Type : " + node.getStepType());
    ExecutionMode mode = facilitatorResponse.getExecutionMode();
    Consumer<UpdateOperations<NodeExecution>> ops = op -> op.set(NodeExecutionKeys.mode, mode);
    if (facilitatorResponse.getInitialWait() != null && facilitatorResponse.getInitialWait().getSeconds() != 0) {
      FacilitatorResponse finalFacilitatorResponse = facilitatorResponse;
      Preconditions.checkNotNull(engineStatusHelper.updateNodeInstance(ambiance.obtainCurrentRuntimeId(),
          ops.andThen(op
              -> op.set(NodeExecutionKeys.status, NodeExecutionStatus.TIMED_WAITING)
                     .set(NodeExecutionKeys.initialWaitDuration, finalFacilitatorResponse.getInitialWait()))));
      String resumeId =
          delayEventHelper.delay(finalFacilitatorResponse.getInitialWait().getSeconds(), Collections.emptyMap());
      waitNotifyEngine.waitForAllOn(ORCHESTRATION,
          EngineWaitResumeCallback.builder().ambiance(ambiance).facilitatorResponse(finalFacilitatorResponse).build(),
          resumeId);
    } else {
      Preconditions.checkNotNull(engineStatusHelper.updateNodeInstance(ambiance.obtainCurrentRuntimeId(),
          ops.andThen(op -> op.set(NodeExecutionKeys.status, NodeExecutionStatus.RUNNING))));
      invokeState(ambiance, facilitatorResponse);
    }
  }

  public void invokeState(Ambiance ambiance, FacilitatorResponse facilitatorResponse) {
    NodeExecution nodeExecution = Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(ambiance));
    ExecutionNode node = nodeExecution.getNode();
    Step currentStep = stepRegistry.obtain(node.getStepType());
    List<StepTransput> inputs =
        engineObtainmentHelper.obtainInputs(ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
    ExecutableInvoker invoker = executableInvokerFactory.obtainInvoker(facilitatorResponse.getExecutionMode());
    invoker.invokeExecutable(InvokerPackage.builder()
                                 .step(currentStep)
                                 .ambiance(ambiance)
                                 .inputs(inputs)
                                 .parameters(nodeExecution.getResolvedStepParameters())
                                 .passThroughData(facilitatorResponse.getPassThroughData())
                                 .build());
  }

  public void handleStepResponse(@NotNull String nodeExecutionId, StepResponse stepResponse) {
    NodeExecution nodeExecution = engineStatusHelper.updateNodeInstance(nodeExecutionId,
        ops
        -> ops.set(NodeExecutionKeys.status, stepResponse.getStatus())
               .set(NodeExecutionKeys.endTs, System.currentTimeMillis()));
    // TODO => handle before node execution update
    handleOutcomes(nodeExecution.getAmbiance(), stepResponse.getOutcomes());

    // TODO handle Failure
    ExecutionNode node = nodeExecution.getNode();
    if (isEmpty(node.getAdviserObtainments())) {
      endTransition(nodeExecution);
      return;
    }
    Advise advise = null;
    for (AdviserObtainment obtainment : node.getAdviserObtainments()) {
      Adviser adviser = adviserRegistry.obtain(obtainment.getType());
      injector.injectMembers(adviser);
      advise = adviser.onAdviseEvent(AdvisingEvent.builder()
                                         .ambiance(nodeExecution.getAmbiance())
                                         .stepResponse(stepResponse)
                                         .adviserParameters(obtainment.getParameters())
                                         .build());
      if (advise != null) {
        break;
      }
    }
    if (advise == null) {
      endTransition(nodeExecution);
      return;
    }
    handleAdvise(nodeExecution, advise);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void handleOutcomes(Ambiance ambiance, Map<String, StepTransput> outcomes) {
    if (outcomes == null) {
      return;
    }
    outcomes.forEach((name, outcome) -> {
      Resolver resolver = resolverRegistry.obtain(outcome.getRefType());
      resolver.consume(ambiance, name, outcome);
    });
  }

  private void endTransition(NodeExecution nodeInstance) {
    if (isNotEmpty(nodeInstance.getNotifyId())) {
      StatusNotifyResponseData responseData =
          StatusNotifyResponseData.builder().status(NodeExecutionStatus.SUCCEEDED).build();
      waitNotifyEngine.doneWith(nodeInstance.getNotifyId(), responseData);
    } else {
      logger.info("End Execution");
      engineStatusHelper.updateExecutionInstanceStatus(
          nodeInstance.getAmbiance().getPlanExecutionId(), ExecutionInstanceStatus.SUCCEEDED);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void handleAdvise(@NotNull NodeExecution nodeExecution, @NotNull Advise advise) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    AdviseHandler adviseHandler = adviseHandlerFactory.obtainHandler(advise.getType());
    adviseHandler.handleAdvise(ambiance, advise);
  }

  public void resume(String nodeInstanceId, Map<String, ResponseData> response, boolean asyncError) {
    NodeExecution nodeExecution = engineStatusHelper.updateNodeInstance(
        nodeInstanceId, ops -> ops.set(NodeExecutionKeys.status, NodeExecutionStatus.RUNNING));
    if (nodeExecution.getStatus() != NodeExecutionStatus.RUNNING) {
      logger.warn("nodeInstance: {} status {} is no longer in RUNNING state", nodeExecution.getUuid(),
          nodeExecution.getStatus());
      return;
    }
    executorService.execute(EngineResumeExecutor.builder()
                                .nodeExecution(nodeExecution)
                                .response(response)
                                .asyncError(asyncError)
                                .executionEngine(this)
                                .stepRegistry(stepRegistry)
                                .injector(injector)
                                .build());
  }
}