/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.ssh.rollback.copyartifact;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.rollback.RollbackDeploymentInfo;
import io.harness.delegate.task.ssh.artifact.SshWinRmArtifactDelegateConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
@JsonTypeName("CopyArtifactUnitRollbackDeploymentInfo")
public class CopyArtifactUnitRollbackDeploymentInfo implements RollbackDeploymentInfo {
  @NotNull private SshWinRmArtifactDelegateConfig artifactDelegateConfig;
}
