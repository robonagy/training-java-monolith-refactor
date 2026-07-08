variable "environment" {
  type        = string
  description = "Deployment environment: dev, staging, prod"
}

variable "namespace" {
  type        = string
  description = "Kubernetes namespace for this environment"
}

module "platform_core" {
  source      = "./modules/platform-core"
  environment = var.environment
  namespace   = var.namespace
}
