/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.JenkinsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface JenkinsBuildService extends BuildService<JenkinsConfig> {
  @Override
  @DelegateTaskType(TaskType.JENKINS_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, JenkinsConfig config,
      List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.JENKINS_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, JenkinsConfig config,
      List<EncryptedDataDetail> encryptionDetails, int limit);

  @Override
  @DelegateTaskType(TaskType.JENKINS_GET_JOBS)
  List<JobDetails> getJobs(
      JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName);

  @Override
  @DelegateTaskType(TaskType.JENKINS_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(
      String jobName, String groupId, JenkinsConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.JENKINS_LAST_SUCCESSFUL_BUILD)
  BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      JenkinsConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.JENKINS_GET_PLANS)
  Map<String, String> getPlans(JenkinsConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.JENKINS_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptedDataDetails);

  @Override
  @DelegateTaskType(TaskType.JENKINS_GET_JOB)
  JobDetails getJob(String jobName, JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails);
}
