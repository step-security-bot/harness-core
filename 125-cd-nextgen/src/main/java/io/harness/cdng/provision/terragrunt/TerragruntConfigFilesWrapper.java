/*

 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

 */

package io.harness.cdng.provision.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.manifest.yaml.storeConfig.moduleSource.ModuleSource;
import io.harness.validation.Validator;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@RecasterAlias("io.harness.cdng.provision.terragrunt.TerragruntConfigFilesWrapper")
public class TerragruntConfigFilesWrapper {
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String uuid;
  @NotNull StoreConfigWrapper store;
  ModuleSource moduleSource;

  public void validateParams() {
    Validator.notNullCheck("Store cannot be null in Config Files", store);
    store.validateParams();
  }
}
