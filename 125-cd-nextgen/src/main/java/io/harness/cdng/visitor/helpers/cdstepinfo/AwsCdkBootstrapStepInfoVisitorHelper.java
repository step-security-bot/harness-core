/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.provision.awscdk.AwsCdkBootstrapStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class AwsCdkBootstrapStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return AwsCdkBootstrapStepInfo.infoBuilder().build();
  }

  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // nothing to validate
  }
}
