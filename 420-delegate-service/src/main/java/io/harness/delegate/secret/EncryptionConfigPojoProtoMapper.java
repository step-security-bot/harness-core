/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.secret;

import io.harness.delegate.core.beans.EncryptionConfig;

import software.wings.beans.AzureVaultConfig;
import software.wings.beans.VaultConfig;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {SecretManagerTypePojoProtoMapper.class, EncryptionTypePojoProtoMapper.class,
            VaultConfigPojoProtoMapper.class, AzureVaultConfigPojoProtoMapper.class},
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedSourcePolicy = ReportingPolicy.ERROR,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface EncryptionConfigPojoProtoMapper {
  EncryptionConfigPojoProtoMapper INSTANCE = Mappers.getMapper(EncryptionConfigPojoProtoMapper.class);

  @Mapping(target = "secretManagerType", source = "type")
  @Mapping(target = "uuid", source = "uuid",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "accountId", source = "accountId",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "name", source = "name",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "encryptionServiceUrl", source = "encryptionServiceUrl",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "isGlobalKms", source = "globalKms",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @BeanMapping(ignoreUnmappedSourceProperties = {"numOfEncryptedValue", "default", "validationCriteria"})
  @SubclassMapping(target = EncryptionConfig.class, source = VaultConfig.class)
  @SubclassMapping(target = EncryptionConfig.class, source = AzureVaultConfig.class)
  EncryptionConfig
  map(io.harness.security.encryption.EncryptionConfig config);
}
