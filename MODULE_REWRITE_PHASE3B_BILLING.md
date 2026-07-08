# Phase 3b Module Rewrite Specification: Billing Module (Loop 1)

Date: 2026-07-08
Scope: Single-module rewrite specification only (no code)
Module boundary source: [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md)
Rediscovery source: [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)
Substitution source: [SUBSTITUTION_AUDIT_PHASE1.md](SUBSTITUTION_AUDIT_PHASE1.md)

## 0) Selected Module and Legacy Surface

Selected module for this loop: Billing module.

Legacy surface to preserve for gradual traffic switching:
- Billing service API currently implemented in [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java)
- Method signatures at source:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L24)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L54)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L89)

In-scope behavior source rules:
- [BR-01](REDISCOVERY_SPEC.md#L8)
- [BR-02](REDISCOVERY_SPEC.md#L18)
- [BR-03](REDISCOVERY_SPEC.md#L29)
- [BR-04](REDISCOVERY_SPEC.md#L37)

Out of scope for this loop:
- Reporting module direct JSP SQL path from [BR-11](REDISCOVERY_SPEC.md#L111) (covered in separate module loop).

## 1) Deliverable 1: Functional Requirements for Billing Module

Each requirement maps to at least one phase-1 business rule.

### FR-BILL-001 Customer existence gate
The module shall reject bill generation when customer does not exist.
- Legacy-compatible behavior: throw RuntimeException with message Customer not found.
- Trace: [BR-01](REDISCOVERY_SPEC.md#L8)

### FR-BILL-002 Customer bill total calculation semantics
The module shall compute customer bill totals as:
- totalAmount = sum(hours multiplied by category hourlyRate)
- totalHours = sum(hours)
Only entries with resolvable category records are included.
- Trace: [BR-02](REDISCOVERY_SPEC.md#L18)

### FR-BILL-003 Customer bill response schema
The module shall return a map containing keys:
- customer
- billableHours
- totalHours
- totalAmount
- generatedDate
with types and meaning compatible with the legacy behavior.
- Trace: [BR-01](REDISCOVERY_SPEC.md#L8), [BR-02](REDISCOVERY_SPEC.md#L18)

### FR-BILL-004 Monthly report inclusion filter
The module shall include a billable-hour row in monthly totals only when date_logged year and month exactly match requested year and month.
- Trace: [BR-03](REDISCOVERY_SPEC.md#L29)

### FR-BILL-005 Monthly report aggregation semantics
The module shall compute monthly report fields:
- totalRevenue
- totalHours
- revenueByCategory where key is category name and value is summed line revenue
and include generatedDate.
Rows with missing categories are excluded from revenue/hour aggregation.
- Trace: [BR-03](REDISCOVERY_SPEC.md#L29)

### FR-BILL-006 Validation: referential checks
The module shall validate customer and category existence for a billable hour and append legacy error fragments when invalid.
- Invalid customer ID.
- Invalid category ID.
- Trace: [BR-04](REDISCOVERY_SPEC.md#L37)

### FR-BILL-007 Validation: hours and date checks
The module shall append legacy validation fragments for:
- Hours must be greater than zero.
- Date logged is required.
- Date logged cannot be in the future.
- Warning: Hours logged on weekend.
- Trace: [BR-04](REDISCOVERY_SPEC.md#L37)

### FR-BILL-008 Validation output formatting
Validation output shall remain a single trimmed string with message fragments appended in legacy order.
- Trace: [BR-04](REDISCOVERY_SPEC.md#L37)

### FR-BILL-009 Validation DB error handling semantics
Validation shall append legacy DB error fragments when DAO lookup throws SQL exception:
- Database error checking customer.
- Database error checking category.
- Trace: [BR-04](REDISCOVERY_SPEC.md#L37)

### FR-BILL-010 Numeric precision policy for billing math
Module internal billing math shall preserve BigDecimal arithmetic semantics used by legacy BillingService for totals and line amounts.
- Trace: [BR-02](REDISCOVERY_SPEC.md#L18), [SA-07](SUBSTITUTION_AUDIT_PHASE1.md#L17), [SA-01](SUBSTITUTION_AUDIT_PHASE1.md#L11)

## 2) Deliverable 2: Behavior-Preserving Acceptance Tests

All tests are black-box at Billing service contract level. Each functional requirement maps to at least one test.

| test id | given | when | then | traces |
|---|---|---|---|---|
| AT-BILL-001 | Customer exists and has two billable entries with resolvable categories | generateCustomerBill(customerId) | Returns map with keys customer, billableHours, totalHours, totalAmount, generatedDate | FR-BILL-001, FR-BILL-002, FR-BILL-003; [BR-01](REDISCOVERY_SPEC.md#L8), [BR-02](REDISCOVERY_SPEC.md#L18) |
| AT-BILL-002 | Customer id does not exist | generateCustomerBill(missingId) | Throws RuntimeException with exact message Customer not found | FR-BILL-001; [BR-01](REDISCOVERY_SPEC.md#L8) |
| AT-BILL-003 | Billable hours include one row whose category lookup returns null | generateCustomerBill(customerId) | Null-category row does not contribute to totalHours or totalAmount | FR-BILL-002; [BR-02](REDISCOVERY_SPEC.md#L18) |
| AT-BILL-004 | Billable rows span multiple months and years | generateMonthlyReport(targetYear, targetMonth) | Only rows matching target year and month contribute | FR-BILL-004; [BR-03](REDISCOVERY_SPEC.md#L29) |
| AT-BILL-005 | Matching rows exist across multiple categories | generateMonthlyReport(targetYear, targetMonth) | revenueByCategory aggregates by category name with revenue sums | FR-BILL-005; [BR-03](REDISCOVERY_SPEC.md#L29) |
| AT-BILL-006 | BillableHour has nonexistent customerId | validateBillableHour(hour) | Output contains Invalid customer ID. | FR-BILL-006; [BR-04](REDISCOVERY_SPEC.md#L37) |
| AT-BILL-007 | BillableHour has nonexistent categoryId | validateBillableHour(hour) | Output contains Invalid category ID. | FR-BILL-006; [BR-04](REDISCOVERY_SPEC.md#L37) |
| AT-BILL-008 | BillableHour hours is null and then 0 and then negative | validateBillableHour(hour) | Output contains Hours must be greater than zero. in each invalid case | FR-BILL-007; [BR-04](REDISCOVERY_SPEC.md#L37) |
| AT-BILL-009 | BillableHour dateLogged is null | validateBillableHour(hour) | Output contains Date logged is required. | FR-BILL-007; [BR-04](REDISCOVERY_SPEC.md#L37) |
| AT-BILL-010 | BillableHour dateLogged is after current date | validateBillableHour(hour) | Output contains Date logged cannot be in the future. | FR-BILL-007; [BR-04](REDISCOVERY_SPEC.md#L37) |
| AT-BILL-011 | BillableHour dateLogged is weekend and otherwise valid | validateBillableHour(hour) | Output contains Warning: Hours logged on weekend. | FR-BILL-007; [BR-04](REDISCOVERY_SPEC.md#L37) |
| AT-BILL-012 | BillableHour fails multiple checks simultaneously | validateBillableHour(hour) | Output is one trimmed string, concatenated in legacy order: customer, category, hours, date checks | FR-BILL-008; [BR-04](REDISCOVERY_SPEC.md#L37) |
| AT-BILL-013 | DAO customer lookup throws SQLException | validateBillableHour(hour) | Output contains Database error checking customer. | FR-BILL-009; [BR-04](REDISCOVERY_SPEC.md#L37) |
| AT-BILL-014 | DAO category lookup throws SQLException | validateBillableHour(hour) | Output contains Database error checking category. | FR-BILL-009; [BR-04](REDISCOVERY_SPEC.md#L37) |
| AT-BILL-015 | Decimal hours and rates with fractional cents in intermediate multiplication | generateCustomerBill and generateMonthlyReport | Totals equal legacy BigDecimal computation outcomes | FR-BILL-010; [BR-02](REDISCOVERY_SPEC.md#L18), [SA-07](SUBSTITUTION_AUDIT_PHASE1.md#L17) |

### FR-to-test coverage matrix

| functional requirement | acceptance tests |
|---|---|
| FR-BILL-001 | AT-BILL-001, AT-BILL-002 |
| FR-BILL-002 | AT-BILL-001, AT-BILL-003 |
| FR-BILL-003 | AT-BILL-001 |
| FR-BILL-004 | AT-BILL-004 |
| FR-BILL-005 | AT-BILL-005 |
| FR-BILL-006 | AT-BILL-006, AT-BILL-007 |
| FR-BILL-007 | AT-BILL-008, AT-BILL-009, AT-BILL-010, AT-BILL-011 |
| FR-BILL-008 | AT-BILL-012 |
| FR-BILL-009 | AT-BILL-013, AT-BILL-014 |
| FR-BILL-010 | AT-BILL-015 |

## 3) Deliverable 3: Interface Contract Identical to Legacy (for gradual switching)

### 3.1 Public service contract (must remain identical)

Service name compatibility:
- BillingService contract compatible with [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java)

Method signatures to preserve exactly:
- Map<String, Object> generateCustomerBill(Long customerId) throws SQLException
- Map<String, Object> generateMonthlyReport(int year, int month) throws SQLException
- String validateBillableHour(BillableHour hour)

Signature trace:
- [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L24)
- [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L54)
- [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L89)

### 3.2 Return payload compatibility

generateCustomerBill map keys and value compatibility:
- customer: Customer
- billableHours: List<BillableHour>
- totalHours: BigDecimal
- totalAmount: BigDecimal
- generatedDate: LocalDate

generateMonthlyReport map keys and value compatibility:
- year: int
- month: int
- totalRevenue: BigDecimal
- totalHours: BigDecimal
- revenueByCategory: Map<String, BigDecimal>
- generatedDate: LocalDate

Behavior trace:
- [BR-02](REDISCOVERY_SPEC.md#L18)
- [BR-03](REDISCOVERY_SPEC.md#L29)

### 3.3 Error and validation compatibility

- generateCustomerBill missing customer behavior remains RuntimeException Customer not found.
- validateBillableHour returns string messages exactly as legacy fragments and order.

Trace:
- [BR-01](REDISCOVERY_SPEC.md#L8)
- [BR-04](REDISCOVERY_SPEC.md#L37)

### 3.4 Gradual traffic-switch compatibility model

To support strangler rollout without caller changes:
- Keep method signatures and payload schema identical.
- Introduce an interchangeable implementation behind the same BillingService contract boundary.
- Allow dual-run comparison mode in pre-cutover environments using acceptance tests AT-BILL-001 through AT-BILL-015.
- Cut traffic only when test parity is exact.

Trace:
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L223)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md#L230)

## 4) Ambiguity Check (Constraint Compliance)

No unresolved ambiguity blocks this module rewrite specification because this loop is constrained to BillingService legacy contract behavior.
Known global ambiguity remains for separate modules, especially reporting authority and deletion policy:
- [Open Question 1](REDISCOVERY_SPEC.md#L262)
- [Open Question 6](REDISCOVERY_SPEC.md#L282)
These are intentionally excluded from Billing module scope in this loop.

## 5) Loop Continuation Note

This document is phase-3b loop 1.
Continue with the next module loop until legacy path exhaustion, reusing the same three deliverables and strict traceability pattern.
