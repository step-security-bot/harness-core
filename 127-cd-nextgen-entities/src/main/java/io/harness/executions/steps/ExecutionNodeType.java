/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.executions.steps;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.visitor.YamlTypes;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_ECS, HarnessModuleComponent.CDS_GITOPS,
        HarnessModuleComponent.CDS_INFRA_PROVISIONERS})
@OwnedBy(HarnessTeam.CDP)
public enum ExecutionNodeType {
  GITOPS_MERGE_PR("GITOPS_MERGE_PR", StepSpecTypeConstants.GITOPS_MERGE_PR),
  GITOPS_REVERT_PR("GITOPS_REVERT_PR", StepSpecTypeConstants.GITOPS_REVERT_PR),
  GITOPS_SYNC("GITOPS_SYNC", StepSpecTypeConstants.GITOPS_SYNC),
  UPDATE_GITOPS_APP("UPDATE_GITOPS_APP", StepSpecTypeConstants.UPDATE_GITOPS_APP),
  SERVICE("SERVICE", YamlTypes.SERVICE_ENTITY),
  SERVICE_V2("SERVICE_V2", YamlTypes.SERVICE_ENTITY),
  SERVICE_V3("SERVICE_V3", YamlTypes.SERVICE_ENTITY),
  ENVIRONMENT("ENVIRONMENT", YamlTypes.ENVIRONMENT_YAML),
  ENVIRONMENT_V2("ENVIRONMENT_V2", YamlTypes.ENVIRONMENT_YAML),
  SERVICE_CONFIG("SERVICE_CONFIG", YamlTypes.SERVICE_CONFIG),
  SERVICE_SECTION("SERVICE_SECTION", YamlTypes.SERVICE_SECTION),
  SERVICE_DEFINITION("SERVICE_DEFINITION", YamlTypes.SERVICE_DEFINITION),
  SERVICE_SPEC("SERVICE_SPEC", YamlTypes.SERVICE_SPEC),
  ARTIFACTS("ARTIFACTS", "artifacts"),
  ARTIFACTS_V2("ARTIFACTS_V2", "artifacts"),
  ARTIFACT("ARTIFACT", "artifact"),
  ARTIFACT_SYNC("ARTIFACT_SYNC", "artifact"),
  SIDECARS("SIDECARS", "sidecars"),
  MANIFESTS("MANIFESTS", "manifests"),
  MANIFESTS_V2("MANIFESTS_V2", "manifests"),
  MANIFEST_FETCH("MANIFEST_FETCH", YamlTypes.MANIFEST_LIST_CONFIG),
  MANIFEST("MANIFEST", YamlTypes.MANIFEST_CONFIG),

  PIPELINE_SETUP("PIPELINE_SETUP", "pipeline"),
  INFRASTRUCTURE_SECTION("INFRASTRUCTURE_SECTION", YamlTypes.PIPELINE_INFRASTRUCTURE),
  INFRASTRUCTURE("INFRASTRUCTURE", YamlTypes.INFRASTRUCTURE_DEF),
  INFRASTRUCTURE_V2("INFRASTRUCTURE_V2", YamlTypes.INFRASTRUCTURE_DEF),
  INFRASTRUCTURE_TASKSTEP_V2("INFRASTRUCTURE_TASKSTEP_V2", YamlTypes.INFRASTRUCTURE_DEF),
  GITOPS_CLUSTERS("GITOPS CLUSTERS", YamlTypes.GITOPS_CLUSTERS),
  DEPLOYMENT_STAGE_STEP("DEPLOYMENT_STAGE_STEP", "deployment"),
  K8S_ROLLING("K8S_ROLLING", YamlTypes.K8S_ROLLING_DEPLOY),
  K8S_ROLLBACK_ROLLING("K8S_ROLLBACK_ROLLING", YamlTypes.K8S_ROLLING_ROLLBACK),
  K8S_ROLLBACK_ROLLING_V2("K8S_ROLLBACK_ROLLING_V2", YamlTypes.K8S_ROLLING_ROLLBACK),

