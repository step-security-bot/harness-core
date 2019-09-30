package software.wings.search.entities.pipeline;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import io.harness.beans.WorkflowType;
import io.harness.persistence.HIterator;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.query.Sort;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityInfo;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
@FieldNameConstants(innerTypeName = "PipelineViewBuilderKeys")
class PipelineViewBuilder {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private static final int MAX_RELATED_ENTITIES_COUNT = 3;
  private static final int DAYS_TO_RETAIN = 7;
  private PipelineView pipelineView;

  private void populateServices(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestration();
    workflow.setServices(
        serviceResourceService.fetchServicesByUuids(workflow.getAppId(), orchestrationWorkflow.getServiceIds()));
  }

  private void createBaseView(Pipeline pipeline) {
    this.pipelineView = new PipelineView(pipeline.getUuid(), pipeline.getName(), pipeline.getDescription(),
        pipeline.getAccountId(), pipeline.getCreatedAt(), pipeline.getLastUpdatedAt(), EntityType.PIPELINE,
        pipeline.getCreatedBy(), pipeline.getLastUpdatedBy(), pipeline.getAppId());
  }

  private void setAuditsAndAuditTimestamps(Pipeline pipeline) {
    long startTimestamp = System.currentTimeMillis() - DAYS_TO_RETAIN * 86400 * 1000;
    List<RelatedAuditView> audits = new ArrayList<>();
    List<Long> auditTimestamps = new ArrayList<>();
    try (HIterator<AuditHeader> iterator = new HIterator<>(wingsPersistence.createQuery(AuditHeader.class)
                                                               .field(AuditHeaderKeys.accountId)
                                                               .equal(pipeline.getAccountId())
                                                               .field("entityAuditRecords.entityId")
                                                               .equal(pipeline.getUuid())
                                                               .field(PipelineKeys.createdAt)
                                                               .greaterThanOrEq(startTimestamp)
                                                               .order(Sort.descending(AuditHeaderKeys.createdAt))
                                                               .fetch())) {
      while (iterator.hasNext()) {
        final AuditHeader auditHeader = iterator.next();
        for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
          if (entityAuditRecord.getAffectedResourceType().equals(EntityType.PIPELINE.name())
              && entityAuditRecord.getAffectedResourceId().equals(pipeline.getUuid())) {
            if (audits.size() < MAX_RELATED_ENTITIES_COUNT) {
              audits.add(relatedAuditViewBuilder.getAuditRelatedEntityView(auditHeader, entityAuditRecord));
            }
            auditTimestamps.add(TimeUnit.MILLISECONDS.toSeconds(auditHeader.getCreatedAt()));
          }
        }
      }
    }
    Collections.reverse(audits);
    Collections.reverse(auditTimestamps);
    pipelineView.setAudits(audits);
    pipelineView.setAuditTimestamps(auditTimestamps);
  }

  private void setDeploymentsAndDeploymentTimestamps(Pipeline pipeline) {
    long startTimestamp = System.currentTimeMillis() - DAYS_TO_RETAIN * 86400 * 1000;
    List<Long> deploymentTimestamps = new ArrayList<>();
    List<RelatedDeploymentView> deployments = new ArrayList<>();
    try (HIterator<WorkflowExecution> iterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class)
                                 .field(WorkflowExecutionKeys.appId)
                                 .equal(pipeline.getAppId())
                                 .filter(WorkflowExecutionKeys.workflowId, pipeline.getUuid())
                                 .field(PipelineKeys.createdAt)
                                 .greaterThanOrEq(startTimestamp)
                                 .order(Sort.descending(WorkflowExecutionKeys.createdAt))
                                 .fetch())) {
      while (iterator.hasNext()) {
        final WorkflowExecution workflowExecution = iterator.next();
        if (workflowExecution.getWorkflowType().equals(WorkflowType.PIPELINE)) {
          if (deployments.size() < MAX_RELATED_ENTITIES_COUNT) {
            deployments.add(new RelatedDeploymentView(workflowExecution));
          }
          deploymentTimestamps.add(TimeUnit.MILLISECONDS.toSeconds(workflowExecution.getCreatedAt()));
        }
      }
    }
    Collections.reverse(deployments);
    Collections.reverse(deploymentTimestamps);
    pipelineView.setDeploymentTimestamps(deploymentTimestamps);
    pipelineView.setDeployments(deployments);
  }

  private Application getApplication(Pipeline pipeline) {
    if (pipeline.getAppId() != null) {
      return wingsPersistence.get(Application.class, pipeline.getAppId());
    }
    return null;
  }

  private void setApplicationName(Pipeline pipeline) {
    Application application = getApplication(pipeline);
    if (application != null) {
      pipelineView.setAppName(application.getName());
    }
  }

  private void setServicesAndWorkflows(Pipeline pipeline) {
    String worklowIdKey = "workflowId";
    if (pipeline.getPipelineStages() != null) {
      Set<EntityInfo> workflows = new HashSet<>();
      Set<EntityInfo> services = new HashSet<>();
      for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
        for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
          if (pipelineStageElement != null && pipelineStageElement.getProperties() != null
              && pipelineStageElement.getProperties().get(worklowIdKey) != null) {
            Workflow workflow =
                wingsPersistence.get(Workflow.class, pipelineStageElement.getProperties().get(worklowIdKey).toString());
            EntityInfo workflowInfo =
                new EntityInfo(pipelineStageElement.getProperties().get(worklowIdKey).toString(), workflow.getName());
            workflows.add(workflowInfo);
            if (workflow.getOrchestration() != null) {
              populateServices(workflow);
              workflow.getServices().forEach(
                  service -> services.add(new EntityInfo(service.getUuid(), service.getName())));
            }
          }
        }
      }
      pipelineView.setServices(services);
      pipelineView.setWorkflows(workflows);
    }
  }

  PipelineView createPipelineView(Pipeline pipeline) {
    if (getApplication(pipeline) != null) {
      createBaseView(pipeline);
      setApplicationName(pipeline);
      setServicesAndWorkflows(pipeline);
      setDeploymentsAndDeploymentTimestamps(pipeline);
      setAuditsAndAuditTimestamps(pipeline);
      return pipelineView;
    }
    return null;
  }

  PipelineView createPipelineView(Pipeline pipeline, DBObject changeDocument) {
    if (getApplication(pipeline) != null) {
      createBaseView(pipeline);
      if (changeDocument.containsField(PipelineKeys.appId)) {
        setApplicationName(pipeline);
      }
      if (changeDocument.containsField(PipelineKeys.pipelineStages)) {
        setServicesAndWorkflows(pipeline);
      }
      return pipelineView;
    }
    return null;
  }
}
