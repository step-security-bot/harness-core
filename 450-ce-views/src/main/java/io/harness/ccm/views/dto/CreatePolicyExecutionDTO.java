package io.harness.ccm.views.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;

import io.harness.ccm.views.entities.PolicyExecution;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePolicyExecutionDTO {
  @JsonProperty("policyExecution") @Valid PolicyExecution policyExecution;
}
