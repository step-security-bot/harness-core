/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

import software.wings.beans.Workflow;

@OwnedBy(CDC)
public class WorkflowLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(Workflow.class);

  public WorkflowLogContext(String workflowId, OverrideBehavior behavior) {
    super(ID, workflowId, behavior);
  }
}
