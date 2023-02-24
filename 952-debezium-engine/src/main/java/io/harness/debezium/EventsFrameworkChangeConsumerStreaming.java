/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import io.harness.cf.client.api.CfClient;

import java.util.Optional;

public class EventsFrameworkChangeConsumerStreaming extends EventsFrameworkChangeConsumer {
  public EventsFrameworkChangeConsumerStreaming(ChangeConsumerConfig changeConsumerConfig, CfClient cfClient,
      String collection, DebeziumProducerFactory debeziumProducerFactory, String connectorName) {
    super(changeConsumerConfig, cfClient, collection, debeziumProducerFactory, connectorName);
  }

  @Override
  public boolean shouldStop(Optional<OpType> opType) {
    return false;
  }
}