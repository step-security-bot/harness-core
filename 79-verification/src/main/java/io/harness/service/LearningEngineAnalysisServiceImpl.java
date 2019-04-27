package io.harness.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.common.VerificationConstants.VERIFICATION_TASK_TIMEOUT;
import static software.wings.service.impl.newrelic.LearningEngineAnalysisTask.TIME_SERIES_ANALYSIS_TASK_TIME_OUT;
import static software.wings.utils.Misc.generateSecretKey;
import static software.wings.utils.Misc.replaceUnicodeWithDot;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.ExecutionStatus;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.service.intfc.LearningEngineService;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskKeys;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.MLExperiments;
import software.wings.service.intfc.analysis.ClusterLevel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 1/9/18.
 */
@Slf4j
public class LearningEngineAnalysisServiceImpl implements LearningEngineService {
  private static final String SERVICE_VERSION_FILE = "/service_version.properties";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private HarnessMetricRegistry metricRegistry;

  private final ServiceApiVersion learningEngineApiVersion;

  public LearningEngineAnalysisServiceImpl() throws IOException {
    Properties messages = new Properties();
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream(SERVICE_VERSION_FILE);
      messages.load(in);
    } finally {
      if (in != null) {
        in.close();
      }
    }
    String apiVersion = messages.getProperty(ServiceType.LEARNING_ENGINE.name());
    Preconditions.checkState(!StringUtils.isEmpty(apiVersion));
    learningEngineApiVersion = ServiceApiVersion.valueOf(apiVersion.toUpperCase());
  }

  private ClusterLevel getDefaultClusterLevel() {
    return ClusterLevel.HF;
  }

  @Override
  public boolean addLearningEngineAnalysisTask(LearningEngineAnalysisTask analysisTask) {
    analysisTask.setVersion(learningEngineApiVersion);
    analysisTask.setExecutionStatus(ExecutionStatus.QUEUED);
    analysisTask.setRetry(0);
    if (analysisTask.getCluster_level() == null) {
      analysisTask.setCluster_level(getDefaultClusterLevel().getLevel());
    }
    Query<LearningEngineAnalysisTask> query =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter("workflow_execution_id", analysisTask.getWorkflow_execution_id())
            .filter("state_execution_id", analysisTask.getState_execution_id())
            .field("analysis_minute")
            .lessThanOrEq(analysisTask.getAnalysis_minute())
            .filter("version", learningEngineApiVersion)
            .field("executionStatus")
            .in(Lists.newArrayList(ExecutionStatus.RUNNING, ExecutionStatus.QUEUED, ExecutionStatus.SUCCESS))
            .filter("cluster_level", analysisTask.getCluster_level())
            .filter("ml_analysis_type", analysisTask.getMl_analysis_type())
            .filter("group_name", analysisTask.getGroup_name())
            .order("-createdAt");
    if (!analysisTask.is24x7Task()) {
      query = query.filter("control_nodes", analysisTask.getControl_nodes());
    }
    if (isNotEmpty(analysisTask.getTag())) {
      query = query.filter("tag", analysisTask.getTag());
    }
    LearningEngineAnalysisTask learningEngineAnalysisTask = query.get();

    boolean isTaskCreated = false;
    if (learningEngineAnalysisTask == null) {
      wingsPersistence.save(analysisTask);
      isTaskCreated = true;
    } else if (learningEngineAnalysisTask.getExecutionStatus() == ExecutionStatus.SUCCESS) {
      if (learningEngineAnalysisTask.getAnalysis_minute() < analysisTask.getAnalysis_minute()) {
        wingsPersistence.save(analysisTask);
        isTaskCreated = true;
      } else {
        logger.warn("task is already marked success for min {}. task {}", analysisTask.getAnalysis_minute(),
            learningEngineAnalysisTask);
      }
    } else {
      logger.warn("task is already {}. Will not queue for minute {}, {}",
          learningEngineAnalysisTask.getExecutionStatus(), analysisTask.getAnalysis_minute(),
          learningEngineAnalysisTask);
    }
    return isTaskCreated;
  }

  @Override
  public boolean addLearningEngineExperimentalAnalysisTask(LearningEngineExperimentalAnalysisTask analysisTask) {
    LearningEngineExperimentalAnalysisTask experimentalAnalysisTask =
        wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class)
            .filter("state_execution_id", analysisTask.getState_execution_id())
            .get();
    if (experimentalAnalysisTask != null) {
      logger.info("task already queued for experiment {}", analysisTask.getState_execution_id());
      return false;
    }
    analysisTask.setVersion(learningEngineApiVersion);
    analysisTask.setExecutionStatus(ExecutionStatus.QUEUED);
    analysisTask.setRetry(0);
    wingsPersistence.save(analysisTask);
    return true;
  }

  @Override
  public LearningEngineAnalysisTask getNextLearningEngineAnalysisTask(
      ServiceApiVersion serviceApiVersion, Optional<Boolean> is24x7Task, Optional<List<MLAnalysisType>> taskTypes) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .filter("version", serviceApiVersion)
                                                  .field("retry")
                                                  .lessThanOrEq(LearningEngineAnalysisTask.RETRIES);
    if (is24x7Task.isPresent()) {
      query.filter(LearningEngineAnalysisTaskKeys.is24x7Task, is24x7Task.get());
    }

    if (taskTypes.isPresent()) {
      query.field(LearningEngineAnalysisTaskKeys.ml_analysis_type).in(taskTypes.get());
    }

    query.or(query.criteria("executionStatus").equal(ExecutionStatus.QUEUED),
        query.and(query.criteria("executionStatus").equal(ExecutionStatus.RUNNING),
            query.criteria(LearningEngineAnalysisTask.LAST_UPDATED_AT_KEY)
                .lessThan(System.currentTimeMillis() - TIME_SERIES_ANALYSIS_TASK_TIME_OUT)));
    UpdateOperations<LearningEngineAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineAnalysisTask.class)
            .set("executionStatus", ExecutionStatus.RUNNING)
            .inc("retry")
            .set(LearningEngineAnalysisTask.LAST_UPDATED_AT_KEY, System.currentTimeMillis());
    LearningEngineAnalysisTask task =
        wingsPersistence.findAndModify(query, updateOperations, new FindAndModifyOptions());
    if (task != null && task.getRetry() >= LearningEngineAnalysisTask.RETRIES) {
      // If some task has failed for more than 3 times, mark status as failed.
      logger.info("LearningEngine task {} has failed 3 or more times. Setting the status to FAILED", task.getUuid());
      try {
        wingsPersistence.updateField(
            LearningEngineAnalysisTask.class, task.getUuid(), "executionStatus", ExecutionStatus.FAILED);
      } catch (DuplicateKeyException e) {
        logger.info("task {} for state {} is already marked successful", task.getUuid(), task.getState_execution_id());
      }
      return null;
    }
    return task;
  }

  @Override
  public LearningEngineExperimentalAnalysisTask getNextLearningEngineExperimentalAnalysisTask(
      String experimentName, ServiceApiVersion serviceApiVersion) {
    Query<LearningEngineExperimentalAnalysisTask> query =
        wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class)
            .filter("version", serviceApiVersion)
            .filter("experiment_name", experimentName)
            .field("retry")
            .lessThan(LearningEngineExperimentalAnalysisTask.RETRIES);
    query.or(query.criteria("executionStatus").equal(ExecutionStatus.QUEUED),
        query.and(query.criteria("executionStatus").equal(ExecutionStatus.RUNNING),
            query.criteria(LearningEngineExperimentalAnalysisTask.LAST_UPDATED_AT_KEY)
                .lessThan(System.currentTimeMillis() - TIME_SERIES_ANALYSIS_TASK_TIME_OUT)));
    UpdateOperations<LearningEngineExperimentalAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineExperimentalAnalysisTask.class)
            .set("executionStatus", ExecutionStatus.RUNNING)
            .inc("retry")
            .set(LearningEngineExperimentalAnalysisTask.LAST_UPDATED_AT_KEY, System.currentTimeMillis());
    return wingsPersistence.findAndModify(query, updateOperations, new FindAndModifyOptions());
  }

  @Override
  public boolean hasAnalysisTimedOut(String appId, String workflowExecutionId, String stateExecutionId) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .filter("appId", appId)
                                                  .filter("workflow_execution_id", workflowExecutionId)
                                                  .filter("state_execution_id", stateExecutionId)
                                                  .filter("executionStatus", ExecutionStatus.RUNNING)
                                                  .field("retry")
                                                  .greaterThanOrEq(LearningEngineAnalysisTask.RETRIES);
    return !query.asList().isEmpty();
  }

  @Override
  public void markCompleted(String workflowExecutionId, String stateExecutionId, long analysisMinute,
      MLAnalysisType type, ClusterLevel level) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .filter("workflow_execution_id", workflowExecutionId)
                                                  .filter("state_execution_id", stateExecutionId)
                                                  .filter("executionStatus", ExecutionStatus.RUNNING)
                                                  .filter("analysis_minute", analysisMinute)
                                                  .filter("ml_analysis_type", type)
                                                  .filter("cluster_level", level.getLevel());
    UpdateOperations<LearningEngineAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineAnalysisTask.class)
            .set("executionStatus", ExecutionStatus.SUCCESS);

    wingsPersistence.update(query, updateOperations);
  }

  @Override
  public void markCompleted(String taskId) {
    if (taskId == null) {
      logger.warn("taskId is null");
      return;
    }
    wingsPersistence.updateField(LearningEngineAnalysisTask.class, taskId, "executionStatus", ExecutionStatus.SUCCESS);
  }

  @Override
  public void markExpTaskCompleted(String taskId) {
    if (taskId == null) {
      logger.warn("taskId is null");
      return;
    }
    wingsPersistence.updateField(
        LearningEngineExperimentalAnalysisTask.class, taskId, "executionStatus", ExecutionStatus.SUCCESS);
  }

  @Override
  public void markStatus(
      String workflowExecutionId, String stateExecutionId, long analysisMinute, ExecutionStatus executionStatus) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .filter("workflow_execution_id", workflowExecutionId)
                                                  .filter("state_execution_id", stateExecutionId)
                                                  .filter("executionStatus", ExecutionStatus.RUNNING)
                                                  .filter("cluster_level", getDefaultClusterLevel())
                                                  .field("analysis_minute")
                                                  .lessThanOrEq(analysisMinute);
    UpdateOperations<LearningEngineAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineAnalysisTask.class)
            .set("executionStatus", executionStatus);

    wingsPersistence.update(query, updateOperations);
  }

  @Override
  public void initializeServiceSecretKeys() {
    for (ServiceType serviceType : ServiceType.values()) {
      wingsPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(
          ServiceSecretKey.builder().serviceType(serviceType).serviceSecret(generateSecretKey()).build()));
    }
  }

  @Override
  public String getServiceSecretKey(ServiceType serviceType) {
    Preconditions.checkNotNull(serviceType);
    return wingsPersistence.createQuery(ServiceSecretKey.class)
        .filter("serviceType", serviceType)
        .get()
        .getServiceSecret();
  }

  @Override
  public List<MLExperiments> getExperiments(MLAnalysisType ml_analysis_type) {
    return wingsPersistence.createQuery(MLExperiments.class, excludeAuthority)
        .filter("ml_analysis_type", ml_analysis_type)
        .field("is24x7")
        .doesNotExist()
        .asList();
  }

  @Override
  public AnalysisContext getNextVerificationAnalysisTask(ServiceApiVersion serviceApiVersion) {
    Query<AnalysisContext> query = wingsPersistence.createQuery(AnalysisContext.class)
                                       .filter("version", serviceApiVersion)
                                       .field("retry")
                                       .lessThan(LearningEngineAnalysisTask.RETRIES);
    query.or(query.criteria("executionStatus").equal(ExecutionStatus.QUEUED),
        query.and(query.criteria("executionStatus").equal(ExecutionStatus.RUNNING),
            query.criteria(AnalysisContext.LAST_UPDATED_AT_KEY)
                .lessThan(System.currentTimeMillis() - VERIFICATION_TASK_TIMEOUT)));
    UpdateOperations<AnalysisContext> updateOperations =
        wingsPersistence.createUpdateOperations(AnalysisContext.class)
            .set("executionStatus", ExecutionStatus.RUNNING)
            .inc("retry")
            .set(AnalysisContext.LAST_UPDATED_AT_KEY, System.currentTimeMillis());
    AnalysisContext analysisContext =
        wingsPersistence.findAndModify(query, updateOperations, new FindAndModifyOptions());
    if (analysisContext != null) {
      analysisContext.setControlNodes(getNodesReplaceUniCode(analysisContext.getControlNodes()));
      analysisContext.setTestNodes(getNodesReplaceUniCode(analysisContext.getTestNodes()));
    }
    return analysisContext;
  }

  private Map<String, String> getNodesReplaceUniCode(Map<String, String> nodes) {
    if (isEmpty(nodes)) {
      return Collections.emptyMap();
    }

    Map<String, String> rv = new HashMap<>();
    nodes.forEach((host, groupName) -> rv.put(replaceUnicodeWithDot(host), groupName));
    return rv;
  }

  @Override
  public void markJobScheduled(AnalysisContext verificationAnalysisTask) {
    wingsPersistence.updateField(
        AnalysisContext.class, verificationAnalysisTask.getUuid(), "executionStatus", ExecutionStatus.SUCCESS);
  }

  @Override
  public void checkAndUpdateFailedLETask(String stateExecutionId, int analysisMinute) {
    Query<LearningEngineAnalysisTask> query = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                  .filter("state_execution_id", stateExecutionId)
                                                  .filter("analysis_minute", analysisMinute);
    query.or(query.criteria("executionStatus").equal(ExecutionStatus.FAILED),
        query.and(query.criteria("executionStatus").equal(ExecutionStatus.RUNNING),
            query.criteria("retry").greaterThan(LearningEngineAnalysisTask.RETRIES)));

    UpdateOperations<LearningEngineAnalysisTask> updateOperations =
        wingsPersistence.createUpdateOperations(LearningEngineAnalysisTask.class)
            .set("state_execution_id",
                stateExecutionId + "-retry-" + TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()))
            .set("executionStatus", ExecutionStatus.FAILED);
    wingsPersistence.update(query, updateOperations);
  }
}
