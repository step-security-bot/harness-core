/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.scmerrorhandling.handlers.bitbucket;

import static io.harness.gitsync.common.scmerrorhandling.handlers.bitbucketcloud.BitbucketCreateBranchScmApiErrorHandler.CREATE_BRANCH;
import static io.harness.rule.OwnerRule.ADITHYA;
import static io.harness.rule.OwnerRule.BHAVYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ScmBadRequestException;
import io.harness.exception.ScmUnauthorizedException;
import io.harness.exception.ScmUnexpectedException;
import io.harness.exception.WingsException;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.scmerrorhandling.dtos.ErrorMetadata;
import io.harness.gitsync.common.scmerrorhandling.handlers.bitbucketcloud.BitbucketCreateBranchScmApiErrorHandler;
import io.harness.gitsync.common.scmerrorhandling.handlers.bitbucketcloud.ScmErrorDefaultMessage;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class BitbucketCreateBranchScmApiErrorHandlerTest extends GitSyncTestBase {
  @Inject BitbucketCreateBranchScmApiErrorHandler bitbucketCreateBranchScmApiErrorHandler;

  private static final String errorMessage = "errorMessage";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnUnauthorizedResponse() {
    try {
      bitbucketCreateBranchScmApiErrorHandler.handleError(401, errorMessage, ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnauthorizedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnUnauthenticatedResponse() {
    try {
      bitbucketCreateBranchScmApiErrorHandler.handleError(403, errorMessage, ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnauthorizedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnResourceNotFoundResponse() {
    try {
      bitbucketCreateBranchScmApiErrorHandler.handleError(404, errorMessage, ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmBadRequestException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnBadRequestEntityResponse() {
    try {
      bitbucketCreateBranchScmApiErrorHandler.handleError(400, errorMessage, ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmBadRequestException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleErrorWhenUnexpectedStatusCode() {
    try {
      bitbucketCreateBranchScmApiErrorHandler.handleError(405, errorMessage, ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnexpectedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }
  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testHandleErrorWhenRateLimitCode() {
    try {
      bitbucketCreateBranchScmApiErrorHandler.handleError(429, errorMessage, ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmBadRequestException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testHandleErrorWhenErrorMessageIsEmpty() {
    try {
      bitbucketCreateBranchScmApiErrorHandler.handleError(429, "", ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmBadRequestException.class, ex);
      System.out.println("12345: " + exception.getMessage());
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage())
          .isEqualTo(String.format(ScmErrorDefaultMessage.DEFAULT_ERROR_MESSAGE, CREATE_BRANCH));
    }
  }
}
