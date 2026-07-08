# Target Architecture Specification (Phase 3)

Date: 2026-07-08
Scope: Architecture only (no code, no per-module rewrite tasking)
Sources:
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)
- [SUBSTITUTION_AUDIT_PHASE1.md](SUBSTITUTION_AUDIT_PHASE1.md)

## Architecture Constraints

- Preserve validated business behavior unless explicitly changed by approved policy decisions.
- Resolve open behavior ambiguities through decision records before final cutover.
- All architectural choices below are trace-linked to preserved business rules (BR-xx) and/or substitution-audit rows (SA-xx).

## 1) Target Runtime and Platform

### TA-01 Runtime baseline
- Java 21 LTS as the application runtime baseline.
- Jakarta EE compatible runtime retained on Open Liberty for continuity during migration.
- Trace: BR-07, SA-16, SA-18.

### TA-02 Packaging and deployment model
- Container-first deployment model for application runtime.
- Runtime configuration externalized to environment/secret sources rather than file-local defaults.
- Trace: SA-09, SA-17, SA-18, SA-19.

### TA-03 Data platform
- Replace embedded/file-backed Derby persistence with managed relational database service.
- Continue relational model semantics and SQL parity while migrating storage/runtime operations to platform-managed services.
- Trace: SA-08, SA-09, Data-Model Summary in [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md).

### TA-04 Time and numeric platform primitives
- Standardize temporal handling on modern Java time API and consistent money/decimal arithmetic conventions.
- Maintain legacy output parity where behavior is already defined.
- Trace: BR-02, BR-04, BR-05, BR-06, BR-15, SA-10, SA-11, SA-13.

## 2) Module and Service Boundaries

### TA-05 Domain-aligned boundaries
The target system is decomposed into these architectural boundaries:
- Billing Core: bill generation, monthly summaries, validation policy surface.
- Reporting: customer/monthly/revenue projections and report assembly.
- Time Entry: billable-hour capture and policy checks.
- Category/Pricing: category lifecycle and hourly-rate management.
- Customer Management: customer lifecycle.
- User Management: user lifecycle and identity-linked profile data.

Trace: BR-01..BR-06, BR-10..BR-13, SA-06, SA-07, SA-20.

### TA-06 API boundary principle
- Presentation/UI and external consumers interact through explicit service contracts rather than JSP-embedded SQL/logic.
- No business rule execution in view templates.
- Trace: BR-12, BR-13, SA-06, SA-07.

### TA-07 Compatibility boundary
- A compatibility boundary is retained between legacy contract shapes and modernized internals until migration completion.
- Trace: BR-01..BR-06, SA-01, SA-02, SA-03, SA-20.

## 3) Data Model and Migration Path from Legacy Schema

### TA-08 Canonical relational model
- Preserve current core entities and relationships as canonical migration baseline:
  - users
  - customers
  - billing_categories
  - billable_hours
- Preserve FK topology and key invariants during migration.
- Trace: Data-Model Summary in [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md), BR-02, BR-05.

### TA-09 Data quality and behavioral compatibility rules
- Preserve currently observed behavior where explicit (for example, validation wording and billing totals behavior).
- Treat ambiguous or potentially defective behavior as policy-gated items requiring explicit decision before normalization.
- Trace: BR-06, BR-09, BR-14, SA-12, SA-14, SA-20.

### TA-10 Migration path
- Phase A: schema compatibility layer on managed relational platform (same logical tables/columns and constraints as baseline).
- Phase B: dual-read/dual-write verification where required for parity-critical flows.
- Phase C: controlled cutover to managed platform as system of record.
- Phase D: retire legacy embedded storage paths and filesystem-coupled database assumptions.
- Trace: SA-08, SA-09, SA-19, BR-07.

## 4) Integration Contracts Replacing Flagged Legacy Integrations

| Contract ID | Legacy integration to replace | Target contract | Trace |
|---|---|---|---|
| IC-01 | Direct JDBC usage in DAO/JSP paths | Data Access Contract: repository/service contract with managed connection lifecycle and transaction boundaries | SA-04, SA-05, SA-06, SA-07 |
| IC-02 | Embedded Derby filesystem database | Data Platform Contract: managed relational service endpoint with schema/version compatibility controls | SA-08, SA-09, SA-19 |
| IC-03 | Joda-Time and home-grown temporal helpers | Temporal Contract: java.time-based date/time API with explicit timezone/cutoff policy fields | SA-10, SA-11, Open Question 8 in [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L330) |
| IC-04 | Mixed double/decimal revenue arithmetic in views | Monetary Contract: decimal-only financial calculation API and output formatting policy | BR-02, BR-05, BR-15, SA-13 |
| IC-05 | Runtime file-configured credentials and endpoints | Configuration Contract: externalized config with typed settings and environment profiles | SA-17, SA-18 |
| IC-06 | In-app runtime seeding side effects | Initialization Contract: explicit migration/seed pipeline with environment scoping | BR-08, SA-15 |
| IC-07 | Conflicting report authority (service vs JSP SQL) | Reporting Authority Contract: single authoritative reporting domain boundary with policy-governed semantics | BR-13, BR-14, SA-20, Open Question 1 in [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L299) |

