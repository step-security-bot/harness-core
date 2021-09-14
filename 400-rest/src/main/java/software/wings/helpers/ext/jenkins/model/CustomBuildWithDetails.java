/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.jenkins.model;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.offbytwo.jenkins.model.BuildWithDetails;
import java.io.IOException;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(_960_API_SERVICES)
@Data
@NoArgsConstructor
public class CustomBuildWithDetails extends BuildWithDetails {
  public CustomBuildWithDetails(BuildWithDetails details) {
    super(details);
  }

  @Override
  public CustomBuildWithDetails details() throws IOException {
    return this.client.get(url, CustomBuildWithDetails.class);
  }

  private String url;
}
