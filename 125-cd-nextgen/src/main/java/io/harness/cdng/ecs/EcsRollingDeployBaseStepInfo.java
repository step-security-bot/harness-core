package io.harness.cdng.ecs;

import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("ecsRollingDeployBaseStepInfo")
@FieldNameConstants(innerTypeName = "EcsRollingDeployBaseStepInfoKeys")
public class EcsRollingDeployBaseStepInfo {
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;

  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  @JsonProperty("sameAsAlreadyRunningInstances")
  ParameterField<Boolean> sameAsAlreadyRunningInstances;

  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  @JsonProperty("forceNewDeployment")
  ParameterField<Boolean> forceNewDeployment;
}
