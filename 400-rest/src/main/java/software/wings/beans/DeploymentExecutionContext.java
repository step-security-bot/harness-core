/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.artifact.ServiceArtifactVariableElement;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.artifact.Artifact;
import software.wings.sm.ExecutionContext;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@TargetModule(_870_CG_ORCHESTRATION)
public interface DeploymentExecutionContext extends ExecutionContext {
  WorkflowStandardParams fetchWorkflowStandardParamsFromContext();

  List<Artifact> getArtifacts();

  List<ServiceArtifactVariableElement> getArtifactVariableElements();

  Artifact getArtifactForService(String serviceId);

  Map<String, Artifact> getArtifactsForService(String serviceId);

  Artifact getDefaultArtifactForService(String serviceId);

  List<HelmChart> getHelmCharts();

  HelmChart getHelmChartForService(String serviceId);
}
