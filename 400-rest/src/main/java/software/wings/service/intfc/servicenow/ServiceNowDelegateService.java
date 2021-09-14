/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.ServiceNowExecutionData;
import software.wings.beans.TaskType;
import software.wings.beans.approval.ServiceNowApprovalParams;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowMetaDTO;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(CDC)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface ServiceNowDelegateService {
  @DelegateTaskType(TaskType.SERVICENOW_VALIDATION) boolean validateConnector(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  List<ServiceNowMetaDTO> getStates(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  List<ServiceNowMetaDTO> getApprovalStates(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  Map<String, List<ServiceNowMetaDTO>> getCreateMeta(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  List<ServiceNowMetaDTO> getAdditionalFields(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  ServiceNowExecutionData getIssueUrl(ServiceNowTaskParameters taskParameters, ServiceNowApprovalParams approvalParams);

  // For fields we need values instead of display values
  Map<String, String> getIssueValues(JsonNode issueObj, Set<String> timeFields);
}
