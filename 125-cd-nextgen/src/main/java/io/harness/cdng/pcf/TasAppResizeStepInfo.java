package io.harness.cdng.pcf;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pcf.TasAppResizeBaseStepInfo;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.TasAppResizeStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import software.wings.beans.InstanceUnitType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SimpleVisitorHelper(helperClass = TasAppResizeStepInfoVisitorHelper.class)
@JsonTypeName(StepSpecTypeConstants.TAS_APP_RESIZE)
@TypeAlias("TasAppResizeStepInfo")
@RecasterAlias("io.harness.cdng.pcf.TasAppResizeStepInfo")
public class TasAppResizeStepInfo extends TasAppResizeBaseStepInfo implements CDStepInfo, Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;
  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public TasAppResizeStepInfo(ParameterField<List<TaskSelectorYaml>> delegateSelectors, String tasAppResizeFqn,
      ParameterField<Integer> totalInstanceCount, ParameterField<InstanceUnitType> instanceUnitType,
      ParameterField<Integer> downsizeInstanceCount, ParameterField<InstanceUnitType> downsizeInstanceUnitType) {
    super(delegateSelectors, tasAppResizeFqn, totalInstanceCount, instanceUnitType, downsizeInstanceCount,
        downsizeInstanceUnitType);
  }

  @Override
  public StepType getStepType() {
    return null;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return io.harness.cdng.tas.TasAppResizeStepParameters.infoBuilder()
        .delegateSelectors(this.getDelegateSelectors())
        .build();
  }

  @Override
  public ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
    return getDelegateSelectors();
  }
}
