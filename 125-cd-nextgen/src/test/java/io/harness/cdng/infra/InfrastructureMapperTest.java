/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.infra;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABHISHEK;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.FILIP;
import static io.harness.rule.OwnerRule.MLUKIC;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static io.harness.rule.OwnerRule.TMACARI;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.AzureWebAppInfrastructureOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcomeAbstract;
import io.harness.cdng.infra.beans.K8sAzureInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.infra.beans.PdcInfrastructureOutcome;
import io.harness.cdng.infra.beans.ServerlessAwsLambdaInfrastructureOutcome;
import io.harness.cdng.infra.beans.SshWinRmAzureInfrastructureOutcome;
import io.harness.cdng.infra.beans.host.HostFilter;
import io.harness.cdng.infra.beans.host.HostNamesFilter;
import io.harness.cdng.infra.beans.host.dto.AllHostsFilterDTO;
import io.harness.cdng.infra.beans.host.dto.HostFilterDTO;
import io.harness.cdng.infra.beans.host.dto.HostNamesFilterDTO;
import io.harness.cdng.infra.yaml.AzureWebAppInfrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8sAzureInfrastructure;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.cdng.infra.yaml.PdcInfrastructure;
import io.harness.cdng.infra.yaml.ServerlessAwsLambdaInfrastructure;
import io.harness.cdng.infra.yaml.SshWinRmAzureInfrastructure;
import io.harness.cdng.service.steps.ServiceStepOutcome;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.pdcconnector.HostFilterType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.steps.environment.EnvironmentOutcome;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class InfrastructureMapperTest extends CategoryTest {
  @Mock private ConnectorService connectorService;
  @InjectMocks private InfrastructureMapper infrastructureMapper;
  private final EnvironmentOutcome environment =
      EnvironmentOutcome.builder().identifier("env").type(EnvironmentType.Production).build();
  private final ServiceStepOutcome serviceOutcome = ServiceStepOutcome.builder().identifier("service").build();

  AutoCloseable mocks;
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);

    doReturn(
        Optional.of(
            ConnectorResponseDTO.builder().connector(ConnectorInfoDTO.builder().name("my_connector").build()).build()))
        .when(connectorService)
        .getByRef(anyString(), anyString(), anyString(), anyString());
  }

  @After
  public void tearDown() throws Exception {
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testToOutcome() {
    K8SDirectInfrastructure k8SDirectInfrastructure = K8SDirectInfrastructure.builder()
                                                          .connectorRef(ParameterField.createValueField("connectorId"))
                                                          .namespace(ParameterField.createValueField("namespace"))
                                                          .releaseName(ParameterField.createValueField("release"))
                                                          .build();

    K8sDirectInfrastructureOutcome k8sDirectInfrastructureOutcome =
        K8sDirectInfrastructureOutcome.builder()
            .connectorRef("connectorId")
            .namespace("namespace")
            .releaseName("release")
            .environment(environment)
            .infrastructureKey("11f6673d11711af46238bf33972cb99a4a869244")
            .build();

    InfrastructureOutcome infrastructureOutcome = infrastructureMapper.toOutcome(
        k8SDirectInfrastructure, environment, serviceOutcome, "accountId", "projId", "orgId");
    assertThat(infrastructureOutcome).isEqualTo(k8sDirectInfrastructureOutcome);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testToOutcomeConnectorNotFound() {
    doReturn(Optional.empty()).when(connectorService).getByRef(anyString(), anyString(), anyString(), anyString());

    K8SDirectInfrastructure k8SDirectInfrastructure = K8SDirectInfrastructure.builder()
                                                          .connectorRef(ParameterField.createValueField("connectorId"))
                                                          .namespace(ParameterField.createValueField("namespace"))
                                                          .releaseName(ParameterField.createValueField("release"))
                                                          .build();

    K8sDirectInfrastructureOutcome k8sDirectInfrastructureOutcome =
        K8sDirectInfrastructureOutcome.builder()
            .connectorRef("connectorId")
            .namespace("namespace")
            .releaseName("release")
            .environment(environment)
            .infrastructureKey("11f6673d11711af46238bf33972cb99a4a869244")
            .build();

    InfrastructureOutcome infrastructureOutcome = infrastructureMapper.toOutcome(
        k8SDirectInfrastructure, environment, serviceOutcome, "accountId", "projId", "orgId");
    assertThat(infrastructureOutcome).isEqualTo(k8sDirectInfrastructureOutcome);
    assertThat(infrastructureOutcome.getConnector()).isNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testToOutcomeEmptyValues() {
    K8SDirectInfrastructure emptyReleaseName = K8SDirectInfrastructure.builder()
                                                   .connectorRef(ParameterField.createValueField("connectorId"))
                                                   .namespace(ParameterField.createValueField("namespace"))
                                                   .releaseName(ParameterField.createValueField(""))
                                                   .build();

    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptyReleaseName, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);

    K8SDirectInfrastructure emptyNamespace = K8SDirectInfrastructure.builder()
                                                 .connectorRef(ParameterField.createValueField("connectorId"))
                                                 .namespace(ParameterField.createValueField(""))
                                                 .releaseName(ParameterField.createValueField("releaseName"))
                                                 .build();

    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptyNamespace, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testK8sGcpInfraMapper() {
    K8sGcpInfrastructure k8SGcpInfrastructure = K8sGcpInfrastructure.builder()
                                                    .connectorRef(ParameterField.createValueField("connectorId"))
                                                    .namespace(ParameterField.createValueField("namespace"))
                                                    .releaseName(ParameterField.createValueField("release"))
                                                    .cluster(ParameterField.createValueField("cluster"))
                                                    .build();

    K8sGcpInfrastructureOutcome k8sGcpInfrastructureOutcome =
        K8sGcpInfrastructureOutcome.builder()
            .connectorRef("connectorId")
            .namespace("namespace")
            .releaseName("release")
            .cluster("cluster")
            .environment(environment)
            .infrastructureKey("54874007d7082ff0ab54cd51865954f5e78c5c88")
            .build();

    InfrastructureOutcome infrastructureOutcome = infrastructureMapper.toOutcome(
        k8SGcpInfrastructure, environment, serviceOutcome, "accountId", "projId", "orgId");
    assertThat(infrastructureOutcome).isEqualTo(k8sGcpInfrastructureOutcome);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testK8sGcpInfraMapperEmptyValues() {
    K8sGcpInfrastructure emptyNamespace = K8sGcpInfrastructure.builder()
                                              .connectorRef(ParameterField.createValueField("connectorId"))
                                              .namespace(ParameterField.createValueField(""))
                                              .releaseName(ParameterField.createValueField("release"))
                                              .cluster(ParameterField.createValueField("cluster"))
                                              .build();
    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptyNamespace, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);

    K8sGcpInfrastructure emptyReleaseName = K8sGcpInfrastructure.builder()
                                                .connectorRef(ParameterField.createValueField("connectorId"))
                                                .namespace(ParameterField.createValueField("namespace"))
                                                .releaseName(ParameterField.createValueField(""))
                                                .cluster(ParameterField.createValueField("cluster"))
                                                .build();
    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptyReleaseName, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);

    K8sGcpInfrastructure emptyClusterName = K8sGcpInfrastructure.builder()
                                                .connectorRef(ParameterField.createValueField("connectorId"))
                                                .namespace(ParameterField.createValueField("namespace"))
                                                .releaseName(ParameterField.createValueField("release"))
                                                .cluster(ParameterField.createValueField(""))
                                                .build();
    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptyClusterName, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testServerlessAwsInfraMapper() {
    ServerlessAwsLambdaInfrastructure serverlessAwsLambdaInfrastructure =
        ServerlessAwsLambdaInfrastructure.builder()
            .connectorRef(ParameterField.createValueField("connectorId"))
            .region(ParameterField.createValueField("region"))
            .stage(ParameterField.createValueField("stage"))
            .build();

    ServerlessAwsLambdaInfrastructureOutcome expectedOutcome =
        ServerlessAwsLambdaInfrastructureOutcome.builder()
            .connectorRef("connectorId")
            .region("region")
            .stage("stage")
            .environment(environment)
            .infrastructureKey("ad53b5ff347a533d21b0d02bab1ae1d62506068c")
            .build();

    expectedOutcome.setConnector(Connector.builder().name("my_connector").build());

    InfrastructureOutcome infrastructureOutcome = infrastructureMapper.toOutcome(
        serverlessAwsLambdaInfrastructure, environment, serviceOutcome, "accountId", "projId", "orgId");
    assertThat(infrastructureOutcome).isEqualTo(expectedOutcome);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testServerlessAwsInfraMapperEmptyValues() {
    ServerlessAwsLambdaInfrastructure emptyRegion = ServerlessAwsLambdaInfrastructure.builder()
                                                        .connectorRef(ParameterField.createValueField("connectorId"))
                                                        .region(ParameterField.createValueField(""))
                                                        .stage(ParameterField.createValueField("stage"))
                                                        .build();
    assertThatThrownBy(
        () -> infrastructureMapper.toOutcome(emptyRegion, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);

    ServerlessAwsLambdaInfrastructure emptyStage = ServerlessAwsLambdaInfrastructure.builder()
                                                       .connectorRef(ParameterField.createValueField("connectorId"))
                                                       .region(ParameterField.createValueField("region"))
                                                       .stage(ParameterField.createValueField(""))
                                                       .build();
    assertThatThrownBy(
        () -> infrastructureMapper.toOutcome(emptyStage, environment, serviceOutcome, "accountId", "projId", "orgId"));
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void testPdcInfrastructureWithConnectorToOutcome() {
    PdcInfrastructure infrastructure =
        PdcInfrastructure.builder()
            .credentialsRef(ParameterField.createValueField("ssh-key-ref"))
            .connectorRef(ParameterField.createValueField("connector-ref"))
            .hostFilter(HostFilter.builder()
                            .type(HostFilterType.HOST_NAMES)
                            .spec(HostNamesFilter.builder()
                                      .value(ParameterField.createValueField(Arrays.asList("host1", "host2")))
                                      .build())
                            .build())
            .build();

    InfrastructureOutcome infrastructureOutcome =
        infrastructureMapper.toOutcome(infrastructure, environment, serviceOutcome, "accountId", "projId", "orgId");

    PdcInfrastructureOutcome outcome =
        PdcInfrastructureOutcome.builder()
            .credentialsRef("ssh-key-ref")
            .connectorRef("connector-ref")
            .hostFilter(HostFilterDTO.builder()
                            .type(HostFilterType.HOST_NAMES)
                            .spec(HostNamesFilterDTO.builder()
                                      .value(ParameterField.createValueField(Arrays.asList("host1", "host2")))
                                      .build())
                            .build())
            .environment(environment)
            .build();
    outcome.setConnector(Connector.builder().name("my_connector").build());

    assertThat(infrastructureOutcome).isEqualToIgnoringGivenFields(outcome, "infrastructureKey");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void testPdcInfrastructureWithHostsToOutcome() {
    PdcInfrastructure infrastructure =
        PdcInfrastructure.builder()
            .credentialsRef(ParameterField.createValueField("ssh-key-ref"))
            .hosts(ParameterField.createValueField(Arrays.asList("host1", "host2", "host3")))
            .build();

    InfrastructureOutcome infrastructureOutcome =
        infrastructureMapper.toOutcome(infrastructure, environment, serviceOutcome, "accountId", "projId", "orgId");

    assertThat(infrastructureOutcome)
        .isEqualToIgnoringGivenFields(
            PdcInfrastructureOutcome.builder()
                .credentialsRef("ssh-key-ref")
                .hosts(Arrays.asList("host1", "host2", "host3"))
                .environment(environment)
                .hostFilter(
                    HostFilterDTO.builder().spec(AllHostsFilterDTO.builder().build()).type(HostFilterType.ALL).build())
                .build(),
            "infrastructureKey");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void testPdcInfrastructureEmptySshKeyRef() {
    PdcInfrastructure emptySshKeyRef =
        PdcInfrastructure.builder().connectorRef(ParameterField.createValueField("connector-ref")).build();

    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptySshKeyRef, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void testPdcInfrastructureEmptyHostsAndConnector() {
    PdcInfrastructure emptySshKeyRef = PdcInfrastructure.builder()
                                           .credentialsRef(ParameterField.createValueField("ssh-key-ref"))
                                           .hosts(ParameterField.ofNull())
                                           .connectorRef(ParameterField.ofNull())
                                           .build();

    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptySshKeyRef, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void testSshWinRmAzureInfrastructureToOutcome() {
    SshWinRmAzureInfrastructure infrastructure =
        SshWinRmAzureInfrastructure.builder()
            .connectorRef(ParameterField.createValueField("connector-ref"))
            .credentialsRef(ParameterField.createValueField("credentials-ref"))
            .resourceGroup(ParameterField.createValueField("res-group"))
            .subscriptionId(ParameterField.createValueField("sub-id"))
            .tags(ParameterField.createValueField(Collections.singletonMap("tag", "val")))
            .hostConnectionType(ParameterField.createValueField("Hostname"))
            .build();

    InfrastructureOutcome infrastructureOutcome =
        infrastructureMapper.toOutcome(infrastructure, environment, serviceOutcome, "accountId", "projId", "orgId");

    SshWinRmAzureInfrastructureOutcome outcome = SshWinRmAzureInfrastructureOutcome.builder()
                                                     .connectorRef("connector-ref")
                                                     .credentialsRef("credentials-ref")
                                                     .resourceGroup("res-group")
                                                     .subscriptionId("sub-id")
                                                     .tags(Collections.singletonMap("tag", "val"))
                                                     .hostConnectionType("Hostname")
                                                     .environment(environment)
                                                     .build();
    outcome.setConnector(Connector.builder().name("my_connector").build());
    assertThat(infrastructureOutcome).isEqualToIgnoringGivenFields(outcome, "infrastructureKey");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void testSshWinRmAzureInfrastructureEmptyCredentialsRefAndResourceGroup() {
    SshWinRmAzureInfrastructure invalidInfra = SshWinRmAzureInfrastructure.builder()
                                                   .credentialsRef(ParameterField.ofNull())
                                                   .resourceGroup(ParameterField.ofNull())
                                                   .connectorRef(ParameterField.createValueField("connector-ref"))
                                                   .subscriptionId(ParameterField.createValueField("sub-id"))
                                                   .build();

    assertThatThrownBy(
        () -> infrastructureMapper.toOutcome(invalidInfra, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void testSshWinRmAzureInfrastructureEmptySubscriptionIdAndConnectorId() {
    SshWinRmAzureInfrastructure invalidInfra = SshWinRmAzureInfrastructure.builder()
                                                   .subscriptionId(ParameterField.ofNull())
                                                   .connectorRef(ParameterField.ofNull())
                                                   .credentialsRef(ParameterField.createValueField("ssh-key-ref"))
                                                   .resourceGroup(ParameterField.createValueField("resource-id"))
                                                   .build();

    assertThatThrownBy(
        () -> infrastructureMapper.toOutcome(invalidInfra, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testK8sAzureInfraMapper() {
    K8sAzureInfrastructure k8SAzureInfrastructure =
        K8sAzureInfrastructure.builder()
            .connectorRef(ParameterField.createValueField("connectorId"))
            .namespace(ParameterField.createValueField("namespace"))
            .releaseName(ParameterField.createValueField("release"))
            .subscriptionId(ParameterField.createValueField("subscriptionId"))
            .resourceGroup(ParameterField.createValueField("resourceGroup"))
            .cluster(ParameterField.createValueField("cluster"))
            .useClusterAdminCredentials(ParameterField.createValueField(true))
            .build();

    K8sAzureInfrastructureOutcome k8sAzureInfrastructureOutcome =
        K8sAzureInfrastructureOutcome.builder()
            .connectorRef("connectorId")
            .namespace("namespace")
            .releaseName("release")
            .subscription("subscriptionId")
            .resourceGroup("resourceGroup")
            .cluster("cluster")
            .environment(environment)
            .infrastructureKey("8f62fc4abbc11a8400589ccac4b76f32ba0f7df2")
            .useClusterAdminCredentials(true)
            .build();

    assertThat(infrastructureMapper.toOutcome(
                   k8SAzureInfrastructure, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isEqualTo(k8sAzureInfrastructureOutcome);

    k8SAzureInfrastructure = K8sAzureInfrastructure.builder()
                                 .connectorRef(ParameterField.createValueField("connectorId"))
                                 .namespace(ParameterField.createValueField("namespace"))
                                 .releaseName(ParameterField.createValueField("release"))
                                 .subscriptionId(ParameterField.createValueField("subscriptionId"))
                                 .resourceGroup(ParameterField.createValueField("resourceGroup"))
                                 .cluster(ParameterField.createValueField("cluster"))
                                 .useClusterAdminCredentials(ParameterField.createValueField(false))
                                 .build();

    k8sAzureInfrastructureOutcome = K8sAzureInfrastructureOutcome.builder()
                                        .connectorRef("connectorId")
                                        .namespace("namespace")
                                        .releaseName("release")
                                        .subscription("subscriptionId")
                                        .resourceGroup("resourceGroup")
                                        .cluster("cluster")
                                        .environment(environment)
                                        .infrastructureKey("8f62fc4abbc11a8400589ccac4b76f32ba0f7df2")
                                        .useClusterAdminCredentials(false)
                                        .build();

    assertThat(infrastructureMapper.toOutcome(
                   k8SAzureInfrastructure, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isEqualTo(k8sAzureInfrastructureOutcome);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testAzureWebAppInfraMapper() {
    AzureWebAppInfrastructure azureWebAppInfrastructure =
        AzureWebAppInfrastructure.builder()
            .connectorRef(ParameterField.createValueField("connectorId"))
            .subscriptionId(ParameterField.createValueField("subscriptionId"))
            .resourceGroup(ParameterField.createValueField("resourceGroup"))
            .build();

    InfrastructureOutcome infrastructureOutcome = infrastructureMapper.toOutcome(
        azureWebAppInfrastructure, environment, serviceOutcome, "accountId", "projId", "orgId");
    AzureWebAppInfrastructureOutcome outcome = AzureWebAppInfrastructureOutcome.builder()
                                                   .connectorRef("connectorId")
                                                   .subscription("subscriptionId")
                                                   .resourceGroup("resourceGroup")
                                                   .environment(environment)
                                                   .build();
    outcome.setConnector(Connector.builder().name("my_connector").build());
    assertThat(infrastructureOutcome).isEqualToIgnoringGivenFields(outcome, "infrastructureKey");
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testK8sAzureInfraMapperEmptyValues() {
    K8sAzureInfrastructure emptyNamespace = K8sAzureInfrastructure.builder()
                                                .connectorRef(ParameterField.createValueField("connectorId"))
                                                .namespace(ParameterField.createValueField(""))
                                                .releaseName(ParameterField.createValueField("release"))
                                                .subscriptionId(ParameterField.createValueField("subscriptionId"))
                                                .resourceGroup(ParameterField.createValueField("resourceGroup"))
                                                .cluster(ParameterField.createValueField("cluster"))
                                                .build();
    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptyNamespace, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);

    K8sAzureInfrastructure emptyReleaseName = K8sAzureInfrastructure.builder()
                                                  .connectorRef(ParameterField.createValueField("connectorId"))
                                                  .namespace(ParameterField.createValueField("namespace"))
                                                  .releaseName(ParameterField.createValueField(""))
                                                  .subscriptionId(ParameterField.createValueField("subscriptionId"))
                                                  .resourceGroup(ParameterField.createValueField("resourceGroup"))
                                                  .cluster(ParameterField.createValueField("cluster"))
                                                  .build();
    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptyReleaseName, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);

    K8sAzureInfrastructure emptySubscription = K8sAzureInfrastructure.builder()
                                                   .connectorRef(ParameterField.createValueField("connectorId"))
                                                   .namespace(ParameterField.createValueField("namespace"))
                                                   .releaseName(ParameterField.createValueField("release"))
                                                   .subscriptionId(ParameterField.createValueField(""))
                                                   .resourceGroup(ParameterField.createValueField("resourceGroup"))
                                                   .cluster(ParameterField.createValueField("cluster"))
                                                   .build();
    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptySubscription, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);

    K8sAzureInfrastructure emptyResourceGroupName =
        K8sAzureInfrastructure.builder()
            .connectorRef(ParameterField.createValueField("connectorId"))
            .namespace(ParameterField.createValueField("namespace"))
            .releaseName(ParameterField.createValueField("release"))
            .subscriptionId(ParameterField.createValueField("subscriptionId"))
            .resourceGroup(ParameterField.createValueField(""))
            .cluster(ParameterField.createValueField("cluster"))
            .build();
    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptyResourceGroupName, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);

    K8sAzureInfrastructure emptyClusterName = K8sAzureInfrastructure.builder()
                                                  .connectorRef(ParameterField.createValueField("connectorId"))
                                                  .namespace(ParameterField.createValueField("namespace"))
                                                  .releaseName(ParameterField.createValueField("release"))
                                                  .subscriptionId(ParameterField.createValueField("subscriptionId"))
                                                  .resourceGroup(ParameterField.createValueField("resourceGroup"))
                                                  .cluster(ParameterField.createValueField(""))
                                                  .build();
    assertThatThrownBy(()
                           -> infrastructureMapper.toOutcome(
                               emptyClusterName, environment, serviceOutcome, "accountId", "projId", "orgId"))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void testSetInfraIdentifierAndName_InfrastructureDetailsAbstract() {
    InfrastructureOutcomeAbstract k8SDirectInfrastructureOutcome = K8sDirectInfrastructureOutcome.builder().build();
    infrastructureMapper.setInfraIdentifierAndName(k8SDirectInfrastructureOutcome, "Identifier", "Name");
    assertThat(k8SDirectInfrastructureOutcome.getInfraIdentifier()).isEqualTo("Identifier");
    assertThat(k8SDirectInfrastructureOutcome.getInfraName()).isEqualTo("Name");
  }
}
