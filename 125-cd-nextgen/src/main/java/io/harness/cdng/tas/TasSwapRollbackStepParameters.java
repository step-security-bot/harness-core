package io.harness.cdng.tas;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.tas.TasSwapRollbackStepParameters")
public class TasSwapRollbackStepParameters extends TasSwapRollbackBaseStepInfo implements SpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public TasSwapRollbackStepParameters(
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, String tasRollbackFqn, String tasSetupFqn, ParameterField<Boolean> upsizeInActiveApp) {
    super(delegateSelectors, tasRollbackFqn, tasSetupFqn, upsizeInActiveApp);
  }
}
