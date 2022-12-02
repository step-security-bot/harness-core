/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.environment.helper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.environment.filters.Entity;
import io.harness.cdng.environment.filters.FilterType;
import io.harness.cdng.environment.filters.FilterYaml;
import io.harness.cdng.environment.filters.TagsFilter;
import io.harness.cdng.gitops.service.ClusterService;
import io.harness.data.structure.CollectionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.gitops.models.Cluster;
import io.harness.gitops.models.ClusterQuery;
import io.harness.gitops.remote.GitopsResourceClient;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.domain.Page;
import retrofit2.Response;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
@Singleton
public class EnvironmentInfraFilterHelper {
  public static final int PAGE_SIZE = 1000;
  public static final String TAGFILTER_MATCHTYPE_ALL = "all";
  public static final String TAGFILTER_MATCHTYPE_ANY = "any";
  @Inject private GitopsResourceClient gitopsResourceClient;
  @Inject private ClusterService clusterService;

  private static final RetryPolicy<Object> retryPolicyForGitopsClustersFetch = RetryUtils.getRetryPolicy(
      "Error getting clusters from Harness Gitops..retrying", "Failed to fetch clusters from Harness Gitops",
      Collections.singletonList(IOException.class), Duration.ofMillis(10), 3, log);

  public boolean areAllTagFiltersMatching(List<NGTag> entityTags, List<NGTag> tagsInFilter) {
    int count = 0;
    for (NGTag tag : entityTags) {
      if (tagsInFilter.contains(tag)) {
        count++;
      }
    }
    return count != 0 && count == entityTags.size();
  }

  public boolean areAnyTagFiltersMatching(List<NGTag> entityTags, List<NGTag> tagsInFilter) {
    for (NGTag tag : entityTags) {
      if (tagsInFilter.contains(tag)) {
        return true;
      }
    }
    return false;
  }

  /**
   *
   * @param filterYaml - Contains the information of filters along with it's type
   * @param envs - List of environments to apply filters on
   * @return - List of filtered Environments
   */
  public Set<Environment> processTagsFilterYamlForEnvironments(FilterYaml filterYaml, Set<Environment> envs) {
    if (filterYaml.getType().name().equals(FilterType.all.name())) {
      return envs;
    }
    // filter env that match all tags
    Set<Environment> filteredEnvs = new HashSet<>();
    if (filterYaml.getType().equals(FilterType.tags)) {
      TagsFilter tagsFilter = (TagsFilter) filterYaml.getSpec();
      for (Environment environment : envs) {
        if (applyMatchAllFilter(environment.getTags(), tagsFilter)) {
          filteredEnvs.add(environment);
          continue;
        }
        if (applyMatchAnyFilter(environment.getTags(), tagsFilter)) {
          filteredEnvs.add(environment);
          continue;
        }
        if (isSupportedFilter(tagsFilter)) {
          throw new InvalidRequestException(
              String.format("TagFilter of type [%s] is not supported", tagsFilter.getMatchType().name()));
        }
      }
    }

    return filteredEnvs;
  }

  private boolean applyMatchAnyFilter(List<NGTag> entityTags, TagsFilter tagsFilter) {
    return tagsFilter.getMatchType().name().equals(TAGFILTER_MATCHTYPE_ANY)
        && areAnyTagFiltersMatching(entityTags, TagMapper.convertToList(tagsFilter.getTags()));
  }

  private boolean applyMatchAllFilter(List<NGTag> entityTags, TagsFilter tagsFilter) {
    return tagsFilter.getMatchType().name().equals(TAGFILTER_MATCHTYPE_ALL)
        && areAllTagFiltersMatching(entityTags, TagMapper.convertToList(tagsFilter.getTags()));
  }

  /**
   *
   * @param filterYaml - Contains the information of filters along with it's type
   * @param clusters - List of clusters to apply filters on
   * @param ngGitOpsClusters - Cluster Entity containing tag information for applying filtering
   * @return - List of filtered Clusters
   */
  public List<io.harness.cdng.gitops.entity.Cluster> processTagsFilterYamlForGitOpsClusters(FilterYaml filterYaml,
      Set<Cluster> clusters, Map<String, io.harness.cdng.gitops.entity.Cluster> ngGitOpsClusters) {
    if (filterYaml.getType().name().equals(FilterType.all.name())) {
      return ngGitOpsClusters.values().stream().collect(Collectors.toList());
    }

    List<io.harness.cdng.gitops.entity.Cluster> filteredClusters = new ArrayList<>();
    if (filterYaml.getType().equals(FilterType.tags)) {
      TagsFilter tagsFilter = (TagsFilter) filterYaml.getSpec();
      for (Cluster cluster : clusters) {
        if (applyMatchAllFilter(TagMapper.convertToList(cluster.getTags()), tagsFilter)) {
          filteredClusters.add(ngGitOpsClusters.get(cluster.getIdentifier()));
          continue;
        }
        if (applyMatchAnyFilter(TagMapper.convertToList(cluster.getTags()), tagsFilter)) {
          filteredClusters.add(ngGitOpsClusters.get(cluster.getIdentifier()));
          continue;
        }
        if (isSupportedFilter(tagsFilter)) {
          throw new InvalidRequestException(
              String.format("TagFilter of type [%s] is not supported", tagsFilter.getMatchType().name()));
        }
      }
    }
    return filteredClusters;
  }

