/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.kubectl;

public class VersionCommand extends AbstractExecutable {
  private boolean jsonVersion;
  public VersionCommand(Kubectl client) {
    this.client = client;
  }

  Kubectl client;

  public VersionCommand jsonVersion() {
    this.jsonVersion = true;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("version");
    if (jsonVersion) {
      command.append(" --output=json");
    }
    return command.toString();
  }
}
