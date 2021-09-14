/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.commons.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@OwnedBy(HarnessTeam.PL)
public abstract class EventListenerService implements Managed {
  private final EventListener eventListener;
  private final ExecutorService executorService;
  private Future<?> eventListenerFuture;

  public abstract String getServiceName();

  public EventListenerService(EventListener eventListener) {
    this.eventListener = eventListener;
    executorService = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat(getServiceName() + "-listener-main-thread").build());
  }

  @Override
  public void start() throws Exception {
    eventListenerFuture = executorService.submit(eventListener);
  }

  @Override
  public void stop() throws Exception {
    eventListenerFuture.cancel(true);
    executorService.shutdownNow();
  }
}
