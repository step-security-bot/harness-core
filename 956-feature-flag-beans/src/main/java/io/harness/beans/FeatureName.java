/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag.Scope;

import lombok.Getter;

/**
 * Add your feature name here. When the feature is fully launched and no longer needs to be flagged,
 * delete the feature name.
 */
@OwnedBy(HarnessTeam.PL)
public enum FeatureName {
  SPG_UI_ALLOW_ENCODING_FOR_JENKINS_ARTIFACT("Enables correct encoding for jenkins artifact", HarnessTeam.SPG),

  SPG_ALLOW_REFRESH_PIPELINE_EXECUTION_BEFORE_CONTINUE_PIPELINE("Enables refresh pipeline when trigger "
          + "continue pipeline execution",
      HarnessTeam.SPG),
  SPG_NG_GITHUB_WEBHOOK_AUTHENTICATION("Enables authentication for GitHub webhook triggers in NG", HarnessTeam.SPG),
  SPG_ALLOW_DISABLE_TRIGGERS("Allow disabling triggers at application level for CG", HarnessTeam.SPG),
  SPG_ALLOW_UI_JIRA_CUSTOM_DATETIME_FIELD("Enables backend parse custom field time of jira as the UI", HarnessTeam.SPG),
  SPG_ALLOW_DISABLE_USER_GITCONFIG(
      "Allow disabling local delegate user's .gitconfig when running git commands", HarnessTeam.SPG),
  SPG_NEW_DEPLOYMENT_FREEZE_EXCLUSIONS(
      "Flag to support deployment freeze exclusions. Depends on NEW_DEPLOYMENT_FREEZE", HarnessTeam.SPG),
  SPG_ENABLE_EMAIL_VALIDATION("Enable email validation in GraphQL approveOrRejectApprovals mutation", HarnessTeam.SPG),
  DISABLE_HELM_REPO_YAML_CACHE(
      "Enable to create a temporary folder (based on execution id) to store repository.yaml file", HarnessTeam.CDP),
  DEPRECATE_K8S_STEADY_STATE_CHECK_STEP,
  NG_GITOPS,
  ARGO_PHASE1,
  ARGO_PHASE2_MANAGED,
  ARTIFACT_PERPETUAL_TASK,
  ARTIFACT_PERPETUAL_TASK_MIGRATION,
  ARTIFACT_STREAM_DELEGATE_SCOPING,
  ARTIFACT_STREAM_DELEGATE_TIMEOUT,
  AUTO_ACCEPT_SAML_ACCOUNT_INVITES,
  AZURE_WEBAPP,
  BIND_FETCH_FILES_TASK_TO_DELEGATE,
  CCM_SUSTAINABILITY("Sustainability Feature in CCM Module", HarnessTeam.CE),
  CDNG_ENABLED,
  CENG_ENABLED("Enable the CCM module on NG", HarnessTeam.CE),
  CE_SAMPLE_DATA_GENERATION("Used to show sample data in CCM CG", HarnessTeam.CE),
  CE_HARNESS_ENTITY_MAPPING("Internal FF to decide if harness entities mapping is needed", HarnessTeam.CE),
  CE_HARNESS_INSTANCE_QUERY("Internal FF to decide which table to use for querying mapping data", HarnessTeam.CE),
  CE_GCP_CUSTOM_PRICING("Use custom pricing data for k8s gcp from billing export", HarnessTeam.CE),
  CCM_WORKLOAD_LABELS_OPTIMISATION("Use workload labels from instance data instead of k8sworkload", HarnessTeam.CE),
  CFNG_ENABLED,
  CF_CUSTOM_EXTRACTION,
  CF_ROLLBACK_CONFIG_FILTER,
  CING_ENABLED,
  CI_INDIRECT_LOG_UPLOAD,
  CI_LE_STATUS_REST_ENABLED(
      "Used for sending step status for CI via REST APIs instead of gRPC from Lite Engine to manager", HarnessTeam.CI),
  CUSTOM_DASHBOARD,
  CUSTOM_DEPLOYMENT_ARTIFACT_FROM_INSTANCE_JSON,
  CUSTOM_MAX_PAGE_SIZE,
  EXTRA_LARGE_PAGE_SIZE,
  CVNG_ENABLED,
  CV_DEMO,
  CV_HOST_SAMPLING,
  CV_SUCCEED_FOR_ANOMALY,
  DEFAULT_ARTIFACT,
  DEPLOY_TO_SPECIFIC_HOSTS,
  ENABLE_ADDING_SERVICE_VARS_TO_ECS_SPEC,
  DISABLE_LOGML_NEURAL_NET,
  DISABLE_METRIC_NAME_CURLY_BRACE_CHECK,
  DISABLE_SERVICEGUARD_LOG_ALERTS,
  DISABLE_WINRM_COMMAND_ENCODING(
      "To disable Base64 encoding done to WinRM command script which is sent to remote server for execution",
      HarnessTeam.CDP),
  ENABLE_WINRM_ENV_VARIABLES,
  FF_PIPELINE,
  FF_GITSYNC,
  NG_TEMPLATE_GITX,
  FFM_1513,
  FFM_1512,
  FFM_1827,
  FFM_1859,
  FFM_2134_FF_PIPELINES_TRIGGER,
  FFM_3938_STALE_FLAGS_ACTIVE_CARD_HIDE_SHOW,
  FFM_4117_INTEGRATE_SRM("Enable Feature Flags to send events to the SRM module", HarnessTeam.CF),
  FFM_3959_FF_MFE_Environment_Detail("Enable Feature Flag MFE Environment page", HarnessTeam.CF),
  FFM_3961_ENHANCED_ONBOARDING("Enable new onboarding experience for FeatureFlags", HarnessTeam.CF),
  WINRM_COPY_CONFIG_OPTIMIZE,
  ECS_MULTI_LBS,
  ENTITY_AUDIT_RECORD,
  EXPORT_TF_PLAN,
  GCB_CI_SYSTEM,
  GCP_WORKLOAD_IDENTITY,
  GIT_HOST_CONNECTIVITY,
  GLOBAL_COMMAND_LIBRARY,
  GLOBAL_DISABLE_HEALTH_CHECK(Scope.GLOBAL),
  GRAPHQL_DEV,
  HARNESS_TAGS,
  HELM_CHART_AS_ARTIFACT,
  HELM_STEADY_STATE_CHECK_1_16,
  HELM_CHART_NAME_SPLIT,
  HELM_MERGE_CAPABILITIES("Add helm merge capabilities", HarnessTeam.CDP),
  INLINE_SSH_COMMAND,
  LIMIT_PCF_THREADS,
  OPA_FF_GOVERNANCE,
  OPA_GIT_GOVERNANCE,
  OPA_PIPELINE_GOVERNANCE,
  PCF_OLD_APP_RESIZE,
  LOCAL_DELEGATE_CONFIG_OVERRIDE,
  LOGS_V2_247,
  MOVE_AWS_AMI_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_AMI_SPOT_INST_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_CODE_DEPLOY_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_LAMBDA_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_SSH_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_CONTAINER_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_PCF_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  PDC_PERPETUAL_TASK,
  NG_DASHBOARDS("", HarnessTeam.CE),
  NODE_RECOMMENDATION_AGGREGATE("K8S Node recommendation Feature in CCM", HarnessTeam.CE),
  ON_NEW_ARTIFACT_TRIGGER_WITH_LAST_COLLECTED_FILTER,
  OUTAGE_CV_DISABLE,
  OVERRIDE_VALUES_YAML_FROM_HELM_CHART,
  PIPELINE_GOVERNANCE,
  PRUNE_KUBERNETES_RESOURCES,
  REJECT_TRIGGER_IF_ARTIFACTS_NOT_MATCH,
  ROLLBACK_NONE_ARTIFACT,
  SEARCH_REQUEST,
  SEND_SLACK_NOTIFICATION_FROM_DELEGATE,
  SIDE_NAVIGATION,
  SKIP_SWITCH_ACCOUNT_REAUTHENTICATION,
  SLACK_APPROVALS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_AMI_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_AMI_SPOT_INST_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_CODE_DEPLOY_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_LAMBDA_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_SSH_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_PDC_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AZURE_INFRA_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_CONTAINER_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_PCF_DEPLOYMENTS,
  SUPERVISED_TS_THRESHOLD,
  THREE_PHASE_SECRET_DECRYPTION,
  TIME_RANGE_FREEZE_GOVERNANCE,
  TRIGGER_FOR_ALL_ARTIFACTS,
  TRIGGER_YAML,
  USE_NEXUS3_PRIVATE_APIS,
  ENABLE_CVNG_INTEGRATION,
  DYNATRACE_MULTI_SERVICE,
  REFACTOR_STATEMACHINEXECUTOR,
  WORKFLOW_DATA_COLLECTION_ITERATOR,
  ENABLE_CERT_VALIDATION,
  RESOURCE_CONSTRAINT_MAX_QUEUE,
  AWS_OVERRIDE_REGION,
  CLEAN_UP_OLD_MANAGER_VERSIONS(Scope.PER_ACCOUNT),
  ECS_AUTOSCALAR_REDESIGN,
  SAVE_SHELL_SCRIPT_PROVISION_OUTPUTS_TO_SWEEPING_OUTPUT,
  SAVE_TERRAFORM_OUTPUTS_TO_SWEEPING_OUTPUT,
  SAVE_TERRAFORM_APPLY_SWEEPING_OUTPUT_TO_WORKFLOW,
  NEW_DEPLOYMENT_FREEZE,
  ECS_REGISTER_TASK_DEFINITION_TAGS,
  CUSTOM_DASHBOARD_INSTANCE_FETCH_LONGER_RETENTION_DATA,
  CUSTOM_DASHBOARD_DEPLOYMENT_FETCH_LONGER_RETENTION_DATA,
  CUSTOM_DASHBOARD_ENABLE_REALTIME_INSTANCE_AGGREGATION,
  CUSTOM_DASHBOARD_ENABLE_REALTIME_DEPLOYMENT_MIGRATION,
  CUSTOM_DASHBOARD_ENABLE_CRON_INSTANCE_DATA_MIGRATION,
  CUSTOM_DASHBOARD_ENABLE_CRON_DEPLOYMENT_DATA_MIGRATION,
  WHITELIST_PUBLIC_API,
  WHITELIST_GRAPHQL,
  TIMEOUT_FAILURE_SUPPORT,
  LOG_APP_DEFAULTS,
  ENABLE_LOGIN_AUDITS,
  CUSTOM_MANIFEST,
  WEBHOOK_TRIGGER_AUTHORIZATION,
  ENHANCED_GCR_CONNECTIVITY_CHECK,
  USE_TF_CLIENT,
  GITHUB_WEBHOOK_AUTHENTICATION,
  NG_LICENSES_ENABLED,
  ECS_BG_DOWNSIZE,
  LIMITED_ACCESS_FOR_HARNESS_USER_GROUP,
  REMOVE_STENCIL_MANUAL_INTERVENTION,
  CI_OVERVIEW_PAGE("UI flag to show CI overview page", HarnessTeam.CI),
  SKIP_BASED_ON_STACK_STATUSES,
  WF_VAR_MULTI_SELECT_ALLOWED_VALUES,
  CF_CLI7,
  CF_APP_NON_VERSIONING_INACTIVE_ROLLBACK,
  CF_ALLOW_SPECIAL_CHARACTERS,
  HTTP_HEADERS_CAPABILITY_CHECK,
  AMI_IN_SERVICE_HEALTHY_WAIT,
  SETTINGS_OPTIMIZATION,
  CG_SECRET_MANAGER_DELEGATE_SELECTORS,
  ARTIFACT_COLLECTION_CONFIGURABLE,
  ROLLBACK_PROVISIONER_AFTER_PHASES,
  FEATURE_ENFORCEMENT_ENABLED,
  FREE_PLAN_ENFORCEMENT_ENABLED,
  VIEW_USAGE_ENABLED,
  SOCKET_HTTP_STATE_TIMEOUT,
  TERRAFORM_CONFIG_INSPECT_VERSION_SELECTOR,
  VALIDATE_PROVISIONER_EXPRESSION,
  WORKFLOW_PIPELINE_PERMISSION_BY_ENTITY,
  AMAZON_ECR_AUTH_REFACTOR,
  AMI_ASG_CONFIG_COPY,
  OPTIMIZED_GIT_FETCH_FILES,
  CVNG_VERIFY_STEP_DEMO,
  CVNG_MONITORED_SERVICE_DEMO,
  MANIFEST_INHERIT_FROM_CANARY_TO_PRIMARY_PHASE,
  USE_LATEST_CHARTMUSEUM_VERSION,
  NEW_KUSTOMIZE_BINARY,
  KUSTOMIZE_PATCHES_CG,
  SSH_JSCH_LOGS,
  RESOLVE_DEPLOYMENT_TAGS_BEFORE_EXECUTION,
  LDAP_USER_ID_SYNC,
  NEW_KUBECTL_VERSION,
  CUSTOM_DASHBOARD_V2, // To be used only by ui to control flow from cg dashbaords to ng
  TIME_SCALE_CG_SYNC,
  CI_INCREASE_DEFAULT_RESOURCES,
  DISABLE_DEPLOYMENTS_SEARCH_AND_LIMIT_DEPLOYMENT_STATS,
  RATE_LIMITED_TOTP,
  CLOSE_TIME_SCALE_SYNC_PROCESSING_ON_FAILURE(Scope.GLOBAL),
  RESOURCE_CENTER_ENABLED,
  USE_IMMUTABLE_DELEGATE("Use immutable delegate on download delegate from UI", HarnessTeam.DEL),
  ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS,
  TERRAFORM_AWS_CP_AUTHENTICATION,
  CI_DOCKER_INFRASTRUCTURE,
  CI_TESTTAB_NAVIGATION,
  CI_DISABLE_GIT_SAFEDIR,
  OPTIMIZED_TF_PLAN,
  SELF_SERVICE_ENABLED,
  CLOUDFORMATION_SKIP_WAIT_FOR_RESOURCES,
  CLOUDFORMATION_CHANGE_SET,
  FAIL_WORKFLOW_IF_SECRET_DECRYPTION_FAILS,
  DEPLOY_TO_INLINE_HOSTS,
  HONOR_DELEGATE_SCOPING,
  CG_LICENSE_USAGE,
  RANCHER_SUPPORT,
  BYPASS_HELM_FETCH,
  FREEZE_DURING_MIGRATION,
  USE_ANALYTIC_MONGO_FOR_GRAPHQL_QUERY,
  CCM_AS_DRY_RUN("Dry Run functionality of the AutoStopping Rules", HarnessTeam.CE),
  CCM_COMMORCH("Commitment Orchestration", HarnessTeam.CE),
  CCM_SUNSETTING_CG("Sunsetting CCM CG Features", HarnessTeam.CE),
  CCM_CURRENCY_PREFERENCES("Currency Preferences", HarnessTeam.CE),
  RECOMMENDATION_EFFICIENCY_VIEW_UI("Enable efficiency view instead cost view in Recommendation", HarnessTeam.CE),
  CCM_ENABLE_CLOUD_ASSET_GOVERNANCE_UI("Enable Cloud Asset governance UI", HarnessTeam.CE),
  DONT_RESTRICT_PARALLEL_STAGE_COUNT,
  NG_EXECUTION_INPUT,
  SKIP_ADDING_TRACK_LABEL_SELECTOR_IN_ROLLING,
  EXTERNAL_USERID_BASED_LOGIN,
  LDAP_SYNC_WITH_USERID,
  DISABLE_HARNESS_SM,
  REFACTOR_ARTIFACT_SELECTION,
  CCM_DEV_TEST("", HarnessTeam.CE),
  CV_FAIL_ON_EMPTY_NODES,
  SHOW_REFINER_FEEDBACK,
  SHOW_NG_REFINER_FEEDBACK,
  HELM_VERSION_3_8_0,
  DELEGATE_ENABLE_DYNAMIC_HANDLING_OF_REQUEST("Enable dynamic handling of task request", HarnessTeam.DEL),
  YAML_GIT_CONNECTOR_NAME,
  SSH_NG,
  ARTIFACT_STREAM_METADATA_ONLY,
  OUTCOME_GRAPHQL_WITH_INFRA_DEF,
  AUTO_REJECT_PREVIOUS_APPROVALS,
  BIND_CUSTOM_VALUE_AND_MANIFEST_FETCH_TASK,
  AZURE_BLOB_SM,
  CONSIDER_ORIGINAL_STATE_VERSION,
  SINGLE_MANIFEST_SUPPORT,
  ENV_GROUP,
  REDUCE_DELEGATE_MEMORY_SIZE("Reduce CG delegate memory to 4GB", HarnessTeam.DEL),
  PIPELINE_PER_ENV_DEPLOYMENT_PERMISSION,
  DISABLE_LOCAL_LOGIN,
  WINRM_KERBEROS_CACHE_UNIQUE_FILE,
  HIDE_ABORT,
  CUSTOM_ARTIFACT_NG,
  APPLICATION_DROPDOWN_MULTISELECT,
  NG_GIT_EXPERIENCE,
  WORKFLOW_EXECUTION_REFRESH_STATUS,
  TRIGGERS_PAGE_PAGINATION,
  STALE_FLAGS_FFM_1510,
  NG_SVC_ENV_REDESIGN,
  EARLY_ACCESS_ENABLED,
  HELP_PANEL,
  CHAOS_ENABLED,
  DEPLOYMENT_SUBFORMIK_APPLICATION_DROPDOWN,
  USAGE_SCOPE_RBAC,
  ALLOW_USER_TYPE_FIELDS_JIRA("used to hide jira userfields input in ui in both cg and ng", HarnessTeam.SPG),
  ACTIVITY_ID_BASED_TF_BASE_DIR,
  JDK11_UPGRADE_BANNER,
  DISABLE_CI_STAGE_DEL_SELECTOR,
  JENKINS_ARTIFACT,
  ENABLE_DEFAULT_TIMEFRAME_IN_DEPLOYMENTS,
  ADD_MANIFEST_COLLECTION_STEP,
  ACCOUNT_BASIC_ROLE,
  CVNG_TEMPLATE_MONITORED_SERVICE,
  CVNG_TEMPLATE_VERIFY_STEP,
  CVNG_METRIC_THRESHOLD,
  WORKFLOW_EXECUTION_ZOMBIE_MONITOR,
  USE_PAGINATED_ENCRYPT_SERVICE, // To be only used by UI for safeguarding encrypt component changes in CG
  INFRA_MAPPING_BASED_ROLLBACK_ARTIFACT,
  DEPLOYMENT_SUBFORMIK_PIPELINE_DROPDOWN,
  DEPLOYMENT_SUBFORMIK_WORKFLOW_DROPDOWN,
  TI_DOTNET,
  TG_USE_AUTO_APPROVE_FLAG,
  CVNG_SPLUNK_METRICS,
  AUTO_FREE_MODULE_LICENSE,
  SRM_LICENSE_ENABLED,
  ACCOUNT_BASIC_ROLE_ONLY,
  SEARCH_USERGROUP_BY_APPLICATION("Search in usergroup by application in CG", HarnessTeam.SPG),
  GITOPS_BYO_ARGO,
  CCM_MICRO_FRONTEND("Micro front for CCM", HarnessTeam.CE),
  CVNG_LICENSE_ENFORCEMENT,
  SRM_SLO_TOGGLE,
  SERVICE_DASHBOARD_V2,
  CDC_ENVIRONMENT_DASHBOARD_NG("New environment details dashboard is behind this", HarnessTeam.CDC),
  CDC_DASHBOARD_ENHANCEMENT_NG("New APIs added to send trend object in response for change rates", HarnessTeam.CDC),
  DEBEZIUM_ENABLED,
  TEMPLATE_SCHEMA_VALIDATION,
  YAML_APIS_GRANULAR_PERMISSION,
  AZURE_ARTIFACTS_NG,
  CD_AMI_ARTIFACTS_NG("AMI Artifact Source NG", HarnessTeam.CDC),
  GITHUB_PACKAGES,
  DO_NOT_RENEW_APPROLE_TOKEN(
      "CAUTION: USE THIS ONLY WHEN THE CUSTOMER DELEGATE IS IN VERSION HIGHER OR EQUAL TO 764xx. Used for disabling appRole token renewal and fetching token on the fly before CRUD",
      HarnessTeam.PL),
  ENABLE_DEFAULT_NG_EXPERIENCE_FOR_ONPREM,
  NG_SETTINGS("Enable Settings at various scopes in NG", HarnessTeam.PL),
  QUEUED_COUNT_FOR_QUEUEKEY("Used to display the count of the queue in CG git sync", HarnessTeam.SPG),
  NG_EMAIL_STEP,
  NG_GOOGLE_ARTIFACT_REGISTRY,
  USE_OLD_GIT_SYNC("Used for enabling old Git Experience on projects", HarnessTeam.PL),
  DISABLE_PIPELINE_SCHEMA_VALIDATION(
      "Used to disable pipeline yaml schema as We saw some intermittent issue in Schema Validation due to invalid schema generation. Will keep this FF until root cause is found and fixed.",
      HarnessTeam.PIPELINE),
  USE_K8S_API_FOR_STEADY_STATE_CHECK,
  WINRM_ASG_ROLLBACK("Used for Collect remaining instances rollback step", HarnessTeam.CDP),
  NEW_LEFT_NAVBAR_SETTINGS("Used for new left navbar configuration", HarnessTeam.PL),
  SAVE_ARTIFACT_TO_DB("Saves artifact to db and proceed in artifact collection step if not found", HarnessTeam.CDC),
  NG_INLINE_MANIFEST,
  CI_DISABLE_RESOURCE_OPTIMIZATION(
      "Used for disabling the resource optimization, AXA had asked this flag", HarnessTeam.CI),
  ENABLE_EXPERIMENTAL_STEP_FAILURE_STRATEGIES(
      "Used to enable rollback workflow strategy on step failure", HarnessTeam.SPG),
  REMOVE_USERGROUP_CHECK(
      "Customers started facing NPE due to migration of usergroup reference, removed null check behind FF - ticket ID - CDS-39770, CG",
      HarnessTeam.SPG),

