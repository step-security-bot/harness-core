package io.harness.cdng.googlefunctions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.ecs.EcsRollingDeployBaseStepInfo;
import io.harness.cdng.ecs.EcsRollingDeployStep;
import io.harness.cdng.ecs.EcsRollingDeployStepParameters;
import io.harness.cdng.pipeline.CDAbstractStepInfo;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.EcsRollingDeployStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SimpleVisitorHelper(helperClass = EcsRollingDeployStepInfoVisitorHelper.class)
@JsonTypeName(StepSpecTypeConstants.GOOGLE_CLOUD_FUNCTIONS_DEPLOY)
@TypeAlias("googleFunctionsDeployStepInfo")
@RecasterAlias("io.harness.cdng.googlefunctions.GoogleFunctionsDeployStepInfo")
public class GoogleFunctionsDeployStepInfo extends GoogleFunctionsDeployBaseStepInfo implements CDAbstractStepInfo, Visitable {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    private String uuid;
    // For Visitor Framework Impl
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

    @Builder(builderMethodName = "infoBuilder")
    public GoogleFunctionsDeployStepInfo(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
                                    ParameterField<String> updateFieldMask) {
        super(delegateSelectors, updateFieldMask);
    }
    @Override
    public StepType getStepType() {
        return GoogleFunctionsDeployStep.STEP_TYPE;
    }

    @Override
    public String getFacilitatorType() {
        return OrchestrationFacilitatorType.TASK_CHAIN;
    }

    @Override
    public SpecParameters getSpecParameters() {
        return GoogleFunctionsDeployStepParameters.infoBuilder()
                .delegateSelectors(this.getDelegateSelectors())
                .updateFieldMask(this.getUpdateFieldMask())
                .build();
    }

    @Override
    public ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
        return getDelegateSelectors();
    }

}
