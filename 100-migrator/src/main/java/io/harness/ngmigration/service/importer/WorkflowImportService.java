/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.importer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ngmigration.beans.DiscoverEntityInput;
import io.harness.ngmigration.beans.DiscoveryInput;
import io.harness.ngmigration.dto.ImportDTO;
import io.harness.ngmigration.dto.WorkflowFilter;
import io.harness.ngmigration.service.DiscoveryService;
import io.harness.persistence.HPersistence;

import software.wings.beans.Base;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.ngmigration.DiscoveryResult;
import software.wings.ngmigration.NGMigrationEntityType;

import com.google.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
public class WorkflowImportService implements ImportService {
  @Inject DiscoveryService discoveryService;
  @Inject HPersistence hPersistence;

  public DiscoveryResult discover(String authToken, ImportDTO importConnectorDTO) {
    WorkflowFilter filter = (WorkflowFilter) importConnectorDTO.getFilter();
    String accountId = importConnectorDTO.getAccountIdentifier();
    String appId = filter.getAppId();
    Set<String> workflowIds = filter.getWorkflowIds();

    if (EmptyPredicate.isEmpty(workflowIds)) {
      List<Workflow> workflowList = hPersistence.createQuery(Workflow.class)
                                        .filter(Workflow.ACCOUNT_ID_KEY, accountId)
                                        .filter(WorkflowKeys.appId, appId)
                                        .project(WorkflowKeys.uuid, true)
                                        .asList();
      if (EmptyPredicate.isNotEmpty(workflowList)) {
        workflowIds = workflowList.stream().map(Base::getUuid).collect(Collectors.toSet());
      }
    }

    return discoveryService.discoverMulti(accountId,
        DiscoveryInput.builder()
            .exportImage(false)
            .entities(workflowIds.stream()
                          .map(workflowId
                              -> DiscoverEntityInput.builder()
                                     .entityId(workflowId)
                                     .type(NGMigrationEntityType.WORKFLOW)
                                     .appId(appId)
                                     .build())
                          .collect(Collectors.toList()))
            .build());
  }
}
