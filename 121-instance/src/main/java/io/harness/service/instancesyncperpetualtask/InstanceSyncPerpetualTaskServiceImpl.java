package io.harness.service.instancesyncperpetualtask;

import static java.util.Collections.emptyList;

import io.harness.entities.DeploymentSummary;
import io.harness.entities.InstanceSyncPerpetualTaskInfo;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;
import io.harness.ff.FeatureFlagService;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.repositories.instancesyncperpetualtask.InstanceSyncPerpetualTaskRepository;
import io.harness.service.AbstractInstanceHandler;
import io.harness.service.instance.InstanceService;
import io.harness.service.instancehandlerfactory.InstanceHandlerFactoryService;

import software.wings.service.impl.instance.InstanceSyncByPerpetualTaskHandler;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InstanceSyncPerpetualTaskServiceImpl implements InstanceSyncPerpetualTaskService {
  @Inject private InstanceService instanceService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private InstanceHandlerFactoryService instanceHandlerFactory;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private InstanceSyncPerpetualTaskRepository instanceSyncPerpetualTaskRepository;

  @Override
  public void createPerpetualTasksForNewDeployment(
      InfrastructureMapping infrastructureMapping, DeploymentSummary deploymentSummary) {
    AbstractInstanceHandler handler = getInstanceHandler(infrastructureMapping);
    if (handler == null) {
      // TODO handle it gracefully with logs
      return;
    }

    List<PerpetualTaskRecord> existingTasks = getExistingPerpetualTasks(infrastructureMapping);

    String newPerpetualTaskId = handler.getInstanceSyncPerpetualTaskCreator().createPerpetualTaskForNewDeployment(
        deploymentSummary, infrastructureMapping);

    if (existingTasks.stream().filter(task -> task.getUuid().equals(newPerpetualTaskId)).findAny().orElse(null)
        == null) {
      // new perpetual task has been created, so add it to instance sync perpetual task
      instanceSyncPerpetualTaskRepository.save(
          infrastructureMapping.getAccountIdentifier(), infrastructureMapping.getId(), newPerpetualTaskId);
    }
  }

  @Override
  public void deletePerpetualTasks(InfrastructureMapping infrastructureMapping) {
    deletePerpetualTasks(infrastructureMapping.getAccountIdentifier(), infrastructureMapping.getId());
  }

  @Override
  public void deletePerpetualTasks(String accountId, String infrastructureMappingId) {
    Optional<InstanceSyncPerpetualTaskInfo> info =
        instanceSyncPerpetualTaskRepository.findByAccountIdentifierAndInfrastructureMappingId(
            accountId, infrastructureMappingId);
    if (!info.isPresent()) {
      return;
    }

    for (String taskId : info.get().getPerpetualTaskIds()) {
      deletePerpetualTask(accountId, infrastructureMappingId, taskId);
    }
  }

  public void resetPerpetualTask(String accountId, String perpetualTaskId) {
    perpetualTaskService.resetTask(accountId, perpetualTaskId, null);
  }

  @Override
  public void deletePerpetualTask(String accountId, String infrastructureMappingId, String perpetualTaskId) {
    perpetualTaskService.deleteTask(accountId, perpetualTaskId);

    Optional<InstanceSyncPerpetualTaskInfo> optionalInfo =
        instanceSyncPerpetualTaskRepository.findByAccountIdentifierAndInfrastructureMappingId(
            accountId, infrastructureMappingId);
    if (!optionalInfo.isPresent()) {
      return;
    }
    InstanceSyncPerpetualTaskInfo info = optionalInfo.get();
    boolean wasFound = info.getPerpetualTaskIds().remove(perpetualTaskId);
    if (!wasFound) {
      return;
    }
    if (info.getPerpetualTaskIds().isEmpty()) {
      instanceSyncPerpetualTaskRepository.deleteByInfrastructureMappingId(infrastructureMappingId);
    } else {
      instanceSyncPerpetualTaskRepository.save(info);
    }
  }

  @Override
  public boolean isInstanceSyncByPerpetualTaskEnabled(InfrastructureMapping infrastructureMapping) {
    AbstractInstanceHandler abstractInstanceHandler = getInstanceHandler(infrastructureMapping);
    if (abstractInstanceHandler == null) {
      return false;
    }

    if (abstractInstanceHandler instanceof InstanceSyncByPerpetualTaskHandler) {
      InstanceSyncByPerpetualTaskHandler handler = (InstanceSyncByPerpetualTaskHandler) abstractInstanceHandler;
      return featureFlagService.isEnabled(
          handler.getFeatureFlagToEnablePerpetualTaskForInstanceSync(), infrastructureMapping.getAccountIdentifier());
    }

    return false;
  }

  // ---------------------- PRIVATE METHODS -----------------------

  private AbstractInstanceHandler getInstanceHandler(InfrastructureMapping infrastructureMapping) {
    try {
      return instanceHandlerFactory.getInstanceHandler(infrastructureMapping);
    } catch (Exception ex) {
      return null;
    }
  }

  private boolean shouldCreatePerpetualTasks(InfrastructureMapping infrastructureMapping) {
    // TODO Fix the method acc to NG
    long instanceCount = 0;
    //        instanceService.getInstanceCount(infrastructureMapping.getAppId(), infrastructureMapping.getId());
    return instanceCount > 0 && !perpetualTasksAlreadyExists(infrastructureMapping);
  }

  private boolean perpetualTasksAlreadyExists(InfrastructureMapping infrastructureMapping) {
    Optional<InstanceSyncPerpetualTaskInfo> info =
        instanceSyncPerpetualTaskRepository.findByAccountIdentifierAndInfrastructureMappingId(
            infrastructureMapping.getAccountIdentifier(), infrastructureMapping.getId());
    return info.isPresent() && !info.get().getPerpetualTaskIds().isEmpty();
  }

  private List<PerpetualTaskRecord> getExistingPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    Optional<InstanceSyncPerpetualTaskInfo> info =
        instanceSyncPerpetualTaskRepository.findByAccountIdentifierAndInfrastructureMappingId(
            infrastructureMapping.getAccountIdentifier(), infrastructureMapping.getId());
    return info
        .map(instanceSyncPerpetualTaskInfo
            -> instanceSyncPerpetualTaskInfo.getPerpetualTaskIds()
                   .stream()
                   .map(id -> perpetualTaskService.getTaskRecord(id))
                   .collect(Collectors.toList()))
        .orElse(emptyList());
  }
}
