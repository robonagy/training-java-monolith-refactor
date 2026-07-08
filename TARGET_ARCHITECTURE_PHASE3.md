# Target Architecture Specification (Phase 3)

Date: 2026-07-08
Scope: Architecture only (no code, no per-module rewrite task breakdown)
Inputs:
- Rediscovery: [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)
- Substitution audit: [SUBSTITUTION_AUDIT_PHASE1.md](SUBSTITUTION_AUDIT_PHASE1.md)

## 0) Architectural Principles (Constraint Guardrails)

- Preserve validated business semantics for billing and reporting math.
- Remove duplicated execution paths that bypass domain logic.
- Move infrastructure policy (runtime, data access, secrets, networking) to platform configuration.
- Enforce data invariants at both application and database boundaries.
- Default migration approach is strangler-fig with backward-compatible contract boundaries.

Trace:
- [BR-01](REDISCOVERY_SPEC.md#L8), [BR-02](REDISCOVERY_SPEC.md#L18), [BR-03](REDISCOVERY_SPEC.md#L29), [BR-04](REDISCOVERY_SPEC.md#L37)
- [SA-01](SUBSTITUTION_AUDIT_PHASE1.md#L11), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16), [SA-14](SUBSTITUTION_AUDIT_PHASE1.md#L24), [SA-15](SUBSTITUTION_AUDIT_PHASE1.md#L25)

## 1) Target Runtime and Platform

### 1.1 Runtime
- Java 21 LTS application runtime.
- Spring Boot 3.x (modular monolith at first, service extraction-ready boundaries).
- Jakarta-servlet/JSP runtime is not retained as the primary runtime model.

Trace:
- [SA-05](SUBSTITUTION_AUDIT_PHASE1.md#L15), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16), [SA-20](SUBSTITUTION_AUDIT_PHASE1.md#L30), [SA-19](SUBSTITUTION_AUDIT_PHASE1.md#L29)

### 1.2 Platform
- Containerized deployment on Kubernetes (or equivalent managed container platform).
- Managed relational database service (PostgreSQL class) as primary transactional store.
- Managed ingress/load balancing with environment-driven host/port routing.
- Managed identity provider (OIDC/OAuth2) and managed secrets store.

Trace:
- [SA-08](SUBSTITUTION_AUDIT_PHASE1.md#L18), [SA-09](SUBSTITUTION_AUDIT_PHASE1.md#L19), [SA-10](SUBSTITUTION_AUDIT_PHASE1.md#L20), [SA-11](SUBSTITUTION_AUDIT_PHASE1.md#L21), [SA-12](SUBSTITUTION_AUDIT_PHASE1.md#L22), [SA-14](SUBSTITUTION_AUDIT_PHASE1.md#L24)
- [Integration Inventory](REDISCOVERY_SPEC.md#L209)

## 2) Module and Service Boundaries

Target starts as a modular monolith with explicit domain modules and anti-corruption boundaries; extraction to independent services remains optional and evidence-driven.

### 2.1 Domain modules
- Customer module: customer lifecycle and identity attributes.
- User module: internal billable users and assignment references.
- Category/Pricing module: billing categories and hourly rates.
- Time Entry module: billable-hour capture, validation, and lifecycle.
- Billing module: invoice/bill generation, line calculations, totals.
- Reporting module: monthly and revenue summaries, customer bill views.

Trace:
- [BR-01](REDISCOVERY_SPEC.md#L8) through [BR-14](REDISCOVERY_SPEC.md#L142)
- [SA-01](SUBSTITUTION_AUDIT_PHASE1.md#L11), [SA-02](SUBSTITUTION_AUDIT_PHASE1.md#L12), [SA-03](SUBSTITUTION_AUDIT_PHASE1.md#L13), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16)

### 2.2 Cross-module interaction rules
- Reporting reads through domain query interfaces, not direct page-level SQL.
- Billing is the canonical owner of monetary arithmetic and totals.
- Time Entry owns validation policy execution before persistence.
- Platform adapter layer owns external systems (database, identity, observability, secrets).

Trace:
- [BR-02](REDISCOVERY_SPEC.md#L18), [BR-04](REDISCOVERY_SPEC.md#L37), [BR-11](REDISCOVERY_SPEC.md#L111), [BR-13](REDISCOVERY_SPEC.md#L133)
- [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16), [SA-07](SUBSTITUTION_AUDIT_PHASE1.md#L17), [SA-14](SUBSTITUTION_AUDIT_PHASE1.md#L24)

### 2.3 Service decomposition trigger criteria (future, not immediate)
- Only extract module into standalone service when one or more conditions hold:
- Independent scaling profile is sustained.
- Distinct release cadence is required.
- Strong ownership boundary exists.
- Integration contract is stable and versioned.

Trace:
- [Open Questions](REDISCOVERY_SPEC.md#L260)
- [SA coverage notes](SUBSTITUTION_AUDIT_PHASE1.md#L35)

## 3) Data Model and Migration Path from Legacy Schema

### 3.1 Target data model (logical)
- Retain core entities: users, customers, billing_categories, billable_hours.
- Normalize money and hours precision with strict decimal semantics.
- Preserve existing relationship cardinalities and foreign keys.
- Add explicit constraint layer:
- Positive hours constraint.
- Non-negative hourly rate constraint.
- Referential integrity retained.

Trace:
- [Data Model Summary](REDISCOVERY_SPEC.md#L152), [Relationships](REDISCOVERY_SPEC.md#L171), [DB-enforced invariants](REDISCOVERY_SPEC.md#L179)
- [Important non-invariants](REDISCOVERY_SPEC.md#L202)
- [SA-15](SUBSTITUTION_AUDIT_PHASE1.md#L25), [SA-07](SUBSTITUTION_AUDIT_PHASE1.md#L17)

### 3.2 Legacy compatibility model
- Keep legacy IDs as stable business keys during transition.
- Introduce immutable audit columns (created_at/updated_at provenance policy) in target schema.
- Preserve historical semantics for billing totals and report outputs as acceptance baseline.

Trace:
- [BR-02](REDISCOVERY_SPEC.md#L18), [BR-03](REDISCOVERY_SPEC.md#L29), [BR-11](REDISCOVERY_SPEC.md#L111)
- [SA-01](SUBSTITUTION_AUDIT_PHASE1.md#L11), [SA-17](SUBSTITUTION_AUDIT_PHASE1.md#L27)

### 3.3 Migration path (schema and data)
- Path M1: Establish target schema in managed database with compatibility views for legacy naming.
- Path M2: Bulk-load legacy data with deterministic type conversions (date/time and decimal).
- Path M3: Reconcile known defect classes in controlled data-quality pass (including user name/email swap candidates).
- Path M4: Run dual-read verification for key financial reports before traffic cutover.
- Path M5: Finalize constraints after reconciliation window closes.

Trace:
- [BR-06](REDISCOVERY_SPEC.md#L59), [BR-12](REDISCOVERY_SPEC.md#L124), [BR-13](REDISCOVERY_SPEC.md#L133)
- [Open Question 3](REDISCOVERY_SPEC.md#L270), [Open Question 5](REDISCOVERY_SPEC.md#L278), [Open Question 8](REDISCOVERY_SPEC.md#L290)
- [SA-08](SUBSTITUTION_AUDIT_PHASE1.md#L18), [SA-15](SUBSTITUTION_AUDIT_PHASE1.md#L25), [SA-18](SUBSTITUTION_AUDIT_PHASE1.md#L28), [SA-20](SUBSTITUTION_AUDIT_PHASE1.md#L30)

## 4) Integration Contracts Replacing Flagged Legacy Integrations

### 4.1 Persistence contract
- Replace direct JDBC spread with repository abstraction and transaction boundary contract.
- Contract owner: platform persistence adapter.
- Contract guarantees: typed queries, transaction demarcation, optimistic locking policy.

Replaces:
- Hand-rolled DAO/JDBC pattern and page-level SQL.

Trace:
- [SA-02](SUBSTITUTION_AUDIT_PHASE1.md#L12), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16), [BR-11](REDISCOVERY_SPEC.md#L111)

### 4.2 Validation contract
- Replace string-concatenated ad-hoc validation with declarative validation and standardized error model.
- Contract owner: Time Entry and Billing domain boundaries.
- Contract guarantees: invariant checks on all write paths.

Trace:
- [SA-03](SUBSTITUTION_AUDIT_PHASE1.md#L13), [BR-04](REDISCOVERY_SPEC.md#L37), [BR-08](REDISCOVERY_SPEC.md#L81)

### 4.3 Time and calendar contract
- Replace mixed temporal types/utilities with a single Java time contract.
- Contract owner: shared domain foundation module.
- Contract guarantees: timezone policy, period boundary correctness, month-end handling.

Trace:
- [SA-04](SUBSTITUTION_AUDIT_PHASE1.md#L14), [SA-17](SUBSTITUTION_AUDIT_PHASE1.md#L27), [SA-20](SUBSTITUTION_AUDIT_PHASE1.md#L30), [BR-12](REDISCOVERY_SPEC.md#L124)

### 4.4 Identity and access contract
- Replace local basic registry credentials with OIDC-based authentication and role mapping.
- Contract owner: platform security adapter.
- Contract guarantees: token validation, role claims mapping, no in-repo credentials.

Trace:
- [SA-11](SUBSTITUTION_AUDIT_PHASE1.md#L21), [Open Question 7](REDISCOVERY_SPEC.md#L286)

### 4.5 Configuration and secret contract
- Replace hard-coded ports, DB URLs, filesystem paths, and credentials with externalized configuration and secret references.
- Contract owner: runtime platform configuration layer.
- Contract guarantees: environment-specific overrides, secret rotation support, immutable deployment artifacts.

Trace:
- [SA-09](SUBSTITUTION_AUDIT_PHASE1.md#L19), [SA-10](SUBSTITUTION_AUDIT_PHASE1.md#L20), [SA-11](SUBSTITUTION_AUDIT_PHASE1.md#L21), [SA-12](SUBSTITUTION_AUDIT_PHASE1.md#L22), [SA-14](SUBSTITUTION_AUDIT_PHASE1.md#L24)

### 4.6 UI contract
- Replace JSP/scriptlets with API-driven UI boundary.
- Contract owner: presentation layer.
- Contract guarantees: no direct DB access from presentation, typed API responses, domain-owned calculations.

Trace:
- [SA-05](SUBSTITUTION_AUDIT_PHASE1.md#L15), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16), [BR-11](REDISCOVERY_SPEC.md#L111), [BR-13](REDISCOVERY_SPEC.md#L133)

## 5) Cross-Cutting Concerns

### 5.1 Authentication and authorization
- OIDC/OAuth2 for user authentication.
- Role-based authorization at API boundary and domain policy layer.
- Authorization decisions centralized and auditable.

Trace:
- [SA-11](SUBSTITUTION_AUDIT_PHASE1.md#L21), [Open Question 7](REDISCOVERY_SPEC.md#L286)

### 5.2 Logging and auditability
- Structured JSON logs with correlation IDs.
- Domain audit events for billing, rate changes, and deletions.
- Sensitive-data redaction policy.

Trace:
- [BR-02](REDISCOVERY_SPEC.md#L18), [BR-09](REDISCOVERY_SPEC.md#L93), [BR-10](REDISCOVERY_SPEC.md#L102)
- [SA-07](SUBSTITUTION_AUDIT_PHASE1.md#L17), [SA-16](SUBSTITUTION_AUDIT_PHASE1.md#L26)

### 5.3 Configuration
- Twelve-factor configuration model.
- Environment-specific values supplied by platform at deploy time.

Trace:
- [SA-09](SUBSTITUTION_AUDIT_PHASE1.md#L19), [SA-12](SUBSTITUTION_AUDIT_PHASE1.md#L22), [SA-14](SUBSTITUTION_AUDIT_PHASE1.md#L24)

### 5.4 Secrets
- All credentials and keys sourced from managed secrets service.
- Application only references secret identifiers.

Trace:
- [SA-11](SUBSTITUTION_AUDIT_PHASE1.md#L21), [Identity/security integrations](REDISCOVERY_SPEC.md#L242)

### 5.5 Observability
- OpenTelemetry traces, metrics, and logs.
- Domain SLOs for report generation latency and billing correctness checks.
- Health/readiness probes and dependency checks.

Trace:
- [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16), [SA-08](SUBSTITUTION_AUDIT_PHASE1.md#L18), [BR-03](REDISCOVERY_SPEC.md#L29), [BR-11](REDISCOVERY_SPEC.md#L111)

## 6) Migration Strategy (Default: Strangler-Fig)

### 6.1 Strategy shape
- Introduce a facade/edge gateway in front of legacy and new runtime.
- Route selected capabilities to new architecture incrementally by domain boundary.
- Keep legacy behavior as fallback until parity gates pass.

Trace:
- [BR-11](REDISCOVERY_SPEC.md#L111), [Open Question 1](REDISCOVERY_SPEC.md#L262)
- [SA-05](SUBSTITUTION_AUDIT_PHASE1.md#L15), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16)

### 6.2 Parity gates
- Gate G1: Billing math parity against preserved business rules.
- Gate G2: Reporting parity for monthly/revenue/customer bills.
- Gate G3: Validation parity for hours/date/rate constraints.
- Gate G4: Operational parity for auth/logging/secrets/config and rollback capability.

Trace:
- [BR-01](REDISCOVERY_SPEC.md#L8), [BR-02](REDISCOVERY_SPEC.md#L18), [BR-03](REDISCOVERY_SPEC.md#L29), [BR-04](REDISCOVERY_SPEC.md#L37), [BR-11](REDISCOVERY_SPEC.md#L111), [BR-12](REDISCOVERY_SPEC.md#L124)
- [SA-03](SUBSTITUTION_AUDIT_PHASE1.md#L13), [SA-07](SUBSTITUTION_AUDIT_PHASE1.md#L17), [SA-17](SUBSTITUTION_AUDIT_PHASE1.md#L27)

### 6.3 Legacy retirement sequence (architectural level)
- Retire presentation-coupled logic first (JSP/scriptlet/report SQL path).
- Retire embedded DB and local filesystem assumptions after stable managed DB adoption.
- Retire in-app environment switching and local credentials once platform-native config/secrets are active.

Trace:
- [SA-05](SUBSTITUTION_AUDIT_PHASE1.md#L15), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16), [SA-08](SUBSTITUTION_AUDIT_PHASE1.md#L18), [SA-09](SUBSTITUTION_AUDIT_PHASE1.md#L19), [SA-11](SUBSTITUTION_AUDIT_PHASE1.md#L21), [SA-14](SUBSTITUTION_AUDIT_PHASE1.md#L24)

## 7) Risks

- R1: Financial parity drift between legacy and new reporting/billing calculations.
- R2: Data-quality issues during migration (name/email swaps, invalid rate/hour values, date ambiguities).
- R3: Constraint hardening may reject legacy records currently accepted.
- R4: Identity transition may break access assumptions if role mapping is incomplete.
- R5: Cutover complexity from dual paths can increase operational incidents.
- R6: Deletion-policy ambiguity remains unresolved and can block final data contract closure.

Trace:
- [BR-02](REDISCOVERY_SPEC.md#L18), [BR-06](REDISCOVERY_SPEC.md#L59), [BR-10](REDISCOVERY_SPEC.md#L102), [BR-12](REDISCOVERY_SPEC.md#L124), [BR-13](REDISCOVERY_SPEC.md#L133)
- [Open Question 3](REDISCOVERY_SPEC.md#L270), [Open Question 5](REDISCOVERY_SPEC.md#L278), [Open Question 6](REDISCOVERY_SPEC.md#L282), [Open Question 8](REDISCOVERY_SPEC.md#L290)
- [SA-07](SUBSTITUTION_AUDIT_PHASE1.md#L17), [SA-15](SUBSTITUTION_AUDIT_PHASE1.md#L25), [SA-16](SUBSTITUTION_AUDIT_PHASE1.md#L26), [SA-18](SUBSTITUTION_AUDIT_PHASE1.md#L28)

## 8) Decision Trace Matrix

| architecture choice | trace source(s) |
|---|---|
| AC-01 Preserve billing semantics as invariant | [BR-01](REDISCOVERY_SPEC.md#L8), [BR-02](REDISCOVERY_SPEC.md#L18), [SA-01](SUBSTITUTION_AUDIT_PHASE1.md#L11) |
| AC-02 Retire JSP/scriptlet execution model | [BR-08](REDISCOVERY_SPEC.md#L81), [BR-13](REDISCOVERY_SPEC.md#L133), [SA-05](SUBSTITUTION_AUDIT_PHASE1.md#L15) |
| AC-03 Retire direct SQL-in-view reporting path | [BR-11](REDISCOVERY_SPEC.md#L111), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16) |
| AC-04 Standardize persistence via framework abstraction | [SA-02](SUBSTITUTION_AUDIT_PHASE1.md#L12), [BR-07](REDISCOVERY_SPEC.md#L72), [BR-10](REDISCOVERY_SPEC.md#L102) |
| AC-05 Move to managed relational platform DB | [SA-08](SUBSTITUTION_AUDIT_PHASE1.md#L18), [SA-09](SUBSTITUTION_AUDIT_PHASE1.md#L19), [SA-10](SUBSTITUTION_AUDIT_PHASE1.md#L20) |
| AC-06 Externalize secrets/config/network assumptions | [SA-11](SUBSTITUTION_AUDIT_PHASE1.md#L21), [SA-12](SUBSTITUTION_AUDIT_PHASE1.md#L22), [SA-14](SUBSTITUTION_AUDIT_PHASE1.md#L24) |
| AC-07 Standardize time/validation/money handling | [SA-03](SUBSTITUTION_AUDIT_PHASE1.md#L13), [SA-04](SUBSTITUTION_AUDIT_PHASE1.md#L14), [SA-07](SUBSTITUTION_AUDIT_PHASE1.md#L17), [SA-17](SUBSTITUTION_AUDIT_PHASE1.md#L27), [SA-20](SUBSTITUTION_AUDIT_PHASE1.md#L30) |
| AC-08 Apply strangler-fig migration with parity gates | [Open Question 1](REDISCOVERY_SPEC.md#L262), [SA-05](SUBSTITUTION_AUDIT_PHASE1.md#L15), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16) |
| AC-09 Keep unresolved deletion business policy unchanged until policy decision | [BR-10](REDISCOVERY_SPEC.md#L102), [Open Question 6](REDISCOVERY_SPEC.md#L282), [SA-16](SUBSTITUTION_AUDIT_PHASE1.md#L26) |
