package io.harness.delegate.task.artifacts.azuremachineimage;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import io.harness.delegate.task.artifacts.mappers.AzureMachineImageResponseMapper;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class AzureMachineImageTaskHandler extends DelegateArtifactTaskHandler<AzureMachineImageDelegateRequest> {
  private final SecretDecryptionService secretDecryptionService;
  private final AzureMachineImageTaskHelper azureMachineImageTaskHelper;

  public ArtifactTaskExecutionResponse getResourceGroups(AzureMachineImageDelegateRequest attributesRequest) {
    AzureConfig azureConfig =
        AzureMachineImageResponseMapper.toAzureInternalConfig(attributesRequest, secretDecryptionService);
    return null;
  }
  public void decryptRequestDTOs(AzureMachineImageDelegateRequest azureMachineImageDelegateRequest) {
    if (azureMachineImageDelegateRequest.getAzureConnectorDTO().getCredential().getConfig()
            instanceof AzureManualDetailsDTO) {
      AzureManualDetailsDTO azureManualDetailsDTO =
          (AzureManualDetailsDTO) azureMachineImageDelegateRequest.getAzureConnectorDTO().getCredential().getConfig();
      if (azureManualDetailsDTO != null) {
        secretDecryptionService.decrypt(azureManualDetailsDTO.getAuthDTO().getCredentials(),
            azureMachineImageDelegateRequest.getEncryptedDataDetails());
      }
    }
  }
}
