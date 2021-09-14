/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.ProgressCallback;

@OwnedBy(HarnessTeam.PIPELINE)
public interface AsyncWaitEngine {
  void waitForAllOn(NotifyCallback notifyCallback, ProgressCallback progressCallback, String... correlationIds);
}
