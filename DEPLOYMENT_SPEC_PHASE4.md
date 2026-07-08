# Deployment Specification (Phase 4)

Date: 2026-07-08
Scope: Deployment specification only (no live deployment actions)
Sources:
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md)
- [SPECKIT_PLAN_PHASE3B.md](SPECKIT_PLAN_PHASE3B.md)
- [SPECKIT_TASKS_PHASE3B.md](SPECKIT_TASKS_PHASE3B.md)
- [build.gradle](build.gradle)
- [README.md](README.md)

## Deployment Constraints

- No live deployment, no environment mutation, no runtime cutover execution in this phase.
- This document defines deployable artifacts and execution order only.
- All choices align with phase-3 architecture decisions (TA-01..TA-17).

## 0) Platform Baseline (from phase 3a)

Target platform stack:
- Runtime: Java 21 + Open Liberty-compatible container runtime.
- Packaging: container image per release.
- Orchestration: Kubernetes-compatible platform.
- Data platform: managed relational database service.
- Config/secrets: externalized platform config and secret store.
- Observability: centralized logs, metrics, traces.

Trace:
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L14)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L24)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L29)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L112)

## 1) CI Pipeline (Build, Test, Scan)

Pipeline goals:
- Build reproducible release artifacts.
- Enforce quality and security gates before promotion.
- Produce immutable, traceable deployment bundles.

### 1.1 Trigger model
- Pull Request: compile + unit tests + static scans + container build (non-release).
- Main branch merge: full pipeline including signed release artifact and deploy package generation.
- Release tag: promotion-ready build, vulnerability baseline freeze, deployment manifest packaging.

### 1.2 Pipeline stages
1. Checkout and provenance
- Fetch source at commit SHA.
- Generate build metadata (commit, timestamp, pipeline run id).

2. Toolchain bootstrap
- Java 21 runtime setup.
- Gradle wrapper execution.

3. Build and test
- `./gradlew clean build`
- `./gradlew test`
- Produce WAR artifact and test reports.
- Current baseline commands source: [README.md](README.md#L32), [build.gradle](build.gradle#L42).

4. Static quality scans
- SAST scan for Java code and dependency risks.
- IaC lint/validation for deployment manifests.
- Secret leak scan for repository content.

5. Dependency and container security scans
- Software composition analysis on Gradle dependencies.
- Container image vulnerability scan.
- Policy gate: fail on Critical vulnerabilities unless approved exception record exists.

6. Package and sign
- Build container image.
- Sign image and attach SBOM.
- Publish image + metadata to artifact registry.

7. Deploy package assembly
- Bundle environment overlays (dev/staging/prod).
- Bundle migration scripts and rollback manifests.
- Bundle module traffic-shift config templates.

8. Promotion gate checks
- Ensure tests, scans, and policy controls passed.
- Approver checkpoint for staging and production promotions.

### 1.3 CI deliverable artifacts
- Build artifact: WAR + container image.
- Test reports: JUnit/XML and summary.
- Security artifacts: SBOM + vulnerability reports.
- Deployable bundle: manifests, overlays, migration scripts, rollback specs, switch configs.

## 2) Infrastructure as Code (IaC)

IaC objective:
- A fresh platform environment can be created from declarative code only.

### 2.1 IaC scope per environment
Provision these stacks per environment (`dev`, `staging`, `prod`):
- Kubernetes namespace and RBAC.
- Application deployment + service + ingress.
- Managed relational database instance and network policy.
- Secret store bindings.
- Observability stack bindings (log/metric/trace exporters).
- Artifact pull credentials.

### 2.2 IaC module set
Required IaC module groups:
1. `platform-core`
- namespace, network policies, RBAC, ingress class references.

2. `app-runtime`
- deployment, service, autoscaling, health probes, rollout strategy.

3. `data-platform`
- managed relational DB, backups, retention, restore policies.

4. `config-and-secrets`
- non-secret config maps and secret references.

5. `observability-wiring`
- log sink integration, metrics scraping, tracing endpoints.

6. `traffic-control`
- routing rules for legacy/new module path switching.

### 2.3 State and drift controls
- Remote state backend with locking.
- Required `plan` review before `apply` in protected environments.
- Drift detection run on schedule and before production promotion.

Trace:
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L24)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L29)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L90)

## 3) Secrets, Config, and Observability Wiring

### 3.1 Secrets model
- No secrets in source or image layers.
- Secrets sourced at runtime from managed secret store.
- Minimum required secrets:
  - database credentials
  - TLS material references
  - artifact registry pull credentials
  - external integration credentials (if any)

### 3.2 Config model
- Environment-specific values externalized.
- Required config keys include:
  - `APP_ENV`
  - `JAVA_OPTS`
  - `DB_HOST`, `DB_PORT`, `DB_NAME`
  - `BILLING_MODE` (legacy/rewritten)
  - module-specific routing mode keys for strangler traffic shifts

