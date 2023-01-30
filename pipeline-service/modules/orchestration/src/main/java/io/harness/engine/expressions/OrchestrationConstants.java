/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface OrchestrationConstants {
  String STAGE_SUCCESS = "OnStageSuccess";
  String STAGE_FAILURE = "OnStageFailure";
  String PIPELINE_SUCCESS = "OnPipelineSuccess";
  String PIPELINE_FAILURE = "OnPipelineFailure";
  String ALWAYS = "Always";
  String CURRENT_STATUS = "currentStatus";
  String LIVE_STATUS = "liveStatus";
}
