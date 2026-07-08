variable "environment" { type = string }
variable "namespace" { type = string }

resource "kubernetes_namespace" "ns" {
  metadata {
    name = var.namespace
    labels = {
      environment = var.environment
      managed_by  = "terraform"
    }
  }
}
