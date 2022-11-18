/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.elastigroup;

import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.spotconnector.SpotPermanentTokenConfigSpecDTO;
import io.harness.delegate.beans.elastigroup.ElastigroupSetupResult;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.exception.ElastigroupNGException;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.elastigroup.ElastigroupCommandTaskNGHelper;
import io.harness.delegate.task.elastigroup.request.ElastigroupCommandRequest;
import io.harness.delegate.task.elastigroup.request.ElastigroupSetupCommandRequest;
import io.harness.delegate.task.elastigroup.request.ElastigroupSwapRouteCommandRequest;
import io.harness.delegate.task.elastigroup.response.ElastigroupCommandResponse;
import io.harness.delegate.task.elastigroup.response.ElastigroupSetupResponse;
import io.harness.delegate.task.elastigroup.response.SpotInstConfig;
import io.harness.elastigroup.ElastigroupCommandUnitConstants;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.spotinst.SpotInstHelperServiceDelegate;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import io.harness.spotinst.model.ElastiGroupRenameRequest;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;

import java.util.List;
import java.util.Optional;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.STAGE_ELASTI_GROUP_NAME_SUFFIX;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static software.wings.beans.LogHelper.color;

@OwnedBy(HarnessTeam.CDP)
@NoArgsConstructor
@Slf4j
public class ElastigroupSwapRouteCommandTaskHandler extends ElastigroupCommandTaskNGHandler {
  @Inject private ElastigroupCommandTaskNGHelper elastigroupCommandTaskNGHelper;
  @Inject protected SpotInstHelperServiceDelegate spotInstHelperServiceDelegate;
  private long timeoutInMillis;

  @Override
  protected ElastigroupCommandResponse executeTaskInternal(ElastigroupCommandRequest elastigroupCommandRequest,
      ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(elastigroupCommandRequest instanceof ElastigroupSwapRouteCommandRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("elastigroupCommandRequest", "Must be instance of ElastigroupSwapRouteCommandRequest"));
    }
    ElastigroupSwapRouteCommandRequest elastigroupSwapRouteCommandRequest =
        (ElastigroupSwapRouteCommandRequest) elastigroupCommandRequest;

    timeoutInMillis = elastigroupSwapRouteCommandRequest.getTimeoutIntervalInMin() * 60000;

