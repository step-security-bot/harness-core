/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.audit.client.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuthenticationInfoDTO;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;

@OwnedBy(PL)
public class NoopAuditClientServiceImpl implements AuditClientService {
  public boolean publishAudit(AuditEntry auditEntry, GlobalContext globalContext) {
    return true;
  }

  @Override
  public boolean publishAudit(
      AuditEntry auditEntry, AuthenticationInfoDTO authenticationInfo, GlobalContext globalContext) {
    return true;
  }

  @Override
  public AuditEventDTO getAuditEventDTO(
      AuditEntry auditEntry, GlobalContext globalContext, AuthenticationInfoDTO authenticationInfoDTO) {
    return null;
  }
}
