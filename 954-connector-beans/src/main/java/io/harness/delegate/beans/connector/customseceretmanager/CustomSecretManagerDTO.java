/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.customseceretmanager;

import io.harness.SecretManagerDescriptionConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.TemplateInfo;
import io.harness.security.encryption.EncryptedDataParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.*;
import lombok.experimental.FieldDefaults;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("CustomSecretManager")
@OwnedBy(HarnessTeam.DX)
@Schema(name = "CustomSecretManager", description = "This contains details of Custom Secret Manager connectors")
public class CustomSecretManagerDTO extends ConnectorConfigDTO implements DelegateSelectable {
  Set<String> delegateSelectors;
  private boolean onDelegate;

  @Schema(description = SecretManagerDescriptionConstants.AUTH_TOKEN) private String connectorRef;
  private String host;
  private String workingDirectory;
  private TemplateInfo templateRef;
  private Set<EncryptedDataParams> testVariables;
  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return Collections.singletonList(this);
  }
}
