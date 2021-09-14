/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Swapni on 28/08/18
 */
@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class LdapUserResponse {
  @NotBlank String dn;
  @NotBlank String email;
  @NotBlank String name;

  /**
   * In harness User class, we always save the user email in lowercase letters. But the
   * email id we get from the LDAP server could contain few capital letters also which
   * were causing some string comparisons to fail in LdapGroupSyncJob.
   */
  public String getEmail() {
    return StringUtils.lowerCase(email);
  }
}
