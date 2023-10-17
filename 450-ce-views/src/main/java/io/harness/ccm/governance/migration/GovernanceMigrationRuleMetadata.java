/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.governance.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.helper.RuleCloudProviderType;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CE)
@Value
@Builder
public class GovernanceMigrationRuleMetadata {
  @NotNull Boolean isOOTB;
  @NotNull Boolean forRecommendation;
  @NotNull Boolean deleted;
  @NotNull String name;
  @NotNull RuleCloudProviderType cloudProvider;
  @NotNull String resourceType;
  @NotNull String description;
  @NotNull String rulesYamlPath;
}
