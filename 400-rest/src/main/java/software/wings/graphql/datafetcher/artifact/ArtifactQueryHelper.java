/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.datafetcher.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactKeys;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.artifact.QLArtifactFilter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;

@OwnedBy(CDC)
@Singleton
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ArtifactQueryHelper {
  @Inject private DataFetcherUtils utils;

  public void setQuery(List<QLArtifactFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Artifact>> field;

      if (filter.getArtifact() != null) {
        field = query.field("_id");
        QLIdFilter artifactFilter = filter.getArtifact();
        utils.setIdFilter(field, artifactFilter);
      }

      if (filter.getArtifactSource() != null) {
        field = query.field(ArtifactKeys.artifactStreamId);
        QLIdFilter artifactSourceFilter = filter.getArtifactSource();
        utils.setIdFilter(field, artifactSourceFilter);
      }

      if (filter.getArtifactStreamType() != null) {
        field = query.field(ArtifactKeys.artifactStreamType);
        QLIdFilter artifactStreamTypeFilter = filter.getArtifactStreamType();
        utils.setIdFilter(field, artifactStreamTypeFilter);
      }
    });
  }
}
