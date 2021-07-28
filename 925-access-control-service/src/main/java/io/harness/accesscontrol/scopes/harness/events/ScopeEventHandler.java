package io.harness.accesscontrol.scopes.harness.events;

import static io.harness.accesscontrol.common.filter.ManagedFilter.ONLY_CUSTOM;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ACCOUNT;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ORGANIZATION;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.PROJECT;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeParams;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Slf4j
public class ScopeEventHandler implements EventHandler {
  private final RoleAssignmentService roleAssignmentService;
  private final RoleService roleService;
  private final ScopeService scopeService;

  @Inject
  public ScopeEventHandler(
      RoleAssignmentService roleAssignmentService, RoleService roleService, ScopeService scopeService) {
    this.roleAssignmentService = roleAssignmentService;
    this.roleService = roleService;
    this.scopeService = scopeService;
  }

  @Override
  public boolean handle(Message message) {
    try {
      Optional<Scope> scope = Optional.empty();
      String entityType = message.getMessage().getMetadataMap().get(ENTITY_TYPE);
      if (ORGANIZATION.getEventEntityName().equals(entityType)) {
        scope = buildOrganizationScope(message);
      } else if (entityType.equals(PROJECT.getEventEntityName())) {
        scope = buildProjectScope(message);
      } else if (entityType.equals(ACCOUNT.getEventEntityName())) {
        scope = buildAccountScope(message);
      }
      if (scope.isPresent()) {
        deleteRoleAssignments(scope.get());
        deleteRoles(scope.get());
      }
    } catch (Exception e) {
      log.error("Could not process the message due to error", e);
      return false;
    }
    return true;
  }

  private Optional<Scope> buildAccountScope(Message message) {
    AccountEntityChangeDTO accountEntityChangeDTO = null;
    try {
      accountEntityChangeDTO = AccountEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking AccountEntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(accountEntityChangeDTO)) {
      return Optional.empty();
    }
    ScopeParams scopeParams =
        HarnessScopeParams.builder().accountIdentifier(stripToNull(accountEntityChangeDTO.getAccountId())).build();
    log.info("Handling delete event for Account {}", scopeParams);
    return Optional.of(scopeService.buildScopeFromParams(scopeParams));
  }

  private Optional<Scope> buildOrganizationScope(Message message) {
    OrganizationEntityChangeDTO organizationEntityChangeDTO = null;
    try {
      organizationEntityChangeDTO = OrganizationEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking OrganizationEntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(organizationEntityChangeDTO)) {
      return Optional.empty();
    }
    ScopeParams scopeParams = HarnessScopeParams.builder()
                                  .accountIdentifier(stripToNull(organizationEntityChangeDTO.getAccountIdentifier()))
                                  .orgIdentifier(stripToNull(organizationEntityChangeDTO.getIdentifier()))
                                  .build();
    log.info("Handling Delete event for Organization {}", scopeParams);
    return Optional.of(scopeService.buildScopeFromParams(scopeParams));
  }

  private Optional<Scope> buildProjectScope(Message message) {
    ProjectEntityChangeDTO projectEntityChangeDTO = null;
    try {
      projectEntityChangeDTO = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking ProjectEntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(projectEntityChangeDTO)) {
      return Optional.empty();
    }
    ScopeParams scopeParams = HarnessScopeParams.builder()
                                  .accountIdentifier(stripToNull(projectEntityChangeDTO.getAccountIdentifier()))
                                  .orgIdentifier(stripToNull(projectEntityChangeDTO.getOrgIdentifier()))
                                  .projectIdentifier(stripToNull(projectEntityChangeDTO.getIdentifier()))
                                  .build();
    log.info("Handling Delete event for Project {}", scopeParams);
    return Optional.of(scopeService.buildScopeFromParams(scopeParams));
  }

  private void deleteRoleAssignments(Scope scope) {
    RoleAssignmentFilter roleAssignmentFilter = RoleAssignmentFilter.builder().scopeFilter(scope.toString()).build();
    roleAssignmentService.deleteMulti(roleAssignmentFilter);
  }

  private void deleteRoles(Scope scope) {
    RoleFilter roleFilter = RoleFilter.builder().scopeIdentifier(scope.toString()).managedFilter(ONLY_CUSTOM).build();
    roleService.deleteMulti(roleFilter);
  }
}
