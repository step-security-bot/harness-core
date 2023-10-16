/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.polling.service.intfc;

import static io.harness.rule.OwnerRule.INDER;
import static io.harness.rule.OwnerRule.MEET;
import static io.harness.rule.OwnerRule.VINICIUS;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.dto.PollingResponseDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.polling.bean.ArtifactPolledResponse;
import io.harness.polling.bean.HelmChartManifestInfo;
import io.harness.polling.bean.ManifestInfo;
import io.harness.polling.bean.ManifestPolledResponse;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.bean.PollingType;
import io.harness.polling.bean.artifact.DockerHubArtifactInfo;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.mapper.PollingDocumentMapper;
import io.harness.polling.service.impl.PollingServiceImpl;
import io.harness.repositories.polling.PollingRepository;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CDC)
@RunWith(MockitoJUnitRunner.class)
public class PollingServiceTest extends CategoryTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String ORG_ID = "orgId";
  private static final String PROJECT_ID = "projectId";
  private static final String SIGNATURE_1 = "signature1";
  @InjectMocks PollingServiceImpl pollingService;
  @Mock PollingRepository pollingRepository;
  @Mock PollingDocumentMapper pollingDocumentMapper;

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testErrorCasesOnSave() {
    PollingDocument pollingDocument =
        PollingDocument.builder().failedAttempts(0).orgIdentifier(ORG_ID).projectIdentifier(PROJECT_ID).build();

    assertThatThrownBy(() -> pollingService.save(pollingDocument))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("AccountId should not be empty");

    pollingDocument.setAccountId(ACCOUNT_ID);

    assertThatThrownBy(() -> pollingService.save(pollingDocument))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Signature should not be empty");
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testFirstTimeSubscriber() {
    PollingItem pollingItem = PollingItem.newBuilder().build();
    PollingDocument pollingDocument = getArtifactPollingDocument(null);
    when(pollingDocumentMapper.toPollingDocument(pollingItem)).thenReturn(pollingDocument);
    PollingDocument savedPollingDocument = getArtifactPollingDocument("id1");
    when(pollingRepository.save(pollingDocument)).thenReturn(savedPollingDocument);

    String id = pollingService.subscribe(pollingItem).getPollingDocId();
    assertThat(id).isEqualTo("id1");
    verify(pollingDocumentMapper).toPollingDocument(pollingItem);
    verify(pollingRepository).save(pollingDocument);
    verify(pollingRepository, never()).findByUuidAndAccountIdAndSignature(anyString(), anyString(), any());
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testSubscribe() throws InvalidRequestException {
    // Create a sample PollingItem and a corresponding PollingDocument
    PollingItem pollingItem = PollingItem.newBuilder().build();
    PollingDocument pollingDocument = PollingDocument.builder()
                                          .uuid("id")
                                          .accountId("account")
                                          .signatures(Collections.singletonList("signature"))
                                          .build();

    // Mock the behavior of pollingDocumentMapper and pollingRepository
    when(pollingDocumentMapper.toPollingDocument(pollingItem)).thenReturn(pollingDocument);
    when(pollingRepository.findByUuidAndAccountIdAndSignature(any(), any(), any())).thenReturn(null);
    when(pollingRepository.save(any())).thenReturn(pollingDocument);

    // Call the method you want to test
    PollingResponseDTO response = pollingService.subscribe(pollingItem);

    // Assert the result and verify interactions
    assertNotNull(response);
    assertEquals(pollingDocument.getUuid(), response.getPollingDocId());
    assertFalse(response.getIsExistingPollingDoc());
    // Add more assertions based on the method's logic

    // Verify that the expected methods were called
    verify(pollingDocumentMapper, times(1)).toPollingDocument(pollingItem);
    verify(pollingRepository, times(1)).findByUuidAndAccountIdAndSignature(any(), any(), any());
    verify(pollingRepository, times(1)).save(any());

    Set<String> allPolledKeys = new HashSet<>();
    allPolledKeys.add("key");
    pollingDocument = PollingDocument.builder()
                          .uuid("id")
                          .accountId("account")
                          .pollingType(PollingType.ARTIFACT)
                          .signatures(Collections.singletonList("signature"))
                          .lastModifiedAt(10L)
                          .lastModifiedPolledResponseTime(20L)
                          .polledResponse(ArtifactPolledResponse.builder().allPolledKeys(allPolledKeys).build())
                          .pollingInfo(DockerHubArtifactInfo.builder().build())
                          .build();

    // Mock the behavior of pollingDocumentMapper and pollingRepository
    when(pollingDocumentMapper.toPollingDocument(pollingItem)).thenReturn(pollingDocument);
    when(pollingRepository.findByUuidAndAccountIdAndSignature(any(), any(), any())).thenReturn(pollingDocument);
    response = pollingService.subscribe(pollingItem);
    assertNotNull(response);
    assertEquals(pollingDocument.getUuid(), response.getPollingDocId());
    assertThat(response.getLastPollingUpdate()).isEqualTo(20L);
    assertThat(response.getLastPolled().get(0)).isEqualTo("key");
    assertTrue(response.getIsExistingPollingDoc());

    allPolledKeys = new HashSet<>();
    allPolledKeys.add("1");
    allPolledKeys.add("2");
    allPolledKeys.add("3");
    allPolledKeys.add("4");
    allPolledKeys.add("5");
    allPolledKeys.add("6");
    allPolledKeys.add("7");
    allPolledKeys.add("8");
    allPolledKeys.add("9");
    allPolledKeys.add("10");
    allPolledKeys.add("11");
    allPolledKeys.add("12");

    pollingDocument = PollingDocument.builder()
                          .uuid("id")
                          .accountId("account")
                          .pollingType(PollingType.ARTIFACT)
                          .signatures(Collections.singletonList("signature"))
                          .lastModifiedAt(10L)
                          .lastModifiedPolledResponseTime(20L)
                          .polledResponse(ArtifactPolledResponse.builder().allPolledKeys(allPolledKeys).build())
                          .pollingInfo(DockerHubArtifactInfo.builder().build())
                          .build();

    // Mock the behavior of pollingDocumentMapper and pollingRepository
    when(pollingDocumentMapper.toPollingDocument(pollingItem)).thenReturn(pollingDocument);
    when(pollingRepository.findByUuidAndAccountIdAndSignature(any(), any(), any())).thenReturn(pollingDocument);
    response = pollingService.subscribe(pollingItem);
    assertNotNull(response);
    assertEquals(pollingDocument.getUuid(), response.getPollingDocId());
    assertThat(response.getLastPollingUpdate()).isEqualTo(20L);
    assertThat(response.getLastPolled().size()).isEqualTo(10);
    assertTrue(response.getIsExistingPollingDoc());

    allPolledKeys = new HashSet<>();
    allPolledKeys.add("key");
    pollingDocument = PollingDocument.builder()
                          .uuid("id")
                          .accountId("account")
                          .pollingType(PollingType.MANIFEST)
                          .signatures(Collections.singletonList("signature"))
                          .lastModifiedAt(10L)
                          .lastModifiedPolledResponseTime(20L)
                          .polledResponse(ManifestPolledResponse.builder().allPolledKeys(allPolledKeys).build())
                          .pollingInfo(HelmChartManifestInfo.builder().build())
                          .build();

    // Mock the behavior of pollingDocumentMapper and pollingRepository
    when(pollingDocumentMapper.toPollingDocument(pollingItem)).thenReturn(pollingDocument);
    when(pollingRepository.findByUuidAndAccountIdAndSignature(any(), any(), any())).thenReturn(pollingDocument);
    response = pollingService.subscribe(pollingItem);
    assertNotNull(response);
    assertEquals(pollingDocument.getUuid(), response.getPollingDocId());
    assertThat(response.getLastPollingUpdate()).isEqualTo(20L);
    assertThat(response.getLastPolled().get(0)).isEqualTo("key");
    assertTrue(response.getIsExistingPollingDoc());
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testSubscribeOnSameEntityTwice() {
    PollingItem pollingItem = PollingItem.newBuilder().build();
    PollingDocument pollingDocument = getArtifactPollingDocument("id1");
    when(pollingDocumentMapper.toPollingDocument(pollingItem)).thenReturn(pollingDocument);
    when(pollingRepository.findByUuidAndAccountIdAndSignature(
             pollingDocument.getUuid(), pollingDocument.getAccountId(), pollingDocument.getSignatures()))
        .thenReturn(pollingDocument);
    when(pollingRepository.save(pollingDocument)).thenReturn(pollingDocument);

    assertThat(pollingService.subscribe(pollingItem).getPollingDocId()).isEqualTo("id1");
    verify(pollingDocumentMapper).toPollingDocument(pollingItem);
    verify(pollingRepository, never()).save(pollingDocument);
    verify(pollingRepository).findByUuidAndAccountIdAndSignature(anyString(), anyString(), any());
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testSubscribeWithUpdate() {
    PollingItem pollingItem = PollingItem.newBuilder().build();
    PollingDocument newPollingDocument = getArtifactPollingDocument("id1");
    PollingDocument existingPollingDocument = getManifestPollingDocument("id1");
    when(pollingDocumentMapper.toPollingDocument(pollingItem)).thenReturn(newPollingDocument);
    when(pollingRepository.findByUuidAndAccountIdAndSignature(
             newPollingDocument.getUuid(), newPollingDocument.getAccountId(), newPollingDocument.getSignatures()))
        .thenReturn(existingPollingDocument);

    PollingDocument artifactPollingDocument2 = getArtifactPollingDocument("id1");
    when(pollingRepository.save(any())).thenReturn(artifactPollingDocument2);

    assertThat(pollingService.subscribe(pollingItem).getPollingDocId()).isEqualTo("id1");
    verify(pollingDocumentMapper).toPollingDocument(pollingItem);
    verify(pollingRepository).findByUuidAndAccountIdAndSignature(anyString(), anyString(), any());
    verify(pollingRepository).removeDocumentIfOnlySubscriber(anyString(), anyString(), any());
    verify(pollingRepository).save(newPollingDocument);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testUnsubscribeIfOnlySubscriber() {
    PollingItem pollingItem = PollingItem.newBuilder().build();
    PollingDocument newPollingDocument = getArtifactPollingDocument("id1");
    when(pollingDocumentMapper.toPollingDocumentWithoutPollingInfo(pollingItem)).thenReturn(newPollingDocument);
    when(pollingRepository.removeDocumentIfOnlySubscriber(
             newPollingDocument.getAccountId(), newPollingDocument.getUuid(), newPollingDocument.getSignatures()))
        .thenReturn(newPollingDocument);

    assertThat(pollingService.unsubscribe(pollingItem)).isTrue();
    verify(pollingDocumentMapper).toPollingDocumentWithoutPollingInfo(pollingItem);
    verify(pollingRepository).removeDocumentIfOnlySubscriber(anyString(), anyString(), any());
    verify(pollingRepository, never())
        .removeSubscribersFromExistingPollingDoc(
            newPollingDocument.getAccountId(), newPollingDocument.getUuid(), newPollingDocument.getSignatures());
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testUnsubscribeWithMultipleSubscribers() {
    PollingItem pollingItem = PollingItem.newBuilder().build();
    PollingDocument newPollingDocument = getArtifactPollingDocument("id1");
    when(pollingDocumentMapper.toPollingDocumentWithoutPollingInfo(pollingItem)).thenReturn(newPollingDocument);

    assertThat(pollingService.unsubscribe(pollingItem)).isTrue();
    verify(pollingDocumentMapper).toPollingDocumentWithoutPollingInfo(pollingItem);
    verify(pollingRepository).removeDocumentIfOnlySubscriber(anyString(), anyString(), any());
    verify(pollingRepository)
        .removeSubscribersFromExistingPollingDoc(
            newPollingDocument.getAccountId(), newPollingDocument.getUuid(), newPollingDocument.getSignatures());
  }

  @Test
  @Owner(developers = VINICIUS)
  @Category(UnitTests.class)
  public void testGetUuidsBySignature() {
    when(pollingRepository.findUuidsBySignaturesAndAccountId(any(), any()))
        .thenReturn(List.of(getArtifactPollingDocument("id1")));
    List<String> pollingDocIds = pollingService.getUuidsBySignatures(ACCOUNT_ID, List.of("sig1"));
    assertThat(pollingDocIds.size()).isEqualTo(1);
    assertThat(pollingDocIds.contains("id1")).isTrue();
  }

  @Test
  @Owner(developers = VINICIUS)
  @Category(UnitTests.class)
  public void testGetMany() {
    when(pollingRepository.findManyByUuidsAndAccountId(any(), any()))
        .thenReturn(List.of(getArtifactPollingDocument("id1")));
    List<PollingDocument> pollingDocs = pollingService.getMany(ACCOUNT_ID, List.of("id1"));
    assertThat(pollingDocs.size()).isEqualTo(1);
    assertThat(pollingDocs.get(0).getUuid()).isEqualTo("id1");
  }

  private PollingDocument getArtifactPollingDocument(String uuid) {
    DockerHubArtifactInfo dockerHubArtifactInfo = DockerHubArtifactInfo.builder().imagePath("imagePath").build();
    return getPollingDocument(uuid, dockerHubArtifactInfo);
  }

  private PollingDocument getManifestPollingDocument(String uuid) {
    HelmChartManifestInfo helmChartManifestInfo = HelmChartManifestInfo.builder().chartName("chartName").build();
    return getPollingDocument(uuid, helmChartManifestInfo);
  }

  private PollingDocument getPollingDocument(String uuid, PollingInfo pollingInfo) {
    PollingType pollingType;
    if (pollingInfo instanceof ManifestInfo) {
      pollingType = PollingType.MANIFEST;
    } else {
      pollingType = PollingType.ARTIFACT;
    }
    return PollingDocument.builder()
        .uuid(uuid)
        .pollingType(pollingType)
        .pollingInfo(pollingInfo)
        .failedAttempts(0)
        .accountId(ACCOUNT_ID)
        .orgIdentifier(ORG_ID)
        .projectIdentifier(PROJECT_ID)
        .signatures(Collections.singletonList(SIGNATURE_1))
        .build();
  }
}
