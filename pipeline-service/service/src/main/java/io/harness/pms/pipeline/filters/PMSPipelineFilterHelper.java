/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.filters;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.gitsync.beans.StoreType;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.gitsync.PMSUpdateGitDetailsParams;

import java.util.LinkedList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_GITX, HarnessModuleComponent.CDS_PIPELINE})
@UtilityClass
public class PMSPipelineFilterHelper {
  public Update getUpdateOperationsForPatch(PipelineEntity pipelineEntity, long timestamp) {
    Update update = new Update();
    // TODO(Shalini): Change conditions to not null check when CDS-81968 is done
    if (isNotEmpty(pipelineEntity.getYaml())) {
      update.set(PipelineEntityKeys.yaml, pipelineEntity.getYaml());
    }
    update.set(PipelineEntityKeys.lastUpdatedAt, timestamp);
    update.set(PipelineEntityKeys.deleted, false);
    if (isNotEmpty(pipelineEntity.getName())) {
      update.set(PipelineEntityKeys.name, pipelineEntity.getName());
    }
    if (isNotEmpty(pipelineEntity.getDescription())) {
      update.set(PipelineEntityKeys.description, pipelineEntity.getDescription());
    }
    if (isNotEmpty(pipelineEntity.getTags())) {
      update.set(PipelineEntityKeys.tags, pipelineEntity.getTags());
    }
    if (isNotEmpty(pipelineEntity.getFilters())) {
      update.set(PipelineEntityKeys.filters, pipelineEntity.getFilters());
    }
    if (pipelineEntity.getStageCount() != null) {
      update.set(PipelineEntityKeys.stageCount, pipelineEntity.getStageCount());
    }
    if (isNotEmpty(pipelineEntity.getStageNames())) {
      update.set(PipelineEntityKeys.stageNames, pipelineEntity.getStageNames());
    }
    if (pipelineEntity.getAllowStageExecutions() != null) {
      update.set(PipelineEntityKeys.allowStageExecutions, pipelineEntity.getAllowStageExecutions());
    }
    update.set(PipelineEntityKeys.harnessVersion, pipelineEntity.getHarnessVersion());
    if (pipelineEntity.getYamlHash() != null) {
      update.set(PipelineEntityKeys.yamlHash, pipelineEntity.getYamlHash());
    }
    return update;
  }

  public Update getUpdateOperations(PipelineEntity pipelineEntity, long timestamp) {
    Update update = new Update();
    update.set(PipelineEntityKeys.yaml, pipelineEntity.getYaml());
    update.set(PipelineEntityKeys.lastUpdatedAt, timestamp);
    update.set(PipelineEntityKeys.deleted, false);
    update.set(PipelineEntityKeys.name, pipelineEntity.getName());
    update.set(PipelineEntityKeys.description, pipelineEntity.getDescription());
    update.set(PipelineEntityKeys.tags, pipelineEntity.getTags());
    update.set(PipelineEntityKeys.filters, pipelineEntity.getFilters());
    update.set(PipelineEntityKeys.stageCount, pipelineEntity.getStageCount());
    update.set(PipelineEntityKeys.stageNames, pipelineEntity.getStageNames());
    update.set(PipelineEntityKeys.allowStageExecutions, pipelineEntity.getAllowStageExecutions());
    update.set(PipelineEntityKeys.harnessVersion, pipelineEntity.getHarnessVersion());
    update.set(PipelineEntityKeys.yamlHash, pipelineEntity.getYamlHash());
    return update;
  }

  public Update getPipelineFilterUpdateOperations(PipelineEntity pipelineEntity) {
    Update update = new Update();
    update.set(PipelineEntityKeys.filters, pipelineEntity.getFilters());
    update.set(PipelineEntityKeys.stageCount, pipelineEntity.getStageCount());
    update.set(PipelineEntityKeys.stageNames, pipelineEntity.getStageNames());
    return update;
  }

