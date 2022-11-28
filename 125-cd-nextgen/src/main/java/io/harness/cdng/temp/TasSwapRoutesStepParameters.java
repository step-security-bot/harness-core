package io.harness.cdng.temp;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.tas.TasSwapRoutesStepParameters")
public class TasSwapRoutesStepParameters extends TasSwapRoutesBaseStepInfo implements SpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public TasSwapRoutesStepParameters(
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, boolean downSizeOldApplication, String tasSetupFqn) {
    super(delegateSelectors, downSizeOldApplication, tasSetupFqn);
  }
}
