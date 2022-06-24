/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless.request;

import static io.harness.delegate.beans.storeconfig.StoreDelegateConfigType.GIT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryCapabilityHelper;
import io.harness.delegate.beans.connector.awsconnector.AwsCapabilityHelper;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.scm.GitCapabilityHelper;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.ServerlessInstallationCapability;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.serverless.ServerlessArtifactConfig;
import io.harness.delegate.task.serverless.ServerlessArtifactType;
import io.harness.delegate.task.serverless.ServerlessArtifactoryArtifactConfig;
import io.harness.delegate.task.serverless.ServerlessArtifactsConfig;
import io.harness.delegate.task.serverless.ServerlessAwsLambdaInfraConfig;
import io.harness.delegate.task.serverless.ServerlessAwsLambdaManifestConfig;
import io.harness.delegate.task.serverless.ServerlessCommandType;
import io.harness.delegate.task.serverless.ServerlessEcrArtifactConfig;
import io.harness.delegate.task.serverless.ServerlessInfraConfig;
import io.harness.delegate.task.serverless.ServerlessManifestConfig;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.CDP)
public interface ServerlessCommandRequest extends TaskParameters, ExecutionCapabilityDemander {
  String getAccountId();
  @NotEmpty ServerlessCommandType getServerlessCommandType();
  String getCommandName();
  CommandUnitsProgress getCommandUnitsProgress();
  ServerlessInfraConfig getServerlessInfraConfig();
  ServerlessManifestConfig getServerlessManifestConfig();
  Integer getTimeoutIntervalInMin();
  ServerlessArtifactsConfig getServerlessArtifactsConfig();

  @Override
  default List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    ServerlessInfraConfig serverlessInfraConfig = getServerlessInfraConfig();
    ServerlessManifestConfig serverlessManifestConfig = getServerlessManifestConfig();
    ServerlessArtifactsConfig serverlessArtifactsConfig = getServerlessArtifactsConfig();
    List<EncryptedDataDetail> cloudProviderEncryptionDetails = serverlessInfraConfig.getEncryptionDataDetails();

    List<ExecutionCapability> capabilities =
        new ArrayList<>(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
            cloudProviderEncryptionDetails, maskingEvaluator));
    if (serverlessInfraConfig instanceof ServerlessAwsLambdaInfraConfig) {
      AwsConnectorDTO awsConnectorDTO = ((ServerlessAwsLambdaInfraConfig) serverlessInfraConfig).getAwsConnectorDTO();
      capabilities.addAll(AwsCapabilityHelper.fetchRequiredExecutionCapabilities(awsConnectorDTO, maskingEvaluator));
    }
    if (getServerlessManifestConfig() != null) {
      if (serverlessManifestConfig instanceof ServerlessAwsLambdaManifestConfig) {
        ServerlessAwsLambdaManifestConfig serverlessAwsLambdaManifestConfig =
            (ServerlessAwsLambdaManifestConfig) serverlessManifestConfig;
        if (serverlessAwsLambdaManifestConfig.getGitStoreDelegateConfig().getType() == GIT) {
          GitStoreDelegateConfig gitStoreDelegateConfig = serverlessAwsLambdaManifestConfig.getGitStoreDelegateConfig();
          capabilities.addAll(GitCapabilityHelper.fetchRequiredExecutionCapabilities(
              ScmConnectorMapper.toGitConfigDTO(gitStoreDelegateConfig.getGitConfigDTO()),
              gitStoreDelegateConfig.getEncryptedDataDetails(), gitStoreDelegateConfig.getSshKeySpecDTO()));
          capabilities.addAll(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
              gitStoreDelegateConfig.getEncryptedDataDetails(), maskingEvaluator));
        }
      }
    }

    if (serverlessArtifactsConfig != null) {
      ServerlessArtifactConfig primaryArtifact = serverlessArtifactsConfig.getPrimary();
      if (primaryArtifact != null && primaryArtifact instanceof ServerlessArtifactoryArtifactConfig) {
        capabilities.addAll(ArtifactoryCapabilityHelper.fetchRequiredExecutionCapabilities(
            serverlessArtifactoryArtifactConfig.getConnectorDTO().getConnectorConfig(), maskingEvaluator));
      } else if (serverlessArtifactConfig instanceof ServerlessEcrArtifactConfig) {
        AwsConnectorDTO connectorConfigDTO = (AwsConnectorDTO) ((ServerlessEcrArtifactConfig) serverlessArtifactConfig)
                                                 .getConnectorDTO()
                                                 .getConnectorConfig();
        capabilities.addAll(
            AwsCapabilityHelper.fetchRequiredExecutionCapabilities(connectorConfigDTO, maskingEvaluator));
                ((ServerlessArtifactoryArtifactConfig) primaryArtifact).getConnectorDTO().getConnectorConfig(), maskingEvaluator));
      }
      if (serverlessArtifactsConfig.getSidecars() != null) {
        for(ServerlessArtifactConfig serverlessArtifactConfig : serverlessArtifactsConfig.getSidecars().values()) {
          if (serverlessArtifactConfig.getServerlessArtifactType().equals(ServerlessArtifactType.ARTIFACTORY)) {
            capabilities.addAll(ArtifactoryCapabilityHelper.fetchRequiredExecutionCapabilities(
                    ((ServerlessArtifactoryArtifactConfig) serverlessArtifactConfig).getConnectorDTO().getConnectorConfig(), maskingEvaluator));
          }
        }
      }

    capabilities.add(ServerlessInstallationCapability.builder().criteria("Serverless Installed").build());
    return capabilities;
  }
}
