/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.execution.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSdkEventFrameworkConstants {
  public static final String PT_INTERRUPT_CONSUMER = "INTERRUPT_CONSUMER";
  public static final String PT_FACILITATOR_CONSUMER = "FACILITATOR_CONSUMER";
  public static final String PT_NODE_START_CONSUMER = "NODE_START_CONSUMER";
  public static final String PT_PROGRESS_CONSUMER = "PROGRESS_CONSUMER";
  public static final String PT_NODE_ADVISE_CONSUMER = "NODE_ADVISE_CONSUMER";
  public static final String PT_NODE_RESUME_CONSUMER = "NODE_RESUME_CONSUMER";
  public static final String PT_ORCHESTRATION_EVENT_CONSUMER = "ORCHESTRATION_EVENT_CONSUMER";
  public static final String PT_ORCHESTRATION_EVENT_LISTENER = "ORCHESTRATION_EVENT_LISTENER";

  public static final String PT_START_PLAN_CREATION_EVENT_CONSUMER = "PT_START_PLAN_CREATION_EVENT_CONSUMER";
}
