/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.beans.sumologic;

import io.harness.cvng.beans.DataCollectionRequestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("SUMOLOGIC_LOG_SAMPLE_DATA")
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "SumologicSampleDataRequestKeys")
public class SumologicLogSampleDataRequest extends AbstractSumologicDataRequest {
  private String query;
  private String from;
  private String to;

  private String dsl;

  @Override
  public String getDSL() {
    return dsl;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> dslEnvVariables = super.fetchDslEnvVariables();
    dslEnvVariables.put(SumologicSampleDataRequestKeys.query, query);
    dslEnvVariables.put(SumologicSampleDataRequestKeys.from, from);
    dslEnvVariables.put(SumologicSampleDataRequestKeys.to, to);
    return dslEnvVariables;
  }

  @Override
  public DataCollectionRequestType getType() {
    return DataCollectionRequestType.SUMOLOGIC_LOG_SAMPLE_DATA;
  }
}
