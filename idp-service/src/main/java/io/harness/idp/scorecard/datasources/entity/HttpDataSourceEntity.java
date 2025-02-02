/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasources.entity;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.common.beans.DataSourceType;
import io.harness.idp.scorecard.common.beans.HttpConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@JsonTypeName("Http")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "HttpDataSourceKeys")
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.IDP)
public class HttpDataSourceEntity extends DataSourceEntity {
  public HttpDataSourceEntity() {
    super.setType(DataSourceType.HTTP);
  }

  private HttpConfig httpConfig;
}
