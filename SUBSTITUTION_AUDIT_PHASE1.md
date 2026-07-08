# Substitution Audit (Phase 1)

Date: 2026-07-08
Scope: Audit only (no target architecture design)
Source of truth: [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)

## Audit Constraints

- This document identifies substitution candidates only.
- Proposals are limited to: keep-as-is, replace-with-library, replace-with-platform, retire.
- Every audit row links to a specific rediscovery entry.

## 1) Substitution-Audit Table

| legacy element | proposal | reason | trade-off |
|---|---|---|---|
| SA-01: Billing calculation behavior in [BR-02](REDISCOVERY_SPEC.md#L19) and [BR-03](REDISCOVERY_SPEC.md#L30) | keep-as-is | Financial behavior is business-critical and already explicit; must be treated as behavioral contract baseline before any refactor. | Preserves known quirks and may keep non-ideal structure temporarily. |
| SA-02: Monthly filter and aggregation behavior in [BR-04](REDISCOVERY_SPEC.md#L36) and [BR-05](REDISCOVERY_SPEC.md#L45) | keep-as-is | Report semantics are explicit and relied on by existing flows; parity-first handling reduces regression risk. | May perpetuate legacy output format constraints until later policy decisions. |
| SA-03: Validation message composition in [BR-06](REDISCOVERY_SPEC.md#L52) | keep-as-is | Output strings are externally observable behavior and should remain stable during substitution waves. | Keeps brittle string-coupled behavior in short term. |
| SA-04: Raw JDBC DAO persistence pattern from [Integration Inventory](REDISCOVERY_SPEC.md#L275) and anti-pattern notes in [README.md](README.md#L51) | replace-with-platform | JDBC plumbing is repetitive and cross-cutting; platform-managed data access can reduce boilerplate and error surface. | Migration requires careful parity tests for SQL behavior and transaction boundaries. |
| SA-05: Manual connection handling from [Integration Inventory](REDISCOVERY_SPEC.md#L275) | replace-with-platform | Connection lifecycle concerns are operational, not domain logic; platform-level pooling/management is better fit. | Less direct SQL control and requires stronger observability in platform layer. |
| SA-06: Direct JDBC in JSP reporting path from [BR-13](REDISCOVERY_SPEC.md#L134) | retire | Query execution in JSP is a mixed-concern anti-pattern and a coupling hotspot. | Requires staged extraction to avoid report regressions. |
| SA-07: Scriptlet-heavy JSP flow pattern indicated by [BR-10](REDISCOVERY_SPEC.md#L102), [BR-11](REDISCOVERY_SPEC.md#L112), [BR-12](REDISCOVERY_SPEC.md#L123), and [README anti-patterns](README.md#L57) | retire | Business logic in presentation layer blocks maintainability, testability, and safe substitution. | Retirement can be labor-intensive due to hidden page-level dependencies. |
| SA-08: Embedded Derby file-backed store from [Integration Inventory](REDISCOVERY_SPEC.md#L275) and [Filesystem paths](REDISCOVERY_SPEC.md#L287) | replace-with-platform | Local embedded persistence is unfit for cloud scaling, HA, and managed operations. | Platform DB migration introduces data migration and operational cutover complexity. |
| SA-09: Dual DB path behavior (embedded vs Liberty output dir) from [BR-07](REDISCOVERY_SPEC.md#L72) and [Filesystem paths](REDISCOVERY_SPEC.md#L291) | replace-with-platform | Environment-dependent data location increases deployment drift risk and impairs reproducibility. | Standardization reduces local flexibility and needs environment migration planning. |
| SA-10: Joda-Time usage from [Dependency-level integrations](REDISCOVERY_SPEC.md#L327) and temporal open question [Q8](REDISCOVERY_SPEC.md#L330) | replace-with-library | Joda-Time is a dated integration relative to modern Java date/time APIs. | Temporal conversion differences can create subtle behavior drift if parity tests are weak. |
| SA-11: Home-grown date utility behavior in [BR-06 reference to DateTimeUtils](REDISCOVERY_SPEC.md#L60) and [Q8](REDISCOVERY_SPEC.md#L330) | replace-with-library | Utility methods overlap with mature date/time libraries and increase inconsistency risk. | Replacements may alter edge-case formatting/weekday behavior unless locked by tests. |
| SA-12: Hard-coded fixed day-31 monthly range in [BR-14](REDISCOVERY_SPEC.md#L146) | keep-as-is | Behavior is explicit but policy-ambiguous; preserve during audit to avoid silent semantic changes. | Maintains potentially incorrect month-end semantics until policy decision. |
| SA-13: Mixed decimal/double revenue handling in [BR-15](REDISCOVERY_SPEC.md#L158) vs decimal rules in [BR-02](REDISCOVERY_SPEC.md#L19) | replace-with-library | Numeric consistency should rely on deterministic decimal arithmetic utilities for money-related totals. | May reveal latent discrepancies in existing reports that stakeholders currently accept. |
| SA-14: users.jsp constructor-order defect candidate in [BR-09](REDISCOVERY_SPEC.md#L91) | keep-as-is | Intent is unresolved; preserving observed behavior avoids accidental data-shape changes before decision. | Keeps known bad data mapping behavior until adjudicated. |
| SA-15: Startup-time sample data seeding from [BR-08](REDISCOVERY_SPEC.md#L81) | replace-with-platform | Seed lifecycle is operational concern; platform migrations/init tooling is a better fit than runtime side effects. | Requires separate migration discipline and environment controls. |
| SA-16: Liberty datasource fallback logic from [BR-07](REDISCOVERY_SPEC.md#L72) | keep-as-is | Current runtime selection behavior is part of deployment semantics and should remain stable until operational policy is finalized. | Continued dual-mode operation can mask environment-specific bugs. |
| SA-17: Hard-coded credentials in config from [Hard-coded credentials and security config](REDISCOVERY_SPEC.md#L314) | replace-with-platform | Secrets in source/config are cloud-hostile and operationally unsafe. | Secret-management rollout adds platform dependencies and deployment steps. |
| SA-18: Hard-coded local ports and context-root assumptions from [Hard-coded endpoints and network values](REDISCOVERY_SPEC.md#L301) | replace-with-platform | Fixed network assumptions reduce portability and cloud environment compatibility. | Externalized config can increase complexity for local onboarding. |
| SA-19: Filesystem-coupled Derby system/log paths from [Filesystem paths](REDISCOVERY_SPEC.md#L291) | replace-with-platform | Local filesystem coupling is brittle in containerized/ephemeral runtimes. | Requires centralized logging/storage adaptation effort. |
| SA-20: Service/JSP reporting authority conflict from [Open Question 1](REDISCOVERY_SPEC.md#L299) | keep-as-is | Behavior authority is unresolved; freezing both paths during audit avoids premature normalization. | Delays simplification and keeps duplicated reporting logic active. |

## 2) Required Coverage Matrix

The matrix below shows that the audit spans all required areas.

| required coverage area | covered rows |
|---|---|
| Home-grown code with modern equivalents | SA-04, SA-05, SA-11, SA-15 |
| Dated integrations | SA-10, SA-13 |
| Unfit data stores | SA-08, SA-09 |
| EOL/obsolete runtime-framework usage patterns | SA-06, SA-07 |
| Cloud-hostile operational assumptions | SA-17, SA-18, SA-19 |

## 3) Audit Notes (Non-Architectural)

- Rows marked keep-as-is are behavioral freeze decisions for parity, not long-term endorsement.
- Rows marked retire indicate anti-pattern elimination targets, but this audit intentionally avoids prescribing a destination architecture.
- Open-question-linked rows (SA-12, SA-14, SA-20) should not be converted into implementation decisions without explicit business adjudication.
