/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.execution.export.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ExportExecutionsRequestLimitChecks {
  @Value
  @Builder
  public static class LimitCheck {
    long limit;
    long value;

    public boolean hasViolation() {
      return value > limit;
    }
  }

  LimitCheck queuedRequests;
  LimitCheck executionCount;

  public void validate() {
    if (queuedRequests.hasViolation()) {
      throw new InvalidRequestException(format(
          "%d export executions requests are already in queue for the current account", queuedRequests.getLimit()));
    }

    if (executionCount.getValue() == 0) {
      throw new InvalidRequestException(
          "No executions found matching request filters. Note that executions that were not finished when request was made are ignored");
    } else if (executionCount.hasViolation()) {
      throw new InvalidRequestException(format(
          "Number of executions for the given filters is: %d which is greater than the limit for a single export operation: %d",
          executionCount.getValue(), executionCount.getLimit()));
    }
  }
}
