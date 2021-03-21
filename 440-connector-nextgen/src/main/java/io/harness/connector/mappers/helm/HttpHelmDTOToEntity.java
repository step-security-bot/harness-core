package io.harness.connector.mappers.helm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.delegate.beans.connector.ConnectorType.HTTP_HELM_REPO;
import static io.harness.delegate.beans.connector.helm.HttpHelmAuthType.USER_PASSWORD;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.helm.HttpHelmConnector;
import io.harness.connector.entities.embedded.helm.HttpHelmUsernamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class HttpHelmDTOToEntity implements ConnectorDTOToEntityMapper<HttpHelmConnectorDTO, HttpHelmConnector> {
  @Override
  public HttpHelmConnector toConnectorEntity(HttpHelmConnectorDTO connectorDTO) {
    HttpHelmConnector connectorEntity = HttpHelmConnector.builder()
                                            .url(connectorDTO.getHelmRepoUrl())
                                            .authType(connectorDTO.getAuth().getAuthType())
                                            .httpHelmAuthentication(createHttpHelmAuthentication(connectorDTO))
                                            .build();

    connectorEntity.setType(HTTP_HELM_REPO);
    return connectorEntity;
  }

  private HttpHelmUsernamePasswordAuthentication createHttpHelmAuthentication(HttpHelmConnectorDTO connectorDTO) {
    if (connectorDTO.getAuth().getAuthType() != USER_PASSWORD) {
      return null;
    }

    HttpHelmUsernamePasswordDTO usernamePasswordDTO =
        (HttpHelmUsernamePasswordDTO) connectorDTO.getAuth().getCredentials();
    return HttpHelmUsernamePasswordAuthentication.builder()
        .username(usernamePasswordDTO.getUsername())
        .usernameRef(SecretRefHelper.getSecretConfigString(usernamePasswordDTO.getUsernameRef()))
        .passwordRef(SecretRefHelper.getSecretConfigString(usernamePasswordDTO.getPasswordRef()))
        .build();
  }
}
