# Phase 3b Module Rewrite Specification: Reporting Module (Loop 2)

Date: 2026-07-08
Scope: Single-module rewrite specification only (no code)
Module boundary source: [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md)
Decision source: [REPORTING_DECISIONS_PHASE3B.md](REPORTING_DECISIONS_PHASE3B.md)
Rediscovery source: [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)
Substitution source: [SUBSTITUTION_AUDIT_PHASE1.md](SUBSTITUTION_AUDIT_PHASE1.md)

## 0) Selected Module and Legacy Surface

Selected module for this loop: Reporting module.

Legacy compatibility surface to preserve for gradual switching:
- Existing reporting entry point in [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp)
- Existing report selector behavior and parameters in [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L69)

Authoritative decisions applied:
- Service-layer logic is authoritative where available: [REPORTING_DECISIONS_PHASE3B.md](REPORTING_DECISIONS_PHASE3B.md)
- Calendar month end is authoritative: [REPORTING_DECISIONS_PHASE3B.md](REPORTING_DECISIONS_PHASE3B.md)

In-scope phase-1 rule traces:
- [BR-03](REDISCOVERY_SPEC.md#L29)
- [BR-11](REDISCOVERY_SPEC.md#L111)
- [BR-12](REDISCOVERY_SPEC.md#L124)

## 1) Deliverable 1: Functional Requirements for Reporting Module

Each requirement maps to at least one phase-1 business rule.

### FR-REP-001 Report type support
The module shall support three report types with legacy-compatible selection semantics:
- customer
- monthly
- revenue
Trace: [BR-11](REDISCOVERY_SPEC.md#L111)

### FR-REP-002 Customer bill report composition
For customer report type, the module shall provide line-level output containing date, user, category, hours, rate, amount, and note, with total hours and total amount.
Trace: [BR-11](REDISCOVERY_SPEC.md#L111), [BR-02](REDISCOVERY_SPEC.md#L18)

### FR-REP-003 Monthly summary aggregation
For monthly report type, the module shall aggregate by customer and return total hours and total revenue per customer plus monthly totals.
Trace: [BR-11](REDISCOVERY_SPEC.md#L111), [BR-03](REDISCOVERY_SPEC.md#L29)

### FR-REP-004 Monthly period boundary policy
Monthly report date filtering shall use calendar month end as authoritative boundary policy.
Trace: [BR-12](REDISCOVERY_SPEC.md#L124), [REPORTING_DECISIONS_PHASE3B.md](REPORTING_DECISIONS_PHASE3B.md)

### FR-REP-005 Revenue summary by customer
For revenue report type, the module shall produce by-customer totals including total hours, total revenue, and average rate.
Trace: [BR-11](REDISCOVERY_SPEC.md#L111)

### FR-REP-006 Revenue summary by category
For revenue report type, the module shall produce by-category totals including hourly rate, total hours, and total revenue.
Trace: [BR-11](REDISCOVERY_SPEC.md#L111)

### FR-REP-007 Monetary arithmetic consistency
Reporting calculations shall use consistent decimal semantics aligned with authoritative billing logic.
Trace: [BR-02](REDISCOVERY_SPEC.md#L18), [BR-13](REDISCOVERY_SPEC.md#L133), [SA-07](SUBSTITUTION_AUDIT_PHASE1.md#L17)

### FR-REP-008 Remove direct SQL-in-view behavior from module internals
Reporting module internals shall not rely on page-level SQL execution as business authority.
Trace: [BR-11](REDISCOVERY_SPEC.md#L111), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16)

## 2) Deliverable 2: Behavior-Preserving Acceptance Tests

All tests are module-level black-box tests. They preserve phase-1 report behavior where not superseded by an approved decision.

| test id | given | when | then | traces |
|---|---|---|---|---|
| AT-REP-001 | Report request uses customer type with existing customer and entries | execute customer report | Returns customer bill lines and totals with expected fields | FR-REP-001, FR-REP-002; [BR-11](REDISCOVERY_SPEC.md#L111) |
| AT-REP-002 | Report request uses monthly type with data across multiple customers | execute monthly report for a month | Returns grouped customer rows and monthly totals | FR-REP-001, FR-REP-003; [BR-11](REDISCOVERY_SPEC.md#L111), [BR-03](REDISCOVERY_SPEC.md#L29) |
| AT-REP-003 | Report request uses revenue type with mixed customer and category activity | execute revenue report | Returns by-customer and by-category summary sections with totals | FR-REP-001, FR-REP-005, FR-REP-006; [BR-11](REDISCOVERY_SPEC.md#L111) |
| AT-REP-004 | Monthly report for February in non-leap year with entries on 28th and none after | execute monthly report | Includes valid in-month entries and does not fail on day-boundary creation | FR-REP-004; [BR-12](REDISCOVERY_SPEC.md#L124), [REPORTING_DECISIONS_PHASE3B.md](REPORTING_DECISIONS_PHASE3B.md) |
| AT-REP-005 | Monthly report for leap-year February with entry on 29th | execute monthly report | Includes 29th entry as in-period | FR-REP-004; [BR-12](REDISCOVERY_SPEC.md#L124), [REPORTING_DECISIONS_PHASE3B.md](REPORTING_DECISIONS_PHASE3B.md) |
| AT-REP-006 | Customer bill and reporting use same underlying entries and rates | execute billing and customer report projections | Monetary totals remain semantically consistent with authoritative billing logic | FR-REP-007; [BR-02](REDISCOVERY_SPEC.md#L18), [BR-13](REDISCOVERY_SPEC.md#L133) |
| AT-REP-007 | Reporting internals are exercised in rewritten path | execute each report type | No direct SQL-in-view dependency in module core behavior | FR-REP-008; [BR-11](REDISCOVERY_SPEC.md#L111), [SA-06](SUBSTITUTION_AUDIT_PHASE1.md#L16) |

### FR-to-test coverage matrix

| functional requirement | acceptance tests |
|---|---|
| FR-REP-001 | AT-REP-001, AT-REP-002, AT-REP-003 |
| FR-REP-002 | AT-REP-001 |
| FR-REP-003 | AT-REP-002 |
| FR-REP-004 | AT-REP-004, AT-REP-005 |
| FR-REP-005 | AT-REP-003 |
| FR-REP-006 | AT-REP-003 |
| FR-REP-007 | AT-REP-006 |
| FR-REP-008 | AT-REP-007 |

## 3) Deliverable 3: Interface Contract Identical to Legacy (for gradual switching)

### 3.1 Legacy-compatible request contract
For gradual traffic switching, the reporting compatibility endpoint shall preserve legacy request shape:
- Path compatibility: reports.jsp-equivalent route
- Method compatibility: GET
- Parameters:
  - reportType with values customer, monthly, revenue
  - customerId for customer report
  - year and month for monthly report
Trace: [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L69)

### 3.2 Legacy-compatible response contract
The compatibility response shall preserve legacy-visible section structure:
- Customer Bill section with line table and totals
- Monthly Summary section with per-customer totals and monthly total
- Revenue Summary with by-customer and by-category sections
Trace: [BR-11](REDISCOVERY_SPEC.md#L111)

### 3.3 Error-path compatibility
Compatibility endpoint shall preserve user-visible report-generation error handling semantics (error section rendering) while routing to rewritten module internals.
Trace: [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L291)

### 3.4 Gradual switching model
- Keep identical external route and parameters for compatibility path.
- Internally switch execution from legacy SQL view path to rewritten reporting module.
- Dual-run compare mode in pre-cutover environments validates AT-REP-001 through AT-REP-007 before traffic cutover.
Trace: [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L223)

## 4) Constraint Compliance and Scope Notes

- No behavior was invented where rules were ambiguous.
- Ambiguities for reporting authority and monthly boundary were resolved first in [REPORTING_DECISIONS_PHASE3B.md](REPORTING_DECISIONS_PHASE3B.md).
- This loop is architecture/spec only and defines no code tasks.

## 5) Loop Continuation Note

Loop 2 complete for Reporting module specification.
Next phase-3b loop should proceed with Time Entry module, gated by unresolved policy decisions in:
- [Open Question 2](REDISCOVERY_SPEC.md#L266)
- [Open Question 3](REDISCOVERY_SPEC.md#L270)
- [Open Question 8](REDISCOVERY_SPEC.md#L290)
