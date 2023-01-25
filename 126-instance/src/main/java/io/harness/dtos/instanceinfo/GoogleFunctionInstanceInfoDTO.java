/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.dtos.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.util.InstanceSyncKey;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class GoogleFunctionInstanceInfoDTO extends InstanceInfoDTO {
  @NotNull private String functionName;
  @NotNull private String project;
  @NotNull private String region;

  private String revision;
  private String source;
  private long updatedTime;
  private String memorySize;
  private String runTime;

  private String infraStructureKey;

  @Override
  public String prepareInstanceKey() {
    return InstanceSyncKey.builder()
        .clazz(GoogleFunctionInstanceInfoDTO.class)
        .part(infraStructureKey)
        .part(functionName)
        .build()
        .toString();
  }

  @Override
  public String prepareInstanceSyncHandlerKey() {
    return InstanceSyncKey.builder().part(infraStructureKey).build().toString();
  }

  @Override
  public String getPodName() {
    return functionName;
  }

  @Override
  public String getType() {
    return "GoogleCloudFunctions";
  }
}
