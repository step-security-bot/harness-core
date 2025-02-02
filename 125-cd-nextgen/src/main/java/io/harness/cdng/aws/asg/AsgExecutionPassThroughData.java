/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.asg;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;

@Getter
@NoArgsConstructor
@OwnedBy(CDP)
@TypeAlias("asgExecutionPassThroughData")
@RecasterAlias("io.harness.cdng.aws.asg.AsgExecutionPassThroughData")
public class AsgExecutionPassThroughData implements PassThroughData {
  InfrastructureOutcome infrastructure;
  @Setter UnitProgressData lastActiveUnitProgressData;
  AsgManifestFetchData asgManifestFetchData;

  @Builder()
  public AsgExecutionPassThroughData(InfrastructureOutcome infrastructure, UnitProgressData lastActiveUnitProgressData,
      AsgManifestFetchData asgManifestFetchData) {
    this.infrastructure = infrastructure;
    this.lastActiveUnitProgressData = lastActiveUnitProgressData;
    this.asgManifestFetchData = asgManifestFetchData;
  }
}
