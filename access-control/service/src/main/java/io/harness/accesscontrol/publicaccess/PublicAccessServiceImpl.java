/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.publicaccess;

import static io.harness.accesscontrol.publicaccess.PublicAccessUtils.PUBLIC_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.accesscontrol.publicaccess.PublicAccessUtils.PUBLIC_RESOURCE_GROUP_NAME;
import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupFactory;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.resources.resourcetypes.ResourceType;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.accesscontrol.roles.persistence.RoleDBO.RoleDBOKeys;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeLevel;
import io.harness.exception.InvalidRequestException;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.v2.model.ResourceFilter;
import io.harness.resourcegroup.v2.model.ResourceSelector;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupRequest;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.spec.server.accesscontrol.v1.model.Scope;
import io.harness.utils.CryptoUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PL)
@Slf4j
public class PublicAccessServiceImpl implements PublicAccessService {
  private final RoleAssignmentService roleAssignmentService;
  private final ResourceGroupClient resourceGroupClient;
  private final ResourceGroupService resourceGroupService;
  private final ResourceGroupFactory resourceGroupFactory;

  private final MongoTemplate mongoTemplate;

  private final ScopeService scopeService;

  private final PublicAccessUtils publicAccessUtil;
  List<PublicAccessRoleAssignmentMapping> publicAccessRoleAssignmentMappings;

  @Inject
  public PublicAccessServiceImpl(RoleAssignmentService roleAssignmentService,
      @Named("PRIVILEGED") ResourceGroupClient resourceGroupClient, ResourceGroupService resourceGroupService,
      ResourceGroupFactory resourceGroupFactory, MongoTemplate mongoTemplate, ScopeService scopeService,
      PublicAccessUtils publicAccessUtil) {
    this.roleAssignmentService = roleAssignmentService;
    this.resourceGroupClient = resourceGroupClient;
    this.resourceGroupService = resourceGroupService;
    this.resourceGroupFactory = resourceGroupFactory;
    this.mongoTemplate = mongoTemplate;
    this.scopeService = scopeService;
    this.publicAccessUtil = publicAccessUtil;
    this.publicAccessRoleAssignmentMappings = publicAccessUtil.getPublicAccessRoleAssignmentMapping();
  }
  @Override
  public boolean enable(String resourceIdentifier, ResourceType resourceType, Scope resourceScope) {
    Optional<ResourceGroupResponse> existingResourceGroupOptional = Optional.ofNullable(
        NGRestUtils.getResponse(resourceGroupClient.getResourceGroup(PUBLIC_RESOURCE_GROUP_IDENTIFIER,
            resourceScope.getAccount(), resourceScope.getOrg(), resourceScope.getProject())));
    if (existingResourceGroupOptional.isPresent()) {
      log.info("Public resource group already present in given scope");
      updatePublicResourceGroup(existingResourceGroupOptional.get(), resourceIdentifier, resourceType, resourceScope);
    } else {
      createPublicResourceGroup(resourceIdentifier, resourceType, resourceScope);
      createRoleAssignment(resourceType, resourceScope);
    }
    return true;
  }

  private void createRoleAssignment(ResourceType resourceType, Scope resourceScope) {
    HarnessScopeParams harnessScopeParams = HarnessScopeParams.builder()
                                                .accountIdentifier(resourceScope.getAccount())
                                                .orgIdentifier(resourceScope.getOrg())
                                                .projectIdentifier(resourceScope.getProject())
                                                .build();
    io.harness.accesscontrol.scopes.core.Scope scope =
        scopeService.getOrCreate(ScopeMapper.fromParams(harnessScopeParams));
    List<String> publicRoles = getPublicRoles();
    final List<PublicAccessRoleAssignmentMapping> filteredMappings =
        publicAccessRoleAssignmentMappings.stream()
            .filter(x -> resourceType.getIdentifier().equals(x.getResourceType()))
            .collect(Collectors.toList());
    List<String> mappedRoles = filteredMappings.stream()
                                   .map(PublicAccessRoleAssignmentMapping::getRoleIdentifier)
                                   .collect(Collectors.toList());
    if (!publicRoles.containsAll(mappedRoles)) {
      throw new InvalidRequestException("Unable to update public resource group", USER);
    }
    for (PublicAccessRoleAssignmentMapping publicAccessRoleAssignmentMapping : filteredMappings) {
      RoleAssignment roleAssignment = buildRoleAssignment(scope.getLevel().toString(), scope.toString(),
          publicAccessRoleAssignmentMapping.getRoleIdentifier(),
          publicAccessRoleAssignmentMapping.getPrincipalIdentifier());
      roleAssignmentService.create(roleAssignment);
    }
  }

  private List<String> getPublicRoles() {
    Criteria criteria = Criteria.where(RoleDBOKeys.isPublic).is(true);
    Query query = new Query(criteria);
    return mongoTemplate.find(query, RoleDBO.class).stream().map(RoleDBO::getIdentifier).collect(Collectors.toList());
  }

