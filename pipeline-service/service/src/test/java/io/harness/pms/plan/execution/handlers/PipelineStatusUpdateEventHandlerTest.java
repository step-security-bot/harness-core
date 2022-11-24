/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution.handlers;

import static io.harness.rule.OwnerRule.FERNANDOD;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.skip.SkipInfo;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO;
import io.harness.repositories.executions.PmsExecutionSummaryRepository;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class PipelineStatusUpdateEventHandlerTest extends PipelineServiceTestBase {
  @Mock private PlanExecutionService planExecutionService;
  @Mock private PmsExecutionSummaryRepository pmsExecutionSummaryRepository;
  @Mock private OrchestrationEventEmitter eventEmitter;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  private PipelineStatusUpdateEventHandler pipelineStatusUpdateEventHandler;

  @Before
  public void setUp() throws Exception {
    pipelineStatusUpdateEventHandler = new PipelineStatusUpdateEventHandler(
        planExecutionService, pmsExecutionSummaryRepository, eventEmitter, waitNotifyEngine);
  }

  @Test
  @Owner(developers = FERNANDOD)
  @Category(UnitTests.class)
  public void shouldOnEndEmitEventsWhenExecutedModulesHasNullElements() {
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity = createPipelineExecutionSummaryEntity();
    when(pmsExecutionSummaryRepository
             .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionIdAndPipelineDeletedNot(
                 "accountId", "orgIdentifier", "projectIdentifier", "planExecutionId", true))
        .thenReturn(Optional.of(pipelineExecutionSummaryEntity));
    when(pmsExecutionSummaryRepository.update(notNull(), notNull())).thenReturn(pipelineExecutionSummaryEntity);

    pipelineStatusUpdateEventHandler.onEnd(createAmbiance());

    // IN THIS SCENARIO WE ARE ONLY VERIFYING THAT EVENT WAS EMITTED TWICE AND WITHOUT NPE
    // A NEW TEST CASE SHOULD BE CREATED TO ASSERT EMITTED EVENT PROPERTIES.
    verify(eventEmitter, times(2)).emitEvent(notNull());
  }

  private Ambiance createAmbiance() {
    return Ambiance.newBuilder()
        .putSetupAbstractions(SetupAbstractionKeys.accountId, "accountId")
        .putSetupAbstractions(SetupAbstractionKeys.orgIdentifier, "orgIdentifier")
        .putSetupAbstractions(SetupAbstractionKeys.projectIdentifier, "projectIdentifier")
        .setPlanExecutionId("planExecutionId")
        .build();
  }
  // CREATE ENTITY CONTAINING NULL MODULES TO FORCE THE NPE DURING THE EXECUTION.
  private PipelineExecutionSummaryEntity createPipelineExecutionSummaryEntity() {
    Map<String, GraphLayoutNodeDTO> layoutNode = new HashMap<>();
    layoutNode.put(
        "keyA", GraphLayoutNodeDTO.builder().status(ExecutionStatus.SUCCESS).skipInfo(null).module("moduleA").build());
    layoutNode.put(
        "keyB", GraphLayoutNodeDTO.builder().status(ExecutionStatus.SUCCESS).skipInfo(null).module("moduleB").build());
    layoutNode.put("keyC",
        GraphLayoutNodeDTO.builder()
            .status(ExecutionStatus.SUCCESS)
            .skipInfo(SkipInfo.newBuilder().setEvaluatedCondition(true).build())
            .module(null)
            .build());
    layoutNode.put(
        "keyD", GraphLayoutNodeDTO.builder().status(ExecutionStatus.SUCCESS).skipInfo(null).module(null).build());
    layoutNode.put(
        "keyE", GraphLayoutNodeDTO.builder().status(ExecutionStatus.WAITING).skipInfo(null).module("moduleE").build());

    Map<String, Document> moduleInfo = new HashMap<>();
    moduleInfo.put("moduleA", null);
    moduleInfo.put("moduleB", null);

    return PipelineExecutionSummaryEntity.builder()
        .layoutNodeMap(layoutNode)
        .moduleInfo(moduleInfo)
        .status(ExecutionStatus.SUCCESS)
        .endTs(4321L)
        .build();
  }
}
