# Substitution Audit (Phase 1)

Date: 2026-07-08
Scope: Audit only. This document identifies substitution candidates and rationale without proposing a target architecture.
Traceability rule: Every row links to a specific rediscovery entry in [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md).

## Deliverable 1: Substitution-Audit Table

| legacy element | proposal (keep-as-is \| replace-with-library \| replace-with-platform \| retire) | reason | trade-off |
|---|---|---|---|
| SA-01: Core billing arithmetic and customer-existence gate in service flow ([BR-01](REDISCOVERY_SPEC.md#L8), [BR-02](REDISCOVERY_SPEC.md#L18)) | keep-as-is | These are domain rules, not accidental infrastructure. They encode bill integrity and should be preserved. | Keeping as-is preserves behavior but does not reduce technical debt by itself. |
| SA-02: Hand-rolled JDBC DAO CRUD layer and manual mapping ([BR-07](REDISCOVERY_SPEC.md#L72), [BR-10](REDISCOVERY_SPEC.md#L102), [Data Model Summary](REDISCOVERY_SPEC.md#L152)) | replace-with-library | Home-grown persistence plumbing duplicates commodity functionality (mapping, parameter binding, transaction conventions). | Reduced boilerplate and consistency gains vs migration effort and possible SQL-level control loss. |
| SA-03: String-accumulating custom validation routine for billable hours ([BR-04](REDISCOVERY_SPEC.md#L37), [Important non-invariants](REDISCOVERY_SPEC.md#L202)) | replace-with-library | Validation behavior exists but is not consistently enforced across entry points; a standard validation library improves reuse and enforceability. | Better consistency vs refactoring effort and potential message format changes. |
| SA-04: Date/time helper utilities and mixed temporal handling ([BR-04](REDISCOVERY_SPEC.md#L37), [Open Question 8](REDISCOVERY_SPEC.md#L290), [Joda-Time dependency](REDISCOVERY_SPEC.md#L256)) | replace-with-library | Home-grown date utility and mixed date types increase ambiguity and maintenance cost; modern standard time APIs are better supported. | Cleaner time semantics vs conversion work and regression risk around date boundaries. |
| SA-05: JSP scriptlet-based page logic and rendering ([BR-08](REDISCOVERY_SPEC.md#L81), [BR-13](REDISCOVERY_SPEC.md#L133)) | retire | Scriptlets mix presentation, business logic, and data access concerns; hard to test and evolve safely. | Improved maintainability vs significant rewrite cost for UI flows. |
| SA-06: Reports page direct JDBC and SQL in JSP, bypassing DAOs/services ([BR-11](REDISCOVERY_SPEC.md#L111), [Open Question 1](REDISCOVERY_SPEC.md#L262)) | retire | Parallel data-access paths create drift risk and duplicate business logic paths. | Better consistency and testability vs short-term reporting refactor effort. |
| SA-07: Duplicate calculation paths using double in JSP and BigDecimal in service ([BR-13](REDISCOVERY_SPEC.md#L133), [BR-02](REDISCOVERY_SPEC.md#L18)) | replace-with-library | Inconsistent numeric handling invites rounding divergence; standardized monetary handling libraries reduce that risk. | Financial precision consistency vs changes in display and rounding behavior that need validation. |
| SA-08: Embedded Derby as primary runtime store ([Integration: External systems](REDISCOVERY_SPEC.md#L211), [Filesystem paths](REDISCOVERY_SPEC.md#L220)) | replace-with-platform | Embedded file-based DB assumptions are operationally fragile for shared, scalable, and cloud-oriented deployment patterns. | Platform reliability and operability gains vs higher operational complexity and migration effort. |
| SA-09: Hard-coded local DB path assumptions (./data and server output dir) ([Filesystem paths](REDISCOVERY_SPEC.md#L220)) | replace-with-platform | Local filesystem coupling is cloud-hostile and complicates stateless runtime operation. | Better portability vs requirement to externalize runtime configuration and state management. |
| SA-10: Auto-create database behavior at connection URL/config ([Filesystem paths](REDISCOVERY_SPEC.md#L220), [Integration: External systems](REDISCOVERY_SPEC.md#L211)) | replace-with-platform | Runtime schema creation is convenient for demo bootstrap but weak for controlled lifecycle management. | Safer change control vs losing zero-touch local bootstrap simplicity. |
| SA-11: Hard-coded credentials and local basic registry credentials ([Identity/security integrations](REDISCOVERY_SPEC.md#L242), [Open Question 7](REDISCOVERY_SPEC.md#L286)) | replace-with-platform | Credentials in code/config are unsuitable for hardened operations and secret rotation practices. | Stronger security posture vs dependency on platform secret/identity services and rollout coordination. |
| SA-12: Fixed ports and endpoint assumptions in config and scripts ([Hard-coded endpoints and ports](REDISCOVERY_SPEC.md#L231)) | replace-with-platform | Static network assumptions reduce deploy flexibility across environments. | Better environment portability vs additional configuration surface area. |
| SA-13: Startup sample-data initialization in runtime listener ([BR-05](REDISCOVERY_SPEC.md#L53), [BR-14](REDISCOVERY_SPEC.md#L142)) | retire | Data seeding at app startup is useful for demos but risky for production-like environments. | Safer operational behavior vs less convenience for first-run demos. |
| SA-14: Environment switching via JNDI lookup fallback logic in app code ([BR-14](REDISCOVERY_SPEC.md#L142), [Integration: External systems](REDISCOVERY_SPEC.md#L211)) | replace-with-platform | Environment resolution and datasource policy are better handled by deployment/runtime config than in-app branching. | Cleaner app boundary vs tighter dependency on platform conventions. |
| SA-15: No explicit DB CHECK constraints for positive hours/rates ([Important non-invariants](REDISCOVERY_SPEC.md#L202), [Open Question 3](REDISCOVERY_SPEC.md#L270)) | replace-with-platform | Critical data integrity constraints should be enforceable at persistence boundary, not only via UI/service code paths. | Stronger invariants vs schema migration effort and legacy data cleanup requirements. |
| SA-16: Deletion semantics rely on FK failure behavior without explicit business policy ([BR-10](REDISCOVERY_SPEC.md#L102), [Open Question 6](REDISCOVERY_SPEC.md#L282)) | keep-as-is | Current behavior is technically defined by DB constraints, but business intent is unresolved; substitution should wait for policy decision. | Defers risk of wrong substitution vs continuing ambiguity for users/support teams. |
| SA-17: Monthly end-date fixed to day 31 in report logic ([BR-12](REDISCOVERY_SPEC.md#L124), [Open Question 4](REDISCOVERY_SPEC.md#L274)) | replace-with-library | Calendar boundary handling is error-prone and should use standard date-period APIs instead of manual string composition. | Better correctness vs regression testing effort for historical report parity. |
| SA-18: User create path constructor-argument mismatch ([BR-06](REDISCOVERY_SPEC.md#L59), [Open Question 5](REDISCOVERY_SPEC.md#L278)) | retire | Defect-prone object construction pattern indicates unsafe home-grown mapping assumptions. | Reduced data-quality risk vs migration/backfill effort for existing bad records. |
| SA-19: Open Liberty runtime and Jakarta EE baseline ([Integration: External systems](REDISCOVERY_SPEC.md#L211), [Build/dependency integrations](REDISCOVERY_SPEC.md#L251)) | keep-as-is | Rediscovery evidence does not show explicit runtime EOL. Premature substitution without lifecycle policy evidence would be speculative. | Avoids unnecessary churn vs potential missed optimization if lifecycle policy later changes. |
| SA-20: Joda-Time dependency ([Build/dependency integrations](REDISCOVERY_SPEC.md#L251), [Open Question 8](REDISCOVERY_SPEC.md#L290)) | replace-with-library | Joda-Time is a dated integration relative to standard modern Java time APIs and increases type-mixing risk. | Better long-term supportability vs conversion and compatibility testing cost. |

## Deliverable 2: Coverage Confirmation

This audit spans all required coverage dimensions:

- Home-grown code with modern equivalents:
  - SA-02, SA-03, SA-04, SA-07, SA-17, SA-18
- Dated integrations:
  - SA-05, SA-06, SA-20
- Unfit data stores:
  - SA-08, SA-09, SA-10
- EOL runtimes/frameworks:
  - SA-19 (status captured as keep-as-is pending explicit lifecycle evidence), plus legacy-framework retirement pressure captured in SA-05
- Cloud-hostile operational assumptions:
  - SA-09, SA-11, SA-12, SA-13, SA-14

Notes:
- This is an audit artifact, not a target architecture design.
- Rows marked keep-as-is are intentional phase-1 audit outcomes where substitution would be speculative without policy decisions.
