package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;
import io.harness.when.beans.StageWhenCondition;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("stageElementParameters")
@OwnedBy(CDC)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public class StageElementParameters implements StepParameters {
  String uuid;
  String identifier;
  String name;
  ParameterField<String> description;

  ParameterField<String> skipCondition;
  StageWhenCondition when;

  List<FailureStrategyConfig> failureStrategies;
  @SkipAutoEvaluation ParameterField<Map<String, Object>> variables;
  Map<String, String> tags;
  String type;
  SpecParameters specConfig;

  @Override
  public String toViewJson() {
    StageElementParameters stageElementParameters = cloneParameters();
    stageElementParameters.setSpecConfig(specConfig.getViewJsonObject());
    return RecastOrchestrationUtils.toJson(stageElementParameters);
  }

  public StageElementParameters cloneParameters() {
    return StageElementParameters.builder()
        .uuid(this.uuid)
        .type(this.type)
        .name(this.name)
        .description(this.description)
        .identifier(this.identifier)
        .failureStrategies(this.failureStrategies)
        .when(this.when)
        .skipCondition(this.skipCondition)
        .variables(this.variables)
        .tags(this.tags)
        .build();
  }
}
