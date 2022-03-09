/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.cloudevents.aws.ecs.service.CEClusterDao;
import io.harness.batch.processing.cloudevents.aws.ecs.service.util.ClusterIdAndServiceArn;
import io.harness.batch.processing.cloudevents.aws.ecs.service.util.UtilizationDataWithTime;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.dao.recommendation.ECSRecommendationDAO;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSPartialRecommendationHistogram;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSServiceRecommendation;
import io.harness.histogram.Histogram;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.histogram.HistogramImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.*;

@Slf4j
@Singleton
public class AwsECSServiceRecommendationTasklet implements Tasklet {
  @Autowired private CEClusterDao ceClusterDao;
  @Autowired private UtilizationDataServiceImpl utilizationDataService;
  @Autowired private ECSRecommendationDAO ecsRecommendationDAO;

  private static final int BATCH_SIZE = 20;
  private static final int AVG_UTILIZATION_WEIGHT = 2;
  private static final int RECOMMENDATION_FOR_DAYS = 7;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    final JobConstants jobConstants = CCMJobConstants.fromContext(chunkContext);
    String accountId = jobConstants.getAccountId();
    Instant startTime = Instant.ofEpochMilli(jobConstants.getJobStartTime());
    Instant endTime = Instant.ofEpochMilli(jobConstants.getJobEndTime());
    // 1. Get all clusters for current account
    Map<String, String> ceClusters = ceClusterDao.getClusterIdNameMapping(accountId);
    if (CollectionUtils.isEmpty(ceClusters)) {
      return null;
    }
    List<String> clusterIds = new ArrayList<>(ceClusters.keySet());

    for (List<String> ceClustersPartition: Lists.partition(clusterIds, BATCH_SIZE)) {
      // 2. Get utilization data for all clusters in this batch for a day
      Map<ClusterIdAndServiceArn, List<UtilizationDataWithTime>> utilMap =
          utilizationDataService.getUtilizationDataForECSClusters(accountId, ceClustersPartition,
              startTime.toString(), endTime.toString());

      for (ClusterIdAndServiceArn clusterIdAndServiceArn : utilMap.keySet()) {
        // get service details cpuUnits and memoryMb
        int cpuUnits = 2048;
        int memoryMb = 7764;
        List<UtilizationDataWithTime> utilData = utilMap.get(clusterIdAndServiceArn);
        Instant today = Instant.now();
        String clusterId = clusterIdAndServiceArn.getClusterId();
        String clusterName = ceClusters.get(clusterId);
        String serviceArn = clusterIdAndServiceArn.getServiceArn();
        String serviceName = serviceNameFromServiceArn(serviceArn);
        // 3. Create Partial Histogram for a day for this service
        ECSPartialRecommendationHistogram partialRecommendationHistogram = ECSPartialRecommendationHistogram.builder()
            .accountId(accountId)
            .clusterId(clusterId)
            .clusterName(clusterName)
            .serviceArn(serviceArn)
            .serviceName(serviceName)
            .date(today)
            .lastUpdateTime(today)
            .cpuHistogram(cpuHistogramCheckpointFromUtilData(utilData, cpuUnits))
            .memoryHistogram(memoryHistogramCheckpointFromUtilData(utilData, memoryMb))
            .firstSampleStart(utilData.isEmpty() ? null : utilData.get(0).getStartTime())
            .lastSampleStart(utilData.isEmpty() ? null : utilData.get(utilData.size() - 1).getStartTime())
            .totalSamplesCount(utilData.size())
            .windowEnd(utilData.isEmpty() ? null : utilData.get(utilData.size()-1).getEndTime())
            .version(1)
            .build();
        ecsRecommendationDAO.savePartialRecommendation(partialRecommendationHistogram);

        // 4. Get Partial recommendations for last 6 days
        List<ECSPartialRecommendationHistogram> partialHistograms =
            ecsRecommendationDAO.fetchPartialRecommendationHistograms(accountId, clusterId, clusterName, serviceName,
                serviceArn, today.minus(Duration.ofDays(RECOMMENDATION_FOR_DAYS + 1)), today.minusSeconds(1));
        // add today's histogram to the list
        partialHistograms.add(partialRecommendationHistogram);

        ECSServiceRecommendation ecsServiceRecommendation = ecsRecommendationDAO.fetchServiceRecommendation(accountId,
            clusterId, clusterName, serviceName, serviceArn);
        Histogram cpuHistogram = newCpuHistogramV2();
        Histogram memoryHistogram = newMemoryHistogramV2();
        for (ECSPartialRecommendationHistogram partialHistogram: partialHistograms) {
          cpuHistogram.merge(loadFromCheckpointV2(partialHistogram.getCpuHistogram()));
          Histogram partialMemoryHistogram = newMemoryHistogramV2();
          partialMemoryHistogram.loadFromCheckPoint(partialHistogram.getMemoryHistogram());
          memoryHistogram.merge(partialMemoryHistogram);
        }
        ecsServiceRecommendation.setCpuHistogram(cpuHistogram.saveToCheckpoint());
        ecsServiceRecommendation.setMemoryHistogram(memoryHistogram.saveToCheckpoint());

        ecsRecommendationDAO.saveRecommendation(ecsServiceRecommendation);
      }
    }

    // generate recommendation
    // (done) 1. get partial recommendations for this cluster for the last 7 days
    // (done) 2. merge
    // 3. create percentile recommendations
    // (done) 4. save

    return null;
  }

  private HistogramCheckpoint cpuHistogramCheckpointFromUtilData
      (List<UtilizationDataWithTime> utilizationForDay, int cpuUnits) {
    Histogram histogram = cpuHistogramFromUtilData(utilizationForDay, cpuUnits);
    return histogram.saveToCheckpoint();
  }

  private HistogramCheckpoint memoryHistogramCheckpointFromUtilData
      (List<UtilizationDataWithTime> utilizationForDay, int memoryMb) {
    Histogram histogram = memoryHistogramFromUtilData(utilizationForDay, memoryMb);
    return histogram.saveToCheckpoint();
  }

  private Histogram cpuHistogramFromUtilData(List<UtilizationDataWithTime> utilizationForDay, int cpuUnits) {
    Histogram histogram = newCpuHistogramV2();
    // utilization data is in percentage
    for (UtilizationDataWithTime utilizationForHour: utilizationForDay) {
      histogram.addSample(utilizationForHour.getUtilizationData().getAvgCpuUtilization() * cpuUnits, 2,
          utilizationForHour.getStartTime());
      histogram.addSample(utilizationForHour.getUtilizationData().getMaxCpuUtilization() * cpuUnits, 1,
          utilizationForHour.getStartTime());
    }
    return histogram;
  }

  private Histogram memoryHistogramFromUtilData(List<UtilizationDataWithTime> utilizationForDay, int memoryMb) {
    Histogram histogram = newMemoryHistogramV2();
    // utilization data is in percentage
    for (UtilizationDataWithTime utilizationForHour: utilizationForDay) {
      histogram.addSample(utilizationForHour.getUtilizationData().getAvgMemoryUtilization() * memoryMb, 2,
          utilizationForHour.getStartTime());
      histogram.addSample(utilizationForHour.getUtilizationData().getAvgMemoryUtilization() * memoryMb, 1,
          utilizationForHour.getStartTime());
    }
    return histogram;
  }

  private String serviceNameFromServiceArn(String serviceArn) {
    return serviceArn.substring(serviceArn.lastIndexOf('/') + 1);
  }
}
