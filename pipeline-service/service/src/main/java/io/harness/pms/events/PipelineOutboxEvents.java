/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineOutboxEvents {
  public static final String INPUT_SET_CREATED = "InputSetCreated";
  public static final String INPUT_SET_UPDATED = "InputSetUpdated";
  public static final String INPUT_SET_DELETED = "InputSetDeleted";
  public static final String PIPELINE_CREATED = "PipelineCreated";
  public static final String PIPELINE_UPDATED = "PipelineUpdated";
  public static final String PIPELINE_DELETED = "PipelineDeleted";
  public static final String PIPELINE_START = "PipelineStart";
  public static final String PIPELINE_END = "PipelineEnd";
  public static final String STAGE_START = "StageStart";
  public static final String STAGE_END = "StageEnd";
}
