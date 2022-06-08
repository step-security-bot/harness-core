/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import java.time.OffsetDateTime;
import java.util.Date;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.SerializationFormat;
import software.wings.beans.TaskType;

@Value
@Builder
@Entity(value = "!!!custom_delegateSyncTaskResponses", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "DelegateSyncTaskResponseKeys")
@StoreIn(DbAliases.ALL)
public class DelegateSyncTaskResponse implements PersistentEntity {
  @Id @org.springframework.data.annotation.Id private String uuid;
  private byte[] responseData;
  private TaskType taskType;
  private SerializationFormat serializationFormat;

  @FdTtlIndex @Builder.Default private Date validUntil = Date.from(OffsetDateTime.now().plusHours(2).toInstant());
}
