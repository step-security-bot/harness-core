/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.azure.model.AzureConstants.DEPLOYMENT_SLOT_PRODUCTION_NAME;
import static io.harness.azure.model.AzureConstants.SHIFT_TRAFFIC_SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.SOURCE_SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.TARGET_SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.TRAFFIC_WEIGHT_IN_PERCENTAGE_INVALID_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.delegate.task.azure.appservice.deployment.AzureAppServiceDeploymentService;
import io.harness.delegate.task.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
import io.harness.delegate.task.azure.common.AzureContainerRegistryService;
import io.harness.delegate.task.azure.common.AzureLogCallbackProvider;
import io.harness.exception.InvalidArgumentsException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Slf4j
@Singleton
public class AzureAppServiceResourceUtilities {
  @Inject private AzureContainerRegistryService azureContainerRegistryService;
  @Inject protected AzureAppServiceDeploymentService azureAppServiceDeploymentService;

  private static final int defaultTimeoutInterval = 10;

  public void swapSlots(AzureWebClientContext webClientContext, AzureLogCallbackProvider logCallbackProvider,
      String deploymentSlot, String targetSlot, Integer timeoutIntervalInMin) {
    if (DEPLOYMENT_SLOT_PRODUCTION_NAME.equalsIgnoreCase(deploymentSlot)) {
      String initialTargetSlot = targetSlot;
      targetSlot = deploymentSlot;
      deploymentSlot = initialTargetSlot;
    }

    AzureAppServiceDeploymentContext azureAppServiceDeploymentContext = new AzureAppServiceDeploymentContext();
    azureAppServiceDeploymentContext.setAzureWebClientContext(webClientContext);
    azureAppServiceDeploymentContext.setLogCallbackProvider(logCallbackProvider);
    azureAppServiceDeploymentContext.setSlotName(deploymentSlot);
    azureAppServiceDeploymentContext.setSteadyStateTimeoutInMin(timeoutIntervalInMin);

    azureAppServiceDeploymentService.swapSlotsUsingCallback(
        azureAppServiceDeploymentContext, targetSlot, logCallbackProvider);
  }

  public Map<String, AzureAppServiceApplicationSetting> getAppSettingsToAdd(
      List<AzureAppServiceApplicationSetting> applicationSettings) {
    return applicationSettings.stream().collect(
        Collectors.toMap(AzureAppServiceApplicationSetting::getName, Function.identity()));
  }

  public Map<String, AzureAppServiceConnectionString> getConnectionSettingsToAdd(
      List<AzureAppServiceConnectionString> connectionStrings) {
    return connectionStrings.stream().collect(
        Collectors.toMap(AzureAppServiceConnectionString::getName, Function.identity()));
  }

  public void validateSlotShiftTrafficParameters(String webAppName, String deploymentSlot, double trafficPercent) {
    if (isBlank(webAppName)) {
      throw new InvalidArgumentsException(WEB_APP_NAME_BLANK_ERROR_MSG);
    }

    if (isBlank(deploymentSlot)) {
      throw new InvalidArgumentsException(SHIFT_TRAFFIC_SLOT_NAME_BLANK_ERROR_MSG);
    }

    if (trafficPercent > 100.0 || trafficPercent < 0) {
      throw new InvalidArgumentsException(TRAFFIC_WEIGHT_IN_PERCENTAGE_INVALID_ERROR_MSG);
    }
  }

  public void validateSlotSwapParameters(String webAppName, String sourceSlot, String targetSlot) {
    if (isBlank(webAppName)) {
      throw new InvalidArgumentsException(WEB_APP_NAME_BLANK_ERROR_MSG);
    }

    if (isBlank(sourceSlot)) {
      throw new InvalidArgumentsException(SOURCE_SLOT_NAME_BLANK_ERROR_MSG);
    }

    if (isBlank(targetSlot)) {
      throw new InvalidArgumentsException(TARGET_SLOT_NAME_BLANK_ERROR_MSG);
    }
  }

  public int getTimeoutIntervalInMin(Integer timeoutIntervalInMin) {
    if (timeoutIntervalInMin != null) {
      return timeoutIntervalInMin;
    } else {
      log.warn("Missing timeout interval. Setting timeout interval to default 10 min");
      return defaultTimeoutInterval;
    }
  }
}
