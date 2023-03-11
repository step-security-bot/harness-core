/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.onboarding.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.idp.onboarding.beans.CatalogInfraConnectorType;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.spec.server.idp.v1.model.ConnectorDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@Data
@Builder
@FieldNameConstants(innerTypeName = "IdpCatalogConnectorKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@StoreIn(DbAliases.IDP)
@Entity(value = "idp_catalog_connector", noClassnameStored = true)
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.IDP)
public class CatalogConnector
    implements PersistentEntity, UuidAware, UpdatedAtAware, CreatedAtAware, CreatedByAware, UpdatedByAware {
  @Id private String uuid;
  @NotNull String accountId;
  @NotNull String identifier;
  @NotNull ConnectorDetails infraConnector;
  @NotNull ConnectorDetails sourceConnector;
  @NotNull CatalogInfraConnectorType type;
  @NotNull String repo;
  @NotNull String branch;
  @NotNull String path;
  @SchemaIgnore @CreatedBy private EmbeddedUser createdBy;
  @SchemaIgnore @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  private long createdAt;
  private long lastUpdatedAt;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("idp_catalog_connector_acc_identifier_idx")
                 .field(IdpCatalogConnectorKeys.accountId)
                 .field(IdpCatalogConnectorKeys.identifier)
                 .build())
        .build();
  }
}
