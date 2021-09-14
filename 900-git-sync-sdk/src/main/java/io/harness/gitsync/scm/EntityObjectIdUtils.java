/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.common.helper.GitObjectIdHelper;
import io.harness.ng.core.utils.NGYamlUtils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class EntityObjectIdUtils {
  public static String getObjectIdOfYaml(YamlDTO yamlObject) {
    final String yamlString = NGYamlUtils.getYamlString(yamlObject);
    return GitObjectIdHelper.getObjectIdForString(yamlString);
  }

  public static String getObjectIdOfYaml(String yamlString) {
    return GitObjectIdHelper.getObjectIdForString(yamlString);
  }
}
