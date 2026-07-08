# Terraform Layout

This Terraform layout maps to deployment phase-4 requirements.

## Module groups

- modules/platform-core
- modules/app-runtime
- modules/data-platform
- modules/config-and-secrets
- modules/observability-wiring
- modules/traffic-control

## Environments

- environments/dev
- environments/staging
- environments/prod

## Usage

Run planning only for specify/plan/tasks phases:

- `terraform init`
- `terraform validate`
- `terraform plan -var-file=terraform.tfvars`

Do not run `terraform apply` during specification phases.
