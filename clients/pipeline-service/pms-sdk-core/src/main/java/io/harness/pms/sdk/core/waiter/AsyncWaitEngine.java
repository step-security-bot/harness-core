/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.ProgressCallback;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface AsyncWaitEngine {
  void waitForAllOn(
      NotifyCallback notifyCallback, ProgressCallback progressCallback, List<String> correlationIds, long timeout);

  /**
   * This method notifies that they have acquired the task for the given correlationId
   * @param correlationId
   */
  void taskAcquired(String correlationId);
}
