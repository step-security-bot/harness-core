/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.Collection;
import java.util.Set;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
public interface EncryptedSettingAttributes extends OwnedByAccount {
  Collection<SettingAttribute> listEncryptedSettingAttributes(String accountId);

  Collection<SettingAttribute> listEncryptedSettingAttributes(String accountId, Set<String> categories);
}
