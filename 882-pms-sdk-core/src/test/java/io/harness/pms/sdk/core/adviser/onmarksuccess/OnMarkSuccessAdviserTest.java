package io.harness.pms.sdk.core.adviser.onmarksuccess;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.MarkSuccessAdvise;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviser;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviserParameters;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OnMarkSuccessAdviserTest extends PmsSdkCoreTestBase {
  public static final String NODE_EXECUTION_ID = generateUuid();
  public static final String NODE_SETUP_ID = generateUuid();
  public static final String NODE_IDENTIFIER = "DUMMY";
  public static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType(NODE_IDENTIFIER).build();

  @Inject OnMarkSuccessAdviser onMarkSuccessAdviser;
  @Inject KryoSerializer kryoSerializer;

  private Ambiance ambiance;

  @Before
  public void setup() {
    ambiance = Ambiance.newBuilder()
                   .addLevels(Level.newBuilder()
                                  .setSetupId(NODE_SETUP_ID)
                                  .setRuntimeId(NODE_EXECUTION_ID)
                                  .setIdentifier(NODE_IDENTIFIER)
                                  .setStepType(DUMMY_STEP_TYPE)
                                  .build())
                   .build();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestOnAdviseEvent() {
    NodeExecutionProto nodeExecutionProto = NodeExecutionProto.newBuilder().setAmbiance(ambiance).build();
    String nextNodeId = generateUuid();
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .nodeExecution(nodeExecutionProto)
                                      .toStatus(Status.SUCCEEDED)
                                      .adviserParameters(kryoSerializer.asBytes(
                                          OnMarkSuccessAdviserParameters.builder().nextNodeId(nextNodeId).build()))
                                      .build();
    AdviserResponse adviserResponse = onMarkSuccessAdviser.onAdviseEvent(advisingEvent);
    assertThat(adviserResponse).isInstanceOf(AdviserResponse.class);
    MarkSuccessAdvise nextStepAdvise = adviserResponse.getMarkSuccessAdvise();
    assertThat(nextStepAdvise.getNextNodeId()).isEqualTo(nextNodeId);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestCanAdvise() {
    NodeExecutionProto nodeExecutionProto = NodeExecutionProto.newBuilder().setAmbiance(ambiance).build();
    AdvisingEvent advisingEvent =
        AdvisingEvent.builder()
            .nodeExecution(nodeExecutionProto)
            .toStatus(Status.FAILED)
            .adviserParameters(kryoSerializer.asBytes(OnMarkSuccessAdviserParameters.builder().build()))
            .build();
    boolean canAdvise = onMarkSuccessAdviser.canAdvise(advisingEvent);
    assertThat(canAdvise).isTrue();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestCanAdviseWithFailureTypes() {
    NodeExecutionProto nodeExecutionProto =
        NodeExecutionProto.newBuilder()
            .setAmbiance(ambiance)
            .setFailureInfo(FailureInfo.newBuilder().addFailureTypes(FailureType.DELEGATE_PROVISIONING_FAILURE).build())
            .build();
    byte[] paramBytes = kryoSerializer.asBytes(
        OnMarkSuccessAdviserParameters.builder()
            .applicableFailureTypes(EnumSet.of(FailureType.CONNECTIVITY_FAILURE, FailureType.AUTHENTICATION_FAILURE))
            .build());
    AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                      .nodeExecution(nodeExecutionProto)
                                      .toStatus(Status.ABORTED)
                                      .adviserParameters(paramBytes)
                                      .build();
    boolean canAdvise = onMarkSuccessAdviser.canAdvise(advisingEvent);
    assertThat(canAdvise).isFalse();
  }
}
