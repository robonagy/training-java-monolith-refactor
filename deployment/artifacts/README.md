# Deployment Artifact Families

This folder tree contains deployment execution artifacts generated from deployment specification:

- CI pipeline definitions: .github/workflows/
- IaC modules and overlays: infra/terraform/
- Config and secret schemas: deployment/config/, deployment/secrets/
- Observability wiring values: deployment/observability/
- Promotion and rollback runbooks: deployment/promotion/, deployment/rollback/
- Traffic-shift templates: deployment/traffic-shift/

These artifacts are intended for plan/validate/package phases first.
