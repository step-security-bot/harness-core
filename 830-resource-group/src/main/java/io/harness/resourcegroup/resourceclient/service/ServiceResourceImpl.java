package io.harness.resourcegroup.resourceclient.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.resourcegroup.beans.ValidatorType.DYNAMIC;
import static io.harness.resourcegroup.beans.ValidatorType.NESTED_DYNAMIC;
import static io.harness.resourcegroup.beans.ValidatorType.STATIC;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.ng.core.service.dto.ServiceResponse;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceInfo;
import io.harness.service.remote.ServiceResourceClient;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class ServiceResourceImpl implements Resource {
  @Inject ServiceResourceClient serviceResourceClient;
  @Override
  public String getType() {
    return "SERVICE";
  }

  @Override
  public Set<ScopeLevel> getValidScopeLevels() {
    return EnumSet.of(ScopeLevel.ACCOUNT, ScopeLevel.ORGANIZATION, ScopeLevel.PROJECT);
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.of(EventsFrameworkMetadataConstants.SERVICE_ENTITY);
  }

  @Override
  public ResourceInfo getResourceInfoFromEvent(Message message) {
    EntityChangeDTO entityChangeDTO = null;
    try {
      entityChangeDTO = EntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking EntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(entityChangeDTO)) {
      return null;
    }
    return ResourceInfo.builder()
        .accountIdentifier(stripToNull(entityChangeDTO.getAccountIdentifier().getValue()))
        .orgIdentifier(stripToNull(entityChangeDTO.getOrgIdentifier().getValue()))
        .projectIdentifier(stripToNull(entityChangeDTO.getProjectIdentifier().getValue()))
        .resourceType(getType())
        .resourceIdentifier(entityChangeDTO.getIdentifier().getValue())
        .build();
  }

  @Override
  public List<Boolean> validate(List<String> resourceIds, Scope scope) {
    if (isEmpty(resourceIds)) {
      return Collections.emptyList();
    }
    List<ServiceResponse> serviceResponses =
        NGRestUtils
            .getResponse(
                serviceResourceClient.listServicesForProject(0, resourceIds.size(), scope.getAccountIdentifier(),
                    scope.getOrgIdentifier(), scope.getProjectIdentifier(), resourceIds, null))
            .getContent();
    Set<String> validResourceId =
        serviceResponses.stream().map(e -> e.getService().getIdentifier()).collect(Collectors.toSet());
    return resourceIds.stream().map(validResourceId::contains).collect(Collectors.toList());
  }

  @Override
  public Map<ScopeLevel, EnumSet<ValidatorType>> getSelectorKind() {
    return ImmutableMap.of(ScopeLevel.ACCOUNT, EnumSet.of(STATIC, DYNAMIC, NESTED_DYNAMIC), ScopeLevel.ORGANIZATION,
        EnumSet.of(STATIC, DYNAMIC, NESTED_DYNAMIC), ScopeLevel.PROJECT, EnumSet.of(STATIC, DYNAMIC));
  }
}
