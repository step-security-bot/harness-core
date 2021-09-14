/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.SetupEnvCommandUnit;
import software.wings.beans.command.SetupEnvCommandUnit.Yaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class SetupEnvCommandUnitYamlHandler extends AbstractExecCommandUnitYamlHandler<Yaml, SetupEnvCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(SetupEnvCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected SetupEnvCommandUnit getCommandUnit() {
    return new SetupEnvCommandUnit();
  }
}
