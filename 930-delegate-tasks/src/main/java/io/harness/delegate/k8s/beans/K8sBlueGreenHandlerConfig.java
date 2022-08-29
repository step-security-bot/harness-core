/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.k8s.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.k8s.PrePruningInfo;
import io.harness.k8s.model.KubernetesResource;

import io.kubernetes.client.openapi.models.V1Secret;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class K8sBlueGreenHandlerConfig extends K8sHandlerConfig {
  private KubernetesResource managedWorkload;
  private KubernetesResource primaryService;
  private KubernetesResource stageService;
  private String primaryColor;
  private String stageColor;
  private PrePruningInfo prePruningInfo;
  private List<V1Secret> releaseHistory;
  private V1Secret currentRelease;
  private int currentReleaseNumber;
}
