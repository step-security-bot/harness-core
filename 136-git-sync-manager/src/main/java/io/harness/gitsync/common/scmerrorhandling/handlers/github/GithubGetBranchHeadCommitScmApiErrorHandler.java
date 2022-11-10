/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.scmerrorhandling.handlers.github;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.SCMExceptionErrorMessages;
import io.harness.exception.ScmBadRequestException;
import io.harness.exception.ScmUnauthorizedException;
import io.harness.exception.ScmUnexpectedException;
import io.harness.exception.WingsException;
import io.harness.gitsync.common.scmerrorhandling.dtos.ErrorMetadata;
import io.harness.gitsync.common.scmerrorhandling.handlers.ScmApiErrorHandler;
import io.harness.gitsync.common.scmerrorhandling.util.ErrorMessageFormatter;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@OwnedBy(PL)
public class GithubGetBranchHeadCommitScmApiErrorHandler implements ScmApiErrorHandler {
  public static final String GET_BRANCH_HEAD_COMMIT_FAILED_MESSAGE =
      "Failed to fetch branch head commit details from Github.";

  @Override
  public void handleError(int statusCode, String errorMessage, ErrorMetadata errorMetadata) throws WingsException {
    switch (statusCode) {
      case 401:
      case 403:
        throw NestedExceptionUtils.hintWithExplanationException(
            ErrorMessageFormatter.formatMessage(ScmErrorHints.INVALID_CREDENTIALS, errorMetadata),
            ErrorMessageFormatter.formatMessage(
                GET_BRANCH_HEAD_COMMIT_FAILED_MESSAGE + ScmErrorExplanations.INVALID_CONNECTOR_CREDS, errorMetadata),
            new ScmUnauthorizedException(errorMessage));
      case 404:
        throw NestedExceptionUtils.hintWithExplanationException(
            ErrorMessageFormatter.formatMessage(ScmErrorHints.REPO_NOT_FOUND, errorMetadata),
            ErrorMessageFormatter.formatMessage(
                GET_BRANCH_HEAD_COMMIT_FAILED_MESSAGE + ScmErrorExplanations.REPO_NOT_FOUND, errorMetadata),
            new ScmBadRequestException(errorMessage));
      case 422:
        throw NestedExceptionUtils.hintWithExplanationException(
            ErrorMessageFormatter.formatMessage(ScmErrorHints.BRANCH_NOT_FOUND, errorMetadata),
            ErrorMessageFormatter.formatMessage(
                GET_BRANCH_HEAD_COMMIT_FAILED_MESSAGE + ScmErrorExplanations.BRANCH_NOT_FOUND, errorMetadata),
            new ScmBadRequestException(SCMExceptionErrorMessages.BRANCH_NOT_FOUND_ERROR));
      default:
        log.error(String.format(
            "Error while fetching the branch head commit from github: [%s: %s]", statusCode, errorMessage));
        throw new ScmUnexpectedException(errorMessage);
    }
  }
}