  K8S_BLUE_GREEN("K8S_BLUE_GREEN", YamlTypes.K8S_BLUE_GREEN_DEPLOY),
  K8S_BLUE_GREEN_V2("K8S_BLUE_GREEN_V2", YamlTypes.K8S_BLUE_GREEN_DEPLOY),
  K8S_APPLY("K8S_APPLY", YamlTypes.K8S_APPLY),
  K8S_SCALE("K8S_SCALE", YamlTypes.K8S_SCALE),
  K8S_SCALE_V2("K8S_SCALE_V2", YamlTypes.K8S_SCALE),
  K8S_CANARY("K8S_CANARY", YamlTypes.K8S_CANARY_DEPLOY),
  K8S_CANARY_V2("K8S_CANARY_V2", YamlTypes.K8S_CANARY_DEPLOY),
  K8S_BG_SWAP_SERVICES("K8S_BG_SWAP_SERVICES", YamlTypes.K8S_BG_SWAP_SERVICES),
  K8S_BG_SWAP_SERVICES_V2("K8S_BG_SWAP_SERVICES_V2", YamlTypes.K8S_BG_SWAP_SERVICES),
  K8S_DELETE("K8S_DELETE", YamlTypes.K8S_DELETE),
  K8S_CANARY_DELETE("K8S_CANARY_DELETE", YamlTypes.K8S_CANARY_DELETE),
  K8S_CANARY_DELETE_V2("K8S_CANARY_DELETE_V2", YamlTypes.K8S_CANARY_DELETE),
  K8S_DRY_RUN_MANIFEST("K8S_DRY_RUN", YamlTypes.K8S_DRY_RUN_MANIFEST),
  INFRASTRUCTURE_PROVISIONER_STEP("INFRASTRUCTURE_PROVISIONER_STEP", "infraProvisionerStep"),
  CD_EXECUTION_STEP("CD_EXECUTION_STEP", "cdExecutionStep"),
  CD_STEPS_STEP("CD_EXECUTION_STEP", "cdExecutionStep"),
  INFRASTRUCTURE_DEFINITION_STEP("INFRASTRUCTURE_DEFINITION_STEP", "infraDefStep"),
  EXECUTION_ROLLBACK_STEP("EXECUTION_ROLLBACK_STEP", "executionRollbackStep"),
  ROLLBACK_SECTION("ROLLBACK_SECTION", "rollbackSection"),
  GENERIC_SECTION("GENERIC_SECTION", "genericSection"),
  TERRAFORM_APPLY("TERRAFORM_APPLY", StepSpecTypeConstants.TERRAFORM_APPLY),
  TERRAFORM_PLAN("TERRAFORM_PLAN", StepSpecTypeConstants.TERRAFORM_PLAN),
  TERRAFORM_DESTROY("TERRAFORM_DESTROY", StepSpecTypeConstants.TERRAFORM_DESTROY),
  TERRAFORM_ROLLBACK("TERRAFORM_ROLLBACK", StepSpecTypeConstants.TERRAFORM_ROLLBACK),
  TERRAFORM_PLAN_V2("TERRAFORM_PLAN_V2", StepSpecTypeConstants.TERRAFORM_PLAN),
  TERRAFORM_APPLY_V2("TERRAFORM_APPLY_V2", StepSpecTypeConstants.TERRAFORM_APPLY),
  TERRAFORM_DESTROY_V2("TERRAFORM_DESTROY_V2", StepSpecTypeConstants.TERRAFORM_DESTROY),
  TERRAFORM_ROLLBACK_V2("TERRAFORM_ROLLBACK_V2", StepSpecTypeConstants.TERRAFORM_ROLLBACK),
  HELM_DEPLOY("HELM_DEPLOY", YamlTypes.HELM_DEPLOY),
  HELM_ROLLBACK("HELM_ROLLBACK", YamlTypes.HELM_ROLLBACK),
  CLOUDFORMATION_CREATE_STACK("CLOUDFORMATION_CREATE_STACK", StepSpecTypeConstants.CLOUDFORMATION_CREATE_STACK),
  CLOUDFORMATION_DELETE_STACK("CLOUDFORMATION_DELETE_STACK", StepSpecTypeConstants.CLOUDFORMATION_DELETE_STACK),
  CLOUDFORMATION_ROLLBACK_STACK("CLOUDFORMATION_ROLLBACK_STACK", StepSpecTypeConstants.CLOUDFORMATION_ROLLBACK_STACK),
  SERVERLESS_AWS_LAMBDA_DEPLOY("SERVERLESS_AWS_LAMBDA_DEPLOY", YamlTypes.SERVERLESS_AWS_LAMBDA_DEPLOY),
  SERVERLESS_AWS_LAMBDA_ROLLBACK("SERVERLESS_AWS_LAMBDA_ROLLBACK", YamlTypes.SERVERLESS_AWS_LAMBDA_ROLLBACK),

