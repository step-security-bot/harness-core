/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.principals.usergroups.events;

import static io.harness.accesscontrol.principals.usergroups.events.UserGroupEventConsumer.USER_GROUP_ENTITY_TYPE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.authorization.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;

import static java.time.Duration.ofNanos;
import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.accesscontrol.commons.metrics.AccessControlMetricsContext;
import io.harness.accesscontrol.principals.usergroups.HarnessUserGroupService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.metrics.service.api.MetricService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@Slf4j
public class UserGroupEventHandler implements EventHandler {
  public static final String ACCESS_CONTROL_ENTITY_PROCESSING_TIME = "access_control_entity_processing_time";
  private final HarnessUserGroupService harnessUserGroupService;
  private final MetricService metricService;

  @Inject
  public UserGroupEventHandler(HarnessUserGroupService harnessUserGroupService, MetricService metricService) {
    this.harnessUserGroupService = harnessUserGroupService;
    this.metricService = metricService;
  }

  @Override
  public boolean handle(Message message) {
    EntityChangeDTO entityChangeDTO = null;
    long eventReadTime = System.nanoTime();
    try {
      entityChangeDTO = EntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking EntityChangeDTO for user group event with key {}", message.getId(), e);
    }
    if (Objects.isNull(entityChangeDTO)) {
      return true;
    }
    log.debug(String.format("Processing event id: %s User Group %s %s", message.getId(),
        stripToNull(entityChangeDTO.getIdentifier().getValue()), getEventType(message)));
    try {
      HarnessScopeParams params = HarnessScopeParams.builder()
                                      .accountIdentifier(stripToNull(entityChangeDTO.getAccountIdentifier().getValue()))
                                      .orgIdentifier(stripToNull(entityChangeDTO.getOrgIdentifier().getValue()))
                                      .projectIdentifier(stripToNull(entityChangeDTO.getProjectIdentifier().getValue()))
                                      .build();
      Scope scope = ScopeMapper.fromParams(params);
      if (getEventType(message).equals(DELETE_ACTION)) {
        harnessUserGroupService.deleteIfPresent(stripToNull(entityChangeDTO.getIdentifier().getValue()), scope);
      } else {
        harnessUserGroupService.sync(stripToNull(entityChangeDTO.getIdentifier().getValue()), scope);
      }
    } catch (Exception e) {
      log.error("Could not process the resource group change event {} due to error", entityChangeDTO, e);
      return false;
    } finally {
      long eventProcessingTime = System.nanoTime() - eventReadTime;
      try (AccessControlMetricsContext context =
               new AccessControlMetricsContext(stripToNull(entityChangeDTO.getIdentifier().getValue()),
                   USER_GROUP_ENTITY_TYPE, getEventType(message), ACCESS_CONTROL_SERVICE.getServiceId())) {
        metricService.recordDuration(ACCESS_CONTROL_ENTITY_PROCESSING_TIME, ofNanos(eventProcessingTime));
      }
    }
    return true;
  }

  private String getEventType(Message message) {
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    return metadataMap.get(ACTION);
  }
}
