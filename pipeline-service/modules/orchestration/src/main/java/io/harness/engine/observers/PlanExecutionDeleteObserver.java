/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.observers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecution;

import java.util.Date;
import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PlanExecutionDeleteObserver {
  /**
   * Observer method to do operation on nodeExecutions ttl update
   * @param planExecutionIds
   */
  void onPlanExecutionsExpiryUpdate(List<PlanExecution> planExecutionIds, Date ttlDate);
}
