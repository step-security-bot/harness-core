package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import io.harness.AuthorizationServiceHeader;
import io.harness.EntityType;
import io.harness.SCMGrpcClientModule;
import io.harness.ScmConnectionConfig;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.entityInfo.EntityGitPersistenceHelperService;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitAwarePersistenceImpl;
import io.harness.gitsync.persistance.GitAwareRepository;
import io.harness.gitsync.persistance.GitAwareRepositoryImpl;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.grpc.client.GrpcClientConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OwnedBy(DX)
public abstract class AbstractGitSyncSdkModule extends AbstractModule {
  private static final String GIT_SYNC_SDK = "GitSyncSdk";
  @Override
  protected void configure() {
    install(new SCMGrpcClientModule(getScmConnectionConfig()));
    install(GitSyncSdkModule.getInstance());
    if (getGitSyncSdkConfiguration().getEventsRedisConfig().getRedisUrl().equals("dummyRedisUrl")) {
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.GIT_CONFIG_STREAM))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
    } else {
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH,
              getGitSyncSdkConfiguration().getEventsRedisConfig(),
              EventsFrameworkConstants.HARNESS_TO_GIT_PUSH_MAX_TOPIC_SIZE,
              getAuthorizationServiceHeader().getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.GIT_CONFIG_STREAM))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.GIT_CONFIG_STREAM,
              getGitSyncSdkConfiguration().getServiceHeader().getServiceId(),
              getGitSyncSdkConfiguration().getEventsRedisConfig(),
              EventsFrameworkConstants.GIT_CONFIG_STREAM_PROCESSING_TIME,
              EventsFrameworkConstants.GIT_CONFIG_STREAM_READ_BATCH_SIZE));
    }
    final Set<GitSyncEntitiesConfiguration> gitSyncEntitiesConfiguration =
        getGitSyncSdkConfiguration().getGitSyncEntitiesConfiguration();
    gitSyncEntitiesConfiguration.forEach(gitSyncEntitiesConfig -> {
      final Class<? extends GitSyncableEntity> entityClass = gitSyncEntitiesConfig.getEntityClass();
      final Class<? extends YamlDTO> yamlClass = gitSyncEntitiesConfig.getYamlClass();
      Class<? extends EntityGitPersistenceHelperService<?, ?>> entityHelperClass =
          gitSyncEntitiesConfig.getEntityHelperClass();

      bindGitAware(entityClass, yamlClass).toInstance(new GitAwarePersistenceImpl(entityClass, yamlClass));

      bindGitRepository(entityClass, yamlClass)
          .toInstance(new GitAwareRepositoryImpl<>(new GitAwarePersistenceImpl(entityClass, yamlClass)));

      final MapBinder<String, EntityGitPersistenceHelperService> gitPersistenceHelperServiceMapBinder =
          MapBinder.newMapBinder(binder(), String.class, EntityGitPersistenceHelperService.class);
      gitPersistenceHelperServiceMapBinder.addBinding(entityClass.getCanonicalName()).to(entityHelperClass);
    });
  }

  <B extends GitSyncableEntity, Y extends YamlDTO> AnnotatedBindingBuilder<GitAwarePersistence<B, Y>> bindGitAware(
      Class<B> beanClass, Class<Y> yamlClass) {
    ParameterizedType type = Types.newParameterizedType(GitAwarePersistence.class, yamlClass, beanClass);
    TypeLiteral<GitAwarePersistence<B, Y>> typeLiteral = (TypeLiteral<GitAwarePersistence<B, Y>>) TypeLiteral.get(type);
    return bind(typeLiteral);
  }

  <B extends GitSyncableEntity, Y extends YamlDTO> AnnotatedBindingBuilder<GitAwareRepository<B, Y>> bindGitRepository(
      Class<B> beanClass, Class<Y> yamlClass) {
    ParameterizedType type = Types.newParameterizedType(GitAwareRepository.class, yamlClass, beanClass);
    TypeLiteral<GitAwareRepository<B, Y>> typeLiteral = (TypeLiteral<GitAwareRepository<B, Y>>) TypeLiteral.get(type);
    return bind(typeLiteral);
  }

  public abstract GitSyncSdkConfiguration getGitSyncSdkConfiguration();

  @Provides
  @Singleton
  public ScmConnectionConfig getScmConnectionConfig() {
    return getGitSyncSdkConfiguration().getScmConnectionConfig();
  }

  @Provides
  @Singleton
  public GitSyncSdkConfiguration gitSyncSdkConfiguration() {
    return getGitSyncSdkConfiguration();
  }

  @Provides
  @Singleton
  @Named("GitSyncGrpcClientConfig")
  public GrpcClientConfig grpcClientConfig() {
    return getGitSyncSdkConfiguration().getGrpcClientConfig();
  }

  @Provides
  @Singleton
  @Named("GitSyncSortOrder")
  public Supplier<List<EntityType>> getSortOrder() {
    return getGitSyncSdkConfiguration().getGitSyncSortOrder();
  }

  @Provides
  @Singleton
  @Named("git-msvc")
  public AuthorizationServiceHeader getAuthorizationServiceHeader() {
    return getGitSyncSdkConfiguration().getServiceHeader();
  }

  @Provides
  @Singleton
  @Named("GitSyncEntityConfigurations")
  public Map<EntityType, GitSyncEntitiesConfiguration> getGitSyncEntityConfigurations() {
    return emptyIfNull(getGitSyncSdkConfiguration().getGitSyncEntitiesConfiguration())
        .stream()
        .collect(Collectors.toMap(GitSyncEntitiesConfiguration::getEntityType, Function.identity()));
  }
}
