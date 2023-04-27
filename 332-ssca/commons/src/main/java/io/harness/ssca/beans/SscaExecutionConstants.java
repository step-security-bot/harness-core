/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.SSCA)
public interface SscaExecutionConstants {
  String SSCA_SERVICE_ENDPOINT_VARIABLE = "HARNESS_SSCA_SERVICE_ENDPOINT";
  String SSCA_SERVICE_TOKEN_VARIABLE = "HARNESS_SSCA_SERVICE_TOKEN";
}
