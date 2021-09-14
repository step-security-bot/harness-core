/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.aggregator.consumers;

import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface UserGroupCRUDEventHandler {
  void handleUserGroupCreate(@NotNull @Valid UserGroupDBO userGroupDBO);
  void handleUserGroupUpdate(@NotNull @Valid UserGroupDBO userGroupDBO);
  void handleUserGroupDelete(@NotEmpty String id);
}
