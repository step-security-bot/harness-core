/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.beans;

import io.harness.cvng.servicelevelobjective.entities.AbstractServiceLevelObjective;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleSLODetails {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String serviceLevelObjectiveRef;

  public static SimpleSLODetailsBuilder getSimpleSLODetailsBuilder(AbstractServiceLevelObjective slo) {
    return SimpleSLODetails.builder()
        .accountId(slo.getAccountId())
        .orgIdentifier(slo.getOrgIdentifier())
        .projectIdentifier(slo.getProjectIdentifier())
        .serviceLevelObjectiveRef(slo.getIdentifier());
  }
}
