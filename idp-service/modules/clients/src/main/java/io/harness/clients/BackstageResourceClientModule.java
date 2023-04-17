/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.clients;

import static io.harness.annotations.dev.HarnessTeam.IDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

@OwnedBy(IDP)
public class BackstageResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig backstageClientConfig;

  @Inject
  public BackstageResourceClientModule(ServiceHttpClientConfig backstageClientConfig) {
    this.backstageClientConfig = backstageClientConfig;
  }

  @Provides
  @Singleton
  private BackstageResourceClientHttpFactory backstageResourceClientHttpFactory() {
    return new BackstageResourceClientHttpFactory(this.backstageClientConfig);
  }

  @Override
  protected void configure() {
    this.bind(BackstageResourceClient.class).toProvider(BackstageResourceClientHttpFactory.class).in(Scopes.SINGLETON);
  }
}
