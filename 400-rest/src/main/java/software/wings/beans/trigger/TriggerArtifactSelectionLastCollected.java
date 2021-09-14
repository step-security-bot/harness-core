/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Value
@Builder
@JsonTypeName("LAST_COLLECTED")
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class TriggerArtifactSelectionLastCollected implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactSelectionType artifactSelectionType = ArtifactSelectionType.LAST_COLLECTED;
  @NotEmpty private String artifactStreamId;
  @NotEmpty private String artifactServerId;
  private transient String artifactStreamName;
  private transient String artifactServerName;
  private transient String artifactStreamType;
  private String artifactFilter;
}
