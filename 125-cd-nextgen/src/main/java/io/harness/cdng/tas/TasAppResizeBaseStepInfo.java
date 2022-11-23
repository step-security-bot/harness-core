package io.harness.cdng.tas;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TasAppResizeBaseStepInfoKeys")
@TypeAlias("TasAppResizeBaseStepInfo")
public class TasAppResizeBaseStepInfo {
    @YamlSchemaTypes({runtime})
    @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
    ParameterField<List<TaskSelectorYaml>> delegateSelectors;
}
