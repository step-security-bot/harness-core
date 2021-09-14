/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.mutation.execution.payload;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLExecution;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Builder
@FieldNameConstants(innerTypeName = "QLResumeExecutionPayloadKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
@AllArgsConstructor
public class QLResumeExecutionPayload {
  private String clientMutationId;
  private QLExecution execution;
}