  STO_STEP_PALETTE_V1("Enable first iteration of individual steps for STO", HarnessTeam.STO),
  STO_STEP_PALETTE_V2("Enable second iteration of individual steps for STO", HarnessTeam.STO),
  HOSTED_BUILDS("Used to enabled Hosted builds in paid accounts", HarnessTeam.CI),
  CD_ONBOARDING_ENABLED,
  SPOT_ELASTIGROUP_NG("Enables Spot Elastigroup implementation on NG", HarnessTeam.CDP),
  ATTRIBUTE_TYPE_ACL_ENABLED("Enable attribute filter on NG UI for ACL", HarnessTeam.PL),
  CREATE_DEFAULT_PROJECT("Enables auto create default project after user signup", HarnessTeam.GTM),
  ANALYSE_TF_PLAN_SUMMARY(
      "Enables parsing of the Terraform plan/apply/destroy summary [add/change/destroy] and exposing them as expressions",
      HarnessTeam.CDP),
  TERRAFORM_REMOTE_BACKEND_CONFIG("Enables storing Terraform backend configuration in a remote repo", HarnessTeam.CDP),
  REMOVE_HINT_YAML_GIT_COMMITS("Removes the hint usage in GitCommits collection", HarnessTeam.SPG),
  FIXED_INSTANCE_ZERO_ALLOW("To allow user to set the fixed instance count to 0 for ECS Deployments", HarnessTeam.CDP),
  ON_DEMAND_ROLLBACK_WITH_DIFFERENT_ARTIFACT(
      "Used to do on demand rollback to previously deployed different artifact on same inframapping", HarnessTeam.CDC),
  CG_GIT_POLLING("Poll git based on account config for git sync in CG.", HarnessTeam.SPG),
  GRAPHQL_WORKFLOW_EXECUTION_OPTIMIZATION(
      "Making multiple optimizations for workflow execution graphql in CG", HarnessTeam.SPG),
  NG_ENABLE_LDAP_CHECK("Enables NG Ldap in NG-UI", HarnessTeam.PL),
  CUSTOM_SECRET_MANAGER_NG("Enable Custom Secret Manager in NG", HarnessTeam.PL),
  CV_AWS_PROMETHEUS("Enable AWS Prometheus for CV State", HarnessTeam.CV),
  CD_GIT_WEBHOOK_POLLING("Used to poll git webhook recent delivery events", HarnessTeam.CDP),
  MULTI_SERVICE_INFRA("Enable multiple service/environment support in NG", HarnessTeam.CDP),
  CD_TRIGGERS_REFACTOR("Enable NG Triggers UI refactoring", HarnessTeam.CDP),
  SORT_ARTIFACTS_IN_UPDATED_ORDER("Sort the collected artifacts by lastUpdatedAt", HarnessTeam.SPG),
  ENABLE_CHECK_STATE_EXECUTION_STARTING(
      "Used to allow create retry state execution when event is status equals to STARTING", HarnessTeam.SPG),
  CI_TI_DASHBOARDS_ENABLED,
  SERVICE_ID_FILTER_FOR_TRIGGERS(
      "Filter last deployed artifacts for triggers using serviceId as well", HarnessTeam.SPG),
  PERSIST_MONITORED_SERVICE_TEMPLATE_STEP(
      "Enables saving of monitored service created during template verify step", HarnessTeam.CV),
  VALIDATE_PHASES_AND_ROLLBACK("Validate that each phase has your own rollback phase", HarnessTeam.SPG),
  CIE_HOSTED_VMS(
      "Enables hosted VMs in favor of hosted K8s for CIE. This flag will be deprecated once all the feature work has been checked in",
      HarnessTeam.CI),
  CHANGE_INSTANCE_QUERY_OPERATOR_TO_NE("Change instance service query operator from $exists to $ne", HarnessTeam.SPG),
  NEXUS3_RAW_REPOSITORY("Enable support for Nexus3 raw repository format on CG", HarnessTeam.SPG),
  CD_TRIGGER_V2("Enable support for nexus3, nexus2, azure, ami trigger", HarnessTeam.CDC),
  NG_ARTIFACT_SOURCES("Flag to support multi artifact sources for service V2", HarnessTeam.CDC),
  UPDATE_EMAILS_VIA_SCIM("Will enable updating emails in Harness via SCIM", HarnessTeam.PL),
  ELK_HEALTH_SOURCE("Will enable ELK health source in SRM", HarnessTeam.CV),
  SRM_COMPOSITE_SLO("Flag to start creating composite SLOs", HarnessTeam.CV),

