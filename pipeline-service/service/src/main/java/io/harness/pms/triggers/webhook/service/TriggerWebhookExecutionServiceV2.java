/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.triggers.webhook.service;

import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.pms.sdk.execution.events.PmsCommonsBaseEventHandler;

public interface TriggerWebhookExecutionServiceV2 extends PmsCommonsBaseEventHandler<WebhookDTO> {
  void processEvent(WebhookDTO webhookDTO);
}