  public PipelineEntity updateFieldsInDBEntryForPatch(
      PipelineEntity oldentityFromDB, PipelineEntity fieldsToUpdate, long timeOfUpdate) {
    return oldentityFromDB.toBuilder()
        .yaml(isNotEmpty(fieldsToUpdate.getYaml()) ? fieldsToUpdate.getYaml() : oldentityFromDB.getYaml())
        .lastUpdatedAt(timeOfUpdate)
        .name(isNotEmpty(fieldsToUpdate.getName()) ? fieldsToUpdate.getName() : oldentityFromDB.getName())
        .description(isNotEmpty(fieldsToUpdate.getDescription()) ? fieldsToUpdate.getDescription()
                                                                 : oldentityFromDB.getDescription())
        .tags(isNotEmpty(fieldsToUpdate.getTags()) ? fieldsToUpdate.getTags() : oldentityFromDB.getTags())
        .filters(isNotEmpty(fieldsToUpdate.getFilters()) ? fieldsToUpdate.getFilters() : oldentityFromDB.getFilters())
        .stageCount(
            fieldsToUpdate.getStageCount() != null ? fieldsToUpdate.getStageCount() : oldentityFromDB.getStageCount())
        .stageNames(isNotEmpty(fieldsToUpdate.getStageNames()) ? fieldsToUpdate.getStageNames()
                                                               : oldentityFromDB.getStageNames())
        .allowStageExecutions(fieldsToUpdate.getAllowStageExecutions() != null
                ? fieldsToUpdate.getAllowStageExecutions()
                : oldentityFromDB.getAllowStageExecutions())
        .yamlHash(fieldsToUpdate.getYamlHash() != null ? fieldsToUpdate.getYamlHash() : oldentityFromDB.getYamlHash())
        .version(oldentityFromDB.getVersion() == null ? 1 : oldentityFromDB.getVersion() + 1)
        .build();
  }

  public PipelineEntity updateFieldsInDBEntry(
      PipelineEntity oldentityFromDB, PipelineEntity fieldsToUpdate, long timeOfUpdate) {
    return oldentityFromDB.toBuilder()
        .yaml(fieldsToUpdate.getYaml())
        .lastUpdatedAt(timeOfUpdate)
        .name(fieldsToUpdate.getName())
        .description(fieldsToUpdate.getDescription())
        .tags(fieldsToUpdate.getTags())
        .filters(fieldsToUpdate.getFilters())
        .stageCount(fieldsToUpdate.getStageCount())
        .stageNames(fieldsToUpdate.getStageNames())
        .allowStageExecutions(fieldsToUpdate.getAllowStageExecutions())
        .yamlHash(fieldsToUpdate.getYamlHash())
        .version(oldentityFromDB.getVersion() == null ? 1 : oldentityFromDB.getVersion() + 1)
        .build();
  }

  public Update getUpdateOperationsForOnboardingToInline() {
    Update update = new Update();
    update.set(PipelineEntityKeys.storeType, StoreType.INLINE);
    return update;
  }

  public Criteria getCriteriaForFind(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean notDeleted) {
    return Criteria.where(PipelineEntityKeys.accountId)
        .is(accountId)
        .and(PipelineEntityKeys.orgIdentifier)
        .is(orgIdentifier)
        .and(PipelineEntityKeys.projectIdentifier)
        .is(projectIdentifier)
        .and(PipelineEntityKeys.identifier)
        .is(identifier)
        .and(PipelineEntityKeys.deleted)
        .is(!notDeleted);
  }

  public Criteria getCriteriaForFind(String uuid, Integer yamlHash) {
    return Criteria.where(PipelineEntityKeys.uuid).is(uuid).and(PipelineEntityKeys.yamlHash).is(yamlHash);
  }

  public Criteria getCriteriaForFind(String uuid) {
    return Criteria.where(PipelineEntityKeys.uuid).is(uuid);
  }

  public Criteria getCriteriaForAllPipelinesInProject(
      String accountId, String orgIdentifier, String projectIdentifier) {
    return Criteria.where(PipelineEntityKeys.accountId)
        .is(accountId)
        .and(PipelineEntityKeys.orgIdentifier)
        .is(orgIdentifier)
        .and(PipelineEntityKeys.projectIdentifier)
        .is(projectIdentifier);
  }

  public Criteria getCriteriaForFileUniquenessCheck(String accountId, String repoURl, String filePath) {
    return Criteria.where(PipelineEntityKeys.accountId)
        .is(accountId)
        .and(PipelineEntityKeys.repoURL)
        .is(repoURl)
        .and(PipelineEntityKeys.filePath)
        .is(filePath);
  }

  public List<String> getPipelineNonMetadataFields() {
    List<String> fields = new LinkedList<>();
    fields.add(PipelineEntityKeys.yaml);
    return fields;
  }

  public Update getUpdateWithGitMetadata(PMSUpdateGitDetailsParams updateGitDetailsParams) {
    Update update = new Update();

    if (isNotEmpty(updateGitDetailsParams.getConnectorRef())) {
      update.set(PipelineEntityKeys.connectorRef, updateGitDetailsParams.getConnectorRef());
    }
    if (isNotEmpty(updateGitDetailsParams.getRepoName())) {
      update.set(PipelineEntityKeys.repo, updateGitDetailsParams.getRepoName());
    }
    if (isNotEmpty(updateGitDetailsParams.getFilePath())) {
      update.set(PipelineEntityKeys.filePath, updateGitDetailsParams.getFilePath());
    }
    if (!update.getUpdateObject().isEmpty()) {
      update.set(PipelineEntityKeys.lastUpdatedAt, System.currentTimeMillis());
    }
    return update;
  }
}
