/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.SSCA;
import static io.harness.authorization.AuthorizationServiceHeader.SSCA_SERVICE;

import static org.modelmapper.convention.MatchingStrategies.STRICT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.serializer.KryoRegistrar;
import io.harness.spec.server.ssca.v1.EnforcementResultApi;
import io.harness.spec.server.ssca.v1.EnforcementSummaryApi;
import io.harness.spec.server.ssca.v1.NormalizeSbomApi;
import io.harness.spec.server.ssca.v1.SbomProcessorApi;
import io.harness.spec.server.ssca.v1.TokenApi;
import io.harness.spec.server.ssca.v1.model.EnforcementSummaryDTO;
import io.harness.ssca.api.EnforcementResultApiImpl;
import io.harness.ssca.api.EnforcementSummaryApiImpl;
import io.harness.ssca.api.NormalizedSbomApiImpl;
import io.harness.ssca.api.SbomProcessorApiImpl;
import io.harness.ssca.api.TokenApiImpl;
import io.harness.ssca.entities.EnforcementSummaryEntity;
import io.harness.ssca.serializer.SSCAManagerModuleRegistrars;
import io.harness.ssca.services.ArtifactService;
import io.harness.ssca.services.ArtifactServiceImpl;
import io.harness.ssca.services.EnforceSBOMWorkflowService;
import io.harness.ssca.services.EnforceSBOMWorkflowServiceImpl;
import io.harness.ssca.services.EnforcementResultService;
import io.harness.ssca.services.EnforcementResultServiceImpl;
import io.harness.ssca.services.EnforcementSummaryService;
import io.harness.ssca.services.EnforcementSummaryServiceImpl;
import io.harness.ssca.services.NextGenService;
import io.harness.ssca.services.NextGenServiceImpl;
import io.harness.ssca.services.NormalizeSbomService;
import io.harness.ssca.services.NormalizeSbomServiceImpl;
import io.harness.ssca.services.ProcessSbomWorkflowService;
import io.harness.ssca.services.ProcessSbomWorkflowServiceImpl;
import io.harness.ssca.services.RuleEngineService;
import io.harness.ssca.services.RuleEngineServiceImpl;
import io.harness.ssca.utils.transformers.Transformer;
import io.harness.ssca.utils.transformers.TransformerImpl;
import io.harness.token.TokenClientModule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.morphia.converters.TypeConverter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.TypeMap;
import org.springframework.core.convert.converter.Converter;

@Slf4j
@OwnedBy(SSCA)
public class SSCAManagerModule extends AbstractModule {
  private final io.harness.SSCAManagerConfiguration configuration;

  private static SSCAManagerModule sscaManagerModule;
  private SSCAManagerModule(io.harness.SSCAManagerConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    install(new AbstractMongoModule() {
      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }
    });
    install(new io.harness.SSCAManagerModulePersistence());
    bind(HPersistence.class).to(MongoPersistence.class);
    bind(SbomProcessorApi.class).to(SbomProcessorApiImpl.class);
    bind(NormalizeSbomApi.class).to(NormalizedSbomApiImpl.class);
    bind(EnforcementResultApi.class).to(EnforcementResultApiImpl.class);
    bind(EnforcementSummaryApi.class).to(EnforcementSummaryApiImpl.class);
    bind(TokenApi.class).to(TokenApiImpl.class);
    bind(ArtifactService.class).to(ArtifactServiceImpl.class);
    bind(ProcessSbomWorkflowService.class).to(ProcessSbomWorkflowServiceImpl.class);
    bind(EnforceSBOMWorkflowService.class).to(EnforceSBOMWorkflowServiceImpl.class);
    bind(RuleEngineService.class).to(RuleEngineServiceImpl.class);
    bind(NormalizeSbomService.class).to(NormalizeSbomServiceImpl.class);
    bind(EnforcementResultService.class).to(EnforcementResultServiceImpl.class);
    bind(EnforcementSummaryService.class).to(EnforcementSummaryServiceImpl.class);
    bind(NextGenService.class).to(NextGenServiceImpl.class);
    bind(Transformer.class).to(TransformerImpl.class);
    install(new TokenClientModule(this.configuration.getNgManagerServiceHttpClientConfig(),
        this.configuration.getNgManagerServiceSecret(), SSCA_SERVICE.getServiceId()));
  }

  @Provides
  @Singleton
  @Named("ngManagerServiceHttpClientConfig")
  public ServiceHttpClientConfig ngManagerServiceHttpClientConfig() {
    return this.configuration.getNgManagerServiceHttpClientConfig();
  }

  @Provides
  @Singleton
  public ModelMapper modelMapper() {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setMatchingStrategy(STRICT);

    TypeMap<EnforcementSummaryEntity, EnforcementSummaryDTO> typeMap =
        modelMapper.createTypeMap(EnforcementSummaryEntity.class, EnforcementSummaryDTO.class);
    log.info(typeMap.getMappings().toString());
    PropertyMap<EnforcementSummaryEntity, EnforcementSummaryDTO> normalizedSbomComponentDTOPropertyMap =
        new PropertyMap<>() {
          protected void configure() {
            map().setStatus(source.getStatus());
          }
        };
    modelMapper.addMappings(normalizedSbomComponentDTOPropertyMap);
    // Object o = modelMapper.addMappings(normalizedSbomComponentDTOPropertyMap);
    /*PropertyMap<NormalizedSbomComponentDTO, NormalizedSBOMComponentEntity> normalizedSBOMComponentEntityPropertyMap =
    new PropertyMap<>() {
      @Override
      protected void configure() {
        map().setCreatedOn(Instant.ofEpochMilli(source.getCreated().longValue()));
        map().setAccountId("test");
      }
    };
    modelMapper.addMappings(normalizedSBOMComponentEntityPropertyMap);*/

    return modelMapper;
  }

  @Provides
  @Singleton
  @Named("ngManagerServiceSecret")
  public String ngManagerServiceSecret() {
    return this.configuration.getNgManagerServiceSecret();
  }

  @Provides
  @Singleton
  @Named("sscaManagerServiceSecret")
  public String sscaManagerServiceSecret() {
    return this.configuration.getSscaManagerServiceSecret();
  }

  @Provides
  @Singleton
  public Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .addAll(SSCAManagerModuleRegistrars.kryoRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(SSCAManagerModuleRegistrars.morphiaRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends TypeConverter>> morphiaConverters() {
    return ImmutableSet.<Class<? extends TypeConverter>>builder()
        .addAll(SSCAManagerModuleRegistrars.morphiaConverters)
        .build();
  }

  @Provides
  @Singleton
  List<Class<? extends Converter<?, ?>>> springConverters() {
    return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
        .addAll(SSCAManagerModuleRegistrars.springConverters)
        .build();
  }

  @Provides
  @Singleton
  public MongoConfig mongoConfig() {
    return configuration.getMongoConfig();
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return Collections.emptyMap();
  }

  public static SSCAManagerModule getInstance(io.harness.SSCAManagerConfiguration sscaManagerConfiguration) {
    if (sscaManagerModule == null) {
      return new SSCAManagerModule(sscaManagerConfiguration);
    }
    return sscaManagerModule;
  }
}
