/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.response.publishers;

import io.harness.pms.contracts.execution.events.SdkResponseEventProto;

public interface SdkResponseEventPublisher {
  void publishEvent(SdkResponseEventProto event);
}