  PIPELINE_CHAINING("UI flag to enable/disable Pipeline Chaining feature", HarnessTeam.PIPELINE),
  PIPELINE_ROLLBACK("Flag to enable/disable Pipeline Rollback", HarnessTeam.PIPELINE),
  SPG_DISABLE_EXPIRING_TO_MANUAL_INTERVENTION_CANDIDATE(
      "Disable the expiration of stateExecutionInstances candidates of manual intervention", HarnessTeam.SPG),
  MERGE_RUNTIME_VARIABLES_IN_RESUME(
      "merge context elements workflow variables while resuming pipeline from a stage", HarnessTeam.SPG),
  USE_TEXT_SEARCH_FOR_EXECUTION(
      "With this instead of using regex search we will use text search for CD page in CG", HarnessTeam.SPG),
  AZURE_WEBAPP_NG_JENKINS_ARTIFACTS(
      "FF to enable Jenkins as an artifact source option for Azure Web App NG", HarnessTeam.CDP),
  AZURE_WEBAPP_NG_AZURE_DEVOPS_ARTIFACTS(
      "FF to enable Azure DevOps Artifacts as an artifact source option for Azure Web App NG", HarnessTeam.CDP),
  DEL_EVALUATE_SECRET_EXPRESSION_SYNC(
      "FF to disable asynchronous evaluation of secrets. With FF set to true and pipeline has more than one secret, they will get evaluate one after other",
      HarnessTeam.DEL),
  SRM_ENABLE_HEALTHSOURCE_CLOUDWATCH_METRICS("UI FF to enable CloudWatch Metrics healthsource", HarnessTeam.CV),
  SRM_ENABLE_VERIFY_STEP_LONG_DURATION("Enable longer duration for verify step", HarnessTeam.CV),
  SETTING_ATTRIBUTES_SERVICE_ACCOUNT_TOKEN_MIGRATION("Migrate erroneous service account tokens", HarnessTeam.PL),
  ARTIFACT_SOURCE_TEMPLATE("Flag to add support for artifact source templates", HarnessTeam.CDC),
  LOOKER_ENTITY_RECONCILIATION,
  STAGE_AND_STEP_LEVEL_DEPLOYMENT_DETAILS,
  NG_DEPLOYMENT_FREEZE("Enables Deployment freeze for NG", HarnessTeam.CDC),
  NG_DEPLOYMENT_FREEZE_OVERRIDE("Override freeze for NG", HarnessTeam.CDC),
  NEW_EXECUTION_LIST_VIEW(
      "Enables the new UX for Executions list view for Pipelines and Projects", HarnessTeam.PIPELINE),
  SPG_FIX_APPROVAL_WAITING_FOR_INPUTS(
      "Fixes a bug where approval step is going to waiting for inputs state", HarnessTeam.SPG),
  PL_ACCESS_SECRET_DYNAMICALLY_BY_PATH(
      "For NG, it enables to read secrets from HashiCorp Vault directly using an fully qualified path expression without the need of creating secret in Harness.",
      HarnessTeam.PL),
  PL_NO_EMAIL_FOR_SAML_ACCOUNT_INVITES("No email for users in account where SAML auth is enabled", HarnessTeam.PL),
  PL_ENABLE_GOOGLE_SECRET_MANAGER_IN_NG("Enable Google Secret Manager in NG", HarnessTeam.PL),
  SPG_2K_DEFAULT_PAGE_SIZE("Increase the default page size to 2000 elements in CG", HarnessTeam.SPG),
  SPG_DISABLE_SEARCH_DEPLOYMENTS_PAGE("Disable search on deployment page in CG.", HarnessTeam.SPG),
  WINRM_SCRIPT_COMMAND_SPLIT(
      "Enables the new way of how to copy powershell/winrm script commands content to file on remote. (Copy is done in chunks of 4KB) ",
      HarnessTeam.CDP),
  SPG_USE_NEW_METADATA("To use new metadata endpoint for jira server version greater than 9.0", HarnessTeam.SPG),
  SPG_OPTIMIZE_WORKFLOW_EXECUTIONS_LISTING(
      "Make the workflowExecutions listing better providing appId for children ids", HarnessTeam.SPG),
  SPG_OPTIMIZE_CONCILIATION_QUERY(
      "Change the conciliation query from using notExists to use equal null", HarnessTeam.SPG),
  CD_TRIGGER_CATALOG("Enables UI for Trigger catalog for Nexus ", HarnessTeam.CDC),
  SRM_HOST_SAMPLING_ENABLE("Enables Host Sampling feature for Learning Engine.", HarnessTeam.CV),
  SRM_LOG_HOST_SAMPLING_ENABLE("Enables Host Sampling for log for Learning Engine.", HarnessTeam.CV),
  CDS_SHOW_CREATE_PR("Start showing CreatePR step on the plan creator if enabled", HarnessTeam.GITOPS),
  SPG_PIPELINE_ROLLBACK("Enables pipeline rollback on failure option", HarnessTeam.SPG),
  PL_FORCE_DELETE_CONNECTOR_SECRET(
      "Enables force delete of connectors and secrets irrespective of existing references.", HarnessTeam.PL),
  PL_CONNECTOR_ENCRYPTION_PRIVILEGED_CALL("make the encryption/decryption call as pirvileged call", HarnessTeam.PL),
  SPG_DASHBOARD_STATS_OPTIMIZE_DEPLOYMENTS(
      "Dashboard stats slow api call optimization. Also solves MOM issue in CG.", HarnessTeam.SPG),
  SPG_DASHBOARD_STATS_OPTIMIZE_ACTIVE_SERVICES(
      "Active services slow api call optimization. Also solves MOM issue in CG.", HarnessTeam.SPG),
  SPG_LIVE_DASHBOARD_STATS_DEBUGGING("Live debugging for dashboard stats in CG", HarnessTeam.SPG),
  TI_MFE_ENABLED("Migrating TI UI to Microfrontend. This feature flag is needed to test/control the new architecture",
      HarnessTeam.CI),
  CI_CACHE_INTELLIGENCE("Feature flag for cache intelligence feature", HarnessTeam.CI),
  SPG_ENFORCE_TIME_RANGE_DEPLOYMENTS_WITHOUT_APP_ID(
      "This feature flag enforces maximum time range for workflow execution queries without appId", HarnessTeam.SPG),
  SPG_REDUCE_KEYWORDS_PERSISTENCE_ON_EXECUTIONS(
      "Gradually reducing the amount of keywords being stored on workflow executions", HarnessTeam.SPG),
  SPG_CG_END_OF_LIFE_BANNER(
      "Shows the user a banner notifying about the End of Life of CG CD new features", HarnessTeam.SPG),
  SYNC_GIT_CLONE_AND_COPY_TO_DEST_DIR(
      "This feature flag helps in synchronizing the git clone of repo and copying the files then to destination directory",
      HarnessTeam.CDP),
  ECS_ROLLBACK_MAX_DESIRED_COUNT("Changes ECS Rollback Desired Count to Max(OldService, NewService)", HarnessTeam.CDP),
  CI_YAML_VERSIONING("Feature flag for yaml simplification", HarnessTeam.CI),
  SRM_ET_EXPERIMENTAL("Feature flag for SRM only Error Tracking development", HarnessTeam.CV),
  SRM_ET_RESOLVED_EVENTS("Feature flag for Error Tracking resolved events", HarnessTeam.CV),
  SRM_CODE_ERROR_NOTIFICATIONS("Feature flag for Code Error notification condition", HarnessTeam.CV),
  SRM_ENABLE_HEALTHSOURCE_AWS_PROMETHEUS("UI FF to enable AWS Managed Prometheus healthsource", HarnessTeam.CV),
  DEL_SECRET_EVALUATION_VERBOSE_LOGGING(
      "FF to enable verbose logging for the manager process when secret gets evaluated", HarnessTeam.DEL),
  CI_MFE_ENABLED("Feature flag is needed to test/control the microfrontend architecture for CI UI", HarnessTeam.CI),
  INSTANCE_SYNC_V2_CG("Enable Instance Sync V2 framework in CG for direct K8s cloud provider", HarnessTeam.CDP),
  CF_ROLLBACK_CUSTOM_STACK_NAME(
      "Use custom stack name and region to find lates successful couldformation rollback data", HarnessTeam.CDP),
  IACM_ENABLED("Enable support for IACM micro front end capabilities", HarnessTeam.IACM),
  AZURE_WEB_APP_NG_NEXUS_PACKAGE("Enable support for Nexus package artifact in Azure Web App NG", HarnessTeam.CDP),
  CODE_ENABLED("Enable Harness Code", HarnessTeam.CODE),
  BOOKING_RECOMMENDATIONS("Feature flag for booking.com recommendations", HarnessTeam.CE),
  USE_GET_FILE_V2_GIT_CALL(
      "FF for customers on updated delegate to use GetFileV2 call which is more performance efficient",
      HarnessTeam.PIPELINE),
  SPG_CD_RUN_STEP("CD run step in NG", HarnessTeam.SPG),
  GITOPS_ONPREM_ENABLED("Enable the gitops tab in the UI in case of ONPREM/SMP", HarnessTeam.GITOPS),
  CIE_HOSTED_VMS_MAC("FF for enabling hosted builds for mac os", HarnessTeam.CI),
  SPG_DELETE_ENVIRONMENTS_ON_SERVICE_RENAME_GIT_SYNC(
      "On service rename delete stale folders inside environments folders.", HarnessTeam.SPG),
  SPG_DASHBOARD_PROJECTION("Enables projection on CG Dashboard", HarnessTeam.SPG),
  GITOPS_API_PARAMS_MERGE_PR("Show API parameters in Merge PR step", HarnessTeam.GITOPS),
  PL_HIDE_LAUNCH_NEXTGEN("FF to hide Launch NextGen button", HarnessTeam.PL),
  PL_LDAP_PARALLEL_GROUP_SYNC(
      "Enables User Group sync operation to fetch data from Ldap Server in Parallel. Enable only if Ldap Server can take the load",
      HarnessTeam.PL),
  CDS_TAS_NG("FF for enabling TAS deployment in NG", HarnessTeam.CDP),
  SPG_WFE_OPTIMIZE_UPDATE_PIPELINE_ESTIMATES("Optimization for updatePipelineEstimates method", HarnessTeam.SPG),
  CDS_OrgAccountLevelServiceEnvEnvGroup(
      "Support Creation and Use of Org and Account level Services and Environments", HarnessTeam.CDC),
  CE_NET_AMORTISED_COST_ENABLED("Enable cost calculation through Net Amortised cost", HarnessTeam.CE),
  GITOPS_DR_ENABLED("Enable disaster recovery feature", HarnessTeam.GITOPS),
  GITOPS_RECONCILER_ENABLED("Enable reconcile processing", HarnessTeam.GITOPS),
  CE_RERUN_HOURLY_JOBS("Rerunning Hourly billing jobs", HarnessTeam.CE),
  CCM_MONTHLY_BUDGET_BREAKDOWN("Use monthly breakdown feature in Yearly Period Budget", HarnessTeam.CE),
  SPG_WFE_OPTIMIZE_WORKFLOW_LISTING("Optimizes the wfe fetching from wf page.", HarnessTeam.SPG),
  SPG_INSTANCE_OPTIMIZE_DELETED_APPS("Optimizes the instance deleted apps fetching.", HarnessTeam.SPG),
  SPG_OPTIMIZE_PIPELINE_QUERY_ON_AUTH("Optimizes auth on pipelines making the query more efficient.", HarnessTeam.SPG),
  GITOPS_FETCH_LINKED_APPS("Fetch Linked Apps Step and new Manifest in GitOps", HarnessTeam.GITOPS),
  SRM_SUMO("Will enable Sumologic health source in SRM", HarnessTeam.CV),
  SPG_SAVE_REJECTED_BY_FREEZE_WINDOWS(
      "Flag that enables populating WorkflowExecution with ids of freeze windows that rejected the execution",
      HarnessTeam.SPG),
  SPG_INSTANCE_ENABLE_HINT_ON_GET_INSTANCES(
      "Enable db hint on getInstancesForAccount for performance stability", HarnessTeam.SPG),
  TERRAGRUNT_PROVISION_NG("FF to enable Terragrunt provision in NG", HarnessTeam.CDP),
  LANDING_OVERVIEW_PAGE_V2("Supports new entities for landing overview page", HarnessTeam.SPG),
  CDS_STEPGROUP_TEMPLATE("Added support for Step group templates", HarnessTeam.CDC),
  CDS_FILTER_INFRA_CLUSTERS_ON_TAGS(
      "For supporting filtering of infras and gitOps clusters based on tags", HarnessTeam.CDC),
  CDS_TERRAFORM_S3_SUPPORT(
      "Enable support for AWS S3 bucket and URIs for Terraform Source, tfVars and Backend Config", HarnessTeam.CDP),
  CCM_BUDGET_CASCADES("Enable to allow nested budgets for Financial Management", HarnessTeam.CE),
  PIE_NG_GITX_CACHING("FF to enable caching on new git experience", HarnessTeam.PIPELINE),
  INSTANT_DELEGATE_DOWN_ALERT("FF to instantly alert when delegates are down", HarnessTeam.SPG),
  QUEUE_CI_EXECUTIONS("FF to enable queueing in CI builds", HarnessTeam.CI),
  WINRM_SCRIPT_COMMAND_SPLIT_NG(
      "Enables the new way of how to copy powershell/winrm script commands content to file on remote. (Copy is done in chunks of 6KB) ",
      HarnessTeam.CDP),
  DISABLE_WINRM_COMMAND_ENCODING_NG(
      "To disable Base64 encoding done to WinRM command script which is sent to remote server for execution",
      HarnessTeam.CDP),
  PURGE_DANGLING_APP_ENV_REFS("Explicitly purge dangling references of app/env", HarnessTeam.SPG),
  SPG_FETCH_ARTIFACT_FROM_DB("Fetch artifact from database if available in artifact collection step", HarnessTeam.SPG),
  PL_SUPPORT_JWT_TOKEN_SCIM_API("Enable support for external OAuth JWT token for SCIM API calls", HarnessTeam.PL),
  CCM_INSTANCE_DATA_CLUSTERID_FILTER("Query from instanceData collection based on clusterId", HarnessTeam.CE),
  CDC_SEND_NOTIFICATION_FOR_FREEZE("Send notifications for deployment freeze", HarnessTeam.CDC),
  DISABLE_CDS_SERVICE_ENV_SCHEMA_VALIDATION(
      "Used to disable Schema validation for Service Env for new service env redesign", HarnessTeam.CDC),
  DO_NOT_LOAD_SUPPORT_ACCOUNTS_UNLESS_REQUIRED(
      "Avoid loading unnecessary support accounts unnecessarily. Enabling it will reduce memory pressure",
      HarnessTeam.PL),
  PL_SIMPLIFY_ACL_CHECK("Evaluate access using role assignments instead of ACL's.", HarnessTeam.PL),
  ASG_NG("Supports Amazon ASG in NG", HarnessTeam.CDP),
  CDP_UPDATE_INSTANCE_DETAILS_WITH_IMAGE_SUFFIX("Update instance details if image suffix matches", HarnessTeam.CDP),
  SRM_INTERNAL_CHANGE_SOURCE_FF("Control Feature Flag Internal Change Source On UI", HarnessTeam.CV),
  CD_NG_DYNAMIC_PROVISIONING_ENV_V2(
      "Enable dynamic provisioning support in v2 environment. Epic: CDS-39606", HarnessTeam.CDC),
  CDS_ARTIFACTORY_REPOSITORY_URL_MANDATORY(
      "FF to make the Artifactory Repository Url as mandatory in case of docker repositoryFormat", HarnessTeam.CDC),
  VALIDATE_SERVICE_NAME_IN_FILE_PATH("Validate the service name in yaml file path", HarnessTeam.SPG),
  NG_K8_COMMAND_FLAGS("Added Support for adding Command flags to K8s commands. PM Rohan", HarnessTeam.CDP),
  CDP_PUBLISH_INSTANCE_STATS_FOR_ENV_NG(
      "Publish instance stats at environment granularity in NG. PM Rohan", HarnessTeam.CDP);

  @Deprecated
  FeatureName() {
    scope = Scope.PER_ACCOUNT;
  }

  @Deprecated
  FeatureName(Scope scope) {
    this.scope = scope;
  }

  @Getter private FeatureFlag.Scope scope;

  FeatureName(String description, HarnessTeam owner) {
    this.description = description;
    this.owner = owner;
    this.scope = Scope.PER_ACCOUNT;
  }

  FeatureName(String description, HarnessTeam owner, FeatureFlag.Scope scope) {
    this.description = description;
    this.owner = owner;
    this.scope = scope;
  }

  @Getter private String description;
  private HarnessTeam owner;

  public String getOwner() {
    return owner.name();
  }
}
