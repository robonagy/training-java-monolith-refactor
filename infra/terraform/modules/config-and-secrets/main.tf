variable "namespace" { type = string }

# Placeholder config-and-secrets module.
# Define ConfigMaps and external secret references.
output "config_namespace" {
  value = var.namespace
}
