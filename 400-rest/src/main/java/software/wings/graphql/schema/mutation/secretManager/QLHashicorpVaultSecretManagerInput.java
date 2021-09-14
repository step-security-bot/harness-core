/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.mutation.secretManager;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLHashicorpVaultSecretManagerInput extends QLSecretManagerInput {
  String name;
  String namespace;
  String vaultUrl;
  QLHashicorpVaultAuthDetails authDetails;
  String basePath;
  boolean isReadOnly;
  String secretEngineName;
  int secretEngineVersion;
  long secretEngineRenewalInterval;
}
