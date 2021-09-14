/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.FileStatus;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "GitProcessingRequestKeys")
@Document("gitProcessRequestSdk")
@TypeAlias("io.harness.gitsync.beans.GitProcessRequest")
@Entity(value = "gitProcessRequestSdk", noClassnameStored = true)
@StoreIn(DbAliases.ALL)
@OwnedBy(DX)
public class GitProcessRequest {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountId_repo_branch_commit_index")
                 .fields(Arrays.asList(GitProcessingRequestKeys.accountId, GitProcessingRequestKeys.repoUrl,
                     GitProcessingRequestKeys.branch, GitProcessingRequestKeys.commitId))
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotNull String commitId;
  List<FileStatus> fileStatuses;
  @NotNull String accountId;
  @NotNull String repoUrl;
  @NotNull String branch;
}
