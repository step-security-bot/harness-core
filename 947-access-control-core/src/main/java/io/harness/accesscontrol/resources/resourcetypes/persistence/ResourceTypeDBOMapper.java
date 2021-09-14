/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.resources.resourcetypes.persistence;

import io.harness.accesscontrol.resources.resourcetypes.ResourceType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class ResourceTypeDBOMapper {
  public static ResourceTypeDBO toDBO(ResourceType resourceType) {
    return ResourceTypeDBO.builder()
        .identifier(resourceType.getIdentifier())
        .permissionKey(resourceType.getPermissionKey())
        .build();
  }

  public static ResourceType fromDBO(ResourceTypeDBO resourceTypeDBO) {
    return ResourceType.builder()
        .identifier(resourceTypeDBO.getIdentifier())
        .permissionKey(resourceTypeDBO.getPermissionKey())
        .build();
  }
}
