variable "namespace" { type = string }
variable "app_image" { type = string }

# Placeholder app-runtime module. Wire deployment, service, HPA, and probes here.
output "runtime_module" {
  value = {
    namespace = var.namespace
    app_image = var.app_image
  }
}