  private RoleAssignment buildRoleAssignment(
      String scopeLevel, String scopeIdentifier, String roleIdentifier, String principalIdentifier) {
    return RoleAssignment.builder()
        .identifier("role_assignment_".concat(CryptoUtils.secureRandAlphaNumString(20)))
        .scopeIdentifier(scopeIdentifier)
        .scopeLevel(scopeLevel)
        .managed(true)
        .internal(true)
        .roleIdentifier(roleIdentifier)
        .resourceGroupIdentifier(PUBLIC_RESOURCE_GROUP_IDENTIFIER)
        .principalIdentifier(principalIdentifier)
        .principalType(PrincipalType.USER)
        .build();
  }

  private ResourceGroupResponse updatePublicResourceGroup(ResourceGroupResponse existingResourceGroup,
      String resourceIdentifier, ResourceType resourceType, Scope resourceScope) {
    final ResourceGroupDTO resourceGroup = existingResourceGroup.getResourceGroup();
    ResourceFilter resourceFilter = resourceGroup.getResourceFilter();
    List<ResourceSelector> resourceSelectors = new ArrayList<>();
    if (resourceFilter != null) {
      resourceSelectors = resourceFilter.getResources();
    }
    List<String> existingResources = resourceSelectors.stream()
                                         .filter(x -> x.getResourceType().equals(resourceType.getIdentifier()))
                                         .map(ResourceSelector::getIdentifiers)
                                         .collect(Collectors.toList())
                                         .get(0);

    if (existingResources.contains(resourceIdentifier)) {
      throw new InvalidRequestException(
          String.format("Resource with identifier [%s] and type [%s] is already marked as public", resourceIdentifier,
              resourceType.getIdentifier()),
          USER);
    }
    ResourceGroupDTO updatedResourceGroup =
        ResourceGroupDTO.builder()
            .identifier(PUBLIC_RESOURCE_GROUP_IDENTIFIER)
            .name(PUBLIC_RESOURCE_GROUP_NAME)
            .projectIdentifier(resourceScope.getProject())
            .orgIdentifier(resourceScope.getOrg())
            .accountIdentifier(resourceScope.getAccount())
            .includedScopes(resourceGroup.getIncludedScopes())
            .allowedScopeLevels(resourceGroup.getAllowedScopeLevels())
            .resourceFilter(
                ResourceFilter.builder()
                    .resources(buildResourceGroupSelector(resourceSelectors, resourceIdentifier, resourceType))
                    .build())
            .build();
    Optional<ResourceGroupResponse> resourceGroupResponse =
        Optional.ofNullable(NGRestUtils.getResponse(resourceGroupClient.updateResourceGroup(
            PUBLIC_RESOURCE_GROUP_IDENTIFIER, resourceScope.getAccount(), resourceScope.getOrg(),
            resourceScope.getProject(), ResourceGroupRequest.builder().resourceGroup(updatedResourceGroup).build())));
    if (resourceGroupResponse.isEmpty()) {
      throw new InvalidRequestException("Unable to update public resource group", USER);
    }
    return resourceGroupResponse.get();
  }

  private ResourceGroupResponse createPublicResourceGroup(
      String resourceIdentifier, ResourceType resourceType, Scope resourceScope) {
    List<ResourceSelector> resourceSelectors = new ArrayList<>();
    ResourceGroupDTO publicResourceGroup =
        ResourceGroupDTO.builder()
            .identifier(PUBLIC_RESOURCE_GROUP_IDENTIFIER)
            .name(PUBLIC_RESOURCE_GROUP_NAME)
            .projectIdentifier(resourceScope.getProject())
            .orgIdentifier(resourceScope.getOrg())
            .accountIdentifier(resourceScope.getAccount())
            .allowedScopeLevels(Sets.newHashSet(
                ScopeLevel.of(resourceScope.getAccount(), resourceScope.getOrg(), resourceScope.getProject())
                    .toString()
                    .toLowerCase()))
            .resourceFilter(
                ResourceFilter.builder()
                    .resources(buildResourceGroupSelector(resourceSelectors, resourceIdentifier, resourceType))
                    .build())
            .build();

    Optional<ResourceGroupResponse> resourceGroupResponse = Optional.ofNullable(NGRestUtils.getResponse(
        resourceGroupClient.createResourceGroup(resourceScope.getAccount(), resourceScope.getOrg(),
            resourceScope.getProject(), ResourceGroupRequest.builder().resourceGroup(publicResourceGroup).build())));

    if (resourceGroupResponse.isEmpty()) {
      throw new InvalidRequestException("Unable to create public resource group", USER);
    }
    try {
      resourceGroupService.upsert(resourceGroupFactory.buildResourceGroup(resourceGroupResponse.get()));
    } catch (Exception e) {
      throw new InvalidRequestException("Unable to sync public resource group.", USER);
    }
    return resourceGroupResponse.get();
  }

  private List<ResourceSelector> buildResourceGroupSelector(
      List<ResourceSelector> resourceSelectors, String resourceIdentifier, ResourceType resourceType) {
    for (Iterator<ResourceSelector> iterator = resourceSelectors.iterator(); iterator.hasNext();) {
      ResourceSelector resourceSelector = iterator.next();
      if (resourceSelector != null && resourceSelector.getResourceType().equals(resourceType.getIdentifier())) {
        resourceSelector.getIdentifiers().add(resourceIdentifier);
        return resourceSelectors;
      }
    }
    resourceSelectors.add(ResourceSelector.builder()
                              .resourceType(resourceType.getIdentifier())
                              .identifiers(List.of(resourceIdentifier))
                              .build());
    return resourceSelectors;
  }
}
