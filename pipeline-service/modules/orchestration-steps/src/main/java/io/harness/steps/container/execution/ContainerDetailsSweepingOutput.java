package io.harness.steps.container.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("containerDetails")
@JsonTypeName("containerDetails")
@OwnedBy(PIPELINE)
@RecasterAlias("io.harness.steps.container.execution.ContainerDetailsSweepingOutput")
public class ContainerDetailsSweepingOutput implements ExecutionSweepingOutput {
  private static final String CONTAINER_DETAILS = "containerDetails";
  public static final String INIT_POD = "containerDetails";
  String stepIdentifier;
  private String accountId;

  public static String getContainerDetailsKey(String stepIdentifier) {
    return CONTAINER_DETAILS + stepIdentifier;
  }

  public String getContainerDetailsKey() {
    return CONTAINER_DETAILS + this.stepIdentifier;
  }
}
