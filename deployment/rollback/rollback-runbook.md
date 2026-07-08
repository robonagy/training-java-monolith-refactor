# Rollback Runbook

## Triggers
- Error budget breach
- Parity mismatch spike
- Critical incident or vulnerability

## Application Rollback
1. Redeploy previous stable image tag.
2. Restore previous routing config.
3. Set module mode key(s) to legacy.

## Database Rollback
1. Confirm pre-deploy snapshot exists.
2. Execute approved restore procedure.
3. Validate schema/data consistency checks.

## Validation
- Health checks green.
- Core acceptance checks pass.
- Observability confirms error stabilization.