  CONFIG_FILE("CONFIG_FILE", YamlTypes.CONFIG_FILE),
  CONFIG_FILES("CONFIG_FILES", YamlTypes.CONFIG_FILES),
  CONFIG_FILES_V2("CONFIG_FILES_V2", YamlTypes.CONFIG_FILES),

  COMMAND("COMMAND", YamlTypes.COMMAND),
  AZURE_SLOT_DEPLOYMENT("AZURE_SLOT_DEPLOYMENT", YamlTypes.AZURE_SLOT_DEPLOYMENT),
  AZURE_TRAFFIC_SHIFT("AZURE_TRAFFIC_SHIFT", YamlTypes.AZURE_TRAFFIC_SHIFT),
  AZURE_SWAP_SLOT("AZURE_SWAP_SLOT", YamlTypes.AZURE_SWAP_SLOT),
  AZURE_WEBAPP_ROLLBACK("AZURE_WEBAPP_ROLLBACK", YamlTypes.AZURE_WEBAPP_ROLLBACK),
  JENKINS_BUILD("JENKINS_BUILD", StepSpecTypeConstants.JENKINS_BUILD),
  BAMBOO_BUILD("BAMBOO_BUILD", StepSpecTypeConstants.BAMBOO_BUILD),
  STARTUP_COMMAND("STARTUP_COMMAND", YamlTypes.STARTUP_COMMAND),
  AZURE_SERVICE_SETTINGS_STEP("AZURE_SERVICE_SETTINGS_STEP", YamlTypes.AZURE_SERVICE_SETTINGS_STEP),
  ELASTIGROUP_SERVICE_SETTINGS_STEP("ELASTIGROUP_SERVICE_SETTINGS_STEP", YamlTypes.ELASTIGROUP_SERVICE_SETTINGS_STEP),
  APPLICATION_SETTINGS("APPLICATION_SETTINGS", YamlTypes.APPLICATION_SETTINGS),
  CONNECTION_STRINGS("CONNECTION_STRINGS", YamlTypes.CONNECTION_STRINGS),
  ECS_ROLLING_DEPLOY("ECS_ROLLING_DEPLOY", YamlTypes.ECS_ROLLING_DEPLOY),
  ECS_ROLLING_ROLLBACK("ECS_ROLLING_ROLLBACK", YamlTypes.ECS_ROLLING_ROLLBACK),
  ECS_CANARY_DEPLOY("ECS_CANARY_DEPLOY", YamlTypes.ECS_CANARY_DEPLOY),
  ECS_CANARY_DELETE("ECS_CANARY_DELETE", YamlTypes.ECS_CANARY_DELETE),
  AZURE_CREATE_ARM_RESOURCE("AZURE_CREATE_ARM_RESOURCE", StepSpecTypeConstants.AZURE_CREATE_ARM_RESOURCE),
  AZURE_CREATE_BP_RESOURCE("AZURE_CREATE_BP_RESOURCE", StepSpecTypeConstants.AZURE_CREATE_BP_RESOURCE),
  AZURE_ROLLBACK_ARM_RESOURCE("AZURE_ROLLBACK_ARM_RESOURCE", StepSpecTypeConstants.AZURE_ROLLBACK_ARM_RESOURCE),
  FETCH_INSTANCE_SCRIPT("FETCH_INSTANCE_SCRIPT", YamlTypes.FETCH_INSTANCE_SCRIPT),
  ECS_BLUE_GREEN_CREATE_SERVICE("ECS_BLUE_GREEN_CREATE_SERVICE", YamlTypes.ECS_BLUE_GREEN_CREATE_SERVICE),
  ECS_BLUE_GREEN_SWAP_TARGET_GROUPS("ECS_BLUE_GREEN_SWAP_TARGET_GROUPS", YamlTypes.ECS_BLUE_GREEN_SWAP_TARGET_GROUPS),
  ECS_BLUE_GREEN_ROLLBACK("ECS_BLUE_GREEN_ROLLBACK", YamlTypes.ECS_BLUE_GREEN_ROLLBACK),
  SHELL_SCRIPT_PROVISION("SHELL_SCRIPT_PROVISION", StepSpecTypeConstants.SHELL_SCRIPT_PROVISION),
  GITOPS_UPDATE_RELEASE_REPO("GITOPS_UPDATE_RELEASE_REPO", StepSpecTypeConstants.GITOPS_UPDATE_RELEASE_REPO),
  ECS_RUN_TASK("ECS_RUN_TASK", StepSpecTypeConstants.ECS_RUN_TASK),
  ELASTIGROUP_DEPLOY("ELASTIGROUP_DEPLOY", YamlTypes.ELASTIGROUP_DEPLOY),
  ELASTIGROUP_ROLLBACK("ELASTIGROUP_ROLLBACK", YamlTypes.ELASTIGROUP_ROLLBACK),
  ELASTIGROUP_SETUP("ELASTIGROUP_SETUP", YamlTypes.ELASTIGROUP_SETUP),
  GITOPS_FETCH_LINKED_APPS("GITOPS_FETCH_LINKED_APPS", StepSpecTypeConstants.GITOPS_FETCH_LINKED_APPS),
  TERRAGRUNT_PLAN("TERRAGRUNT_PLAN", StepSpecTypeConstants.TERRAGRUNT_PLAN),
  TERRAGRUNT_APPLY("TERRAGRUNT_APPLY", StepSpecTypeConstants.TERRAGRUNT_APPLY),
  TERRAGRUNT_DESTROY("TERRAGRUNT_DESTROY", StepSpecTypeConstants.TERRAGRUNT_DESTROY),
  TERRAGRUNT_ROLLBACK("TERRAGRUNT_ROLLBACK", StepSpecTypeConstants.TERRAGRUNT_ROLLBACK),
  ASG_CANARY_DEPLOY("ASG_CANARY_DEPLOY", YamlTypes.ASG_CANARY_DEPLOY),
  ASG_CANARY_DELETE("ASG_CANARY_DELETE", YamlTypes.ASG_CANARY_DELETE),

