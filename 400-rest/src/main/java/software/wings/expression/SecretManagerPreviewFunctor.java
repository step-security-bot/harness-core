/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;

@OwnedBy(CDC)
public class SecretManagerPreviewFunctor implements ExpressionFunctor, SecretManagerFunctorInterface {
  public static final String SECRET_EXPRESSION_FORMATTER = "${secrets.getValue(\"%s\")}";
  public static final String SECRET_NAME_FORMATTER = "<<<%s>>>";

  private String formatter;

  public SecretManagerPreviewFunctor() {
    this.formatter = SECRET_NAME_FORMATTER;
  }

  public SecretManagerPreviewFunctor(String formatter) {
    this.formatter = formatter;
  }

  public static SecretManagerPreviewFunctor functorWithSecretExpressionFormat() {
    return new SecretManagerPreviewFunctor(SECRET_EXPRESSION_FORMATTER);
  }

  @Override
  public Object obtain(String secretName, int token) {
    return String.format(formatter, secretName);
  }

  @Override
  public Object obtainConfigFileAsString(String path, String encryptedFileId, int token) {
    return String.format(formatter, path);
  }

  @Override
  public Object obtainConfigFileAsBase64(String path, String encryptedFileId, int token) {
    return String.format(formatter, path);
  }
}
