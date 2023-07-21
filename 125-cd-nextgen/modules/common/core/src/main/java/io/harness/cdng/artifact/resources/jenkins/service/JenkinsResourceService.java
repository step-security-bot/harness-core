/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.jenkins.service;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.jenkins.dtos.JenkinsJobDetailsDTO;

import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;

import java.util.List;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ARTIFACTS})
public interface JenkinsResourceService {
  JenkinsJobDetailsDTO getJobDetails(
      IdentifierRef dockerConnectorRef, String orgIdentifier, String projectIdentifier, String parentJobName);
  List<String> getArtifactPath(
      IdentifierRef jenkinsConnectorRef, String orgIdentifier, String projectIdentifier, String jobName);
  List<BuildDetails> getBuildForJob(IdentifierRef jenkinsConnectorRef, String orgIdentifier, String projectIdentifier,
      String jobName, List<String> artifactPath);
  List<JobDetails> getJobParameters(
      IdentifierRef jenkinsConnectorRef, String orgIdentifier, String projectIdentifier, String jobName);
}
