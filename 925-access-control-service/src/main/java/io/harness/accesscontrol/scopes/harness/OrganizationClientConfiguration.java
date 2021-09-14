/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.scopes.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
public class OrganizationClientConfiguration {
  ServiceHttpClientConfig organizationServiceConfig;
  String organizationServiceSecret;
}