  TAS_CANARY_APP_SETUP("TAS_CANARY_APP_SETUP", YamlTypes.TAS_CANARY_APP_SETUP),
  TAS_BG_APP_SETUP("TAS_BG_APP_SETUP", YamlTypes.TAS_BG_APP_SETUP),
  TAS_BASIC_APP_SETUP("TAS_BASIC_APP_SETUP", YamlTypes.TAS_BASIC_APP_SETUP),
  TAS_APP_RESIZE("TAS_APP_RESIZE", YamlTypes.TAS_APP_RESIZE),
  TAS_SWAP_ROUTES("TAS_SWAP_ROUTES", YamlTypes.TAS_SWAP_ROUTES),
  TAS_ROLLBACK("TAS_ROLLBACK", YamlTypes.TAS_ROLLBACK),
  SWAP_ROLLBACK("SWAP_ROLLBACK", YamlTypes.SWAP_ROLLBACK),
  TANZU_COMMAND("TANZU_COMMAND", YamlTypes.TANZU_COMMAND),
  ELASTIGROUP_BG_STAGE_SETUP("ELASTIGROUP_BG_STAGE_SETUP", YamlTypes.ELASTIGROUP_BG_STAGE_SETUP),
  ELASTIGROUP_SWAP_ROUTE("ELASTIGROUP_SWAP_ROUTE", YamlTypes.ELASTIGROUP_SWAP_ROUTE),
  ASG_ROLLING_DEPLOY("ASG_ROLLING_DEPLOY", YamlTypes.ASG_ROLLING_DEPLOY),
  ASG_ROLLING_ROLLBACK("ASG_ROLLING_ROLLBACK", YamlTypes.ASG_ROLLING_ROLLBACK),
  TAS_ROLLING_DEPLOY("TAS_ROLLING_DEPLOY", YamlTypes.TAS_ROLLING_DEPLOY),
  TAS_ROLLING_ROLLBACK("TAS_ROLLING_ROLLBACK", YamlTypes.TAS_ROLLING_ROLLBACK),
  ASG_BLUE_GREEN_SWAP_SERVICE("ASG_BLUE_GREEN_SWAP_SERVICE", YamlTypes.ASG_BLUE_GREEN_SWAP_SERVICE),
  ASG_BLUE_GREEN_DEPLOY("ASG_BLUE_GREEN_DEPLOY", YamlTypes.ASG_BLUE_GREEN_DEPLOY),
  ASG_BLUE_GREEN_ROLLBACK("ASG_BLUE_GREEN_ROLLBACK", YamlTypes.ASG_BLUE_GREEN_ROLLBACK),
  TERRAFORM_CLOUD_RUN("TERRAFORM_CLOUD_RUN", YamlTypes.TERRAFORM_CLOUD_RUN),
  TERRAFORM_CLOUD_ROLLBACK("TERRAFORM_CLOUD_ROLLBACK_RUN", YamlTypes.TERRAFORM_CLOUD_ROLLBACK),
  GOOGLE_CLOUD_FUNCTIONS_DEPLOY("GOOGLE_CLOUD_FUNCTIONS_DEPLOY", YamlTypes.GOOGLE_CLOUD_FUNCTIONS_DEPLOY),
  GOOGLE_CLOUD_FUNCTIONS_DEPLOY_WITHOUT_TRAFFIC(
      "GOOGLE_CLOUD_FUNCTIONS_DEPLOY_WITHOUT_TRAFFIC", YamlTypes.GOOGLE_CLOUD_FUNCTIONS_DEPLOY_WITHOUT_TRAFFIC),
  GOOGLE_CLOUD_FUNCTIONS_TRAFFIC_SHIFT(
      "GOOGLE_CLOUD_FUNCTIONS_TRAFFIC_SHIFT", YamlTypes.GOOGLE_CLOUD_FUNCTIONS_TRAFFIC_SHIFT),
  GOOGLE_CLOUD_FUNCTIONS_ROLLBACK("GOOGLE_CLOUD_FUNCTIONS_ROLLBACK", YamlTypes.GOOGLE_CLOUD_FUNCTIONS_ROLLBACK),
  AWS_LAMBDA_DEPLOY("AWS_LAMBDA_DEPLOY", YamlTypes.AWS_LAMBDA_DEPLOY),
  AWS_SAM_DEPLOY("AWS_SAM_DEPLOY", YamlTypes.AWS_SAM_DEPLOY),
  AWS_SAM_BUILD("AWS_SAM_BUILD", YamlTypes.AWS_SAM_BUILD),
  AWS_SAM_ROLLBACK("AWS_SAM_ROLLBACK", YamlTypes.AWS_SAM_ROLLBACK),
  DOWNLOAD_MANIFESTS("DOWNLOAD_MANIFESTS", YamlTypes.DOWNLOAD_MANIFESTS),
  AWS_LAMBDA_ROLLBACK("AWS_LAMBDA_ROLLBACK", YamlTypes.AWS_LAMBDA_ROLLBACK),
  TAS_ROUTE_MAPPING("ROUTE_MAPPING", YamlTypes.TAS_ROUTE_MAPPING),
  GOOGLE_CLOUD_FUNCTIONS_GEN_ONE_DEPLOY(
      "GOOGLE_CLOUD_FUNCTIONS_GEN_ONE_DEPLOY", YamlTypes.GOOGLE_CLOUD_FUNCTIONS_GEN_ONE_DEPLOY),
  GOOGLE_CLOUD_FUNCTIONS_GEN_ONE_ROLLBACK(
      "GOOGLE_CLOUD_FUNCTIONS_GEN_ONE_ROLLBACK", YamlTypes.GOOGLE_CLOUD_FUNCTIONS_GEN_ONE_ROLLBACK),
  SERVICE_HOOKS("SERVICE_HOOKS", YamlTypes.SERVICE_HOOKS),
  K8S_BLUE_GREEN_STAGE_SCALE_DOWN("K8S_BLUE_GREEN_STAGE_SCALE_DOWN", YamlTypes.K8S_BLUE_GREEN_STAGE_SCALE_DOWN),
  K8S_BLUE_GREEN_STAGE_SCALE_DOWN_V2("K8S_BLUE_GREEN_STAGE_SCALE_DOWN_V2", YamlTypes.K8S_BLUE_GREEN_STAGE_SCALE_DOWN),
  SERVERLESS_AWS_LAMBDA_PREPARE_ROLLBACK_V2(
      "SERVERLESS_AWS_LAMBDA_PREPARE_ROLLBACK_V2", YamlTypes.SERVERLESS_AWS_LAMBDA_PREPARE_ROLLBACK_V2),
  SERVERLESS_AWS_LAMBDA_ROLLBACK_V2("SERVERLESS_AWS_LAMBDA_ROLLBACK_V2", YamlTypes.SERVERLESS_AWS_LAMBDA_ROLLBACK_V2),
  SERVERLESS_AWS_LAMBDA_DEPLOY_V2("SERVERLESS_AWS_LAMBDA_DEPLOY_V2", YamlTypes.SERVERLESS_AWS_LAMBDA_DEPLOY_V2),
  SERVERLESS_AWS_LAMBDA_PACKAGE_V2("SERVERLESS_AWS_LAMBDA_DEPLOY_V2", YamlTypes.SERVERLESS_AWS_LAMBDA_PACKAGE_V2),
  AWS_CDK_BOOTSTRAP("AWS_CDK_BOOTSTRAP", YamlTypes.AWS_CDK_BOOTSTRAP),
  AWS_CDK_SYNTH("AWS_CDK_SYNTH", YamlTypes.AWS_CDK_SYNTH),
  AWS_CDK_DIFF("AWS_CDK_DIFF", YamlTypes.AWS_CDK_DIFF),
  AWS_CDK_DEPLOY("AWS_CDK_DEPLOY", YamlTypes.AWS_CDK_DEPLOY),
  AWS_CDK_DESTROY("AWS_CDK_DESTROY", YamlTypes.AWS_CDK_DESTROY),
  AWS_CDK_ROLLBACK("AWS_CDK_ROLLBACK", YamlTypes.AWS_CDK_ROLLBACK),
  ASG_SERVICE_SETTINGS_STEP("ASG_SERVICE_SETTINGS_STEP", YamlTypes.ASG_SERVICE_SETTINGS_STEP),
  CUSTOM_STAGE_ENVIRONMENT("CUSTOM_STAGE_ENVIRONMENT", YamlTypes.ENVIRONMENT_YAML),
  ECS_SERVICE_SETUP("ECS_SERVICE_SETUP", YamlTypes.ECS_SERVICE_SETUP),
  ECS_UPGRADE_CONTAINER("ECS_UPGRADE_CONTAINER", YamlTypes.ECS_UPGRADE_CONTAINER),
  ECS_BASIC_ROLLBACK("ECS_BASIC_ROLLBACK", YamlTypes.ECS_BASIC_ROLLBACK),
  CUSTOM_STAGE("CUSTOM_STAGE", "custom");

  private final String name;
  private final String yamlType;

  ExecutionNodeType(String name, String yamlType) {
    this.name = name;
    this.yamlType = yamlType;
  }

  public String getName() {
    return name;
  }

  public String getYamlType() {
    return yamlType;
  }
}
