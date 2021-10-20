package io.harness.exception;

import static io.harness.eraro.ErrorCode.EXPLANATION;
import static io.harness.eraro.Level.INFO;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.DX)
public class ExplanationException extends WingsException {
  public static String EXPLANATION_IRSA_ROLE_CHECK =
      "IRSA capability for delegate doesn't exist or doesn't have required permissions to perform the activity";
  public static final String EXPLANATION_EMPTY_ACCESS_KEY = "Access key cannot be empty";
  public static String EXPLANATION_EMPTY_SECRET_KEY = "Secret key cannot be empty";
  public static String EXPLANATION_AWS_AM_ROLE_CHECK =
      "IAM role on delegate ec2 doesn't exist or doesn't have required permissions to perform the activity";
  public static String EXPLANATION_AWS_CLIENT_UNKNOWN_ISSUE = "Seems to encounter unknown AWS client issue";
  public static String EXPLANATION_UNEXPECTED_ERROR = "Unexpected error while handling task";
  public static String IMAGE_TAG_METADATA_NOT_FOUND =
      "Tag not found: Unable to fetch artifact metadata for {%s:%s} using Connector %s and the credentials provided";
  public static String IMAGE_METADATA_NOT_FOUND =
      "Image Not Found: Unable to fetch artifact metadata for {%s} using connector %s with the given credentials.";
  public static String REGISTRY_ACCESS_DENIED =
      "Failed to fetch artifact metadata using connector %s with given credentials.";
  public static String COMMAND_TRIED_FOR_ARTIFACT = "Commands tried %s but no metadata was returned";
  public static String ILLEGAL_IMAGE_FORMAT = "Provided image path [%s] does not satisfy ECR image path format";

  // GIT
  public static String INVALID_GIT_REPO = "Provided repo url is invalid.";
  public static String INVALID_GIT_AUTHORIZATION = "Provided credentials are not authorized.";
  public static String INVALID_GIT_AUTHENTICATION = "Authentication is not supported.";

  public static String INVALID_GIT_API_AUTHORIZATION = "Provided api access credentials are not authorized.";

  public static String MALFORMED_GIT_SSH_KEY = "Provided ssh key uses incorrect format.";
  public static String INVALID_GIT_SSH_AUTHORIZATION = "Provided ssh credentials are not authorized.";

  public static String EXPLANATION_MISSING_BRANCH = "Provided branch '%s' doesn't exist in the git repository";
  public static String EXPLANATION_MISSING_REFERENCE = "Provided reference '%s' doesn't exist in the git repository";
  public static String EXPLANATION_GIT_FILE_NOT_FOUND = "Provided file path doesn't exist";

  public static String URL_NOT_FOUND = "Provided URL path does not exist.";
  public static String AUTHORIZATION_FAILURE = "Provided credentials are unauthorized.";
  public ExplanationException(String message, Throwable cause) {
    super(message, cause, EXPLANATION, INFO, USER_SRE, null);
    super.excludeReportTarget(EXPLANATION, EnumSet.of(ReportTarget.LOG_SYSTEM));
    super.param("message", message);
  }
}
