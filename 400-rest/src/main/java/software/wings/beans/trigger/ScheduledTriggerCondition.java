/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.trigger.TriggerConditionType.SCHEDULED;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by sgurubelli on 10/25/17.
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
@JsonTypeName("SCHEDULED")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ScheduledTriggerCondition extends TriggerCondition {
  @NotEmpty private String cronExpression;
  private String cronDescription;
  @Builder.Default private boolean onNewArtifactOnly = true;

  public ScheduledTriggerCondition() {
    super(SCHEDULED);
  }

  public ScheduledTriggerCondition(String cronExpression, String cronDescription, boolean onNewArtifactOnly) {
    this();
    this.cronExpression = cronExpression;
    this.cronDescription = cronDescription;
    this.onNewArtifactOnly = onNewArtifactOnly;
  }
}
