/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rishi on 2/6/17.
 */
@OwnedBy(CDC)
@JsonTypeName("phaseExecutionSummary")
@TargetModule(HarnessModule._957_CG_BEANS)
public class PhaseExecutionSummary extends StepExecutionSummary implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "phaseExecutionSummary";

  private Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap = new HashMap<>();

  public Map<String, PhaseStepExecutionSummary> getPhaseStepExecutionSummaryMap() {
    return phaseStepExecutionSummaryMap;
  }

  public void setPhaseStepExecutionSummaryMap(Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap) {
    this.phaseStepExecutionSummaryMap = phaseStepExecutionSummaryMap;
  }

  @Override
  public String getType() {
    return "phaseExecutionSummary";
  }
}
