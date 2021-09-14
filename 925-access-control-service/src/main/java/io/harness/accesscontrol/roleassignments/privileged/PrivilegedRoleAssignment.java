/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.roleassignments.privileged;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class PrivilegedRoleAssignment {
  String scopeIdentifier;
  boolean global;
  @Wither @NotNull PrincipalType principalType;
  @Wither @NotEmpty String principalIdentifier;
  @NotEmpty String roleIdentifier;
  String linkedRoleAssignment;
  @Wither String userGroupIdentifier;
  boolean managed;
}
