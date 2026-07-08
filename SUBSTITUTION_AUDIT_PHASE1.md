# Substitution Audit (Phase 1)

Date: 2026-07-08
Scope: Audit only. No target architecture design in this artifact.
Primary source: [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)

## 1) Substitution-Audit Table

| legacy element | proposal (keep-as-is \| replace-with-library \| replace-with-platform \| retire) | reason | trade-off |
|---|---|---|---|
| SA-01: Billing semantic gate and core arithmetic from [BR-01](REDISCOVERY_SPEC.md#L8) and [BR-02](REDISCOVERY_SPEC.md#L19) | keep-as-is | These are domain rules (customer existence + line-based billing) and represent intended business behavior rather than accidental technical debt. | Preserves billing correctness baseline, but does not reduce implementation complexity by itself. |
| SA-02: Null-category exclusion behavior from [BR-03](REDISCOVERY_SPEC.md#L30) | keep-as-is | This is an observed production behavior in current logic and affects totals. | Preserves parity; may retain ambiguity if business wanted strict failure instead of exclusion. |
| SA-03: Service-level monthly filtering and category aggregation from [BR-04](REDISCOVERY_SPEC.md#L36) and [BR-05](REDISCOVERY_SPEC.md#L45) | keep-as-is | Current monthly semantics are explicit and traceable in service code. | Stable parity, but existing performance characteristics remain unchanged. |
| SA-04: String-concatenated validation logic from [BR-06](REDISCOVERY_SPEC.md#L52) | replace-with-library | Validation is hand-built and message assembly is ad hoc; standard validation frameworks cover this consistently. | Better consistency and reuse versus migration effort and message-format regression risk. |
| SA-05: Startup environment branching and in-app datasource mode selection from [BR-07](REDISCOVERY_SPEC.md#L72) | replace-with-platform | Runtime environment policy is currently embedded in app code; platform-native configuration can own it. | Cleaner app boundary versus tighter dependence on deployment platform conventions. |
| SA-06: Runtime sample-data seeding from [BR-08](REDISCOVERY_SPEC.md#L83) | retire | Startup seeding is useful for demos but unsafe as a default operational behavior. | Reduces accidental data mutation risk versus loss of instant first-run convenience. |
| SA-07: User creation constructor mismatch behavior from [BR-09](REDISCOVERY_SPEC.md#L90) | retire | This is a defect-prone legacy behavior causing field inversion at persistence time. | Improves data quality versus requiring reconciliation for existing inverted records. |
| SA-08: JSP-driven customer action handling from [BR-10](REDISCOVERY_SPEC.md#L102) | retire | Presentation layer currently executes write operations directly, mixing concerns. | Better separability/testability versus rewrite effort for request handling paths. |
| SA-09: JSP-driven category write/update handling from [BR-11](REDISCOVERY_SPEC.md#L112) | retire | Business writes are coupled to JSP scriptlets and request parsing. | Better maintainability versus migration workload for category workflows. |
| SA-10: JSP-driven hours save path bypassing centralized validator from [BR-12](REDISCOVERY_SPEC.md#L123) | retire | Critical write path bypasses service validation and relies on local JSP checks. | Stronger invariant enforcement versus behavior-change risk if legacy leniencies existed. |
| SA-11: Direct JDBC in reports JSP from [BR-13](REDISCOVERY_SPEC.md#L134) | retire | Reporting logic uses direct SQL in view layer, duplicating domain logic paths. | Better consistency and testability versus short-term rewrite and parity-validation cost. |
| SA-12: Fixed day-31 monthly boundary construction from [BR-14](REDISCOVERY_SPEC.md#L146) | replace-with-library | Manual date-boundary construction is brittle; calendar/time libraries provide safer boundary handling. | Better date correctness versus potential output differences for historically malformed month ranges. |
| SA-13: Double-based monetary arithmetic in JSP from [BR-15](REDISCOVERY_SPEC.md#L156) | replace-with-library | Floating-point arithmetic in billing summaries risks precision drift. | Improved financial precision versus possible display/result deltas requiring parity checks. |
| SA-14: Hand-rolled DAO/JDBC mapping layer from [Data-Model Summary](REDISCOVERY_SPEC.md#L165) | replace-with-library | Home-grown persistence plumbing duplicates commodity repository/ORM capabilities. | Less boilerplate and standardized transactions versus migration effort and SQL control trade-offs. |
| SA-15: Embedded Derby primary runtime store from [Integration Inventory](REDISCOVERY_SPEC.md#L228) | replace-with-platform | Embedded file-backed DB is fragile for concurrent, scaled, or managed operations. | Better operability and resilience versus increased platform/database operations complexity. |
| SA-16: Local filesystem DB path coupling from [Integration Inventory](REDISCOVERY_SPEC.md#L228) | replace-with-platform | Hard dependency on local/output paths is cloud-hostile and stateful. | Better portability/stateless operation versus external state provisioning requirements. |
| SA-17: Hard-coded ports/context endpoint assumptions from [Integration Inventory](REDISCOVERY_SPEC.md#L228) | replace-with-platform | Static network assumptions reduce environment portability and deployment flexibility. | Easier multi-env deployment versus extra configuration management overhead. |
| SA-18: Hard-coded credentials and keystore secrets from [Integration Inventory](REDISCOVERY_SPEC.md#L228) | replace-with-platform | Secrets are in source/config, which is unsuitable for secure rotation and governance. | Stronger security posture versus dependence on external secret-management services. |
| SA-19: Mixed temporal model (Joda + SQL conversions) from [Open Questions](REDISCOVERY_SPEC.md#L299) | replace-with-library | Mixed time APIs increase semantic ambiguity and maintenance risk. | Better temporal consistency versus conversion and regression-test effort. |
| SA-20: Runtime/framework lifecycle evidence gap from [Integration Inventory](REDISCOVERY_SPEC.md#L228) and [Open Questions](REDISCOVERY_SPEC.md#L299) | keep-as-is | Code evidence confirms Liberty/Jakarta usage but does not by itself prove EOL status; substitution without lifecycle evidence would be speculative. | Avoids premature churn, but lifecycle risk remains until validated externally. |

## 2) Coverage Confirmation

This table spans required audit dimensions:

- Home-grown code with modern equivalents:
  - SA-04, SA-12, SA-13, SA-14, SA-19
- Dated integrations:
  - SA-11, SA-19
- Unfit data stores:
  - SA-15, SA-16
- EOL runtimes/frameworks:
  - SA-20 (explicitly marked keep-as-is pending external lifecycle verification)
- Cloud-hostile operational assumptions:
  - SA-05, SA-16, SA-17, SA-18

Notes:
- This document is an audit artifact only.
- No target architecture, module design, or implementation plan is proposed here.
