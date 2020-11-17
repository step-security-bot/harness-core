package io.harness.notification.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

import static io.harness.NotificationRequest.*;

@Data
@Builder
@EqualsAndHashCode()
@JsonTypeName("PagerDuty")
public class PagerDutyChannel implements Channel {
  List<String> pagerDutyIntegrationKeys;
  List<String> userGroupIds;
  Map<String, String> templateData;
  String templateId;

  @Override
  public Object toObjectofProtoSchema() {
    return PagerDuty.newBuilder()
        .addAllPagerDutyIntegrationKeys(pagerDutyIntegrationKeys)
        .addAllUserGroupIds(userGroupIds)
        .putAllTemplateData(templateData)
        .setTemplateId(templateId)
        .build();
  }

  public static PagerDutyChannel toPagerDutyEntity(PagerDuty pagerDutyDetails) {
    return PagerDutyChannel.builder()
        .pagerDutyIntegrationKeys(pagerDutyDetails.getPagerDutyIntegrationKeysList())
        .userGroupIds(pagerDutyDetails.getUserGroupIdsList())
        .templateData(pagerDutyDetails.getTemplateDataMap())
        .templateId(pagerDutyDetails.getTemplateId())
        .build();
  }
}
