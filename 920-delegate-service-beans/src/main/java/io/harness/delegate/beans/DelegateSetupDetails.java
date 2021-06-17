package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.DEL)
public class DelegateSetupDetails implements YamlDTO {
  private String sessionIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  @NotNull private String name;
  private String description;
  @NotNull private DelegateSize size;
  @NotNull private String delegateConfigurationId;

  private K8sConfigDetails k8sConfigDetails;
}
