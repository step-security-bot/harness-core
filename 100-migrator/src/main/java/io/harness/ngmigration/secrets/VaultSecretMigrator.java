/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.secrets;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO.VaultConnectorDTOBuilder;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.ngmigration.dto.SecretManagerCreatedDTO;
import io.harness.ngmigration.service.MigratorUtility;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;

import software.wings.beans.VaultConfig;
import software.wings.ngmigration.CgEntityId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.CDC)
public class VaultSecretMigrator implements SecretMigrator {
  @Override
  public SecretTextSpecDTO getSecretSpec(
      EncryptedData encryptedData, SecretManagerConfig secretManagerConfig, String secretManagerIdentifier) {
    VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
    String value;
    if (StringUtils.isNotBlank(encryptedData.getPath())) {
      value = encryptedData.getPath();
    } else {
      String basePath = StringUtils.isNotBlank(vaultConfig.getBasePath()) ? vaultConfig.getBasePath() : "/harness";
      value = basePath + encryptedData.getEncryptionKey() + "#value";
    }
    return SecretTextSpecDTO.builder()
        .valueType(ValueType.Reference)
        .value(value)
        .secretManagerIdentifier(secretManagerIdentifier)
        .build();
  }

  @Override
  public SecretManagerCreatedDTO getConfigDTO(SecretManagerConfig secretManagerConfig, MigrationInputDTO inputDTO,
      Map<CgEntityId, NGYamlFile> migratedEntities) {
    VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;

    VaultConnectorDTOBuilder connectorDTO = VaultConnectorDTO.builder()
                                                .appRoleId(vaultConfig.getAppRoleId())
                                                .basePath(vaultConfig.getBasePath())
                                                .vaultUrl(vaultConfig.getVaultUrl())
                                                .renewalIntervalMinutes(vaultConfig.getRenewalInterval())
                                                .secretEngineManuallyConfigured(vaultConfig.isEngineManuallyEntered())
                                                .secretEngineName(vaultConfig.getSecretEngineName())
                                                .secretEngineVersion(vaultConfig.getSecretEngineVersion())
                                                .useVaultAgent(vaultConfig.isUseVaultAgent())
                                                .useAwsIam(false)
                                                .isDefault(vaultConfig.isDefault())
                                                .isReadOnly(vaultConfig.isReadOnly())
                                                .delegateSelectors(vaultConfig.getDelegateSelectors());

    String secretIdentifier =
        String.format("migratedHarnessSecret_%s", MigratorUtility.generateIdentifier(vaultConfig.getName()));
    NgEntityDetail secretEntityDetail = NgEntityDetail.builder()
                                            .identifier(secretIdentifier)
                                            .orgIdentifier(inputDTO.getOrgIdentifier())
                                            .projectIdentifier(inputDTO.getProjectIdentifier())
                                            .build();
    List<SecretDTOV2> secrets = new ArrayList<>();

    // Handle Auth Token
    if (StringUtils.isNotBlank(vaultConfig.getAuthToken())) {
      SecretDTOV2 authTokenNG = getSecretDTO(vaultConfig, inputDTO, secretIdentifier, vaultConfig.getAuthToken());
      connectorDTO.useK8sAuth(false).authToken(SecretRefData.builder()
                                                   .scope(MigratorUtility.getScope(secretEntityDetail))
                                                   .identifier(secretIdentifier)
                                                   .build());

      secrets.add(authTokenNG);
    }

    // Handle App Role
    if (StringUtils.isNotBlank(vaultConfig.getSecretId())) {
      SecretDTOV2 appRoleSecret = getSecretDTO(vaultConfig, inputDTO, secretIdentifier, vaultConfig.getSecretId());
      connectorDTO.useK8sAuth(false).authToken(SecretRefData.builder()
                                                   .scope(MigratorUtility.getScope(secretEntityDetail))
                                                   .identifier(secretIdentifier)
                                                   .build());
      secrets.add(appRoleSecret);
    }

    // Handle K8s auth
    if (!StringUtils.isAllBlank(vaultConfig.getVaultK8sAuthRole(), vaultConfig.getServiceAccountTokenPath())) {
      connectorDTO.useK8sAuth(true)
          .vaultK8sAuthRole(vaultConfig.getVaultK8sAuthRole())
          .serviceAccountTokenPath(vaultConfig.getServiceAccountTokenPath())
          .k8sAuthEndpoint(vaultConfig.getK8sAuthEndpoint());
    }

    // Handle Vault Agent
    if (StringUtils.isNotBlank(vaultConfig.getSinkPath())) {
      connectorDTO.useK8sAuth(false).sinkPath(vaultConfig.getSinkPath());
    }

    return SecretManagerCreatedDTO.builder().connector(connectorDTO.build()).secrets(secrets).build();
  }

  private SecretDTOV2 getSecretDTO(
      VaultConfig vaultConfig, MigrationInputDTO inputDTO, String secretIdentifier, String actualSecret) {
    return SecretDTOV2.builder()
        .identifier(secretIdentifier)
        .name(secretIdentifier)
        .description(String.format("Auto Generated Secret for Secret Manager - %s", vaultConfig.getName()))
        .orgIdentifier(inputDTO.getOrgIdentifier())
        .projectIdentifier(inputDTO.getProjectIdentifier())
        .type(SecretType.SecretText)
        .spec(SecretTextSpecDTO.builder()
                  .secretManagerIdentifier(
                      MigratorUtility.getIdentifierWithScope(NgEntityDetail.builder()
                                                                 .projectIdentifier(inputDTO.getProjectIdentifier())
                                                                 .orgIdentifier(inputDTO.getOrgIdentifier())
                                                                 .identifier("harnessSecretManager")
                                                                 .build()))
                  .value(actualSecret)
                  .valueType(ValueType.Inline)
                  .build())
        .build();
  }
}
