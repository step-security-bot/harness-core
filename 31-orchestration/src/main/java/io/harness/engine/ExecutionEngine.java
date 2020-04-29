package io.harness.engine;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdvisingEvent;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.LevelExecution;
import io.harness.annotations.Redesign;
import io.harness.beans.EmbeddedUser;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.advise.AdviseHandlerFactory;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.ExecutableInvokerFactory;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeExecutor;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorObtainment;
import io.harness.facilitate.FacilitatorResponse;
import io.harness.facilitate.modes.ExecutionMode;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.level.LevelRegistry;
import io.harness.registries.state.StateRegistry;
import io.harness.state.State;
import io.harness.state.execution.NodeExecution;
import io.harness.state.execution.NodeExecution.NodeExecutionKeys;
import io.harness.state.execution.PlanExecution;
import io.harness.state.execution.status.ExecutionInstanceStatus;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Slf4j
@Redesign
public class ExecutionEngine implements Engine {
  // For database needs
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  // For leveraging the wait notify engine
  @Inject private WaitNotifyEngine waitNotifyEngine;
  // Guice Injector
  @Inject private Injector injector;
  // ExecutorService for the engine
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  // Registries
  @Inject private StateRegistry stateRegistry;
  @Inject private LevelRegistry levelRegistry;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private FacilitatorRegistry facilitatorRegistry;

  // Helpers

  // For obtaining ambiance related information
  @Inject private AmbianceHelper ambianceHelper;

  // Obtain concrete entities from obtainments
  // States | Advisers | Facilitators | Inputs
  @Inject private EngineObtainmentHelper engineObtainmentHelper;

  // Helper methods for status updates and related methods
  @Inject private EngineStatusHelper engineStatusHelper;

  // Obtain appropriate invoker
  @Inject private ExecutableInvokerFactory executableInvokerFactory;

  // Obtain appropriate advise Handler
  @Inject private AdviseHandlerFactory adviseHandlerFactory;

  public PlanExecution startExecution(@Valid Plan plan, EmbeddedUser createdBy) {
    PlanExecution instance = PlanExecution.builder()
                                 .uuid(generateUuid())
                                 .plan(plan)
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
    Ambiance ambiance = Ambiance.builder()
                            .setupAbstractions(plan.getSetupAbstractions())
                            .executionInstanceId(instance.getUuid())
                            .build();
    triggerExecution(ambiance, plan.fetchStartingNode());
    return instance;
  }

  public void startNodeExecution(Ambiance ambiance) {
    startNodeInstance(ambiance, ambiance.obtainCurrentRuntimeId());
  }

