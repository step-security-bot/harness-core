/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.data.validator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.harness.secretmanagerclient.SecretType;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;

@Documented
@Constraint(validatedBy = {SecretTypeValidator.class})
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@ReportAsSingleViolation
public @interface SecretTypeAllowedValues {
  String message() default "{io.harness.data.validator.SecretTypeAllowedValues.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  SecretType[] allowedValues() default {};
}
