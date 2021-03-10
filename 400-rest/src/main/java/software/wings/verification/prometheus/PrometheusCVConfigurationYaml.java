package software.wings.verification.prometheus;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.TimeSeries;
import software.wings.verification.CVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The type Yaml.
 */
@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@JsonPropertyOrder({"type", "harnessApiVersion"})
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class PrometheusCVConfigurationYaml extends CVConfigurationYaml {
  private List<TimeSeries> timeSeriesList;
}
