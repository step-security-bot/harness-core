/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.notification.beans;

import java.time.Duration;
import java.time.Instant;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
public class NotificationRuleRef {
  @NotNull String notificationRuleRef;
  @NotNull boolean enabled;
  @NotNull Instant lastSuccessFullNotificationSent;

  public boolean isEligible(Instant currentInstant, Duration coolOffDuration) {
    log.info("is Eligible enabled = " + enabled + " lastSuccessFullNotificationSent = " + lastSuccessFullNotificationSent + " currentInstant = " + currentInstant + " coolOffDuration = " + coolOffDuration );
    int comparator = Duration.between(lastSuccessFullNotificationSent, currentInstant).compareTo(coolOffDuration);
    log.info("comparator = " + comparator );
    log.info("isEligible = " + (enabled && comparator >= 0));
    return enabled && comparator >= 0;
  }
}
