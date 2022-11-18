/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.spot;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.spotinst.model.SpotInstConstants.DEFAULT_ELASTIGROUP_MAX_INSTANCES;
import static io.harness.spotinst.model.SpotInstConstants.DEFAULT_ELASTIGROUP_MIN_INSTANCES;
import static io.harness.spotinst.model.SpotInstConstants.DEFAULT_ELASTIGROUP_TARGET_INSTANCES;
import static io.harness.spotinst.model.SpotInstConstants.DELETE_NEW_ELASTI_GROUP;
import static io.harness.spotinst.model.SpotInstConstants.SWAP_ROUTES_COMMAND_UNIT;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.concurrent.HTimeLimiter;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.spotinst.SpotInstHelperServiceDelegate;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import io.harness.spotinst.model.ElastiGroupInstanceHealth;
import io.harness.spotinst.model.ElastiGroupRenameRequest;

import software.wings.beans.AwsConfig;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class ElastigroupDeployTaskHelper {
  @Inject private SpotInstHelperServiceDelegate spotInstHelperServiceDelegate;
  @Inject private AwsElbHelperServiceDelegate awsElbHelperServiceDelegate;
  @Inject private TimeLimiter timeLimiter;

  public void scaleElastigroup(ElastiGroup elastigroup, String spotInstToken, String spotInstAccountId,
      int steadyStateTimeOut, ILogStreamingTaskClient logStreamingTaskClient, String scaleCommandUnit,
      String waitCommandUnit, CommandUnitsProgress commandUnitsProgress) throws Exception {
    final LogCallback scaleLogCallback = getLogCallback(logStreamingTaskClient, scaleCommandUnit, commandUnitsProgress);
    final LogCallback waitLogCallback = getLogCallback(logStreamingTaskClient, waitCommandUnit, commandUnitsProgress);

    if (elastigroup == null || isEmpty(elastigroup.getId())) {
      scaleLogCallback.saveExecutionLog(
          "No Elastigroup eligible for scaling", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      waitLogCallback.saveExecutionLog(
          "No Elastigroup eligible for scaling", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      return;
    }

    updateElastigroup(spotInstToken, spotInstAccountId, elastigroup, scaleLogCallback);
    waitForSteadyState(elastigroup, spotInstAccountId, spotInstToken, steadyStateTimeOut, waitLogCallback);
  }

  public List<String> getAllEc2InstanceIdsOfElastigroup(
      String spotInstToken, String spotInstAccountId, ElastiGroup elastigroup) throws Exception {
    if (elastigroup == null || isEmpty(elastigroup.getId())) {
      return emptyList();
    }

    final List<ElastiGroupInstanceHealth> elastigroupInstanceHealths =
        spotInstHelperServiceDelegate.listElastiGroupInstancesHealth(
            spotInstToken, spotInstAccountId, elastigroup.getId());

    if (isEmpty(elastigroupInstanceHealths)) {
      return emptyList();
    }

    return elastigroupInstanceHealths.stream().map(ElastiGroupInstanceHealth::getInstanceId).collect(toList());
  }

  private LogCallback getLogCallback(ILogStreamingTaskClient logStreamingTaskClient, String commandUnitName,
      CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(logStreamingTaskClient, commandUnitName, true, commandUnitsProgress);
  }

  private void updateElastigroup(String spotInstToken, String spotInstAccountId, ElastiGroup elastiGroup,
      LogCallback logCallback) throws Exception {
    Optional<ElastiGroup> elastigroupInitialOptional =
        spotInstHelperServiceDelegate.getElastiGroupById(spotInstToken, spotInstAccountId, elastiGroup.getId());

    if (!elastigroupInitialOptional.isPresent()) {
      String message = format("Did not find Elastigroup: [%s], Id: [%s]", elastiGroup.getName(), elastiGroup.getId());
      log.error(message);
      logCallback.saveExecutionLog(message, ERROR, CommandExecutionStatus.FAILURE);
      throw new InvalidRequestException(message);
    }

    ElastiGroup elastigroupInitial = elastigroupInitialOptional.get();
    logCallback.saveExecutionLog(
        format("Current state of Elastigroup: [%s], Id: [%s], min: [%d], max: [%d], desired: [%d]",
            elastigroupInitial.getName(), elastigroupInitial.getId(), elastigroupInitial.getCapacity().getMinimum(),
            elastigroupInitial.getCapacity().getMaximum(), elastigroupInitial.getCapacity().getTarget()));

    checkAndUpdateElastigroup(elastiGroup, logCallback);

    logCallback.saveExecutionLog(format(
        "Sending request to Spotinst to update Elastigroup: [%s], Id: [%s] with min: [%d], max: [%d] and target: [%d]",
        elastiGroup.getName(), elastiGroup.getId(), elastiGroup.getCapacity().getMinimum(),
        elastiGroup.getCapacity().getMaximum(), elastiGroup.getCapacity().getTarget()));

    spotInstHelperServiceDelegate.updateElastiGroupCapacity(
        spotInstToken, spotInstAccountId, elastiGroup.getId(), elastiGroup);

    logCallback.saveExecutionLog("Request Sent to update Elastigroup", INFO, SUCCESS);
  }

  private void waitForSteadyState(ElastiGroup elastiGroup, String spotInstAccountId, String spotInstToken,
      int steadyStateTimeOut, LogCallback lLogCallback) {
    lLogCallback.saveExecutionLog(format(
        "Waiting for Elastigroup: [%s], Id: [%s] to reach steady state", elastiGroup.getName(), elastiGroup.getId()));
    try {
      HTimeLimiter.callInterruptible(timeLimiter, Duration.ofMinutes(steadyStateTimeOut), () -> {
        while (true) {
          if (allInstancesHealthy(
                  spotInstToken, spotInstAccountId, elastiGroup, lLogCallback, elastiGroup.getCapacity().getTarget())) {
            return true;
          }
          sleep(ofSeconds(20));
        }
      });
    } catch (ExecutionException | UncheckedExecutionException | ExecutionError e) {
      String errorMessage =
          format("Exception while waiting for steady state for Elastigroup: [%s], Id: [%s]. Error message: [%s]",
              elastiGroup.getName(), elastiGroup.getId(), e.getMessage());
      lLogCallback.saveExecutionLog(errorMessage, ERROR);
      throw new InvalidRequestException(errorMessage, e.getCause());
    } catch (TimeoutException | InterruptedException e) {
      String errorMessage = format("Timed out while waiting for steady state for Elastigroup: [%s], Id: [%s]",
          elastiGroup.getName(), elastiGroup.getId());
      lLogCallback.saveExecutionLog(errorMessage, ERROR);
      throw new InvalidRequestException(errorMessage, e);
    } catch (Exception e) {
      String errorMessage =
          format("Exception while waiting for steady state for Elastigroup: [%s], Id: [%s]. Error message: [%s]",
              elastiGroup.getName(), elastiGroup.getId(), e.getMessage());
      lLogCallback.saveExecutionLog(errorMessage, ERROR);
      throw new InvalidRequestException(errorMessage, e);
    }
  }

  private boolean allInstancesHealthy(String spotInstToken, String spotInstAccountId, ElastiGroup elastigroup,
      LogCallback logCallback, int targetInstances) throws Exception {
    List<ElastiGroupInstanceHealth> instanceHealths = spotInstHelperServiceDelegate.listElastiGroupInstancesHealth(
        spotInstToken, spotInstAccountId, elastigroup.getId());
    int currentTotalCount = isEmpty(instanceHealths) ? 0 : instanceHealths.size();
    int currentHealthyCount = isEmpty(instanceHealths)
        ? 0
        : (int) instanceHealths.stream().filter(health -> "HEALTHY".equals(health.getHealthStatus())).count();
    if (targetInstances == 0) {
      if (currentTotalCount == 0) {
        logCallback.saveExecutionLog(format("Elastigroup: [%s], Id: [%s] does not have any instances.",
                                         elastigroup.getName(), elastigroup.getId()),
            INFO, SUCCESS);
        return true;
      } else {
        logCallback.saveExecutionLog(
            format("Elastigroup: [%s], Id: [%s] still has [%d] total and [%d] healthy instances", elastigroup.getName(),
                elastigroup.getId(), currentTotalCount, currentHealthyCount));
      }
    } else {
      logCallback.saveExecutionLog(format(
          "Desired instances: [%d], Total instances: [%d], Healthy instances: [%d] for Elastigroup: [%s], Id: [%s]",
          targetInstances, currentTotalCount, currentHealthyCount, elastigroup.getName(), elastigroup.getId()));
      if (targetInstances == currentHealthyCount && targetInstances == currentTotalCount) {
        logCallback.saveExecutionLog(
            format("Elastigroup: [%s], Id: [%s] reached steady state", elastigroup.getName(), elastigroup.getId()),
            INFO, SUCCESS);
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if condition 0 <= min <= target <= max is followed. If it fails: if target < 0, we
   * update with default values else update min and/or max to target individually.
   */
  private void checkAndUpdateElastigroup(ElastiGroup elastigroup, LogCallback logCallback) {
    ElastiGroupCapacity capacity = elastigroup.getCapacity();
    if (!(0 <= capacity.getMinimum() && capacity.getMinimum() <= capacity.getTarget()
            && capacity.getTarget() <= capacity.getMaximum())) {
      int min = capacity.getMinimum();
      int target = capacity.getTarget();
      int max = capacity.getMaximum();
      if (target < 0) {
        capacity.setMinimum(DEFAULT_ELASTIGROUP_MIN_INSTANCES);
        capacity.setTarget(DEFAULT_ELASTIGROUP_TARGET_INSTANCES);
        capacity.setMaximum(DEFAULT_ELASTIGROUP_MAX_INSTANCES);
      } else {
        if (min > target) {
          capacity.setMinimum(target);
        }
        if (max < target) {
          capacity.setMaximum(target);
        }
      }
      logCallback.saveExecutionLog(format("Modifying invalid request to Spotinst to update Elastigroup: [%s], Id: [%s] "
              + "Original min: [%d], max: [%d] and target: [%d], Modified min: [%d], max: [%d] and target: [%d] ",
          elastigroup.getName(), elastigroup.getId(), min, max, target, capacity.getMinimum(), capacity.getMaximum(),
          capacity.getTarget()));
    }
  }

  public void deleteElastigroup(ElastiGroup elastigroup, String spotInstToken, String spotInstAccountId,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    final LogCallback logCallback =
        getLogCallback(logStreamingTaskClient, DELETE_NEW_ELASTI_GROUP, commandUnitsProgress);

    if (elastigroup == null || isEmpty(elastigroup.getId())) {
      logCallback.saveExecutionLog("No Elastigroup eligible for deletion.", INFO, SUCCESS);
      return;
    }

    logCallback.saveExecutionLog(
        format("Sending request to Spotinst to delete newly created Elastigroup: [%s], Id: [%s]", elastigroup.getName(),
            elastigroup.getId()));
    spotInstHelperServiceDelegate.deleteElastiGroup(spotInstToken, spotInstAccountId, elastigroup.getId());
    logCallback.saveExecutionLog(
        format("Elastigroup: [%s], Id: [%s] deleted successfully", elastigroup.getName(), elastigroup.getId()), INFO,
        SUCCESS);
  }

  public void renameElastigroup(ElastiGroup elastigroup, String newName, String spotInstAccountId, String spotInstToken,
      ILogStreamingTaskClient logStreamingTaskClient, String commandUnit, CommandUnitsProgress commandUnitsProgress)
      throws Exception {
    final LogCallback logCallback = getLogCallback(logStreamingTaskClient, commandUnit, commandUnitsProgress);

    if (elastigroup == null || isEmpty(elastigroup.getId())) {
      logCallback.saveExecutionLog("No Elastigroup found for renaming", INFO, SUCCESS);
      return;
    }

    logCallback.saveExecutionLog(format(
        "Renaming old Elastigroup: [%s], Id: [%s] to name: [%s]", elastigroup.getId(), elastigroup.getName(), newName));
    spotInstHelperServiceDelegate.updateElastiGroup(spotInstToken, spotInstAccountId, elastigroup.getId(),
        ElastiGroupRenameRequest.builder().name(newName).build());
    logCallback.saveExecutionLog("Successfully renamed Elastigroup", INFO, SUCCESS);
  }

  public void restoreLoadBalancerRoutesIfNeeded(List<LoadBalancerDetailsForBGDeployment> loadBalancerDetails,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    final LogCallback logCallback =
        getLogCallback(logStreamingTaskClient, SWAP_ROUTES_COMMAND_UNIT, commandUnitsProgress);

    if (isEmpty(loadBalancerDetails)) {
      logCallback.saveExecutionLog("No Action Needed", INFO, SUCCESS);
      return;
    }

    for (LoadBalancerDetailsForBGDeployment lbDetail : loadBalancerDetails) {
      if (lbDetail.isUseSpecificRules()) {
        restoreSpecificRulesRoutesIfChanged(lbDetail, logCallback, awsConfig, swapRoutesParameters.getAwsRegion());
      } else {
        restoreDefaultRulesRoutesIfChanged(lbDetail, logCallback, awsConfig, swapRoutesParameters);
      }
    }

    logCallback.saveExecutionLog("Prod Elastigroup is UP with correct traffic", INFO, SUCCESS);
  }

  private void restoreDefaultRulesRoutesIfChanged(LoadBalancerDetailsForBGDeployment lbDetail, LogCallback logCallback,
      AwsConfig awsConfig, SpotInstSwapRoutesTaskParameters swapRoutesParameters) {
    DescribeListenersResult result = awsElbHelperServiceDelegate.describeListenerResult(
        awsConfig, emptyList(), lbDetail.getProdListenerArn(), swapRoutesParameters.getAwsRegion());
    Optional<Action> optionalAction =
        result.getListeners()
            .get(0)
            .getDefaultActions()
            .stream()
            .filter(action -> "forward".equalsIgnoreCase(action.getType()) && isNotEmpty(action.getTargetGroupArn()))
            .findFirst();

    if (optionalAction.isPresent()
        && optionalAction.get().getTargetGroupArn().equals(lbDetail.getStageTargetGroupArn())) {
      logCallback.saveExecutionLog(format("Listener: [%s] is forwarding traffic to: [%s]. Swap routes in rollback",
          lbDetail.getProdListenerArn(), lbDetail.getStageTargetGroupArn()));
      awsElbHelperServiceDelegate.updateDefaultListenersForSpotInstBG(awsConfig, emptyList(),
          lbDetail.getProdListenerArn(), lbDetail.getStageListenerArn(), swapRoutesParameters.getAwsRegion());
    }
  }
}
