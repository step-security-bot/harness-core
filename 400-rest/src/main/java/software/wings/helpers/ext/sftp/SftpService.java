/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.sftp;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.SftpConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

@TargetModule(_960_API_SERVICES)
@OwnedBy(CDC)
public interface SftpService {
  boolean isRunning(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails);

  List<String> getArtifactPaths(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails);

  List<BuildDetails> getBuildDetails(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails,
      List<String> artifactPaths, boolean isExpression);
}
