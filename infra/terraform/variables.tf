variable "app_image" {
  type        = string
  description = "Container image for application deployment"
  default     = "ghcr.io/example/big-bad-monolith:latest"
}

variable "billing_mode" {
  type        = string
  description = "Module traffic switch value for billing mode"
  default     = "legacy"
}
