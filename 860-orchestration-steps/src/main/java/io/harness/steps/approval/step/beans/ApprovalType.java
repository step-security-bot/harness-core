/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.approval.step.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.EntitySubtype;
import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.StepSpecTypeConstants;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(CDC)
public enum ApprovalType implements EntitySubtype {
  @JsonProperty(StepSpecTypeConstants.HARNESS_APPROVAL) HARNESS_APPROVAL(StepSpecTypeConstants.HARNESS_APPROVAL),
  @JsonProperty(StepSpecTypeConstants.JIRA_APPROVAL) JIRA_APPROVAL(StepSpecTypeConstants.JIRA_APPROVAL);

  private final String displayName;

  ApprovalType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  public static ApprovalType fromName(String name) {
    switch (name) {
      case StepSpecTypeConstants.HARNESS_APPROVAL:
        return HARNESS_APPROVAL;
      case StepSpecTypeConstants.JIRA_APPROVAL:
        return JIRA_APPROVAL;
      default:
        return null;
    }
  }
}
