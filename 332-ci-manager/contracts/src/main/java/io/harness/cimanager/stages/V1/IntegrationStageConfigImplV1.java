/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cimanager.stages.V1;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.yaml.extended.clone.Clone;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageInfoConfig;
import io.harness.pms.yaml.YamlNode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CI)
@Data
@Builder
@AllArgsConstructor
@JsonTypeName("ci")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("IntegrationStageConfigImplV1")
@RecasterAlias("io.harness.cimanager.stages.V1.IntegrationStageConfigImplV1")
public class IntegrationStageConfigImplV1 implements StageInfoConfig {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;

  @NotNull @JsonProperty("steps") @Size(min = 1) List<JsonNode> steps;
  @JsonProperty("clone") Clone clone;
  public Clone getClone() {
    if (this.clone == null) {
      this.clone = Clone.builder().build();
    }
    return this.clone;
  }

  @Override
  public ExecutionElementConfig getExecution() {
    List<ExecutionWrapperConfig> stepsList =
        steps.stream().map(step -> ExecutionWrapperConfig.builder().step(step).build()).collect(Collectors.toList());
    return ExecutionElementConfig.builder().steps(stepsList).build();
  }
}
