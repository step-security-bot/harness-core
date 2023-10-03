/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datapoints.parser;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.common.CommonUtils;
import io.harness.idp.scorecard.datapoints.entity.DataPointEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.IDP)
public class JiraIssuesCountParser implements DataPointParser {
  @Override
  public Object parseDataPoint(Map<String, Object> data, DataPointEntity dataPointIdentifier, Set<String> inputValues) {
    Map<String, Object> dataPointData = new HashMap<>();
    for (String inputValue : inputValues) {
      if (!data.containsKey(inputValue)) {
        dataPointData.putAll(constructDataPointInfo(inputValue, false, null));
        continue;
      }
      Map<String, Object> inputValueData = (Map<String, Object>) data.get(inputValue);
      double value = (double) CommonUtils.findObjectByName(inputValueData, "total");
      dataPointData.putAll(constructDataPointInfo(inputValue, value, null));
    }
    return dataPointData;
  }
}
