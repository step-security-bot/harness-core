package software.wings.graphql.datafetcher.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagFilter;
import software.wings.graphql.schema.type.instance.QLInstanceType;
import software.wings.service.intfc.HarnessTagService;

import java.util.List;
import java.util.Set;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
@Slf4j
public class InstanceQueryHelper {
  @Inject protected DataFetcherUtils utils;
  @Inject protected HarnessTagService tagService;

  public void setQuery(String accountId, List<QLInstanceFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Instance>> field;

      if (filter.getApplication() != null) {
        field = query.field("appId");
        QLIdFilter applicationFilter = filter.getApplication();
        utils.setIdFilter(field, applicationFilter);
      }

      if (filter.getCloudProvider() != null) {
        field = query.field("computeProviderId");
        QLIdFilter cloudProviderFilter = filter.getCloudProvider();
        utils.setIdFilter(field, cloudProviderFilter);
      }

      if (filter.getEnvironment() != null) {
        field = query.field("envId");
        QLIdFilter envFilter = filter.getEnvironment();
        utils.setIdFilter(field, envFilter);
      }

      if (filter.getService() != null) {
        field = query.field("serviceId");
        QLIdFilter serviceFilter = filter.getService();
        utils.setIdFilter(field, serviceFilter);
      }

      if (filter.getCreatedAt() != null) {
        field = query.field("createdAt");
        QLTimeFilter createdAtFilter = filter.getCreatedAt();
        utils.setTimeFilter(field, createdAtFilter);
      }

      if (filter.getInstanceType() != null) {
        field = query.field("instanceType");
        QLInstanceType instanceTypeFilter = filter.getInstanceType();
        utils.setStringFilter(field,
            QLStringFilter.builder()
                .operator(QLStringOperator.EQUALS)
                .values(new String[] {instanceTypeFilter.name()})
                .build());
      }

      if (filter.getTagFilter() != null) {
        QLTagFilter tagFilter = filter.getTagFilter();
        Set<String> entityIds = tagService.getEntityIdsWithTag(
            accountId, tagFilter.getName(), tagFilter.getEntityType(), tagFilter.getValues());
        if (isNotEmpty(entityIds)) {
          switch (tagFilter.getEntityType()) {
            case APPLICATION:
              query.field("appId").in(entityIds);
              break;
            case SERVICE:
              query.field("serviceId").in(entityIds);
              break;
            case ENVIRONMENT:
              query.field("envId").in(entityIds);
              break;
            case WORKFLOW:
              query.field("workflowId").in(entityIds);
              break;
            case PIPELINE:
              query.field("pipelineId").in(entityIds);
              break;
            case CLOUD_PROVIDER:
            case CONNECTOR:
              query.field("computeProviderId").in(entityIds);
              break;
            default:
              logger.error("EntityType {} not supported in query", tagFilter.getEntityType());
              throw new InvalidRequestException("Error while compiling query", WingsException.USER);
          }
        }
      }
    });
  }
}
