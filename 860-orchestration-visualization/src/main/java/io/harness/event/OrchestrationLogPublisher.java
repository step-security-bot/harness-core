package io.harness.event;

import io.harness.beans.OrchestrationEventLog;
import io.harness.engine.observers.NodeStatusUpdateObserver;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.engine.observers.NodeUpdateObserver;
import io.harness.engine.observers.PlanStatusUpdateObserver;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.repositories.orchestrationEventLog.OrchestrationEventLogRepository;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.sql.Date;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutorService;

@Singleton
public class OrchestrationLogPublisher
    implements NodeUpdateObserver, NodeStatusUpdateObserver, PlanStatusUpdateObserver {
  @Inject private OrchestrationEventLogRepository orchestrationEventLogRepository;
  @Inject private GraphGenerationService graphGenerationService;

  @Override
  public void onNodeStatusUpdate(NodeUpdateInfo nodeUpdateInfo) {
    createAndHandleEventLog(nodeUpdateInfo.getPlanExecutionId(), nodeUpdateInfo.getNodeExecutionId(),
        OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE);
  }

  @Override
  public void onPlanStatusUpdate(Ambiance ambiance) {
    createAndHandleEventLog(ambiance.getPlanExecutionId(), AmbianceUtils.obtainCurrentRuntimeId(ambiance),
        OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE);
  }

  @Override
  public void onNodeUpdate(NodeUpdateInfo nodeUpdateInfo) {
    createAndHandleEventLog(nodeUpdateInfo.getPlanExecutionId(), nodeUpdateInfo.getNodeExecutionId(),
        OrchestrationEventType.NODE_EXECUTION_UPDATE);
  }

  private void createAndHandleEventLog(
      String planExecutionId, String nodeExecutionId, OrchestrationEventType eventType) {
    orchestrationEventLogRepository.save(
        OrchestrationEventLog.builder()
            .createdAt(System.currentTimeMillis())
            .nodeExecutionId(nodeExecutionId)
            .orchestrationEventType(eventType)
            .planExecutionId(planExecutionId)
            .validUntil(Date.from(OffsetDateTime.now().plus(Duration.ofDays(14)).toInstant()))
            .build());
    graphGenerationService.updateGraph(planExecutionId);
  }
}
