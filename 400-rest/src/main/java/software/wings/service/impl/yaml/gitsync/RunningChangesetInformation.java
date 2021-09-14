/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.gitsync;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@JsonTypeName("RUNNING")
@Builder
public class RunningChangesetInformation implements ChangesetInformation {
  Long queuedAt;
  Long startedRunningAt;
}
