package software.wings.service.impl.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeCount;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.common.VerificationConstants.LOGS_HIGH_RISK_THRESHOLD;
import static software.wings.common.VerificationConstants.LOGS_MEDIUM_RISK_THRESHOLD;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisServiceImpl.CLUSTER_TYPE;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLClusterSummary;
import software.wings.service.impl.splunk.LogMLClusterScores;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.verification.CV24x7DashboardService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration;
import software.wings.verification.HeatMap;
import software.wings.verification.HeatMapResolution;
import software.wings.verification.dashboard.HeatMapUnit;
import software.wings.verification.log.LogsCVConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class CV24x7DashboardServiceImpl implements CV24x7DashboardService {
  private static final Logger logger = LoggerFactory.getLogger(CV24x7DashboardServiceImpl.class);

  @Inject WingsPersistence wingsPersistence;
  @Inject CVConfigurationService cvConfigurationService;
  @Inject AnalysisService analysisService;

  @Override
  public List<HeatMap> getHeatMapForLogs(
      String accountId, String appId, String serviceId, long startTime, long endTime, boolean detailed) {
    List<HeatMap> rv = Collections.synchronizedList(new ArrayList<>());
    List<CVConfiguration> cvConfigurations = getCVConfigurations(appId, serviceId);
    if (isEmpty(cvConfigurations)) {
      logger.info("No cv config found for appId={}, serviceId={}", appId, serviceId);
      return new ArrayList<>();
    }

    cvConfigurations.parallelStream().forEach(cvConfig -> {
      if (VerificationConstants.getLogAnalysisStates().contains(cvConfig.getStateType())) {
        cvConfigurationService.fillInServiceAndConnectorNames(cvConfig);
        String envName = cvConfig.getEnvName();
        logger.info("Environment name {}", envName);
        final HeatMap heatMap = HeatMap.builder().cvConfiguration(cvConfig).build();
        rv.add(heatMap);

        List<HeatMapUnit> units = createAllHeatMapUnits(appId, startTime, endTime, cvConfig);
        List<HeatMapUnit> resolvedUnits = resolveHeatMapUnits(units, startTime, endTime);
        heatMap.getRiskLevelSummary().addAll(resolvedUnits);
      }
    });

    return rv;
  }

  private List<CVConfiguration> getCVConfigurations(String appId, String serviceId) {
    List<CVConfiguration> cvConfigurations = wingsPersistence.createQuery(CVConfiguration.class)
                                                 .filter("appId", appId)
                                                 .filter("serviceId", serviceId)
                                                 .asList();
    if (isEmpty(cvConfigurations)) {
      logger.info("No cv config found for appId={}, serviceId={}", appId, serviceId);
      return new ArrayList<>();
    }
    return cvConfigurations;
  }

  private List<HeatMapUnit> createAllHeatMapUnits(
      String appId, long startTime, long endTime, CVConfiguration cvConfiguration) {
    long cronPollIntervalMs = TimeUnit.MINUTES.toMillis(CRON_POLL_INTERVAL_IN_MINUTES);
    Preconditions.checkState((endTime - startTime) >= cronPollIntervalMs);
    List<LogMLAnalysisRecord> records =
        getLogAnalysisRecordsInTimeRange(appId, startTime, endTime, false, cvConfiguration);

    long startMinute = TimeUnit.MILLISECONDS.toMinutes(startTime);
    long endMinute = TimeUnit.MILLISECONDS.toMinutes(endTime);

    List<HeatMapUnit> units = new ArrayList<>();
    if (isEmpty(records)) {
      while (endMinute > startMinute) {
        units.add(HeatMapUnit.builder()
                      .startTime(TimeUnit.MINUTES.toMillis(startMinute))
                      .endTime(TimeUnit.MINUTES.toMillis(startMinute + CRON_POLL_INTERVAL_IN_MINUTES))
                      .na(1)
                      .build());
        startMinute += CRON_POLL_INTERVAL_IN_MINUTES;
      }

      return units;
    }

    SortedSet<HeatMapUnit> sortedUnitsFromDB = new TreeSet<>();
    records.forEach(record -> {
      HeatMapUnit heatMapUnit =
          HeatMapUnit.builder()
              .startTime(TimeUnit.MINUTES.toMillis(record.getLogCollectionMinute() - CRON_POLL_INTERVAL_IN_MINUTES) + 1)
              .endTime(TimeUnit.MINUTES.toMillis(record.getLogCollectionMinute()))
              .overallScore(-1)
              .build();

      heatMapUnit.updateOverallScore(record.getScore());
      sortedUnitsFromDB.add(heatMapUnit);
    });

    // find the actual start time so that we fill from there
    HeatMapUnit heatMapUnit = sortedUnitsFromDB.first();
    long actualUnitStartTime = heatMapUnit.getStartTime();
    while (startTime < actualUnitStartTime - cronPollIntervalMs) {
      actualUnitStartTime -= cronPollIntervalMs;
    }

    int dbUnitIndex = 0;
    List<HeatMapUnit> unitsFromDB = new ArrayList<>(sortedUnitsFromDB);
    for (long unitTime = actualUnitStartTime; unitTime <= endTime; unitTime += cronPollIntervalMs) {
      heatMapUnit = dbUnitIndex < unitsFromDB.size() ? unitsFromDB.get(dbUnitIndex) : null;
      if (heatMapUnit != null) {
        long timeDifference = TimeUnit.MILLISECONDS.toSeconds(abs(heatMapUnit.getStartTime() - unitTime));
        if (timeDifference != 0 && timeDifference < 60) {
          logger.error(
              "Unexpected state: timeDifference = {}, should have been 0 or > 60, heatmap unit start time = {}",
              timeDifference, heatMapUnit.getStartTime());
        }
      }

      if (heatMapUnit != null && unitTime == heatMapUnit.getStartTime()) {
        units.add(heatMapUnit);
        dbUnitIndex++;
        continue;
      }

      units.add(HeatMapUnit.builder().endTime(unitTime - 1).startTime(unitTime - cronPollIntervalMs).na(1).build());
    }
    return units;
  }

  private List<HeatMapUnit> resolveHeatMapUnits(List<HeatMapUnit> units, long startTime, long endTime) {
    List<HeatMapUnit> resolvedUnits = new ArrayList<>();
    HeatMapResolution heatMapResolution = HeatMapResolution.getResolution(startTime, endTime);

    // time duration represented by each read unit
    int unitDuration = heatMapResolution.getDurationOfHeatMapUnit(heatMapResolution);

    // number of small units to be merged into one reqd unit
    int eventsPerUnit = heatMapResolution.getEventsPerHeatMapUnit(heatMapResolution);

    // total number of read units
    int numberOfUnits = (int) ceil((double) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime) / unitDuration);

    logger.info("total small units = {}, number of required units = {}", units.size(), numberOfUnits);

    for (int i = 0; i < numberOfUnits; i++) {
      // merge [i * eventsPerUnit, (i + 1) * eventsPerUnit)
      // [x, y) denotes x inclusive, y exclusive
      // Note: This works because the smallest unit is composed of exactly 1 event
      int startIndex = i * eventsPerUnit;
      int endIndex = min((i + 1) * eventsPerUnit, units.size());

      if (startIndex >= endIndex) {
        continue;
      }
      List<HeatMapUnit> subList = units.subList(startIndex, endIndex);
      if (subList.size() > 0) {
        resolvedUnits.add(merge(subList));
      }
    }
    return resolvedUnits;
  }

  private HeatMapUnit merge(List<HeatMapUnit> units) {
    HeatMapUnit mergedUnit = HeatMapUnit.builder()
                                 .startTime(units.get(0).getStartTime())
                                 .endTime(units.get(units.size() - 1).getEndTime())
                                 .overallScore(-1)
                                 .build();
    units.forEach(unit -> {
      if (unit.getScoreList() != null) {
        mergedUnit.updateOverallScore(unit.getOverallScore());
      }
    });

    return mergedUnit;
  }

  private List<LogMLAnalysisRecord> getLogAnalysisRecordsInTimeRange(
      String appId, long startTime, long endTime, boolean readDetails, CVConfiguration cvConfiguration) {
    return wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeCount)
        .filter("appId", appId)
        .filter("cvConfigId", cvConfiguration.getUuid())
        .field("logCollectionMinute")
        .greaterThanOrEq(TimeUnit.MILLISECONDS.toMinutes(startTime))
        .field("logCollectionMinute")
        .lessThanOrEq(TimeUnit.MILLISECONDS.toMinutes(endTime))
        .project("analysisDetailsCompressedJson", readDetails)
        .asList();
  }

  public LogMLAnalysisSummary getAnalysisSummary(String cvConfigId, long startTime, long endTime, String appId) {
    CVConfiguration cvConfiguration = cvConfigurationService.getConfiguration(cvConfigId);
    if (!VerificationConstants.getLogAnalysisStates().contains(cvConfiguration.getStateType())) {
      logger.error("Incorrect CVConfigID to fetch logAnalysisSummary {}", cvConfigId);
      return null;
    }
    List<LogMLAnalysisRecord> analysisRecords =
        getLogAnalysisRecordsInTimeRange(appId, startTime, endTime, true, cvConfiguration);

    if (analysisRecords == null) {
      return null;
    }

    analysisRecords.forEach(analysisRecord -> analysisRecord.decompressLogAnalysisRecord());

    // TODO: Incorporate user feedbacks into this.
    final LogMLAnalysisSummary analysisSummary = new LogMLAnalysisSummary();
    analysisSummary.initializeClusters();
    double totalScore = 0.0;
    int unknownFrequency = 0;
    for (LogMLAnalysisRecord record : analysisRecords) {
      analysisSummary.getControlClusters().addAll(
          analysisService.computeCluster(record.getControl_clusters(), Collections.emptyMap(), CLUSTER_TYPE.CONTROL));
      LogMLClusterScores logMLClusterScores =
          record.getCluster_scores() != null ? record.getCluster_scores() : new LogMLClusterScores();
      analysisSummary.getTestClusters().addAll(
          analysisService.computeCluster(record.getTest_clusters(), logMLClusterScores.getTest(), CLUSTER_TYPE.TEST));
      analysisSummary.getUnknownClusters().addAll(analysisService.computeCluster(
          record.getUnknown_clusters(), logMLClusterScores.getUnknown(), CLUSTER_TYPE.UNKNOWN));
      analysisSummary.getIgnoreClusters().addAll(
          analysisService.computeCluster(record.getIgnore_clusters(), Collections.emptyMap(), CLUSTER_TYPE.IGNORE));

      unknownFrequency += getUnexpectedFrequency(record.getTest_clusters());
      analysisSummary.setQuery(record.getQuery());
      totalScore += record.getScore();
    }

    analysisSummary.setScore(totalScore / analysisRecords.size() * 100);

    RiskLevel riskLevel = RiskLevel.NA;

    int unknownClusters = 0;
    int highRiskClusters = 0;
    int mediumRiskCluster = 0;
    int lowRiskClusters = 0;
    if (isNotEmpty(analysisSummary.getUnknownClusters())) {
      for (LogMLClusterSummary clusterSummary : analysisSummary.getUnknownClusters()) {
        if (clusterSummary.getScore() > LOGS_HIGH_RISK_THRESHOLD) {
          ++highRiskClusters;
        } else if (clusterSummary.getScore() > LOGS_MEDIUM_RISK_THRESHOLD) {
          ++mediumRiskCluster;
        } else if (clusterSummary.getScore() > 0) {
          ++lowRiskClusters;
        }
      }
      riskLevel = highRiskClusters > 0
          ? RiskLevel.HIGH
          : mediumRiskCluster > 0 ? RiskLevel.MEDIUM : lowRiskClusters > 0 ? RiskLevel.LOW : RiskLevel.HIGH;

      unknownClusters = analysisSummary.getUnknownClusters().size();
      analysisSummary.setHighRiskClusters(highRiskClusters);
      analysisSummary.setMediumRiskClusters(mediumRiskCluster);
      analysisSummary.setLowRiskClusters(lowRiskClusters);
    }

    if (unknownFrequency > 0) {
      analysisSummary.setHighRiskClusters(analysisSummary.getHighRiskClusters() + unknownFrequency);
      riskLevel = RiskLevel.HIGH;
    }

    String analysisSummaryMsg = "";
    if (highRiskClusters > 0 || mediumRiskCluster > 0 || lowRiskClusters > 0) {
      analysisSummaryMsg = analysisSummary.getHighRiskClusters() + " high risk, "
          + analysisSummary.getMediumRiskClusters() + " medium risk, " + analysisSummary.getLowRiskClusters()
          + " low risk anomalous cluster(s) found";
    } else if (unknownClusters > 0 || unknownFrequency > 0) {
      final int totalAnomalies = unknownClusters + unknownFrequency;
      analysisSummaryMsg = totalAnomalies == 1 ? totalAnomalies + " anomalous cluster found"
                                               : totalAnomalies + " anomalous clusters found";
    }

    analysisSummary.setRiskLevel(riskLevel);
    analysisSummary.setAnalysisSummaryMessage(analysisSummaryMsg);
    analysisSummary.setStateType(cvConfiguration.getStateType());
    analysisSummary.setQuery(((LogsCVConfiguration) cvConfiguration).getQuery());
    return analysisSummary;
  }

  private int getUnexpectedFrequency(Map<String, Map<String, SplunkAnalysisCluster>> testClusters) {
    int unexpectedFrequency = 0;
    if (isEmpty(testClusters)) {
      return unexpectedFrequency;
    }
    for (Entry<String, Map<String, SplunkAnalysisCluster>> labelEntry : testClusters.entrySet()) {
      for (Entry<String, SplunkAnalysisCluster> hostEntry : labelEntry.getValue().entrySet()) {
        final SplunkAnalysisCluster analysisCluster = hostEntry.getValue();
        if (analysisCluster.isUnexpected_freq()) {
          unexpectedFrequency++;
          break;
        }
      }
    }

    return unexpectedFrequency;
  }
}
