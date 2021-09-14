/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.NGRestUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PL)
public class NonPrivilegedAccessControlClientImpl extends AbstractAccessControlClient {
  private final AccessControlHttpClient accessControlHttpClient;

  @Inject
  public NonPrivilegedAccessControlClientImpl(
      @Named("NON_PRIVILEGED") AccessControlHttpClient accessControlHttpClient) {
    this.accessControlHttpClient = accessControlHttpClient;
  }

  @Override
  protected AccessCheckResponseDTO checkForAccess(AccessCheckRequestDTO accessCheckRequestDTO) {
    return NGRestUtils.getResponse(accessControlHttpClient.checkForAccess(accessCheckRequestDTO));
  }
}
