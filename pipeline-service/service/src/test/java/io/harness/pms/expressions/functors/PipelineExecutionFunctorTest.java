/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.expressions.functors;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.gitsync.beans.StoreType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PipelineStoreType;
import io.harness.pms.contracts.plan.RetryExecutionInfo;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.helpers.PipelineExpressionHelper;
import io.harness.rule.Owner;

import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineExecutionFunctorTest extends CategoryTest {
  @Mock PipelineExpressionHelper pipelineExpressionHelper;
  @Mock private PlanExecutionMetadataService planExecutionMetadataService;
  @InjectMocks private PipelineExecutionFunctor pipelineExecutionFunctor;

  String sampleYaml = "pipeline:\n"
      + "  identifier: \"trialselective\"\n"
      + "  name: \"trialselective\"\n"
      + "  projectIdentifier: \"test\"\n"
      + "  orgIdentifier: \"default\"\n"
      + "  tags: {}\n"
      + "  stages:\n"
      + "  - stage:\n"
      + "      identifier: \"Test1\"\n"
      + "      type: \"Custom\"\n"
      + "      name: \"Test1\"\n"
      + "      description: \"\"\n"
      + "      spec:\n"
      + "        execution:\n"
      + "          steps:\n"
      + "          - step:\n"
      + "              identifier: \"Wait_1\"\n"
      + "              type: \"Wait\"\n"
      + "              name: \"Wait_1\"\n"
      + "              spec:\n"
      + "                duration: \"1m\"\n"
      + "          - step:\n"
      + "              identifier: \"ShellScript_1\"\n"
      + "              type: \"ShellScript\"\n"
      + "              name: \"ShellScript_1\"\n"
      + "              spec:\n"
      + "                shell: \"Bash\"\n"
      + "                onDelegate: true\n"
      + "                source:\n"
      + "                  type: \"Inline\"\n"
      + "                  spec:\n"
      + "                    script: \"echo \\\"hi\\\"\\necho <+pipeline.pipeline.triggeredBy.email>\\n\\\n"
      + "                      \\necho <+pipeline.selectedStages>\\n\\necho <+inputSet>\"\n"
      + "                environmentVariables: []\n"
      + "                outputVariables:\n"
      + "                - name: \"selectedStages\"\n"
      + "                  type: \"String\"\n"
      + "                  value: \"<+pipeline.selectedStages>\"\n"
      + "                delegateSelectors:\n"
      + "                - \"localdelegate\"\n"
      + "              timeout: \"10m\"\n"
      + "              failureStrategies: []\n"
      + "      tags: {}\n"
      + "  - parallel:\n"
      + "    - stage:\n"
      + "        identifier: \"test2\"\n"
      + "        type: \"Custom\"\n"
      + "        name: \"test2\"\n"
      + "        description: \"\"\n"
      + "        spec:\n"
      + "          execution:\n"
      + "            steps:\n"
      + "            - step:\n"
      + "                identifier: \"Wait_1\"\n"
      + "                type: \"Wait\"\n"
      + "                name: \"Wait_1\"\n"
      + "                spec:\n"
      + "                  duration: \"1m\"\n"
      + "        tags: {}\n"
      + "    - stage:\n"
      + "        identifier: \"test3\"\n"
      + "        type: \"Custom\"\n"
      + "        name: \"test3\"\n"
      + "        description: \"\"\n"
      + "        spec:\n"
      + "          execution:\n"
      + "            steps:\n"
      + "            - step:\n"
      + "                identifier: \"Wait_1_3\"\n"
      + "                type: \"Wait\"\n"
      + "                name: \"Wait_1_3\"\n"
      + "                spec:\n"
      + "                  duration: \"1m\"\n"
      + "        tags: {}\n"
      + "  - stage:\n"
      + "      identifier: \"Test4\"\n"
      + "      type: \"Custom\"\n"
      + "      name: \"Test4\"\n"
      + "      description: \"\"\n"
      + "      spec:\n"
      + "        execution:\n"
      + "          steps:\n"
      + "          - step:\n"
      + "              identifier: \"Wait_1_4\"\n"
      + "              type: \"Wait\"\n"
      + "              name: \"Wait_1_4\"\n"
      + "              spec:\n"
      + "                duration: \"10m\"\n"
      + "      tags: {}\n"
      + "      strategy:\n"
      + "        parallelism: 2\n"
      + "  allowStageExecutions: true\n";

  String executionUrl =
      "http:127.0.0.1:8080/account/dummyAccount/cd/orgs/dummyOrg/projects/dummyProject/pipelines/dummyPipeline/executions/dummyPlanExecutionId/pipeline";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testBind() {
    Optional<PlanExecutionMetadata> planExecutionMetadataOptional = Optional.of(
        PlanExecutionMetadata.builder()
            .planExecutionId("123234345")
            .yaml(sampleYaml)
            .allowStagesExecution(false)
            .retryExecutionInfo(
                RetryExecutionInfo.newBuilder().setRootExecutionId("rootExecutionId").setIsRetry(Boolean.TRUE).build())
            .build());

    // First test
    Ambiance ambiance =
        Ambiance.newBuilder()
            .putSetupAbstractions("accountId", "accountId")
            .putSetupAbstractions("projectIdentifier", "projectId")
            .putSetupAbstractions("orgIdentifier", "orgIdentifier")
            .setPlanExecutionId(generateUuid())
            .setMetadata(ExecutionMetadata.newBuilder()
                             .setRunSequence(32)
                             .setPipelineStoreType(PipelineStoreType.REMOTE)
                             .setTriggerInfo(ExecutionTriggerInfo.newBuilder()
                                                 .setTriggerType(TriggerType.WEBHOOK)
                                                 .setTriggeredBy(TriggeredBy.newBuilder()
                                                                     .setIdentifier("system")
                                                                     .setTriggerIdentifier("triggerIdentifier")
                                                                     .build())
                                                 .build())
                             .build())
            .build();
    doReturn(planExecutionMetadataOptional)
        .when(planExecutionMetadataService)
        .findByPlanExecutionId(ambiance.getPlanExecutionId());
    on(pipelineExecutionFunctor).set("ambiance", ambiance);

    Map<String, Object> response = (Map<String, Object>) pipelineExecutionFunctor.bind();
    assertEquals(response.get("triggerType"), TriggerType.WEBHOOK.toString());
    Map<String, String> triggeredByMap = (Map<String, String>) response.get("triggeredBy");
    assertNull(triggeredByMap.get("email"));
    assertEquals(triggeredByMap.get("name"), "system");
    assertEquals(triggeredByMap.get("triggerIdentifier"), "triggerIdentifier");
    assertEquals(response.get("resumedExecutionId"),
        planExecutionMetadataOptional.get().getRetryExecutionInfo().getRootExecutionId());
    assertEquals(response.get("storeType"), StoreType.REMOTE);

    // Second test
    Ambiance ambiance1 =
        Ambiance.newBuilder()
            .putSetupAbstractions("accountId", "accountId")
            .putSetupAbstractions("projectIdentifier", "projectId")
            .putSetupAbstractions("orgIdentifier", "orgIdentifier")
            .setPlanExecutionId(generateUuid())
            .setMetadata(ExecutionMetadata.newBuilder()
                             .setRunSequence(32)
                             .setPipelineStoreType(PipelineStoreType.REMOTE)
                             .setTriggerInfo(ExecutionTriggerInfo.newBuilder()
                                                 .setTriggerType(TriggerType.MANUAL)
                                                 .setTriggeredBy(TriggeredBy.newBuilder()
                                                                     .setIdentifier("Admin")
                                                                     .putExtraInfo("email", "admin@harness.io")
                                                                     .build())
                                                 .build())
                             .build())
            .build();
    doReturn(planExecutionMetadataOptional)
        .when(planExecutionMetadataService)
        .findByPlanExecutionId(ambiance1.getPlanExecutionId());
    on(pipelineExecutionFunctor).set("ambiance", ambiance1);

    doReturn(executionUrl).when(pipelineExpressionHelper).generateUrl(ambiance1);
    response = (Map<String, Object>) pipelineExecutionFunctor.bind();
    assertEquals(response.get("resumedExecutionId"),
        planExecutionMetadataOptional.get().getRetryExecutionInfo().getRootExecutionId());
    assertEquals(response.get("triggerType"), TriggerType.MANUAL.toString());
    triggeredByMap = (Map<String, String>) response.get("triggeredBy");
    assertEquals(triggeredByMap.get("email"), "admin@harness.io");
    assertEquals(triggeredByMap.get("name"), "Admin");
    assertNull(triggeredByMap.get("triggerIdentifier"));
    Map<String, String> executionMap = (Map<String, String>) response.get("execution");
    assertEquals(executionMap.size(), 1);
    assertEquals(executionMap.get("url"), executionUrl);
  }
}
