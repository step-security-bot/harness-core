/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.account;

import io.harness.data.validator.Trimmed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@FieldNameConstants(innerTypeName = "AccountSettingsKeys")
@Entity(value = "accountSettings", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("accountSettings")
@Persistent
public class AccountSettings {
  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed String orgIdentifier;
  @Trimmed String projectIdentifier;
  AccountSettingType type;

  @Valid @NotNull AccountSettingConfig accountSettingConfig;
}
