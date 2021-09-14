/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.principals.usergroups;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.UserGroupDTO;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@Singleton
@UtilityClass
public class UserGroupFactory {
  public static UserGroup buildUserGroup(UserGroupDTO userGroupDTO) {
    HarnessScopeParams harnessScopeParams = HarnessScopeParams.builder()
                                                .accountIdentifier(userGroupDTO.getAccountIdentifier())
                                                .orgIdentifier(userGroupDTO.getOrgIdentifier())
                                                .projectIdentifier(userGroupDTO.getProjectIdentifier())
                                                .build();
    Scope scope = ScopeMapper.fromParams(harnessScopeParams);
    Set<String> users =
        userGroupDTO.getUsers() == null ? Collections.emptySet() : new HashSet<>(userGroupDTO.getUsers());
    return UserGroup.builder()
        .identifier(userGroupDTO.getIdentifier())
        .scopeIdentifier(scope.toString())
        .name(userGroupDTO.getName())
        .users(users)
        .build();
  }
}
