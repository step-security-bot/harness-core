/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.ci.vm.VmTaskExecutionResponse;
import io.harness.delegate.beans.ci.vm.dlite.DliteVmCleanupTaskParams;
import io.harness.delegate.beans.ci.vm.dlite.DliteVmExecuteStepTaskParams;
import io.harness.delegate.beans.ci.vm.dlite.DliteVmInitializeTaskParams;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;

@OwnedBy(CDC)
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public enum TaskType {
  CUSTOM_MANIFEST_VALUES_FETCH_TASK_NG(
      TaskGroup.CUSTOM_MANIFEST_VALUES_FETCH_TASK, "Custom Manifest/Values Fetch Task"),
  GITOPS_TASK_NG(TaskGroup.GIT),
  BATCH_CAPABILITY_CHECK(TaskGroup.BATCH_CAPABILITY_CHECK),
  CAPABILITY_VALIDATION(TaskGroup.CAPABILITY_VALIDATION),
  COMMAND(TaskGroup.COMMAND),
  SCRIPT(TaskGroup.SCRIPT),
  HTTP(TaskGroup.HTTP),
  GCB(TaskGroup.GCB),
  JENKINS(TaskGroup.JENKINS),
  JENKINS_COLLECTION(TaskGroup.JENKINS),
  JENKINS_GET_BUILDS(TaskGroup.JENKINS),
  JENKINS_GET_JOBS(TaskGroup.JENKINS),
  JENKINS_GET_JOB(TaskGroup.JENKINS),
  JENKINS_GET_ARTIFACT_PATHS(TaskGroup.JENKINS),
  JENKINS_LAST_SUCCESSFUL_BUILD(TaskGroup.JENKINS),
  JENKINS_GET_PLANS(TaskGroup.JENKINS),
  JENKINS_VALIDATE_ARTIFACT_SERVER(TaskGroup.JENKINS),
  JENKINS_CONNECTIVITY_TEST_TASK(TaskGroup.JENKINS),
  BAMBOO(TaskGroup.BAMBOO),
  BAMBOO_COLLECTION(TaskGroup.BAMBOO),
  BAMBOO_GET_BUILDS(TaskGroup.BAMBOO),
  BAMBOO_GET_JOBS(TaskGroup.BAMBOO),
  BAMBOO_GET_ARTIFACT_PATHS(TaskGroup.BAMBOO),
  BAMBOO_LAST_SUCCESSFUL_BUILD(TaskGroup.BAMBOO),
  BAMBOO_GET_PLANS(TaskGroup.BAMBOO),
  BAMBOO_VALIDATE_ARTIFACT_SERVER(TaskGroup.BAMBOO),
  DOCKER_GET_BUILDS(TaskGroup.DOCKER),
  DOCKER_GET_LABELS(TaskGroup.DOCKER),
  DOCKER_VALIDATE_ARTIFACT_SERVER(TaskGroup.DOCKER),
  DOCKER_VALIDATE_ARTIFACT_STREAM(TaskGroup.DOCKER),
  DOCKER_GET_ARTIFACT_META_INFO(TaskGroup.DOCKER),
  ECR_GET_BUILDS(TaskGroup.ECR),
  ECR_VALIDATE_ARTIFACT_SERVER(TaskGroup.ECR),
  ECR_GET_PLANS(TaskGroup.ECR),
  ECR_GET_ARTIFACT_PATHS(TaskGroup.ECR),
  ECR_VALIDATE_ARTIFACT_STREAM(TaskGroup.ECR),
  ECR_GET_LABELS(TaskGroup.ECR),
  GCR_GET_BUILDS(TaskGroup.GCR),
  GCR_VALIDATE_ARTIFACT_STREAM(TaskGroup.GCR),
  GCR_GET_PLANS(TaskGroup.GCR),
  ECR_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG, "ECR Task"),
  ACR_GET_REGISTRIES(TaskGroup.ACR),
  ACR_GET_REGISTRY_NAMES(TaskGroup.ACR),
  ACR_GET_REPOSITORIES(TaskGroup.ACR),
  ACR_GET_BUILDS(TaskGroup.ACR),
  ACR_VALIDATE_ARTIFACT_STREAM(TaskGroup.ACR),
  ACR_GET_PLANS(TaskGroup.ACR),
  ACR_GET_ARTIFACT_PATHS(TaskGroup.ACR),
  NEXUS_GET_JOBS(TaskGroup.NEXUS),
  NEXUS_GET_PLANS(TaskGroup.NEXUS),
  NEXUS_GET_ARTIFACT_PATHS(TaskGroup.NEXUS),
  NEXUS_GET_GROUP_IDS(TaskGroup.NEXUS),
  NEXUS_GET_BUILDS(TaskGroup.NEXUS),
  NEXUS_LAST_SUCCESSFUL_BUILD(TaskGroup.NEXUS),
  NEXUS_COLLECTION(TaskGroup.NEXUS),
  NEXUS_VALIDATE_ARTIFACT_SERVER(TaskGroup.NEXUS),
  NEXUS_VALIDATE_ARTIFACT_STREAM(TaskGroup.NEXUS),
  GCS_GET_ARTIFACT_PATHS(TaskGroup.GCS),
  GCS_GET_BUILDS(TaskGroup.GCS),
  GCS_GET_BUCKETS(TaskGroup.GCS),
  GCS_GET_PROJECT_ID(TaskGroup.GCS),
  GCS_GET_PLANS(TaskGroup.GCS),
  SFTP_GET_BUILDS(TaskGroup.SFTP),
  SFTP_GET_ARTIFACT_PATHS(TaskGroup.SFTP),
  SFTP_VALIDATE_ARTIFACT_SERVER(TaskGroup.SFTP),
  SMB_GET_BUILDS(TaskGroup.SMB),
  SMB_GET_SMB_PATHS(TaskGroup.SMB),
  SMB_VALIDATE_ARTIFACT_SERVER(TaskGroup.SMB),
  AMAZON_S3_COLLECTION(TaskGroup.S3),
  AMAZON_S3_GET_ARTIFACT_PATHS(TaskGroup.S3),
  AMAZON_S3_LAST_SUCCESSFUL_BUILD(TaskGroup.S3),
  AMAZON_S3_GET_BUILDS(TaskGroup.S3),
  AMAZON_S3_GET_PLANS(TaskGroup.S3),
  AZURE_ARTIFACTS_VALIDATE_ARTIFACT_SERVER(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_VALIDATE_ARTIFACT_STREAM(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_GET_BUILDS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_GET_PROJECTS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_GET_FEEDS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_GET_PACKAGES(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_COLLECTION(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_CONNECTIVITY_TEST_TASK(TaskGroup.AZURE_ARTIFACTS),
  AZURE_GET_SUBSCRIPTIONS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_MACHINE_IMAGE_GET_IMAGE_GALLERIES(TaskGroup.AZURE_ARTIFACTS),
  AZURE_MACHINE_IMAGE_GET_IMAGE_DEFINITIONS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_MACHINE_IMAGE_VALIDATE_ARTIFACT_SERVER(TaskGroup.AZURE_ARTIFACTS),
  AZURE_MACHINE_IMAGE_GET_RESOURCE_GROUPS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_MACHINE_IMAGE_GET_BUILDS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_VMSS_COMMAND_TASK(TaskGroup.AZURE_VMSS),
  AZURE_APP_SERVICE_TASK(TaskGroup.AZURE_APP_SERVICE),
  AZURE_ARM_TASK(TaskGroup.AZURE_ARM),
  AZURE_RESOURCE_TASK(TaskGroup.AZURE_RESOURCE),
  LDAP_TEST_CONN_SETTINGS(TaskGroup.LDAP),
  LDAP_TEST_USER_SETTINGS(TaskGroup.LDAP),
  LDAP_TEST_GROUP_SETTINGS(TaskGroup.LDAP),
  LDAP_VALIDATE_SETTINGS(TaskGroup.LDAP),
  LDAP_AUTHENTICATION(TaskGroup.LDAP),
  LDAP_SEARCH_GROUPS(TaskGroup.LDAP),
  LDAP_FETCH_GROUP(TaskGroup.LDAP),
  NG_LDAP_SEARCH_GROUPS(TaskGroup.LDAP),
  NG_LDAP_TEST_CONN_SETTINGS(TaskGroup.LDAP),
  APM_VALIDATE_CONNECTOR_TASK(TaskGroup.APM),
  CUSTOM_LOG_VALIDATE_CONNECTOR_TASK(TaskGroup.LOG),
  APM_GET_TASK(TaskGroup.APM),
  APPDYNAMICS_CONFIGURATION_VALIDATE_TASK(TaskGroup.APPDYNAMICS),
  CVNG_CONNECTOR_VALIDATE_TASK(TaskGroup.CVNG),
  GET_DATA_COLLECTION_RESULT(TaskGroup.CVNG),
  APPDYNAMICS_GET_APP_TASK(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_GET_APP_TASK_NG(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_GET_TIER_TASK(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_GET_TIER_TASK_NG(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_GET_TIER_MAP(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_COLLECT_METRIC_DATA(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_COLLECT_METRIC_DATA_V2(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7),
  APPDYNAMICS_METRIC_DATA_FOR_NODE(TaskGroup.APPDYNAMICS),
  INSTANA_GET_INFRA_METRICS(TaskGroup.INSTANA),
  INSTANA_GET_TRACE_METRICS(TaskGroup.INSTANA),
  INSTANA_COLLECT_METRIC_DATA(TaskGroup.INSTANA),
  INSTANA_VALIDATE_CONFIGURATION_TASK(TaskGroup.INSTANA),
  NEWRELIC_VALIDATE_CONFIGURATION_TASK(TaskGroup.NEWRELIC),
  BUGSNAG_GET_APP_TASK(TaskGroup.LOG),
  BUGSNAG_GET_RECORDS(TaskGroup.LOG),
  CUSTOM_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7),
  CUSTOM_APM_COLLECT_METRICS_V2(TaskGroup.APM),
  NEWRELIC_GET_APP_TASK(TaskGroup.NEWRELIC),
  NEWRELIC_RESOLVE_APP_TASK(TaskGroup.NEWRELIC),
  NEWRELIC_RESOLVE_APP_ID_TASK(TaskGroup.NEWRELIC),
  NEWRELIC_GET_APP_INSTANCES_TASK(TaskGroup.NEWRELIC),
  NEWRELIC_COLLECT_METRIC_DATA(TaskGroup.NEWRELIC),
  NEWRELIC_COLLECT_METRIC_DATAV2(TaskGroup.NEWRELIC),
  NEWRELIC_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7),
  NEWRELIC_GET_TXNS_WITH_DATA(TaskGroup.NEWRELIC),
  NEWRELIC_GET_TXNS_WITH_DATA_FOR_NODE(TaskGroup.NEWRELIC),
  NEWRELIC_POST_DEPLOYMENT_MARKER(TaskGroup.NEWRELIC),
  STACKDRIVER_COLLECT_METRIC_DATA(TaskGroup.STACKDRIVER),
  STACKDRIVER_METRIC_DATA_FOR_NODE(TaskGroup.STACKDRIVER),
  STACKDRIVER_LOG_DATA_FOR_NODE(TaskGroup.STACKDRIVER),
  STACKDRIVER_LIST_REGIONS(TaskGroup.STACKDRIVER),
  STACKDRIVER_LIST_FORWARDING_RULES(TaskGroup.STACKDRIVER),
  STACKDRIVER_GET_LOG_SAMPLE(TaskGroup.STACKDRIVER),
  STACKDRIVER_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7),
  STACKDRIVER_COLLECT_LOG_DATA(TaskGroup.STACKDRIVER),
  STACKDRIVER_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7),
  SPLUNK(TaskGroup.SPLUNK),
  SPLUNK_CONFIGURATION_VALIDATE_TASK(TaskGroup.SPLUNK),
  SPLUNK_GET_HOST_RECORDS(TaskGroup.SPLUNK),
  SPLUNK_NG_GET_SAVED_SEARCHES(TaskGroup.SPLUNK),
  SPLUNK_NG_VALIDATION_RESPONSE_TASK(TaskGroup.SPLUNK),
  SPLUNK_COLLECT_LOG_DATAV2(TaskGroup.SPLUNK),
  ELK_COLLECT_LOG_DATAV2(TaskGroup.ELK),
  DATA_COLLECTION_NEXT_GEN_VALIDATION(TaskGroup.APPDYNAMICS),
  SUMO_COLLECT_LOG_DATA(TaskGroup.SUMO),
  SUMO_VALIDATE_CONFIGURATION_TASK(TaskGroup.SUMO),
  SUMO_GET_HOST_RECORDS(TaskGroup.SUMO),
  SUMO_GET_LOG_DATA_BY_HOST(TaskGroup.SUMO),
  SUMO_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7),
  ELK_CONFIGURATION_VALIDATE_TASK(TaskGroup.ELK),
  ELK_COLLECT_LOG_DATA(TaskGroup.ELK),
  ELK_COLLECT_INDICES(TaskGroup.ELK),
  ELK_GET_LOG_SAMPLE(TaskGroup.ELK),
  ELK_GET_HOST_RECORDS(TaskGroup.ELK),
  KIBANA_GET_VERSION(TaskGroup.ELK),
  ELK_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7),
  LOGZ_CONFIGURATION_VALIDATE_TASK(TaskGroup.LOGZ),
  LOGZ_COLLECT_LOG_DATA(TaskGroup.LOGZ),
  LOGZ_GET_LOG_SAMPLE(TaskGroup.LOGZ),
  LOGZ_GET_HOST_RECORDS(TaskGroup.ELK),
  ARTIFACTORY_GET_BUILDS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_GET_LABELS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_GET_JOBS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_GET_PLANS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_GET_ARTIFACTORY_PATHS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_GET_GROUP_IDS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_LAST_SUCCSSFUL_BUILD(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_COLLECTION(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_VALIDATE_ARTIFACT_SERVER(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_VALIDATE_ARTIFACT_STREAM(TaskGroup.ARTIFACTORY),

  // Secret Management (Old Tasks)
  VAULT_GET_CHANGELOG(TaskGroup.KMS),
  VAULT_RENEW_TOKEN(TaskGroup.KMS),
  VAULT_LIST_ENGINES(TaskGroup.KMS),
  VAULT_APPROLE_LOGIN(TaskGroup.KMS),
  SSH_SECRET_ENGINE_AUTH(TaskGroup.KMS),
  VAULT_SIGN_PUBLIC_KEY_SSH(TaskGroup.KMS),
  SECRET_DECRYPT(TaskGroup.KMS),
  BATCH_SECRET_DECRYPT(TaskGroup.KMS),
  SECRET_DECRYPT_REF(TaskGroup.KMS),

  // Secret Management (New Tasks)
  DELETE_SECRET(TaskGroup.KMS),
  VALIDATE_SECRET_REFERENCE(TaskGroup.KMS),
  UPSERT_SECRET(TaskGroup.KMS),
  FETCH_SECRET(TaskGroup.KMS),
  ENCRYPT_SECRET(TaskGroup.KMS),
  VALIDATE_SECRET_MANAGER_CONFIGURATION(TaskGroup.KMS),
  NG_VAULT_RENEW_TOKEN(TaskGroup.KMS),
  NG_VAULT_RENEW_APP_ROLE_TOKEN(TaskGroup.KMS),
  NG_VAULT_FETCHING_TASK(TaskGroup.KMS),
  NG_AZURE_VAULT_FETCH_ENGINES(TaskGroup.KMS),

  HOST_VALIDATION(TaskGroup.HOST_VALIDATION),
  CONTAINER_ACTIVE_SERVICE_COUNTS(TaskGroup.CONTAINER),
  CONTAINER_INFO(TaskGroup.CONTAINER),
  CONTROLLER_NAMES_WITH_LABELS(TaskGroup.CONTAINER),
  AMI_GET_BUILDS(TaskGroup.AMI),
  CONTAINER_CE_VALIDATION(TaskGroup.CE),
  CE_DELEGATE_VALIDATION(TaskGroup.CE),
  CONTAINER_CONNECTION_VALIDATION(TaskGroup.CONTAINER),
  LIST_CLUSTERS(TaskGroup.CONTAINER),
  CONTAINER_VALIDATION(TaskGroup.CONTAINER),

  FETCH_MASTER_URL(TaskGroup.CONTAINER),

  DYNA_TRACE_VALIDATE_CONFIGURATION_TASK(TaskGroup.DYNA_TRACE),
  DYNA_TRACE_METRIC_DATA_COLLECTION_TASK(TaskGroup.DYNA_TRACE),
  DYNA_TRACE_GET_TXNS_WITH_DATA_FOR_NODE(TaskGroup.DYNA_TRACE),
  DYNA_TRACE_GET_SERVICES(TaskGroup.DYNA_TRACE),
  DYNATRACE_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7),
  HELM_COMMAND_TASK(TaskGroup.HELM),
  HELM_COMMAND_TASK_NG(TaskGroup.HELM, "Helm Command Task"),
  KUBERNETES_STEADY_STATE_CHECK_TASK(TaskGroup.CONTAINER),
  PCF_COMMAND_TASK(TaskGroup.PCF),
  SPOTINST_COMMAND_TASK(TaskGroup.SPOTINST),
  ECS_COMMAND_TASK(TaskGroup.AWS),
  COLLABORATION_PROVIDER_TASK(TaskGroup.COLLABORATION_PROVIDER),
  PROMETHEUS_METRIC_DATA_PER_HOST(TaskGroup.PROMETHEUS),
  CLOUD_WATCH_COLLECT_METRIC_DATA(TaskGroup.CLOUD_WATCH),
  CLOUD_WATCH_METRIC_DATA_FOR_NODE(TaskGroup.CLOUD_WATCH),
  CLOUD_WATCH_GENERIC_METRIC_STATISTICS(TaskGroup.CLOUD_WATCH),
  CLOUD_WATCH_GENERIC_METRIC_DATA(TaskGroup.CLOUD_WATCH),
  CLOUD_WATCH_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7),
  APM_METRIC_DATA_COLLECTION_TASK(TaskGroup.APM),

  APM_24_7_METRIC_DATA_COLLECTION_TASK(TaskGroup.GUARD_24x7),

  CUSTOM_LOG_COLLECTION_TASK(TaskGroup.LOG),
  CLOUD_FORMATION_TASK(TaskGroup.CLOUD_FORMATION),
  FETCH_S3_FILE_TASK(TaskGroup.AWS),

  TERRAFORM_PROVISION_TASK(TaskGroup.TERRAFORM),
  TERRAFORM_INPUT_VARIABLES_OBTAIN_TASK(TaskGroup.TERRAFORM),
  TERRAFORM_FETCH_TARGETS_TASK(TaskGroup.TERRAFORM),
  TERRAGRUNT_PROVISION_TASK(TaskGroup.TERRAGRUNT),
  KUBERNETES_SWAP_SERVICE_SELECTORS_TASK(TaskGroup.K8S),
  ECS_STEADY_STATE_CHECK_TASK(TaskGroup.CONTAINER),
  AWS_ECR_TASK(TaskGroup.AWS),
  AWS_ELB_TASK(TaskGroup.AWS),
  AWS_ECS_TASK(TaskGroup.AWS),
  AWS_IAM_TASK(TaskGroup.AWS),
  AWS_EC2_TASK(TaskGroup.AWS),
  AWS_ASG_TASK(TaskGroup.AWS),
  AWS_CODE_DEPLOY_TASK(TaskGroup.AWS),
  AWS_LAMBDA_TASK(TaskGroup.AWS),
  AWS_AMI_ASYNC_TASK(TaskGroup.AWS),
  AWS_CF_TASK(TaskGroup.AWS),
  K8S_COMMAND_TASK(TaskGroup.K8S),
  K8S_COMMAND_TASK_NG(TaskGroup.K8S_NG, "K8s Task"),
  K8S_WATCH_TASK(TaskGroup.CE),
  TRIGGER_TASK(TaskGroup.TRIGGER),
  WEBHOOK_TRIGGER_TASK(TaskGroup.TRIGGER),
  JIRA(TaskGroup.JIRA),
  CONNECTIVITY_VALIDATION(TaskGroup.CONNECTIVITY_VALIDATION),
  GIT_COMMAND(TaskGroup.GIT),
  GIT_FETCH_FILES_TASK(TaskGroup.GIT),
  GIT_FETCH_NEXT_GEN_TASK(TaskGroup.GIT, "Git Fetch Files Task"),
  BUILD_SOURCE_TASK(TaskGroup.BUILD_SOURCE),
  DOCKER_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG, "DockerHub Task"),
  GOOGLE_ARTIFACT_REGISTRY_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG, "Google Artifact Registry Task"),
  JENKINS_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG, "Jenkins Task"),
  GCR_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG, "GCR Task"),
  NEXUS_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG),
  ARTIFACTORY_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG),
  AMAZON_S3_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG),
  GITHUB_PACKAGES_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG),
  AZURE_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG, "Azure Task"),
  AMI_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG, "AMI Task"),
  AWS_ROUTE53_TASK(TaskGroup.AWS),
  SHELL_SCRIPT_APPROVAL(TaskGroup.SCRIPT),
  CUSTOM_GET_BUILDS(TaskGroup.CUSTOM),
  CUSTOM_VALIDATE_ARTIFACT_STREAM(TaskGroup.CUSTOM),
  CUSTOM_ARTIFACT_NG(TaskGroup.ARTIFACT_COLLECT_NG, "Custom Artifacts Task"),
  SHELL_SCRIPT_PROVISION_TASK(TaskGroup.SHELL_SCRIPT_PROVISION),
  SERVICENOW_ASYNC(TaskGroup.SERVICENOW),
  SERVICENOW_SYNC(TaskGroup.SERVICENOW),
  SERVICENOW_VALIDATION(TaskGroup.SERVICENOW),
  HELM_REPO_CONFIG_VALIDATION(TaskGroup.HELM_REPO_CONFIG_VALIDATION),
  HELM_VALUES_FETCH(TaskGroup.HELM_VALUES_FETCH_TASK),
  HELM_VALUES_FETCH_NG(TaskGroup.HELM_VALUES_FETCH_TASK, "Helm Values Fetch Task"),
  HELM_COLLECT_CHART(TaskGroup.HELM),
  SLACK(TaskGroup.SLACK),
  INITIALIZATION_PHASE(TaskGroup.CI),
  CI_LE_STATUS(TaskGroup.CI, null, StepStatusTaskResponseData.class, false),
  EXECUTE_COMMAND(TaskGroup.CI),
  CI_CLEANUP(TaskGroup.CI),
  CI_EXECUTE_STEP(TaskGroup.CI),
  AWS_S3_TASK(TaskGroup.AWS),
  CUSTOM_MANIFEST_VALUES_FETCH_TASK(TaskGroup.CUSTOM_MANIFEST_VALUES_FETCH_TASK),
  CUSTOM_MANIFEST_FETCH_TASK(TaskGroup.CUSTOM_MANIFEST_FETCH_TASK),

  // Add all NG tasks below this.
  GCP_TASK(TaskGroup.GCP),
  VALIDATE_KUBERNETES_CONFIG(TaskGroup.CONTAINER),
  NG_GIT_COMMAND(TaskGroup.GIT),
  NG_SSH_VALIDATION(TaskGroup.CONNECTIVITY_VALIDATION),
  NG_WINRM_VALIDATION(TaskGroup.CONNECTIVITY_VALIDATION),
  NG_HOST_CONNECTIVITY_TASK(TaskGroup.CONNECTIVITY_VALIDATION),
  DOCKER_CONNECTIVITY_TEST_TASK(TaskGroup.DOCKER),
  NG_AWS_TASK(TaskGroup.AWS),
  JIRA_TASK_NG(TaskGroup.JIRA_NG, "Jira Task"),
  BUILD_STATUS(TaskGroup.CI),
  GIT_API_TASK(TaskGroup.GIT_NG),
  AWS_CODECOMMIT_API_TASK(TaskGroup.GIT_NG),
  JIRA_CONNECTIVITY_TASK_NG(TaskGroup.JIRA_NG),
  K8_FETCH_NAMESPACES(TaskGroup.CVNG),
  K8_FETCH_WORKLOADS(TaskGroup.CVNG),
  K8_FETCH_EVENTS(TaskGroup.CVNG),
  NOTIFY_SLACK(TaskGroup.NOTIFICATION),
  NOTIFY_PAGERDUTY(TaskGroup.NOTIFICATION),
  NOTIFY_MAIL(TaskGroup.NOTIFICATION),
  NOTIFY_MICROSOFTTEAMS(TaskGroup.NOTIFICATION),
  HTTP_TASK_NG(TaskGroup.HTTP_NG, "Http Task"),
  SHELL_SCRIPT_TASK_NG(TaskGroup.SHELL_SCRIPT_NG, "Shell Script Task"),
  NG_NEXUS_TASK(TaskGroup.NEXUS),
  NG_ARTIFACTORY_TASK(TaskGroup.ARTIFACTORY),
  CE_VALIDATE_KUBERNETES_CONFIG(TaskGroup.CE),
  K8S_SERVICE_ACCOUNT_INFO(TaskGroup.CE, "Fetch Cluster ServiceAccount Info"),
  NG_AWS_CODE_COMMIT_TASK(TaskGroup.AWS),
  HTTP_HELM_CONNECTIVITY_TASK(TaskGroup.HELM_REPO_CONFIG_VALIDATION),
  NG_DECRYT_GIT_API_ACCESS_TASK(TaskGroup.GIT_NG),
  TERRAFORM_TASK_NG(TaskGroup.TERRAFORM_NG, "Terraform Task"),
  SCM_PUSH_TASK(TaskGroup.GIT, "SCM Push Task"),
  SCM_PATH_FILTER_EVALUATION_TASK(TaskGroup.GIT, "SCM Path Filter Evaluation Task"),
  SCM_GIT_REF_TASK(TaskGroup.GIT, "SCM Git Ref Task"),
  SCM_GIT_FILE_TASK(TaskGroup.GIT, "SCM Git File Task"),
  SCM_PULL_REQUEST_TASK(TaskGroup.GIT, "SCM Git PR Task"),
  SCM_GIT_WEBHOOK_TASK(TaskGroup.GIT, "SCM Git Webhook Task"),
  SERVICENOW_CONNECTIVITY_TASK_NG(TaskGroup.SERVICENOW_NG, "ServiceNow connectivity test Task"),
  SERVICENOW_TASK_NG(TaskGroup.SERVICENOW_NG, "ServiceNow Task"),
  RANCHER_RESOLVE_CLUSTERS(TaskGroup.K8S, "Rancher Resolve Clusters"),
  NG_AZURE_TASK(TaskGroup.AZURE_RESOURCE),
  CLOUDFORMATION_TASK_NG(TaskGroup.CLOUDFORMATION_NG, "Cloudformation Task"),
  ACR_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG, "ACR Task"),
  SERVERLESS_GIT_FETCH_TASK_NG(TaskGroup.SERVERLESS_NG, "Serverless Git Fetch Files Task"),
  SERVERLESS_COMMAND_TASK(TaskGroup.SERVERLESS_NG, "Serverless Task"),
  FETCH_S3_FILE_TASK_NG(TaskGroup.AWS, "Fetch S3 files Task"),

  OCI_HELM_CONNECTIVITY_TASK(TaskGroup.HELM_REPO_CONFIG_VALIDATION),
  AZURE_WEB_APP_TASK_NG(TaskGroup.AZURE, "Azure Web App Task"),
  COMMAND_TASK_NG(TaskGroup.COMMAND_TASK_NG, "Command Task"),
  VALIDATE_CUSTOM_SECRET_MANAGER_SECRET_REFERENCE(TaskGroup.COMMAND_TASK_NG),
  FETCH_CUSTOM_SECRET(TaskGroup.COMMAND_TASK_NG),
  RESOLVE_CUSTOM_SM_CONFIG(TaskGroup.COMMAND_TASK_NG),
  NG_LDAP_TEST_USER_SETTINGS(TaskGroup.LDAP),
  NG_LDAP_TEST_GROUP_SETTINGS(TaskGroup.LDAP),
  DLITE_CI_VM_INITIALIZE_TASK(TaskGroup.CI, DliteVmInitializeTaskParams.class, VmTaskExecutionResponse.class, true),
  DLITE_CI_VM_EXECUTE_TASK(TaskGroup.CI, DliteVmExecuteStepTaskParams.class, VmTaskExecutionResponse.class, true),
  DLITE_CI_VM_CLEANUP_TASK(TaskGroup.CI, DliteVmCleanupTaskParams.class, VmTaskExecutionResponse.class, true),
  NG_LDAP_GROUPS_SYNC(TaskGroup.LDAP),
  AZURE_NG_ARM(TaskGroup.AZURE_NG_ARM_BLUEPRINT, "Azure ARM"),
  NG_LDAP_TEST_AUTHENTICATION(TaskGroup.LDAP),
  ECS_GIT_FETCH_TASK_NG(TaskGroup.ECS, "ECS Git Fetch Task"),
  ECS_COMMAND_TASK_NG(TaskGroup.ECS, "ECS Command Task"),
  WIN_RM_SHELL_SCRIPT_TASK_NG(TaskGroup.SHELL_SCRIPT_NG, "Shell Script Task"),
  SHELL_SCRIPT_PROVISION(TaskGroup.SHELL_SCRIPT_PROVISION_NG, "Shell Script Provision Task"),
  ECS_GIT_FETCH_RUN_TASK_NG(TaskGroup.ECS, "ECS Git Fetch Run Task"),
  TRIGGER_AUTHENTICATION_TASK(TaskGroup.TRIGGER),
  SPOT_TASK_NG(TaskGroup.SPOTINST, "Spot NG Task"),
  FETCH_INSTANCE_SCRIPT_TASK_NG(TaskGroup.CUSTOM_DEPLOYMENT_NG, "Fetch Instance Script Task"),
  AZURE_WEB_APP_TASK_NG_V2(TaskGroup.AZURE, "Azure Web App Task V2", false),
  HELM_FETCH_CHART_VERSIONS_TASK_NG(TaskGroup.HELM, "Fetch Helm Chart Versions Task"),
  TERRAFORM_TASK_NG_V2(TaskGroup.TERRAFORM_NG, "Terraform Task NG V2"),
  ELASTIGROUP_SETUP_COMMAND_TASK_NG(TaskGroup.ELASTIGROUP, "Elastigroup Setup Command Task"),
  ELASTIGROUP_STARTUP_SCRIPT_FETCH_RUN_TASK_NG(TaskGroup.ELASTIGROUP, "Elastigroup Startup Script Fetch Run Task"),
  TERRAFORM_SECRET_CLEANUP_TASK_NG(TaskGroup.TERRAFORM_NG, "Terraform Secret Cleanup Task"),
  TERRAGRUNT_PLAN_TASK_NG(TaskGroup.TERRAGRUNT, "Terragrunt Plan Task", true),
  TERRAGRUNT_APPLY_TASK_NG(TaskGroup.TERRAGRUNT, "Terragrunt Apply Task", true),
  TERRAGRUNT_DESTROY_TASK_NG(TaskGroup.TERRAGRUNT, "Terragrunt Destroy Task", true),
  TERRAGRUNT_ROLLBACK_TASK_NG(TaskGroup.TERRAGRUNT, "Terragrunt Rollback Task", true),
  GITOPS_FETCH_APP_TASK(TaskGroup.GITOPS, "Gitops Fetch App Task"),
  VAULT_TOKEN_LOOKUP(TaskGroup.KMS, "Token lookup of token in Hashicorp Vault"),
  NG_VAULT_TOKEN_LOOKUP(TaskGroup.KMS, "Token lookup of token in Hashicorp Vault"),
  VALIDATE_TAS_CONNECTOR_TASK_NG(TaskGroup.TAS, "Tas connector validation task"),
  ECS_S3_FETCH_TASK_NG(TaskGroup.ECS, "ECS S3 Fetch Task"),
  SERVERLESS_S3_FETCH_TASK_NG(TaskGroup.SERVERLESS_NG, "Serverless S3 Fetch File Task"),
  CONTAINER_INITIALIZATION(TaskGroup.CONTAINER_PMS, "Run task container initialization"),
  AWS_ASG_CANARY_DEPLOY_TASK_NG(TaskGroup.ASG, "AWS Asg Canary Deploy"),
  ELASTIGROUP_DEPLOY(TaskGroup.ELASTIGROUP, "Elastigroup Deploy Task"),
  ELASTIGROUP_PARAMETERS_FETCH_RUN_TASK_NG(TaskGroup.ELASTIGROUP, "Elastigroup Parameters Fetch Task"),
  ELASTIGROUP_BG_STAGE_SETUP_COMMAND_TASK_NG(TaskGroup.ELASTIGROUP, "Elastigroup BG Stage Setup Command Task"),
  ELASTIGROUP_SWAP_ROUTE_COMMAND_TASK_NG(TaskGroup.ELASTIGROUP, "Elastigroup Swap Route Command Task"),
  ELASTIGROUP_ROLLBACK(TaskGroup.ELASTIGROUP, "Elastigroup Rollback Task"),
  ELASTIGROUP_PRE_FETCH_TASK_NG(TaskGroup.ELASTIGROUP, "Elastigroup Pre Fetch Task"),

  CONTAINER_LE_STATUS(TaskGroup.CONTAINER_PMS, "Get lite engine status"),
  CONTAINER_CLEANUP(TaskGroup.CONTAINER_PMS, "Cleanup container for run task"),
  CONTAINER_EXECUTE_STEP(TaskGroup.CONTAINER_PMS, "Run step on a container spawned"),
  AWS_ASG_CANARY_DELETE_TASK_NG(TaskGroup.ASG, "AWS Asg Canary Delete"),

  TAS_APP_RESIZE(TaskGroup.TAS, "Tas App resize task"),
  TAS_ROLLBACK(TaskGroup.TAS, "Tas Rollback task"),
  TAS_SWAP_ROUTES(TaskGroup.TAS, "Tas Swap Routes task"),
  TANZU_COMMAND(TaskGroup.TAS, "Tas Command task"),
  TAS_BASIC_SETUP(TaskGroup.TAS, "Tas Basic Setup task"),
  TAS_BG_SETUP(TaskGroup.TAS, "Tas BG Setup task"),
  TAS_SWAP_ROLLBACK(TaskGroup.TAS, "Tas Swap Rollback task"),
  TAS_DATA_FETCH(TaskGroup.TAS, "Tas Data Fetch task"),
  AWS_ASG_ROLLING_DEPLOY_TASK_NG(TaskGroup.ASG, "AWS Asg Rolling Deploy");

  private final TaskGroup taskGroup;
  private final String displayName;
  private final Class<? extends TaskParameters> request;
  private final Class<? extends DelegateResponseData> response;
  // Flag to denote whether the java based delegate supports this task or not
  // All unsupported tasks will be removed from the supported task types on initialization
  // of the java delegate.
  private boolean unsupported;

  TaskType(TaskGroup taskGroup) {
    this.taskGroup = taskGroup;
    this.displayName = null;
    this.request = null;
    this.response = null;
    this.unsupported = false;
  }
  TaskType(TaskGroup taskGroup, String displayName) {
    this.taskGroup = taskGroup;
    this.displayName = displayName;
    this.request = null;
    this.response = null;
    this.unsupported = false;
  }

  TaskType(TaskGroup taskGroup, String displayName, boolean unsupported) {
    this.taskGroup = taskGroup;
    this.displayName = displayName;
    this.request = null;
    this.response = null;
    this.unsupported = unsupported;
  }

  TaskType(TaskGroup taskGroup, Class<? extends TaskParameters> request, Class<? extends DelegateResponseData> response,
      boolean unsupported) {
    this.taskGroup = taskGroup;
    this.request = request;
    this.response = response;
    this.displayName = null;
    this.unsupported = unsupported;
  }

  public TaskGroup getTaskGroup() {
    return taskGroup;
  }
  public String getDisplayName() {
    return displayName != null ? displayName : name();
  }
  public Class<? extends TaskParameters> getRequest() {
    return this.request;
  }
  public Class<? extends DelegateResponseData> getResponse() {
    return this.response;
  }
  public boolean isUnsupported() {
    return this.unsupported;
  }
}
