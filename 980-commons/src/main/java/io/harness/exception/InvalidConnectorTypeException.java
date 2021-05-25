package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_CONNECTOR_TYPE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.PIPELINE)
public class InvalidConnectorTypeException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public InvalidConnectorTypeException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, INVALID_CONNECTOR_TYPE, Level.ERROR, reportTargets, EnumSet.of(FailureType.APPLICATION_ERROR));
    super.param(MESSAGE_KEY, message);
  }

  public InvalidConnectorTypeException(String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(
        message, cause, INVALID_CONNECTOR_TYPE, Level.ERROR, reportTargets, EnumSet.of(FailureType.APPLICATION_ERROR));
    super.param(MESSAGE_KEY, message);
  }
}
