package io.harness.entities.deploymentinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.DX)
// Created for reference, either change its name before modifying or create a new deployment info
public class ReferenceK8sPodInfo extends DeploymentInfo {
  String podName;
}
