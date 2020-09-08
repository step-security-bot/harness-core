package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.ClusteredLog;
import io.harness.cvng.analysis.entities.ClusteredLog.ClusteredLogKeys;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskKeys;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LogClusterServiceImplTest extends CvNextGenTest {
  private String verificationTaskId;
  @Mock LearningEngineTaskService learningEngineTaskService;
  @Inject HPersistence hPersistence;
  @Inject LogClusterService logClusterService;
  @Inject CVConfigService cvConfigService;
  @Inject VerificationTaskService verificationTaskService;

  @Before
  public void setup() {
    CVConfig cvConfig = createCVConfig();
    cvConfigService.save(cvConfig);
    verificationTaskId =
        verificationTaskService.getServiceGuardVerificationTaskId(cvConfig.getAccountId(), cvConfig.getUuid());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testScheduleClusteringTasks_l1Cluster() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    createLogDataRecords(5, start, end);
    AnalysisInput input =
        AnalysisInput.builder().verificationTaskId(verificationTaskId).startTime(start).endTime(end).build();
    logClusterService.scheduleClusteringTasks(input, LogClusterLevel.L1);

    List<LearningEngineTask> tasks = hPersistence.createQuery(LearningEngineTask.class, excludeAuthority)
                                         .filter(LearningEngineTaskKeys.verificationTaskId, verificationTaskId)
                                         .asList();
    assertThat(tasks.size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testScheduleClusteringTasks_l2Cluster() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    List<ClusteredLog> logRecords = createClusteredLogRecords(5, start, end);
    hPersistence.save(logRecords);
    AnalysisInput input =
        AnalysisInput.builder().verificationTaskId(verificationTaskId).startTime(start).endTime(end).build();
    logClusterService.scheduleClusteringTasks(input, LogClusterLevel.L2);

    List<LearningEngineTask> tasks = hPersistence.createQuery(LearningEngineTask.class, excludeAuthority)
                                         .filter(LearningEngineTaskKeys.verificationTaskId, verificationTaskId)
                                         .asList();
    assertThat(tasks.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testScheduleClusteringTasks_l2ClusterNoL1Records() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    AnalysisInput input =
        AnalysisInput.builder().verificationTaskId(verificationTaskId).startTime(start).endTime(end).build();
    logClusterService.scheduleClusteringTasks(input, LogClusterLevel.L2);

    List<LearningEngineTask> tasks = hPersistence.createQuery(LearningEngineTask.class, excludeAuthority)
                                         .filter(LearningEngineTaskKeys.cvConfigId, verificationTaskId)
                                         .asList();
    assertThat(tasks.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetTaskStatus() throws Exception {
    FieldUtils.writeField(logClusterService, "learningEngineTaskService", learningEngineTaskService, true);
    Set<String> taskIds = new HashSet<>();
    taskIds.add("task1");
    taskIds.add("task2");
    logClusterService.getTaskStatus(taskIds);

    Mockito.verify(learningEngineTaskService).getTaskStatus(taskIds);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetDataForLogCluster_l1() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    createLogDataRecords(5, start, end);
    List<LogClusterDTO> recordsTobeClustered = logClusterService.getDataForLogCluster(
        verificationTaskId, start, start.plus(Duration.ofMinutes(1)), "host-0", LogClusterLevel.L1);

    assertThat(recordsTobeClustered).isNotNull();
    assertThat(recordsTobeClustered.size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetDataForLogCluster_l2() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    List<ClusteredLog> logRecords = createClusteredLogRecords(5, start, end);
    hPersistence.save(logRecords);
    List<LogClusterDTO> logClusters =
        logClusterService.getDataForLogCluster(verificationTaskId, start, end, null, LogClusterLevel.L2);

    assertThat(logClusters).isNotNull();
    assertThat(logClusters.size()).isEqualTo(25);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testSaveClusteredData() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    List<LogClusterDTO> clusterDTOList = buildLogClusterDtos(5, start, end);
    logClusterService.saveClusteredData(clusterDTOList, verificationTaskId, end, "taskId1", LogClusterLevel.L2);
    List<ClusteredLog> clusteredLogList =
        hPersistence.createQuery(ClusteredLog.class).filter(ClusteredLogKeys.cvConfigId, verificationTaskId).asList();
    assertThat(clusteredLogList).isNotNull();
  }

  private List<LogClusterDTO> buildLogClusterDtos(int numHosts, Instant startTime, Instant endTime) {
    List<ClusteredLog> clusteredLogs = createClusteredLogRecords(numHosts, startTime, endTime);
    List<LogClusterDTO> clusterDTOList = new ArrayList<>();
    clusteredLogs.forEach(log -> clusterDTOList.add(log.toDTO()));
    return clusterDTOList;
  }

  private List<ClusteredLog> createClusteredLogRecords(int numHosts, Instant startTime, Instant endTime) {
    List<ClusteredLog> logRecords = new ArrayList<>();
    for (int i = 0; i < numHosts; i++) {
      Instant timestamp = startTime;
      while (timestamp.isBefore(endTime)) {
        ClusteredLog record = ClusteredLog.builder()
                                  .verificationTaskId(verificationTaskId)
                                  .timestamp(timestamp)
                                  .host("host-" + i)
                                  .log("sample log record")
                                  .clusterLabel("1")
                                  .clusterCount(4)
                                  .clusterLevel(LogClusterLevel.L1)
                                  .build();
        logRecords.add(record);
        timestamp = timestamp.plus(1, ChronoUnit.MINUTES);
      }
    }

    return logRecords;
  }

  private void createLogDataRecords(int numHosts, Instant startTime, Instant endTime) {
    List<LogRecord> logRecords = new ArrayList<>();
    for (int i = 0; i < numHosts; i++) {
      Instant timestamp = startTime;
      while (timestamp.isBefore(endTime)) {
        LogRecord record = LogRecord.builder()
                               .verificationTaskId(verificationTaskId)
                               .timestamp(timestamp)
                               .host("host-" + i)
                               .log("sample log record")
                               .build();
        logRecords.add(record);
        timestamp = timestamp.plus(1, ChronoUnit.MINUTES);
      }
    }
    hPersistence.save(logRecords);
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(generateUuid());
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(generateUuid());
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setGroupId(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(generateUuid());
  }
}