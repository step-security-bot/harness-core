/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.beans;

import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitToHarnessFileProcessingRequest {
  GitFileChangeDTO fileDetails;
  ChangeType changeType;
}