## 5) Cross-Cutting Concerns

### TA-11 Authentication and authorization
- Centralized authn/authz policy boundary for all business endpoints.
- Route-level authorization explicit and consistent across UI/API surfaces.
- Trace: Open Question 7 in [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L326), SA-07.

### TA-12 Logging and auditability
- Structured logging with correlation identifiers across service boundaries.
- Operational logs and audit events routed to centralized platform sinks (not local filesystem-only assumptions).
- Trace: SA-19, SA-05.

### TA-13 Configuration and secrets
- All sensitive values and environment-specific settings externalized.
- No hard-coded secrets or fixed endpoints in deployable artifacts.
- Trace: SA-17, SA-18.

### TA-14 Observability
- Standard metrics/traces/logs for billing/reporting correctness and migration parity checks.
- Dedicated parity-drift signals for strangler coexistence period.
- Trace: SA-01, SA-02, SA-03, SA-20.

### TA-15 Reliability and data integrity
- Explicit transaction boundaries and referential-integrity protections in write paths.
- Defensive validation policy remains aligned with preserved behavior.
- Trace: BR-06, Data-Model Summary in [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md), SA-04, SA-05.

## 6) Migration Strategy

### TA-16 Default strategy: strangler-fig
- Keep legacy and modernized paths coexisting behind compatibility boundaries.
- Shift traffic incrementally by bounded domain while measuring parity.
- Finalize cutover only after behavior and data parity criteria are met.
- Trace: SA-01, SA-02, SA-03, SA-20; BR-01..BR-06.

### TA-17 Decision-gated migration points
The following behaviors are explicitly decision-gated and must be adjudicated before final authority/cutover:
- Reporting authority conflict.
- Weekend warning vs rejection semantics.
- Fixed day-31 month-end semantics.
- users.jsp constructor-order anomaly.

Trace: Open Questions 1, 2, 4, 5 in [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L299), [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L304), [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L313), [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L317); SA-12, SA-14, SA-20.

## 7) Risks

### R-01 Behavior authority ambiguity risk
- Conflicting implementations may cause inconsistent outputs post-cutover if authority is not decided.
- Trace: BR-13, BR-14, SA-20.

### R-02 Financial parity risk
- Mixed numeric behavior (decimal vs double legacy surfaces) can produce subtle monetary drift.
- Trace: BR-02, BR-05, BR-15, SA-13.

### R-03 Temporal semantics risk
- Joda-time replacement and timezone/cutoff standardization can alter boundary-date behavior.
- Trace: BR-04, BR-06, SA-10, SA-11.

### R-04 Data migration integrity risk
- Embedded-to-managed relational migration may introduce consistency and rollback challenges.
- Trace: SA-08, SA-09, Data-Model Summary in [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md).

### R-05 Security and operations risk
- Hard-coded secrets/endpoints and local filesystem assumptions are incompatible with secure cloud operations.
- Trace: SA-17, SA-18, SA-19.

## Architecture Decision Trace Matrix

| Decision | Choice summary | Trace to BR/SA evidence |
|---|---|---|
| TA-01 | Java 21 + Liberty continuity | BR-07, SA-16, SA-18 |
| TA-02 | Container-first + externalized config | SA-09, SA-17, SA-18, SA-19 |
| TA-03 | Managed relational data platform | SA-08, SA-09 |
| TA-04 | Modern time + decimal consistency | BR-02, BR-04, BR-05, BR-06, BR-15, SA-10, SA-11, SA-13 |
| TA-05 | Domain-aligned service boundaries | BR-01..BR-06, BR-10..BR-13, SA-06, SA-07, SA-20 |
| TA-06 | API/service contracts replace JSP logic | BR-12, BR-13, SA-06, SA-07 |
| TA-07 | Compatibility boundary during migration | BR-01..BR-06, SA-01, SA-02, SA-03 |
| TA-08 | Preserve canonical entity/relationship model | Data-Model Summary, BR-02, BR-05 |
| TA-09 | Policy-gated normalization for ambiguous behavior | BR-09, BR-14, SA-12, SA-14, SA-20 |
| TA-10 | Phased managed-DB migration path | SA-08, SA-09, SA-19, BR-07 |
| IC-01..IC-07 | Integration replacement contracts | SA-04..SA-20 and linked BRs as listed in contract table |
| TA-11..TA-15 | Cross-cutting architecture | Open Question 7, BR-06, SA-05, SA-07, SA-17, SA-18, SA-19 |
| TA-16 | Strangler-fig migration default | SA-01, SA-02, SA-03, SA-20, BR-01..BR-06 |
| TA-17 | Decision-gated cutover points | Open Questions 1,2,4,5; SA-12, SA-14, SA-20 |
