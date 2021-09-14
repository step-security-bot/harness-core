/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.events.GitSyncEventConsumerService;
import io.harness.gitsync.interceptor.GitSyncThreadDecorator;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.dropwizard.setup.Environment;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class GitSyncSdkInitHelper {
  public static void initGitSyncSdk(Injector injector, Environment environment, GitSyncSdkConfiguration config) {
    String serviceName = config.getMicroservice().name();
    initializeServiceManager(injector, serviceName);
    registerInterceptor(environment);
    registerEventConsumer(injector, environment);
  }

  private static void registerEventConsumer(Injector injector, Environment environment) {
    environment.lifecycle().manage(injector.getInstance(GitSyncEventConsumerService.class));
  }

  private static void registerInterceptor(Environment environment) {
    environment.jersey().register(new GitSyncThreadDecorator());
  }

  private static void initializeServiceManager(Injector injector, String serviceName) {
    log.info("Initializing GMS SDK for service: {}", serviceName);
    ServiceManager serviceManager =
        injector.getInstance(Key.get(ServiceManager.class, Names.named("gitsync-sdk-service-manager"))).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
    log.info("Initialized git sync sdk grpc.");
  }
}