  public void triggerExecution(Ambiance ambiance, ExecutionNode node) {
    Ambiance cloned = ambiance.obtainCurrentRuntimeId() == null
        ? ambiance
        : ambiance.cloneForFinish(levelRegistry.obtain(node.getLevelType()));
    String uuid = generateUuid();
    NodeExecution previousNodeExecution = null;
    if (ambiance.obtainCurrentRuntimeId() != null) {
      previousNodeExecution = engineStatusHelper.updateNodeInstance(
          ambiance.obtainCurrentRuntimeId(), ops -> ops.set(NodeExecutionKeys.nextId, uuid));
    }
    cloned.addLevelExecution(LevelExecution.builder()
                                 .setupId(node.getUuid())
                                 .runtimeId(uuid)
                                 .level(levelRegistry.obtain(node.getLevelType()))
                                 .build());

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

  public void startNodeInstance(Ambiance ambiance, @NotNull String nodeExecutionId) {
    // Update to Running Status
    NodeExecution nodeExecution = Preconditions.checkNotNull(
        hPersistence.createQuery(NodeExecution.class).filter(NodeExecutionKeys.uuid, nodeExecutionId).get());

    ExecutionNode node = nodeExecution.getNode();
    // Audit and execute
    List<StateTransput> inputs =
        engineObtainmentHelper.obtainInputs(ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
    FacilitatorResponse facilitatorResponse = null;
    for (FacilitatorObtainment obtainment : node.getFacilitatorObtainments()) {
      Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
      facilitatorResponse = facilitator.facilitate(ambiance, obtainment.getParameters(), inputs);
      if (facilitatorResponse != null) {
        break;
      }
    }
    Preconditions.checkNotNull(facilitatorResponse,
        "No execution mode detected for State. Name: " + node.getName() + "Type : " + node.getStateType());
    ExecutionMode mode = facilitatorResponse.getExecutionMode();

    NodeExecution updatedNodeExecution =
        Preconditions.checkNotNull(engineStatusHelper.updateNodeInstance(nodeExecutionId,
            ops -> ops.set(NodeExecutionKeys.mode, mode).set(NodeExecutionKeys.status, NodeExecutionStatus.RUNNING)));

    invokeState(ambiance, facilitatorResponse, updatedNodeExecution);
  }

  private void invokeState(Ambiance ambiance, FacilitatorResponse facilitatorResponse, NodeExecution nodeExecution) {
    ExecutionNode node = nodeExecution.getNode();
    State currentState = Preconditions.checkNotNull(
        stateRegistry.obtain(node.getStateType()), "Cannot find state for state type: " + node.getStateType());
    injector.injectMembers(currentState);
    List<StateTransput> inputs =
        engineObtainmentHelper.obtainInputs(ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
    ExecutableInvoker invoker = executableInvokerFactory.obtainInvoker(facilitatorResponse.getExecutionMode());
    invoker.invokeExecutable(InvokerPackage.builder()
                                 .state(currentState)
                                 .ambiance(ambiance)
                                 .inputs(inputs)
                                 .parameters(node.getStateParameters())
                                 .passThroughData(facilitatorResponse.getPassThroughData())
                                 .build());
  }

  public void handleStateResponse(@NotNull String nodeExecutionId, StateResponse stateResponse) {
    NodeExecution nodeExecution = engineStatusHelper.updateNodeInstance(nodeExecutionId,
        ops
        -> ops.set(NodeExecutionKeys.status, stateResponse.getStatus())
               .set(NodeExecutionKeys.endTs, System.currentTimeMillis()));

    // TODO handle Failure
    ExecutionNode node = nodeExecution.getNode();
    if (isEmpty(node.getAdviserObtainments())) {
      endTransition(nodeExecution);
      return;
    }
    Advise advise = null;
    for (AdviserObtainment obtainment : node.getAdviserObtainments()) {
      Adviser adviser = adviserRegistry.obtain(obtainment.getType());
      advise = adviser.onAdviseEvent(
          AdvisingEvent.builder().stateResponse(stateResponse).adviserParameters(obtainment.getParameters()).build());
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

  private void endTransition(NodeExecution nodeInstance) {
    if (isNotEmpty(nodeInstance.getNotifyId())) {
      StatusNotifyResponseData responseData =
          StatusNotifyResponseData.builder().status(NodeExecutionStatus.SUCCEEDED).build();
      waitNotifyEngine.doneWith(nodeInstance.getNotifyId(), responseData);
    } else {
      logger.info("End Execution");
      engineStatusHelper.updateExecutionInstanceStatus(
          nodeInstance.getAmbiance().getExecutionInstanceId(), ExecutionInstanceStatus.SUCCEEDED);
    }
  }

  private void handleAdvise(@NotNull NodeExecution nodeExecution, @NotNull Advise advise) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    AdviseHandler adviseHandler = adviseHandlerFactory.obtainHandler(advise.getType());
    adviseHandler.handleAdvise(ambiance, advise);
  }

  public void resume(String nodeInstanceId, Map<String, ResponseData> response, boolean asyncError) {
    NodeExecution nodeExecution = engineStatusHelper.updateNodeInstance(
        nodeInstanceId, ops -> ops.set(NodeExecutionKeys.status, NodeExecutionStatus.RUNNING));

    ExecutionNode node = nodeExecution.getNode();
    State currentState = stateRegistry.obtain(node.getStateType());
    injector.injectMembers(currentState);
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
                                .stateRegistry(stateRegistry)
                                .injector(injector)
                                .build());
  }
}