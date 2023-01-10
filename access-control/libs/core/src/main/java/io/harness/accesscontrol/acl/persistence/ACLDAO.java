/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.acl.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.PermissionCheck;
import io.harness.accesscontrol.acl.api.Principal;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Set;

@OwnedBy(PL)
public interface ACLDAO {
  Set<String> getQueryStrings(PermissionCheck permissionCheck, Principal principal);

  List<List<ACL>> getMatchingACLs(Principal principal, List<PermissionCheck> permissionChecks);
}
