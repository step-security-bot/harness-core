/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice.deployment.context;

import io.harness.azure.client.AzureMonitorClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.logging.LogCallback;

import com.azure.core.http.rest.Response;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import reactor.core.publisher.Mono;

@EqualsAndHashCode(callSuper = false)
public class SwapSlotStatusVerifierContext extends StatusVerifierContext {
  @NonNull @Getter private final AzureMonitorClient azureMonitorClient;

  @Builder
  public SwapSlotStatusVerifierContext(@NonNull LogCallback logCallback, @NonNull String slotName,
      @NonNull AzureWebClient azureWebClient, @NonNull AzureWebClientContext azureWebClientContext,
      AzureMonitorClient azureMonitorClient, Mono<Response<Void>> responseMono) {
    super(logCallback, slotName, azureWebClient, azureWebClientContext, responseMono);
    this.azureMonitorClient = azureMonitorClient;
  }
}
