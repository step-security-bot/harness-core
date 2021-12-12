package io.harness.delegate.beans.ci.vm;

import io.harness.delegate.beans.ci.CICleanupTaskParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;

import io.harness.delegate.beans.executioncapability.CIVmConnectionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.LiteEngineConnectionCapability;
import io.harness.expression.ExpressionEvaluator;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Collections;
import java.util.List;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIVmCleanupTaskParams implements CICleanupTaskParams, ExecutionCapabilityDemander {
  @NotNull private String stageRuntimeId;
  @NotNull private String poolId;

  @Builder.Default private static final CICleanupTaskParams.Type type = CICleanupTaskParams.Type.VM;

  @Override
  public CICleanupTaskParams.Type getType() {
    return type;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.singletonList(CIVmConnectionCapability.builder().poolId(poolId).build());
  }
}
