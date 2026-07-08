variable "billing_mode" { type = string }

# Placeholder traffic-control module.
# Define route and mode controls for strangler traffic shifts.
output "billing_mode" {
  value = var.billing_mode
}
