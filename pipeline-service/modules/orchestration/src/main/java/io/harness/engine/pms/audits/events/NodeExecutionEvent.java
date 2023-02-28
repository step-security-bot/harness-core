/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.audits.events;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PIPELINE)
@Getter
@NoArgsConstructor
public abstract class NodeExecutionEvent implements Event {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineIdentifier;
  String planExecutionId;

  public NodeExecutionEvent(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String planExecutionId) {
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.pipelineIdentifier = pipelineIdentifier;
    this.planExecutionId = planExecutionId;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new ProjectScope(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @JsonIgnore @Override public abstract Resource getResource();

  @JsonIgnore @Override public abstract String getEventType();
}