    LogCallback deployLogCallback = elastigroupCommandTaskNGHelper.getLogCallback(
        iLogStreamingTaskClient, ElastigroupCommandUnitConstants.createSetup.toString(), true, commandUnitsProgress);
    try {
      elastigroupCommandTaskNGHelper.decryptAwsCredentialDTO(
              elastigroupSwapRouteCommandRequest.getConnectorInfoDTO().getConnectorConfig(),
          ((ElastigroupSetupCommandRequest) elastigroupCommandRequest).getAwsEncryptedDetails());
      AwsInternalConfig awsInternalConfig = elastigroupCommandTaskNGHelper.getAwsInternalConfig(
          (AwsConnectorDTO) elastigroupSwapRouteCommandRequest.getConnectorInfoDTO().getConnectorConfig(),
          ((ElastigroupSetupCommandRequest) elastigroupCommandRequest).getAwsRegion());


      SpotInstConfig spotInstConfig = elastigroupSwapRouteCommandRequest.getSpotInstConfig();
      elastigroupCommandTaskNGHelper.decryptSpotInstConfig(spotInstConfig);
      SpotPermanentTokenConfigSpecDTO spotPermanentTokenConfigSpecDTO =
              (SpotPermanentTokenConfigSpecDTO) spotInstConfig.getSpotConnectorDTO().getCredential().getConfig();
      String spotInstAccountId = spotPermanentTokenConfigSpecDTO.getSpotAccountIdRef().getDecryptedValue() != null
              ? spotPermanentTokenConfigSpecDTO.getSpotAccountIdRef().getDecryptedValue().toString()
              : spotPermanentTokenConfigSpecDTO.getSpotAccountId();
      String spotInstApiTokenRef = new String(spotPermanentTokenConfigSpecDTO.getApiTokenRef().getDecryptedValue());


      String prodElastiGroupName = elastigroupSwapRouteCommandRequest.getElastigroupNamePrefix();
      String stageElastiGroupName =
              format("%s__%s", elastigroupSwapRouteCommandRequest.getElastigroupNamePrefix(), STAGE_ELASTI_GROUP_NAME_SUFFIX);
      ElastiGroup newElastiGroup = elastigroupSwapRouteCommandRequest.getNewElastigroup();
      String newElastiGroupId = (newElastiGroup != null) ? newElastiGroup.getId() : EMPTY;
      ElastiGroup oldElastiGroup = elastigroupSwapRouteCommandRequest.getOldElastigroup();
      String oldElastiGroupId = (oldElastiGroup != null) ? oldElastiGroup.getId() : EMPTY;

      if (isNotEmpty(newElastiGroupId)) {
        deployLogCallback.saveExecutionLog(
                format("Sending request to rename Elastigroup with Id: [%s] to [%s]", newElastiGroupId, prodElastiGroupName));
        spotInstHelperServiceDelegate.updateElastiGroup(spotInstApiTokenRef, spotInstAccountId, newElastiGroupId,
                ElastiGroupRenameRequest.builder().name(prodElastiGroupName).build());
      }

      if (isNotEmpty(oldElastiGroupId)) {
        deployLogCallback.saveExecutionLog(
                format("Sending request to rename Elastigroup with Id: [%s] to [%s]", oldElastiGroup, stageElastiGroupName));
        spotInstHelperServiceDelegate.updateElastiGroup(spotInstApiTokenRef, spotInstAccountId, oldElastiGroupId,
                ElastiGroupRenameRequest.builder().name(stageElastiGroupName).build());
      }

      String region = elastigroupSwapRouteCommandRequest.getAwsRegion();

      deployLogCallback.saveExecutionLog("Updating Listener Rules for Load Balancer");
      elastigroupSwapRouteCommandRequest.getLBdetailsForBGDeploymentList().forEach(lbDetailsForBGDeployment -> {
        elastigroupCommandTaskNGHelper.swapTargetGroups(region, deployLogCallback, lbDetailsForBGDeployment, awsInternalConfig);
      });
      deployLogCallback.saveExecutionLog("Route Updated Successfully", INFO, SUCCESS);



      if (elastigroupSwapRouteCommandRequest.getDownsizeOldElastigroup() && isNotEmpty(elastigroupSwapRouteCommandRequest.getOldElastigroup().getId())) {
        ElastiGroup temp = ElastiGroup.builder()
                .id(oldElastiGroupId)
                .name(stageElastiGroupName)
                .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(0).target(0).build())
                .build();
        int steadyStateTimeOut = getTimeOut(swapRoutesParameters.getSteadyStateTimeOut());
        updateElastiGroupAndWait(spotInstToken, spotInstAccountId, temp, steadyStateTimeOut, swapRoutesParameters,
                DOWN_SCALE_COMMAND_UNIT, DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
      } else {
        deployLogCallback = elastigroupCommandTaskNGHelper.getLogCallback(iLogStreamingTaskClient, DOWN_SCALE_COMMAND_UNIT, true, commandUnitsProgress);
        deployLogCallback.saveExecutionLog("Nothing to Downsize.", INFO, SUCCESS);
        deployLogCallback = elastigroupCommandTaskNGHelper.getLogCallback(iLogStreamingTaskClient, DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT, true, commandUnitsProgress);
        deployLogCallback.saveExecutionLog("No Downsize was required, Swap Route Successfully Completed", INFO, SUCCESS);
      }

      //---------------------


      List<LoadBalancerDetailsForBGDeployment> lbDetailList =
          elastigroupCommandTaskNGHelper.fetchAllLoadBalancerDetails(
                  elastigroupSwapRouteCommandRequest, awsInternalConfig, deployLogCallback);

      // Generate STAGE elastiGroup name
      String stageElastiGroupName =
          format("%s__%s", elastigroupSwapRouteCommandRequest.getElastigroupNamePrefix(), STAGE_ELASTI_GROUP_NAME_SUFFIX);

      // Update lbDetails with fetched details, as they have more data field in
      elastigroupSwapRouteCommandRequest.setAwsLoadBalancerConfigs(lbDetailList);

      // Generate final json by substituting name, capacity and LBConfig
      String finalJson =
          elastigroupCommandTaskNGHelper.generateFinalJson(elastigroupSwapRouteCommandRequest, stageElastiGroupName);

      // Check if existing elastigroup with exists with same stage name
      deployLogCallback.saveExecutionLog(format("Querying to find Elastigroup with name: [%s]", stageElastiGroupName));
      Optional<ElastiGroup> stageOptionalElastiGroup = spotInstHelperServiceDelegate.getElastiGroupByName(
          spotInstApiTokenRef, spotInstAccountId, stageElastiGroupName);
      ElastiGroup stageElastiGroup;
      if (stageOptionalElastiGroup.isPresent()) {
        stageElastiGroup = stageOptionalElastiGroup.get();
        deployLogCallback.saveExecutionLog(
            format("Found stage Elastigroup with id: [%s]. Deleting it. ", stageElastiGroup.getId()));
        spotInstHelperServiceDelegate.deleteElastiGroup(
            spotInstApiTokenRef, spotInstAccountId, stageElastiGroup.getId());
      }

      // Create new elastiGroup
      deployLogCallback.saveExecutionLog(
          format("Sending request to create new Elastigroup with name: [%s]", stageElastiGroupName));
      stageElastiGroup =
          spotInstHelperServiceDelegate.createElastiGroup(spotInstApiTokenRef, spotInstAccountId, finalJson);
      String stageElastiGroupId = stageElastiGroup.getId();
      deployLogCallback.saveExecutionLog(
          format("Created Elastigroup with name: [%s] and id: [%s]", stageElastiGroupName, stageElastiGroupId));

      // Prod ELasti Groups
      String prodElastiGroupName = elastigroupSwapRouteCommandRequest.getElastigroupNamePrefix();
      deployLogCallback.saveExecutionLog(
          format("Querying Spotinst for Elastigroup with name: [%s]", prodElastiGroupName));
      Optional<ElastiGroup> prodOptionalElastiGroup = spotInstHelperServiceDelegate.getElastiGroupByName(
          spotInstApiTokenRef, spotInstAccountId, prodElastiGroupName);
      List<ElastiGroup> prodElastiGroupList;
      if (prodOptionalElastiGroup.isPresent()) {
        ElastiGroup prodElastiGroup = prodOptionalElastiGroup.get();
        deployLogCallback.saveExecutionLog(format("Found existing Prod Elastigroup with name: [%s] and id: [%s]",
            prodElastiGroup.getName(), prodElastiGroup.getId()));
        prodElastiGroupList = singletonList(prodElastiGroup);
      } else {
        prodElastiGroupList = emptyList();
      }

      deployLogCallback.saveExecutionLog(
          color(format("Completed Blue green setup for Spotinst"), LogColor.Green, LogWeight.Bold), LogLevel.INFO,
          CommandExecutionStatus.SUCCESS);

      ElastigroupSetupResult elastigroupSetupResult =
          ElastigroupSetupResult.builder()
              .elastiGroupNamePrefix(elastigroupSwapRouteCommandRequest.getElastigroupNamePrefix())
              .newElastiGroup(stageElastiGroup)
              .elastigroupOriginalConfig(elastigroupSwapRouteCommandRequest.getElastigroupOriginalConfig())
              .groupToBeDownsized(prodElastiGroupList)
              .elastiGroupNamePrefix(elastigroupSwapRouteCommandRequest.getElastigroupNamePrefix())
              .isBlueGreen(elastigroupSwapRouteCommandRequest.isBlueGreen())
              .useCurrentRunningInstanceCount(
                  ((ElastigroupSetupCommandRequest) elastigroupCommandRequest).isUseCurrentRunningInstanceCount())
              .currentRunningInstanceCount(elastigroupSetupCommandRequest.getCurrentRunningInstanceCount())
              .maxInstanceCount(elastigroupSetupCommandRequest.getMaxInstanceCount())
              .resizeStrategy(elastigroupSetupCommandRequest.getResizeStrategy())
              .loadBalancerDetailsForBGDeployments(lbDetailList)
              .build();

      return ElastigroupSetupResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .elastigroupSetupResult(elastigroupSetupResult)
          .build();

    } catch (Exception ex) {
      Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(ex);
      deployLogCallback.saveExecutionLog(color(format("Blue Green Stage Setup Failed."), LogColor.Red, LogWeight.Bold),
          LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw new ElastigroupNGException(sanitizedException);
    }
  }
}
