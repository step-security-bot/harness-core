/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.custom;

import static io.harness.rule.OwnerRule.MEET;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.metadata.WebhookRegistrationStatus;
import io.harness.ngtriggers.beans.entity.metadata.status.PollingSubscriptionStatus;
import io.harness.ngtriggers.beans.entity.metadata.status.StatusResult;
import io.harness.ngtriggers.beans.entity.metadata.status.TriggerStatus;
import io.harness.ngtriggers.beans.entity.metadata.status.ValidationStatus;
import io.harness.ngtriggers.beans.entity.metadata.status.WebhookAutoRegistrationStatus;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.beans.source.TriggerUpdateCount;
import io.harness.rule.Owner;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.DeleteResult;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

public class NGTriggerRepositoryCustomImplTest extends CategoryTest {
  @InjectMocks NGTriggerRepositoryCustomImpl ngTriggerRepositoryCustom;
  @Mock MongoTemplate mongoTemplate;
  @Mock BulkOperations bulkOperations;
  @Mock private BulkWriteResult bulkWriteResult;

  private String name = "name";
  private String accountId = "accountId";
  private String identifier = "identifier";
  private String projectId = "projectId";
  private String orgId = "orgId";
  private String pipelineId = "pipelineId";
  private String triggerId = "triggerId";
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testFindAll() {
    NGTriggerEntity ngTriggerEntity =
        NGTriggerEntity.builder()
            .name(name)
            .type(NGTriggerType.WEBHOOK)
            .accountId(accountId)
            .identifier(identifier)
            .projectIdentifier(projectId)
            .orgIdentifier(orgId)
            .targetIdentifier(pipelineId)
            .triggerStatus(TriggerStatus.builder()
                               .webhookAutoRegistrationStatus(WebhookAutoRegistrationStatus.builder()
                                                                  .registrationResult(WebhookRegistrationStatus.SUCCESS)
                                                                  .build())
                               .validationStatus(ValidationStatus.builder().statusResult(StatusResult.SUCCESS).build())
                               .pollingSubscriptionStatus(
                                   PollingSubscriptionStatus.builder().statusResult(StatusResult.SUCCESS).build())
                               .status(StatusResult.SUCCESS)
                               .build())
            .build();

    Criteria criteria = new Criteria();
    Pageable pageable = PageRequest.of(1, 1);
    when(mongoTemplate.find(any(), any())).thenReturn(Collections.singletonList(ngTriggerEntity));
    when(mongoTemplate.count(new Query(criteria).with(pageable), NGTriggerEntity.class)).thenReturn(1L);
    List<NGTriggerEntity> ngTriggerEntities = Collections.singletonList(ngTriggerEntity);
    assertThat(ngTriggerRepositoryCustom.findAll(criteria, pageable).getContent()).isEqualTo(ngTriggerEntities);

    // when trigger status is null
    ngTriggerEntity.setTriggerStatus(null);
    assertThat(ngTriggerRepositoryCustom.findAll(criteria, pageable).getContent().get(0).getTriggerStatus().getStatus())
        .isEqualTo(StatusResult.FAILED);

    // when validationStatus failed
    ngTriggerEntity.setTriggerStatus(
        TriggerStatus.builder()
            .webhookAutoRegistrationStatus(
                WebhookAutoRegistrationStatus.builder().registrationResult(WebhookRegistrationStatus.SUCCESS).build())
            .validationStatus(
                ValidationStatus.builder().detailedMessage("message").statusResult(StatusResult.FAILED).build())
            .pollingSubscriptionStatus(PollingSubscriptionStatus.builder().statusResult(StatusResult.SUCCESS).build())
            .status(StatusResult.FAILED)
            .build());
    Page<NGTriggerEntity> ngTriggerEntities1 = ngTriggerRepositoryCustom.findAll(criteria, pageable);
    assertThat(ngTriggerEntities1.getContent().get(0).getTriggerStatus().getStatus()).isEqualTo(StatusResult.FAILED);
    assertThat(ngTriggerEntities1.getContent().get(0).getTriggerStatus().getDetailMessages().get(0))
        .isEqualTo("message");

    // when pollingStatus failed
    ngTriggerEntity.setTriggerStatus(
        TriggerStatus.builder()
            .webhookAutoRegistrationStatus(
                WebhookAutoRegistrationStatus.builder().registrationResult(WebhookRegistrationStatus.SUCCESS).build())
            .validationStatus(ValidationStatus.builder().statusResult(StatusResult.SUCCESS).build())
            .pollingSubscriptionStatus(PollingSubscriptionStatus.builder()
                                           .detailedMessage("pollingMessage")
                                           .statusResult(StatusResult.FAILED)
                                           .build())
            .status(StatusResult.FAILED)
            .build());
    Page<NGTriggerEntity> ngTriggerEntities2 = ngTriggerRepositoryCustom.findAll(criteria, pageable);

    assertThat(ngTriggerEntities2.getContent().get(0).getTriggerStatus().getStatus()).isEqualTo(StatusResult.FAILED);
    assertThat(ngTriggerEntities2.getContent().get(0).getTriggerStatus().getDetailMessages().get(0))
        .isEqualTo("pollingMessage");

    // when webhookStatus failed
    ngTriggerEntity.setTriggerStatus(
        TriggerStatus.builder()
            .webhookAutoRegistrationStatus(WebhookAutoRegistrationStatus.builder()
                                               .detailedMessage("webhookMessage")
                                               .registrationResult(WebhookRegistrationStatus.FAILED)
                                               .build())
            .validationStatus(ValidationStatus.builder().statusResult(StatusResult.SUCCESS).build())
            .pollingSubscriptionStatus(PollingSubscriptionStatus
                                           .builder()

                                           .statusResult(StatusResult.SUCCESS)
                                           .build())
            .status(StatusResult.FAILED)
            .build());
    Page<NGTriggerEntity> ngTriggerEntities3 = ngTriggerRepositoryCustom.findAll(criteria, pageable);

    assertThat(ngTriggerEntities3.getContent().get(0).getTriggerStatus().getStatus()).isEqualTo(StatusResult.FAILED);
    assertThat(ngTriggerEntities3.getContent().get(0).getTriggerStatus().getDetailMessages().get(0))
        .isEqualTo("webhookMessage");
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testUpdate() {
    NGTriggerEntity ngTriggerEntity =
        NGTriggerEntity.builder()
            .name(name)
            .type(NGTriggerType.WEBHOOK)
            .accountId(accountId)
            .identifier(identifier)
            .projectIdentifier(projectId)
            .orgIdentifier(orgId)
            .targetIdentifier(pipelineId)
            .triggerStatus(TriggerStatus.builder()
                               .webhookAutoRegistrationStatus(WebhookAutoRegistrationStatus.builder()
                                                                  .registrationResult(WebhookRegistrationStatus.SUCCESS)
                                                                  .build())
                               .validationStatus(ValidationStatus.builder().statusResult(StatusResult.SUCCESS).build())
                               .pollingSubscriptionStatus(
                                   PollingSubscriptionStatus.builder().statusResult(StatusResult.SUCCESS).build())
                               .status(StatusResult.SUCCESS)
                               .build())
            .build();

    Criteria criteria = new Criteria();
    Pageable pageable = PageRequest.of(1, 1);
    when(mongoTemplate.findAndModify((Query) any(Query.class), (UpdateDefinition) any(UpdateDefinition.class), any(),
             eq(NGTriggerEntity.class)))
        .thenReturn(ngTriggerEntity);
    assertThat(ngTriggerRepositoryCustom.update(criteria, ngTriggerEntity)).isEqualTo(ngTriggerEntity);

    // Exception
    when(mongoTemplate.findAndModify((Query) any(Query.class), (UpdateDefinition) any(UpdateDefinition.class), any(),
             eq(NGTriggerEntity.class)))
        .thenThrow(new InvalidRequestException("message"));
    assertThatThrownBy(() -> ngTriggerRepositoryCustom.update(criteria, ngTriggerEntity))
        .hasMessage("message")
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testUpdateValidationStatus() {
    NGTriggerEntity ngTriggerEntity =
        NGTriggerEntity.builder()
            .name(name)
            .type(NGTriggerType.WEBHOOK)
            .accountId(accountId)
            .identifier(identifier)
            .projectIdentifier(projectId)
            .orgIdentifier(orgId)
            .targetIdentifier(pipelineId)
            .triggerStatus(TriggerStatus.builder()
                               .webhookAutoRegistrationStatus(WebhookAutoRegistrationStatus.builder()
                                                                  .registrationResult(WebhookRegistrationStatus.SUCCESS)
                                                                  .build())
                               .validationStatus(ValidationStatus.builder().statusResult(StatusResult.SUCCESS).build())
                               .pollingSubscriptionStatus(
                                   PollingSubscriptionStatus.builder().statusResult(StatusResult.SUCCESS).build())
                               .status(StatusResult.SUCCESS)
                               .build())
            .build();

    Criteria criteria = new Criteria();
    Pageable pageable = PageRequest.of(1, 1);
    when(mongoTemplate.findAndModify((Query) any(Query.class), (UpdateDefinition) any(UpdateDefinition.class), any(),
             eq(NGTriggerEntity.class)))
        .thenReturn(ngTriggerEntity);
    assertThat(ngTriggerRepositoryCustom.updateValidationStatus(criteria, ngTriggerEntity)).isEqualTo(ngTriggerEntity);

    // Exception
    when(mongoTemplate.findAndModify((Query) any(Query.class), (UpdateDefinition) any(UpdateDefinition.class), any(),
             eq(NGTriggerEntity.class)))
        .thenThrow(new InvalidRequestException("message"));
    assertThatThrownBy(() -> ngTriggerRepositoryCustom.updateValidationStatus(criteria, ngTriggerEntity))
        .hasMessage("message")
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testUpdateValidationStatusAndMetadata() {
    NGTriggerEntity ngTriggerEntity =
        NGTriggerEntity.builder()
            .name(name)
            .type(NGTriggerType.WEBHOOK)
            .accountId(accountId)
            .identifier(identifier)
            .projectIdentifier(projectId)
            .orgIdentifier(orgId)
            .targetIdentifier(pipelineId)
            .triggerStatus(TriggerStatus.builder()
                               .webhookAutoRegistrationStatus(WebhookAutoRegistrationStatus.builder()
                                                                  .registrationResult(WebhookRegistrationStatus.SUCCESS)
                                                                  .build())
                               .validationStatus(ValidationStatus.builder().statusResult(StatusResult.SUCCESS).build())
                               .pollingSubscriptionStatus(
                                   PollingSubscriptionStatus.builder().statusResult(StatusResult.SUCCESS).build())
                               .status(StatusResult.SUCCESS)
                               .build())
            .build();

    Criteria criteria = new Criteria();
    Pageable pageable = PageRequest.of(1, 1);
    when(mongoTemplate.findAndModify((Query) any(Query.class), (UpdateDefinition) any(UpdateDefinition.class), any(),
             eq(NGTriggerEntity.class)))
        .thenReturn(ngTriggerEntity);
    assertThat(ngTriggerRepositoryCustom.updateValidationStatusAndMetadata(criteria, ngTriggerEntity))
        .isEqualTo(ngTriggerEntity);

    // Exception
    when(mongoTemplate.findAndModify((Query) any(Query.class), (UpdateDefinition) any(UpdateDefinition.class), any(),
             eq(NGTriggerEntity.class)))
        .thenThrow(new InvalidRequestException("message"));
    assertThatThrownBy(() -> ngTriggerRepositoryCustom.updateValidationStatusAndMetadata(criteria, ngTriggerEntity))
        .hasMessage("message")
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testHardDelete() {
    DeleteResult deleteResult = DeleteResult.acknowledged(1L);

    Criteria criteria = new Criteria();
    Pageable pageable = PageRequest.of(1, 1);

    // Exception
    when(mongoTemplate.remove((Query) any(Query.class), eq(NGTriggerEntity.class)))
        .thenThrow(new InvalidRequestException("message"));
    assertThatThrownBy(() -> ngTriggerRepositoryCustom.hardDelete(criteria))
        .hasMessage("message")
        .isInstanceOf(InvalidRequestException.class);

    // Without exception
    when(mongoTemplate.remove((Query) any(Query.class), eq(NGTriggerEntity.class))).thenReturn(deleteResult);
    assertThat(ngTriggerRepositoryCustom.hardDelete(criteria).wasAcknowledged()).isEqualTo(true);
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testUpdateTriggerYaml() {
    NGTriggerEntity ngTriggerEntity =
        NGTriggerEntity.builder()
            .name(name)
            .type(NGTriggerType.WEBHOOK)
            .accountId(accountId)
            .identifier(identifier)
            .projectIdentifier(projectId)
            .orgIdentifier(orgId)
            .targetIdentifier(pipelineId)
            .triggerStatus(TriggerStatus.builder()
                               .webhookAutoRegistrationStatus(WebhookAutoRegistrationStatus.builder()
                                                                  .registrationResult(WebhookRegistrationStatus.SUCCESS)
                                                                  .build())
                               .validationStatus(ValidationStatus.builder().statusResult(StatusResult.SUCCESS).build())
                               .pollingSubscriptionStatus(
                                   PollingSubscriptionStatus.builder().statusResult(StatusResult.SUCCESS).build())
                               .status(StatusResult.SUCCESS)
                               .build())
            .build();

    when(mongoTemplate.bulkOps(any(), eq(NGTriggerEntity.class))).thenReturn(bulkOperations);
    when(bulkOperations.execute()).thenReturn(bulkWriteResult);
    when(bulkWriteResult.getModifiedCount()).thenReturn(1);
    assertThat(ngTriggerRepositoryCustom.updateTriggerYaml(Collections.singletonList(ngTriggerEntity)))
        .isEqualTo(TriggerUpdateCount.builder().failureCount(0).successCount(1).build());

    // Exception case
    when(bulkWriteResult.getModifiedCount()).thenThrow(new InvalidRequestException("message"));
    assertThatThrownBy(() -> ngTriggerRepositoryCustom.updateTriggerYaml(Collections.singletonList(ngTriggerEntity)))
        .isInstanceOf(InvalidRequestException.class);
  }
}
