variable "credentialsFile" {
  type    = "string"
}

variable "deployment" {
  type = "string"
}

variable "projectId" {
  type = "string"
}

provider "google" {
  credentials = var.credentialsFile
  project = var.projectId
}

locals {
  qa_filter_prefix = join("\n", [
    "resource.labels.cluster_name=\"qa-private\"",
    "resource.labels.container_name=\"manager\""
  ])

  prod_filter_prefix = join("\n", [
    "resource.labels.cluster_name=\"prod-private\"",
    "resource.labels.container_name=\"manager\"",
    "resource.labels.namespace_id:\"manager-prod-\""
  ])

  freemium_filter_prefix = join("\n", [
    "resource.type=\"container\"",
    "resource.labels.container_name=\"manager\"",
    "resource.labels.namespace_id:\"manager-free-\""
  ])


  filter_prefx = (var.deployment == "qa"   ? local.qa_filter_prefix :
                 (var.deployment == "prod" ? local.prod_filter_prefix :
                                               local.freemium_filter_prefix))
}