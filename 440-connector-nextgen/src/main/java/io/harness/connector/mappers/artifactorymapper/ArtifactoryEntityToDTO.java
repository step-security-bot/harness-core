package io.harness.connector.mappers.artifactorymapper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryConnector;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class ArtifactoryEntityToDTO
    implements ConnectorEntityToDTOMapper<ArtifactoryConnectorDTO, ArtifactoryConnector> {
  @Override
  public ArtifactoryConnectorDTO createConnectorDTO(ArtifactoryConnector artifactoryConnector) {
    ArtifactoryAuthenticationDTO artifactoryAuthenticationDTO = null;
    if (artifactoryConnector.getAuthType() != ArtifactoryAuthType.ANONYMOUS
        || artifactoryConnector.getArtifactoryAuthentication() != null) {
      ArtifactoryUserNamePasswordAuthentication artifactoryCredentials =
          (ArtifactoryUserNamePasswordAuthentication) artifactoryConnector.getArtifactoryAuthentication();
      ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
          ArtifactoryUsernamePasswordAuthDTO.builder()
              .username(artifactoryCredentials.getUsername())
              .usernameRef(SecretRefHelper.createSecretRef(artifactoryCredentials.getUsernameRef()))
              .passwordRef(SecretRefHelper.createSecretRef(artifactoryCredentials.getPasswordRef()))
              .build();
      artifactoryAuthenticationDTO = ArtifactoryAuthenticationDTO.builder()
                                         .authType(artifactoryConnector.getAuthType())
                                         .credentials(artifactoryUsernamePasswordAuthDTO)
                                         .build();
    } else {
      artifactoryAuthenticationDTO =
          ArtifactoryAuthenticationDTO.builder().authType(artifactoryConnector.getAuthType()).build();
    }

    return ArtifactoryConnectorDTO.builder()
        .artifactoryServerUrl(artifactoryConnector.getUrl())
        .auth(artifactoryAuthenticationDTO)
        .build();
  }
}
