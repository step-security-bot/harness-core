/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.governance.pipeline.service.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Associates a weight with a set of tags.
 */
@Value
@OwnedBy(HarnessTeam.CDC)
public class PipelineGovernanceRule {
  private List<Tag> tags;

  @Nonnull private MatchType matchType;
  private int weight;
  @Nullable private String note;

  public List<Tag> getTags() {
    List<Tag> tags = CollectionUtils.emptyIfNull(this.tags);
    return Collections.unmodifiableList(tags);
  }
}
