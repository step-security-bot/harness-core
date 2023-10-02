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

import io.harness.timescaledb.tables.Anomalies;
import io.harness.timescaledb.tables.BillingData;
import io.harness.timescaledb.tables.CeRecommendations;
import io.harness.timescaledb.tables.Environments;
import io.harness.timescaledb.tables.KubernetesUtilizationData;
import io.harness.timescaledb.tables.NgInstanceStats;
import io.harness.timescaledb.tables.NodeInfo;
import io.harness.timescaledb.tables.PipelineExecutionSummary;
import io.harness.timescaledb.tables.PipelineExecutionSummaryCd;
import io.harness.timescaledb.tables.PipelineExecutionSummaryCi;
import io.harness.timescaledb.tables.Pipelines;
import io.harness.timescaledb.tables.PodInfo;
import io.harness.timescaledb.tables.ServiceInfraInfo;
import io.harness.timescaledb.tables.ServiceInstancesLicenseDailyReport;
import io.harness.timescaledb.tables.Services;
import io.harness.timescaledb.tables.ServicesLicenseDailyReport;
import io.harness.timescaledb.tables.UtilizationData;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

/**
 * A class modelling indexes of tables in public.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Indexes {
  // -------------------------------------------------------------------------
  // INDEX definitions
  // -------------------------------------------------------------------------

  public static final Index ANOMALIES_ANOMALYTIME_IDX = Internal.createIndex(DSL.name("anomalies_anomalytime_idx"),
      Anomalies.ANOMALIES, new OrderField[] {Anomalies.ANOMALIES.ANOMALYTIME.desc()}, false);
  public static final Index ANOMALIES_PKEY = Internal.createIndex(DSL.name("anomalies_pkey"), Anomalies.ANOMALIES,
      new OrderField[] {Anomalies.ANOMALIES.ID, Anomalies.ANOMALIES.ANOMALYTIME}, true);
  public static final Index ANOMALY_ACCOUNTID_INDEX =
      Internal.createIndex(DSL.name("anomaly_accountid_index"), Anomalies.ANOMALIES,
          new OrderField[] {Anomalies.ANOMALIES.ACCOUNTID, Anomalies.ANOMALIES.ANOMALYTIME.desc()}, false);
  public static final Index BILLING_DATA_ACCOUNTID_INDEX =
      Internal.createIndex(DSL.name("billing_data_accountid_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.STARTTIME.desc()}, false);
  public static final Index BILLING_DATA_APPID_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_appid_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.APPID,
              BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_CLOUDPROVIDERID_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_cloudproviderid_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLOUDPROVIDERID,
              BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_CLOUDSERVICENAME_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_cloudservicename_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.CLOUDSERVICENAME, BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_CLUSTERID_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_clusterid_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_LAUNCHTYPE_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_launchtype_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.LAUNCHTYPE, BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_NAMESPACE_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_namespace_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.NAMESPACE, BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_NAMESPACE_WITHOUT_CLUSTER_INDEX =
      Internal.createIndex(DSL.name("billing_data_namespace_without_cluster_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.NAMESPACE,
              BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_STARTTIME_IDX = Internal.createIndex(DSL.name("billing_data_starttime_idx"),
      BillingData.BILLING_DATA, new OrderField[] {BillingData.BILLING_DATA.STARTTIME.desc()}, false);
  public static final Index BILLING_DATA_TASKID_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_taskid_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.TASKID, BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_UNIQUE_INDEX =
      Internal.createIndex(DSL.name("billing_data_unique_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.SETTINGID,
              BillingData.BILLING_DATA.CLUSTERID, BillingData.BILLING_DATA.INSTANCEID,
              BillingData.BILLING_DATA.INSTANCETYPE, BillingData.BILLING_DATA.STARTTIME.desc()},
          true);
  public static final Index BILLING_DATA_WORKLOADNAME_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_workloadname_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.WORKLOADNAME, BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_WORKLOADNAME_WITHOUT_CLUSTER_INDEX =
      Internal.createIndex(DSL.name("billing_data_workloadname_without_cluster_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.WORKLOADNAME,
              BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index ENVIRONMENTS_ACCOUNT_ID_CREATED_AT_IDX =
      Internal.createIndex(DSL.name("environments_account_id_created_at_idx"), Environments.ENVIRONMENTS,
          new OrderField[] {Environments.ENVIRONMENTS.ACCOUNT_ID, Environments.ENVIRONMENTS.CREATED_AT}, false);
  public static final Index ENVIRONMENTS_PKEY = Internal.createIndex(
      DSL.name("environments_pkey"), Environments.ENVIRONMENTS, new OrderField[] {Environments.ENVIRONMENTS.ID}, true);
  public static final Index KUBERNETES_UTILIZATION_DATA_ACCID_CLUSTERID_ACINSTANCEID =
      Internal.createIndex(DSL.name("kubernetes_utilization_data_accid_clusterid_acinstanceid"),
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA,
          new OrderField[] {KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.ACCOUNTID,
              KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.CLUSTERID,
              KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.ACTUALINSTANCEID,
              KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.STARTTIME.desc()},
          false);
  public static final Index KUBERNETES_UTILIZATION_DATA_INSTANCEID_INDEX = Internal.createIndex(
      DSL.name("kubernetes_utilization_data_instanceid_index"), KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA,
      new OrderField[] {KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.INSTANCEID,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.STARTTIME.desc()},
      false);
  public static final Index KUBERNETES_UTILIZATION_DATA_STARTTIME_IDX = Internal.createIndex(
      DSL.name("kubernetes_utilization_data_starttime_idx"), KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA,
      new OrderField[] {KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.STARTTIME.desc()}, false);
  public static final Index KUBERNETES_UTILIZATION_DATA_UNIQUE_INDEX = Internal.createIndex(
      DSL.name("kubernetes_utilization_data_unique_index"), KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA,
      new OrderField[] {KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.ACCOUNTID,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.SETTINGID,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.CLUSTERID,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.INSTANCEID,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.INSTANCETYPE,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.STARTTIME.desc()},
      true);
  public static final Index NG_INSTANCE_STATS_ACCOUNTID_INDEX =
      Internal.createIndex(DSL.name("ng_instance_stats_accountid_index"), NgInstanceStats.NG_INSTANCE_STATS,
          new OrderField[] {
              NgInstanceStats.NG_INSTANCE_STATS.ACCOUNTID, NgInstanceStats.NG_INSTANCE_STATS.REPORTEDAT.desc()},
          false);
  public static final Index NG_INSTANCE_STATS_CLOUDPROVIDERID_INDEX =
      Internal.createIndex(DSL.name("ng_instance_stats_cloudproviderid_index"), NgInstanceStats.NG_INSTANCE_STATS,
          new OrderField[] {
              NgInstanceStats.NG_INSTANCE_STATS.CLOUDPROVIDERID, NgInstanceStats.NG_INSTANCE_STATS.REPORTEDAT.desc()},
          false);
  public static final Index NG_INSTANCE_STATS_ENVID_INDEX = Internal.createIndex(
      DSL.name("ng_instance_stats_envid_index"), NgInstanceStats.NG_INSTANCE_STATS,
      new OrderField[] {NgInstanceStats.NG_INSTANCE_STATS.ENVID, NgInstanceStats.NG_INSTANCE_STATS.REPORTEDAT.desc()},
      false);
  public static final Index NG_INSTANCE_STATS_INSTANCECOUNT_INDEX =
      Internal.createIndex(DSL.name("ng_instance_stats_instancecount_index"), NgInstanceStats.NG_INSTANCE_STATS,
          new OrderField[] {
              NgInstanceStats.NG_INSTANCE_STATS.INSTANCECOUNT, NgInstanceStats.NG_INSTANCE_STATS.REPORTEDAT.desc()},
          false);
  public static final Index NG_INSTANCE_STATS_ORGID_INDEX = Internal.createIndex(
      DSL.name("ng_instance_stats_orgid_index"), NgInstanceStats.NG_INSTANCE_STATS,
      new OrderField[] {NgInstanceStats.NG_INSTANCE_STATS.ORGID, NgInstanceStats.NG_INSTANCE_STATS.REPORTEDAT.desc()},
      false);
  public static final Index NG_INSTANCE_STATS_PROJECTID_INDEX =
      Internal.createIndex(DSL.name("ng_instance_stats_projectid_index"), NgInstanceStats.NG_INSTANCE_STATS,
          new OrderField[] {
              NgInstanceStats.NG_INSTANCE_STATS.PROJECTID, NgInstanceStats.NG_INSTANCE_STATS.REPORTEDAT.desc()},
          false);
  public static final Index NG_INSTANCE_STATS_REPORTEDAT_IDX =
      Internal.createIndex(DSL.name("ng_instance_stats_reportedat_idx"), NgInstanceStats.NG_INSTANCE_STATS,
          new OrderField[] {NgInstanceStats.NG_INSTANCE_STATS.REPORTEDAT.desc()}, false);
  public static final Index NG_INSTANCE_STATS_SERVICEID_INDEX =
      Internal.createIndex(DSL.name("ng_instance_stats_serviceid_index"), NgInstanceStats.NG_INSTANCE_STATS,
          new OrderField[] {
              NgInstanceStats.NG_INSTANCE_STATS.SERVICEID, NgInstanceStats.NG_INSTANCE_STATS.REPORTEDAT.desc()},
          false);
  public static final Index NODE_INFO_ACCID_CLUSTERID_POOLNAME = Internal.createIndex(
      DSL.name("node_info_accid_clusterid_poolname"), NodeInfo.NODE_INFO,
      new OrderField[] {NodeInfo.NODE_INFO.ACCOUNTID, NodeInfo.NODE_INFO.CLUSTERID, NodeInfo.NODE_INFO.NODEPOOLNAME},
      false);

  public static final Index PIPELINE_EXECUTION_SUMMARY_STARTTS_IDX = Internal.createIndex(
      DSL.name("pipeline_execution_summary_startts_idx"), PipelineExecutionSummary.PIPELINE_EXECUTION_SUMMARY,
      new OrderField[] {PipelineExecutionSummary.PIPELINE_EXECUTION_SUMMARY.STARTTS.desc()}, false);

  public static final Index PIPELINE_EXECUTION_SUMMARY_CD_STARTTS_IDX = Internal.createIndex(
      DSL.name("pipeline_execution_summary_cd_startts_idx"), PipelineExecutionSummaryCd.PIPELINE_EXECUTION_SUMMARY_CD,
      new OrderField[] {PipelineExecutionSummaryCd.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.desc()}, false);
  public static final Index PIPELINE_EXECUTION_SUMMARY_CI_STARTTS_IDX = Internal.createIndex(
      DSL.name("pipeline_execution_summary_ci_startts_idx"), PipelineExecutionSummaryCi.PIPELINE_EXECUTION_SUMMARY_CI,
      new OrderField[] {PipelineExecutionSummaryCi.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS.desc()}, false);
  public static final Index PIPELINE_SUMMERY_CI_ACCOUNT_ORG_PROJ_IDX = Internal.createIndex(
      DSL.name("pipeline_summery_ci_account_org_proj_idx"), PipelineExecutionSummaryCi.PIPELINE_EXECUTION_SUMMARY_CI,
      new OrderField[] {PipelineExecutionSummaryCi.PIPELINE_EXECUTION_SUMMARY_CI.ACCOUNTID,
          PipelineExecutionSummaryCi.PIPELINE_EXECUTION_SUMMARY_CI.ORGIDENTIFIER,
          PipelineExecutionSummaryCi.PIPELINE_EXECUTION_SUMMARY_CI.PROJECTIDENTIFIER},
      false);
  public static final Index PIPELINES_ACCOUNT_ID_CREATED_AT_IDX =
      Internal.createIndex(DSL.name("pipelines_account_id_created_at_idx"), Pipelines.PIPELINES,
          new OrderField[] {Pipelines.PIPELINES.ACCOUNT_ID, Pipelines.PIPELINES.CREATED_AT}, false);
  public static final Index PIPELINES_PKEY = Internal.createIndex(
      DSL.name("pipelines_pkey"), Pipelines.PIPELINES, new OrderField[] {Pipelines.PIPELINES.ID}, true);
  public static final Index POD_INFO_KUBESYSTEM_NAMESPACE_PINDEX =
      Internal.createIndex(DSL.name("pod_info_kubesystem_namespace_pindex"), PodInfo.POD_INFO,
          new OrderField[] {PodInfo.POD_INFO.ACCOUNTID, PodInfo.POD_INFO.CLUSTERID, PodInfo.POD_INFO.NAMESPACE,
              PodInfo.POD_INFO.STARTTIME.desc()},
          false);
  public static final Index POD_INFO_STARTTIME_IDX = Internal.createIndex(DSL.name("pod_info_starttime_idx"),
      PodInfo.POD_INFO, new OrderField[] {PodInfo.POD_INFO.STARTTIME.desc()}, false);
  public static final Index SERVICE_INSTANCES_LICENSE_ACCOUNT_ID_INDEX = Internal.createIndex(
      DSL.name("service_instances_license_account_id_index"),
      ServiceInstancesLicenseDailyReport.SERVICE_INSTANCES_LICENSE_DAILY_REPORT,
      new OrderField[] {ServiceInstancesLicenseDailyReport.SERVICE_INSTANCES_LICENSE_DAILY_REPORT.ACCOUNT_ID}, false);
  public static final Index SERVICE_INSTANCES_LICENSE_ACCOUNT_ID_REPORTED_DAY_INDEX =
      Internal.createIndex(DSL.name("service_instances_license_account_id_reported_day_index"),
          ServiceInstancesLicenseDailyReport.SERVICE_INSTANCES_LICENSE_DAILY_REPORT,
          new OrderField[] {ServiceInstancesLicenseDailyReport.SERVICE_INSTANCES_LICENSE_DAILY_REPORT.ACCOUNT_ID,
              ServiceInstancesLicenseDailyReport.SERVICE_INSTANCES_LICENSE_DAILY_REPORT.REPORTED_DAY.desc()},
          false);
  public static final Index SERVICES_LICENSE_ACCOUNT_ID_INDEX = Internal.createIndex(
      DSL.name("services_license_account_id_index"), ServicesLicenseDailyReport.SERVICES_LICENSE_DAILY_REPORT,
      new OrderField[] {ServicesLicenseDailyReport.SERVICES_LICENSE_DAILY_REPORT.ACCOUNT_ID}, false);
  public static final Index SERVICES_LICENSE_ACCOUNT_ID_REPORTED_DAY_INDEX =
      Internal.createIndex(DSL.name("services_license_account_id_reported_day_index"),
          ServicesLicenseDailyReport.SERVICES_LICENSE_DAILY_REPORT,
          new OrderField[] {ServicesLicenseDailyReport.SERVICES_LICENSE_DAILY_REPORT.ACCOUNT_ID,
              ServicesLicenseDailyReport.SERVICES_LICENSE_DAILY_REPORT.REPORTED_DAY.desc()},
          false);
  public static final Index POD_INFO_STARTTIME_UNIQUE_RECORD_INDEX =
      Internal.createIndex(DSL.name("pod_info_starttime_unique_record_index"), PodInfo.POD_INFO,
          new OrderField[] {PodInfo.POD_INFO.ACCOUNTID, PodInfo.POD_INFO.CLUSTERID, PodInfo.POD_INFO.INSTANCEID,
              PodInfo.POD_INFO.STARTTIME.desc()},
          true);
  public static final Index PODINFO_ACC_CLUS_SRTIME_SPTIME_PNDEX =
      Internal.createIndex(DSL.name("podinfo_acc_clus_srtime_sptime_pndex"), PodInfo.POD_INFO,
          new OrderField[] {PodInfo.POD_INFO.ACCOUNTID, PodInfo.POD_INFO.CLUSTERID, PodInfo.POD_INFO.STARTTIME.desc(),
              PodInfo.POD_INFO.STOPTIME.desc()},
          false);
  public static final Index RECOMMENDATION_ACCOUNTID_LASTPROCESSEDAT_ISVALID_RESOURCETYPE_I = Internal.createIndex(
      DSL.name("recommendation_accountid_lastprocessedat_isvalid_resourcetype_i"), CeRecommendations.CE_RECOMMENDATIONS,
      new OrderField[] {CeRecommendations.CE_RECOMMENDATIONS.ACCOUNTID,
          CeRecommendations.CE_RECOMMENDATIONS.LASTPROCESSEDAT, CeRecommendations.CE_RECOMMENDATIONS.ISVALID,
          CeRecommendations.CE_RECOMMENDATIONS.RESOURCETYPE},
      false);
  public static final Index RECOMMENDATION_ACCOUNTID_VALID_INDEX =
      Internal.createIndex(DSL.name("recommendation_accountid_valid_index"), CeRecommendations.CE_RECOMMENDATIONS,
          new OrderField[] {CeRecommendations.CE_RECOMMENDATIONS.ACCOUNTID,
              CeRecommendations.CE_RECOMMENDATIONS.LASTPROCESSEDAT, CeRecommendations.CE_RECOMMENDATIONS.ISVALID},
          false);
  public static final Index SERVICE_INFRA_INFO_PIPELINE_EXECUTION_IDX =
      Internal.createIndex(DSL.name("service_infra_info_pipeline_execution_idx"), ServiceInfraInfo.SERVICE_INFRA_INFO,
          new OrderField[] {ServiceInfraInfo.SERVICE_INFRA_INFO.PIPELINE_EXECUTION_SUMMARY_CD_ID}, false);
  public static final Index SERVICE_INFRA_INFO_SERVICE_STARTTS_IDX =
      Internal.createIndex(DSL.name("service_infra_info_service_startts_idx"), ServiceInfraInfo.SERVICE_INFRA_INFO,
          new OrderField[] {ServiceInfraInfo.SERVICE_INFRA_INFO.SERVICE_STARTTS.desc()}, false);
  public static final Index SERVICES_ACCOUNT_ID_CREATED_AT_IDX =
      Internal.createIndex(DSL.name("services_account_id_created_at_idx"), Services.SERVICES,
          new OrderField[] {Services.SERVICES.ACCOUNT_ID, Services.SERVICES.CREATED_AT}, false);
  public static final Index SERVICES_PKEY =
      Internal.createIndex(DSL.name("services_pkey"), Services.SERVICES, new OrderField[] {Services.SERVICES.ID}, true);
  public static final Index UTILIZATION_DATA_INSTANCEID_INDEX = Internal.createIndex(
      DSL.name("utilization_data_instanceid_index"), UtilizationData.UTILIZATION_DATA,
      new OrderField[] {UtilizationData.UTILIZATION_DATA.INSTANCEID, UtilizationData.UTILIZATION_DATA.STARTTIME.desc()},
      false);
  public static final Index UTILIZATION_DATA_STARTTIME_IDX =
      Internal.createIndex(DSL.name("utilization_data_starttime_idx"), UtilizationData.UTILIZATION_DATA,
          new OrderField[] {UtilizationData.UTILIZATION_DATA.STARTTIME.desc()}, false);
  public static final Index UTILIZATION_DATA_UNIQUE_INDEX =
      Internal.createIndex(DSL.name("utilization_data_unique_index"), UtilizationData.UTILIZATION_DATA,
          new OrderField[] {UtilizationData.UTILIZATION_DATA.ACCOUNTID, UtilizationData.UTILIZATION_DATA.SETTINGID,
              UtilizationData.UTILIZATION_DATA.CLUSTERID, UtilizationData.UTILIZATION_DATA.INSTANCEID,
              UtilizationData.UTILIZATION_DATA.INSTANCETYPE, UtilizationData.UTILIZATION_DATA.STARTTIME.desc()},
          true);
}
