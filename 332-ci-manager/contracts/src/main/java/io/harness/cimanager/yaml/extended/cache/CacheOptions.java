package io.harness.beans.yaml.extended.cache;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.schema.beans.SupportedPossibleFieldTypes;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("cacheOptions")
@TypeAlias("CICacheOptions")
@RecasterAlias("io.harness.beans.yaml.extended.cache.CacheOptions")
@OwnedBy(CI)
public class CacheOptions {
    @YamlSchemaTypes({runtime})
    @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
    private ParameterField<Boolean> enabled;
    @YamlSchemaTypes({runtime})
    @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
    private ParameterField<List<String>> cachedPaths;
}
