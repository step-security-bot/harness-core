package io.harness.cdng.tas;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CdAbstractStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.yaml.core.StepSpecType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.SWAP_ROLLBACK)
@TypeAlias("TasSwapRollbackStepNode")
@RecasterAlias("io.harness.cdng.tas.TasSwapRollbackStepNode")
public class TasSwapRollbackStepNode extends CdAbstractStepNode {
  @JsonProperty("type") @NotNull TasSwapRollbackStepNode.StepType type = StepType.SwapRollback;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  TasSwapRollbackStepInfo tasSwapRollbackStepInfo;

  @Override
  public String getType() {
    return StepSpecTypeConstants.SWAP_ROLLBACK;
  }

  @Override
  public StepSpecType getStepSpecType() {
    return tasSwapRollbackStepInfo;
  }

  enum StepType {
    SwapRollback(StepSpecTypeConstants.SWAP_ROLLBACK);
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