  private static boolean isSupportedFilter(TagsFilter tagsFilter) {
    return !tagsFilter.getMatchType().name().equals(TAGFILTER_MATCHTYPE_ALL)
        && !tagsFilter.getMatchType().name().equals(TAGFILTER_MATCHTYPE_ANY);
  }

  /**
   *
   * @param environments - List of environments
   * @param filterYamls - List of FilterYamls
   * @return Applies filters on Environments Entity. Returns the same list of no filter is applied.
   * Throws exception if environments qualify after applying filters
   */
  public Set<Environment> applyFiltersOnEnvs(Set<Environment> environments, Iterable<FilterYaml> filterYamls) {
    Set<Environment> setOfFilteredEnvs = new HashSet<>();

    boolean filterOnEnvExists = false;
    for (FilterYaml filterYaml : filterYamls) {
      if (filterYaml.getEntities().contains(Entity.environments)) {
        filterOnEnvExists = true;
        setOfFilteredEnvs.addAll(processTagsFilterYamlForEnvironments(filterYaml, environments));
      }
    }

    if (!filterOnEnvExists) {
      setOfFilteredEnvs.addAll(environments);
    }

    if (isEmpty(setOfFilteredEnvs) && filterOnEnvExists) {
      log.info("No Environments are eligible for deployment due to applied filters");
    }
    return setOfFilteredEnvs;
  }

  /**
   *
   * @param filterYamls - List of FilterYamls
   * @param clsToCluster - Map of clusterRef to NG GitOps Cluster Entity
   * @param clusters - List of NG GitOpsClusters
   * @return Applies Filters on GitOpsClusters. Returns the same list of no filter is applied.
   * Throws exception if no clusters qualify after applying filters.
   */
  public Set<io.harness.cdng.gitops.entity.Cluster> applyFilteringOnClusters(Iterable<FilterYaml> filterYamls,
      Map<String, io.harness.cdng.gitops.entity.Cluster> clsToCluster, Set<io.harness.gitops.models.Cluster> clusters) {
    Set<io.harness.cdng.gitops.entity.Cluster> setOfFilteredCls = new HashSet<>();

    boolean filterOnClusterExists = false;
    for (FilterYaml filterYaml : filterYamls) {
      if (filterYaml.getEntities().contains(Entity.gitOpsClusters)) {
        setOfFilteredCls.addAll(processTagsFilterYamlForGitOpsClusters(filterYaml, clusters, clsToCluster));
        filterOnClusterExists = true;
      }
    }

    if (!filterOnClusterExists) {
      setOfFilteredCls.addAll(setOfFilteredCls);
    }

    if (isEmpty(setOfFilteredCls) && filterOnClusterExists) {
      log.info("No GitOps cluster is eligible after applying filters");
    }
    return setOfFilteredCls;
  }

  /**
   * @param accountId
   * @param orgId
   * @param projectId
   * @param clsRefs - List of clusters for fetching tag information
   * @return Fetch GitOps Clusters from GitOpsService. Throw exception if unable to connect to gitOpsService or if no
   *     clusters are returned
   */
  public List<io.harness.gitops.models.Cluster> fetchClustersFromGitOps(
      String accountId, String orgId, String projectId, Set<String> clsRefs) {
    Map<String, Object> filter = ImmutableMap.of("identifier", ImmutableMap.of("$in", clsRefs));
    final ClusterQuery query = ClusterQuery.builder()
                                   .accountId(accountId)
                                   .orgIdentifier(orgId)
                                   .projectIdentifier(projectId)
                                   .pageIndex(0)
                                   .pageSize(clsRefs.size())
                                   .filter(filter)
                                   .build();
    final Response<PageResponse<Cluster>> response =
        Failsafe.with(retryPolicyForGitopsClustersFetch).get(() -> gitopsResourceClient.listClusters(query).execute());

    List<io.harness.gitops.models.Cluster> clusterList;
    if (response.isSuccessful() && response.body() != null) {
      clusterList = CollectionUtils.emptyIfNull(response.body().getContent());
    } else {
      throw new InvalidRequestException("Failed to fetch clusters from gitops-service, cannot apply filter");
    }
    return clusterList;
  }

  /**
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @param envRefs
   * @return Fetch NGGitOps Clusters. These are clusters that are linked in Environments section. Throw Exception if no
   *     clusters are linked.
   */
  public Map<String, io.harness.cdng.gitops.entity.Cluster> getClusterRefToNGGitOpsClusterMap(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<String> envRefs) {
    Page<io.harness.cdng.gitops.entity.Cluster> clusters =
        clusterService.listAcrossEnv(0, PAGE_SIZE, accountIdentifier, orgIdentifier, projectIdentifier, envRefs);

    if (isEmpty(clusters.getContent())) {
      log.info("There are no gitOpsClusters linked to Environments");
    }

    Map<String, io.harness.cdng.gitops.entity.Cluster> clsToCluster = new HashMap<>();
    clusters.getContent().forEach(k -> clsToCluster.put(k.getClusterRef(), k));
    return clsToCluster;
  }
}
