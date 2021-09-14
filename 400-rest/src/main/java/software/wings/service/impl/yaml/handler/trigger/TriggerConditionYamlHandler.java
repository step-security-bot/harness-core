/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.yaml.trigger.TriggerConditionYaml;

import com.google.inject.Inject;
import java.util.List;
import lombok.Data;

@OwnedBy(CDC)
@Data
public abstract class TriggerConditionYamlHandler<Y extends TriggerConditionYaml>
    extends BaseYamlHandler<Y, TriggerCondition> {
  @Inject protected YamlHelper yamlHelper;
  @Override
  public void delete(ChangeContext<Y> changeContext) {}
  @Override
  public TriggerCondition get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override public abstract Y toYaml(TriggerCondition bean, String appId);

  @Override
  public abstract TriggerCondition upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);
}
