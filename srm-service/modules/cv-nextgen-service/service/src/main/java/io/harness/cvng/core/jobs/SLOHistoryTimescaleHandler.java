/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.jobs;

import io.harness.cvng.servicelevelobjective.beans.SLOTargetType;
import io.harness.cvng.servicelevelobjective.entities.AbstractServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.SLOTarget;
import io.harness.cvng.servicelevelobjective.entities.TimePeriod;
import io.harness.cvng.servicelevelobjective.services.api.SLOTimeScaleService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class SLOHistoryTimescaleHandler implements MongoPersistenceIterator.Handler<AbstractServiceLevelObjective> {
  @Inject SLOTimeScaleService sloTimeScaleService;

  @Inject Clock clock;
  @Override
  public void handle(AbstractServiceLevelObjective serviceLevelObjective) {
    SLOTarget sloTarget = serviceLevelObjective.getTarget();
    LocalDateTime currentLocalDate = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    TimePeriod prevTimePeriod = sloTarget.getTimeRangeForHistory(currentLocalDate);
    if (sloTarget.getType().equals(SLOTargetType.ROLLING)
        || !prevTimePeriod.getEndTime().isBefore(currentLocalDate.minus(1, ChronoUnit.DAYS))) {
      sloTimeScaleService.insertSLOHistory(serviceLevelObjective);
    }
  }
}
