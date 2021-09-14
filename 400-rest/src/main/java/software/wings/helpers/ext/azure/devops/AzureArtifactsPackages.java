/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.azure.devops;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@OwnedBy(CDC)
@TargetModule(_960_API_SERVICES)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class AzureArtifactsPackages {
  private int count;
  private List<AzureArtifactsPackage> value;
}