### 3.3 Observability model
- Logs: structured JSON with correlation id.
- Metrics: application + JVM + deployment health metrics.
- Traces: request traces across module boundaries and database calls.
- Migration metrics:
  - parity mismatch count
  - legacy vs rewritten route split
  - module cutover error rate

Trace:
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L112)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L123)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L127)

## 4) Promotion Path: Dev -> Staging -> Production

### 4.1 Promotion rules
- Promote identical immutable artifact across environments.
- No rebuild between environments.
- All environment differences come from external config/secrets only.

### 4.2 Environment gates
1. Dev gate
- Build/test/scan pass.
- Smoke test pass.
- Migration dry-run pass.

2. Staging gate
- Dev gate pass + integration test pass.
- Parity checks pass for targeted rewritten modules.
- Performance baseline within allowed threshold.

3. Production gate
- Staging gate pass + approval checkpoint.
- Rollback package verified.
- On-call readiness confirmed.

### 4.3 Release evidence package
Required before production:
- CI pass report
- security scan report + exceptions
- migration and rollback plan
- parity verification report
- traffic-shift plan for active module cutover

## 5) Rollback Path

Rollback principle:
- Fast, deterministic restore to last known good deployment with data safety controls.

### 5.1 Application rollback
- Keep at least one previous stable release artifact and manifest set.
- Roll back by redeploying previous image + previous routing config.
- Restore `BILLING_MODE` or equivalent module mode keys to legacy setting.

### 5.2 Database rollback
- Use forward-safe migration design (expand/contract), avoiding destructive steps before cutover confidence.
- Pre-deploy backup/snapshot required.
- Restore procedure documented per environment with RPO/RTO expectations.

### 5.3 Trigger conditions
- Error budget breach.
- parity mismatch spike after module shift.
- critical vulnerability or operational incident.

### 5.4 Rollback validation
- Health checks green.
- core billing acceptance checks pass.
- observability confirms traffic reverted and errors stabilized.

Trace:
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L139)
- [SPECKIT_TASKS_PHASE3B.md](SPECKIT_TASKS_PHASE3B.md#L28)

## 6) Strangler-Fig Traffic-Shifting Mechanism

### 6.1 Control mechanism
- Module-level traffic switch keys (example: `BILLING_MODE=legacy|rewritten`).
- Routing and switch config externalized so flips do not require rebuild.
- Compatibility boundary retained for dual-run parity checks.

### 6.2 Shift sequence per module
1. Start at `legacy=100%`.
2. Enable dual-run comparison in shadow mode for selected module.
3. Shift canary percentage to rewritten path.
4. Observe parity/error/latency metrics.
5. Increase traffic in controlled steps.
6. Reach `rewritten=100%` only after gate criteria pass.
7. Keep rollback switch available until stabilization window closes.

### 6.3 Billing module initial mechanism
- Use billing traffic switch service behavior as runtime reference for rollout mode semantics.
- Confirm switch/rollback tests are part of promotion evidence.
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingTrafficSwitchService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingTrafficSwitchService.java)
  - [src/test/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingTrafficSwitchServiceTest.java](src/test/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingTrafficSwitchServiceTest.java)

Trace:
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L139)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L56)
- [SPECKIT_TASKS_PHASE3B.md](SPECKIT_TASKS_PHASE3B.md#L28)

## 7) Fresh Environment Bootstrap Procedure

From these artifacts alone, bootstrap steps are:
1. Provision environment with IaC module set (`platform-core`, `app-runtime`, `data-platform`, `config-and-secrets`, `observability-wiring`, `traffic-control`).
2. Inject environment-specific config and secrets through managed stores.
3. Deploy immutable artifact bundle from CI release outputs.
4. Execute schema compatibility migrations.
5. Run smoke and parity baseline checks.
6. Initialize module traffic mode to legacy.
7. Execute controlled module cutover using strangler shift sequence.

Success criterion for bootstrap and cutover readiness:
- Fresh environment becomes deployable from declared artifacts without manual infrastructure creation.
- A rewritten module can be cut over end-to-end with promotion and rollback paths, using only defined CI/IaC/config/traffic artifacts.

## 8) Required Artifact Set

The following artifact families must exist in-repo before execution:
- CI pipeline definition file(s)
- IaC module definitions + environment overlays
- Migration scripts + rollback scripts
- Secret/config schema definitions
- Observability dashboards/alerts as code
- Traffic-shift configuration templates per module
- Promotion checklists and approval templates

## 9) Non-Goals in This Phase

- No live apply, deploy, or traffic flip in this specify phase.
- No production data migration execution.
- No runtime cutover execution.
