/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb;

import io.harness.timescaledb.tables.CeRecommendations;
import io.harness.timescaledb.tables.NodeInfo;
import io.harness.timescaledb.tables.NodePoolAggregated;
import io.harness.timescaledb.tables.PipelineExecutionSummary;
import io.harness.timescaledb.tables.PipelineExecutionSummaryCd;
import io.harness.timescaledb.tables.PipelineExecutionSummaryCi;
import io.harness.timescaledb.tables.ServiceInfraInfo;
import io.harness.timescaledb.tables.WorkloadInfo;
import io.harness.timescaledb.tables.records.CeRecommendationsRecord;
import io.harness.timescaledb.tables.records.NodeInfoRecord;
import io.harness.timescaledb.tables.records.NodePoolAggregatedRecord;
import io.harness.timescaledb.tables.records.PipelineExecutionSummaryCdRecord;
import io.harness.timescaledb.tables.records.PipelineExecutionSummaryCiRecord;
import io.harness.timescaledb.tables.records.PipelineExecutionSummaryRecord;
import io.harness.timescaledb.tables.records.ServiceInfraInfoRecord;
import io.harness.timescaledb.tables.records.WorkloadInfoRecord;

import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

/**
 * A class modelling foreign key relationships and constraints of tables in
 * public.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Keys {
  // -------------------------------------------------------------------------
  // UNIQUE and PRIMARY KEY definitions
  // -------------------------------------------------------------------------

  public static final UniqueKey<CeRecommendationsRecord> CE_RECOMMENDATIONS_PKEY =
      Internal.createUniqueKey(CeRecommendations.CE_RECOMMENDATIONS, DSL.name("ce_recommendations_pkey"),
          new TableField[] {CeRecommendations.CE_RECOMMENDATIONS.ID}, true);
  public static final UniqueKey<NodeInfoRecord> NODE_INFO_UNIQUE_RECORD_INDEX =
      Internal.createUniqueKey(NodeInfo.NODE_INFO, DSL.name("node_info_unique_record_index"),
          new TableField[] {NodeInfo.NODE_INFO.ACCOUNTID, NodeInfo.NODE_INFO.CLUSTERID, NodeInfo.NODE_INFO.INSTANCEID},
          true);
  public static final UniqueKey<NodePoolAggregatedRecord> NODE_POOL_AGGREGATED_UNIQUE_RECORD_INDEX =
      Internal.createUniqueKey(NodePoolAggregated.NODE_POOL_AGGREGATED,
          DSL.name("node_pool_aggregated_unique_record_index"),
          new TableField[] {NodePoolAggregated.NODE_POOL_AGGREGATED.ACCOUNTID,
              NodePoolAggregated.NODE_POOL_AGGREGATED.CLUSTERID, NodePoolAggregated.NODE_POOL_AGGREGATED.NAME,
              NodePoolAggregated.NODE_POOL_AGGREGATED.STARTTIME, NodePoolAggregated.NODE_POOL_AGGREGATED.ENDTIME},
          true);

  public static final UniqueKey<PipelineExecutionSummaryRecord> PIPELINE_EXECUTION_SUMMARY_PKEY =
      Internal.createUniqueKey(PipelineExecutionSummary.PIPELINE_EXECUTION_SUMMARY,
          DSL.name("pipeline_execution_summary_pkey"),
          new TableField[] {PipelineExecutionSummary.PIPELINE_EXECUTION_SUMMARY.ID,
              PipelineExecutionSummary.PIPELINE_EXECUTION_SUMMARY.STARTTS},
          true);
  public static final UniqueKey<PipelineExecutionSummaryCdRecord> PIPELINE_EXECUTION_SUMMARY_CD_PKEY =
      Internal.createUniqueKey(PipelineExecutionSummaryCd.PIPELINE_EXECUTION_SUMMARY_CD,
          DSL.name("pipeline_execution_summary_cd_pkey"),
          new TableField[] {PipelineExecutionSummaryCd.PIPELINE_EXECUTION_SUMMARY_CD.ID,
              PipelineExecutionSummaryCd.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS},
          true);
  public static final UniqueKey<PipelineExecutionSummaryCiRecord> PIPELINE_EXECUTION_SUMMARY_CI_PKEY =
      Internal.createUniqueKey(PipelineExecutionSummaryCi.PIPELINE_EXECUTION_SUMMARY_CI,
          DSL.name("pipeline_execution_summary_ci_pkey"),
          new TableField[] {PipelineExecutionSummaryCi.PIPELINE_EXECUTION_SUMMARY_CI.ID,
              PipelineExecutionSummaryCi.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS},
          true);
  public static final UniqueKey<ServiceInfraInfoRecord> SERVICE_INFRA_INFO_PKEY = Internal.createUniqueKey(
      ServiceInfraInfo.SERVICE_INFRA_INFO, DSL.name("service_infra_info_pkey"),
      new TableField[] {ServiceInfraInfo.SERVICE_INFRA_INFO.ID, ServiceInfraInfo.SERVICE_INFRA_INFO.SERVICE_STARTTS},
      true);
  public static final UniqueKey<WorkloadInfoRecord> WORKLOAD_INFO_UNIQUE_RECORD_INDEX =
      Internal.createUniqueKey(WorkloadInfo.WORKLOAD_INFO, DSL.name("workload_info_unique_record_index"),
          new TableField[] {WorkloadInfo.WORKLOAD_INFO.ACCOUNTID, WorkloadInfo.WORKLOAD_INFO.CLUSTERID,
              WorkloadInfo.WORKLOAD_INFO.WORKLOADID},
          true);
}
