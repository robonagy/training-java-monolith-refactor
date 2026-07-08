# Phase 3b Module Rewrite Specification: Billing Module (Loop 1)

Date: 2026-07-08
Scope: Single-module rewrite specification only (no code)
Sources:
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)
- [SUBSTITUTION_AUDIT_PHASE1.md](SUBSTITUTION_AUDIT_PHASE1.md)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md)

Selected module boundary:
- Billing Core boundary from [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md)
- Legacy implementation surface in [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java)

In-scope rules for this loop:
- [BR-01](REDISCOVERY_SPEC.md#L8)
- [BR-02](REDISCOVERY_SPEC.md#L19)
- [BR-03](REDISCOVERY_SPEC.md#L30)
- [BR-04](REDISCOVERY_SPEC.md#L36)
- [BR-05](REDISCOVERY_SPEC.md#L45)
- [BR-06](REDISCOVERY_SPEC.md#L52)

Out of scope this loop:
- Reporting authority and month-end policy decisions ([REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L299), [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L313))
- JSP write flow refactors for customer/category/hours ([BR-10](REDISCOVERY_SPEC.md#L102), [BR-11](REDISCOVERY_SPEC.md#L112), [BR-12](REDISCOVERY_SPEC.md#L123))

## 1) Functional Requirements

### FR-BILL-001 Customer existence gate
- Generate-customer-bill must fail when customer lookup returns no record.
- Legacy-compatible error: RuntimeException with message Customer not found.
- Trace: [BR-01](REDISCOVERY_SPEC.md#L8)

### FR-BILL-002 Line-based total calculation
- Per-row amount is hours multiplied by category hourly rate.
- totalAmount and totalHours accumulate over included rows.
- Trace: [BR-02](REDISCOVERY_SPEC.md#L19)

### FR-BILL-003 Missing category exclusion
- If category lookup returns null, the row is excluded from bill totals.
- Trace: [BR-03](REDISCOVERY_SPEC.md#L30)

### FR-BILL-004 Monthly filter semantics
- Monthly report includes rows only when date_logged year and month match the requested period.
- Trace: [BR-04](REDISCOVERY_SPEC.md#L36)

### FR-BILL-005 Monthly aggregation semantics
- Monthly output exposes totalRevenue, totalHours, and revenueByCategory keyed by category name.
- Trace: [BR-05](REDISCOVERY_SPEC.md#L45)

### FR-BILL-006 Validation referential checks
- Validation includes legacy fragments for invalid customer ID and invalid category ID.
- Trace: [BR-06](REDISCOVERY_SPEC.md#L52)

### FR-BILL-007 Validation value/date checks
- Validation includes legacy fragments for:
  - hours null or less than/equal zero
  - missing date
  - future date
  - weekend warning text
- Trace: [BR-06](REDISCOVERY_SPEC.md#L52)

### FR-BILL-008 Validation DB error text checks
- If referential lookups throw SQLException, legacy DB error fragments are appended.
- Trace: [BR-06](REDISCOVERY_SPEC.md#L52)

### FR-BILL-009 Validation output format
- Validation result remains one trimmed string preserving legacy composition order.
- Trace: [BR-06](REDISCOVERY_SPEC.md#L52)

### FR-BILL-010 Decimal consistency
- Billing totals preserve decimal arithmetic behavior for money and hours.
- Trace: [BR-02](REDISCOVERY_SPEC.md#L19), [SA-13](SUBSTITUTION_AUDIT_PHASE1.md#L27)

## 2) Behavior-Preserving Acceptance Tests

| Test ID | Given | When | Then | FR coverage | Rule trace |
|---|---|---|---|---|---|
| AT-BILL-001 | Existing customer with valid rows and categories | generateCustomerBill(customerId) | Returns map with customer, billableHours, totalHours, totalAmount, generatedDate | FR-BILL-001, FR-BILL-002 | [BR-01](REDISCOVERY_SPEC.md#L8), [BR-02](REDISCOVERY_SPEC.md#L19) |
| AT-BILL-002 | Missing customer ID | generateCustomerBill(missingId) | Throws RuntimeException Customer not found | FR-BILL-001 | [BR-01](REDISCOVERY_SPEC.md#L8) |
| AT-BILL-003 | One row references missing category | generateCustomerBill(customerId) | Missing-category row contributes neither amount nor hours | FR-BILL-003 | [BR-03](REDISCOVERY_SPEC.md#L30) |
| AT-BILL-004 | Rows across multiple months/years | generateMonthlyReport(year, month) | Only exact month/year rows are aggregated | FR-BILL-004 | [BR-04](REDISCOVERY_SPEC.md#L36) |
| AT-BILL-005 | Matching rows in multiple categories | generateMonthlyReport(year, month) | revenueByCategory sums by category name | FR-BILL-005 | [BR-05](REDISCOVERY_SPEC.md#L45) |
| AT-BILL-006 | Invalid customer and invalid category | validateBillableHour(hour) | Contains Invalid customer ID. and Invalid category ID. | FR-BILL-006 | [BR-06](REDISCOVERY_SPEC.md#L52) |
| AT-BILL-007 | Null/zero/negative hours | validateBillableHour(hour) | Contains Hours must be greater than zero. | FR-BILL-007 | [BR-06](REDISCOVERY_SPEC.md#L52) |
| AT-BILL-008 | Null date | validateBillableHour(hour) | Contains Date logged is required. | FR-BILL-007 | [BR-06](REDISCOVERY_SPEC.md#L52) |
| AT-BILL-009 | Future date | validateBillableHour(hour) | Contains Date logged cannot be in the future. | FR-BILL-007 | [BR-06](REDISCOVERY_SPEC.md#L52) |
| AT-BILL-010 | Weekend date otherwise valid | validateBillableHour(hour) | Contains Warning: Hours logged on weekend. | FR-BILL-007 | [BR-06](REDISCOVERY_SPEC.md#L52) |
| AT-BILL-011 | Lookup SQLException | validateBillableHour(hour) | Contains Database error checking customer. and/or category. | FR-BILL-008 | [BR-06](REDISCOVERY_SPEC.md#L52) |
| AT-BILL-012 | Multiple failures in one payload | validateBillableHour(hour) | Returns single trimmed string preserving legacy order | FR-BILL-009 | [BR-06](REDISCOVERY_SPEC.md#L52) |
| AT-BILL-013 | Fractional values requiring decimal precision | generateCustomerBill/generateMonthlyReport | Totals match legacy decimal semantics | FR-BILL-010 | [BR-02](REDISCOVERY_SPEC.md#L19), [SA-13](SUBSTITUTION_AUDIT_PHASE1.md#L27) |

## 3) Legacy-Identical Interface Contract

Public contract retained:
- Map<String, Object> generateCustomerBill(Long customerId) throws SQLException
- Map<String, Object> generateMonthlyReport(int year, int month) throws SQLException
- String validateBillableHour(BillableHour hour)

Source contract references:
- [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L24)
- [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L52)
- [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L88)

Response payload compatibility:
- generateCustomerBill keys: customer, billableHours, totalHours, totalAmount, generatedDate
- generateMonthlyReport keys: year, month, totalRevenue, totalHours, revenueByCategory, generatedDate

Source payload references:
- [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L43)
- [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L77)

Error compatibility:
- Customer not found exception text is unchanged.
- Validation error/warning fragments and composition order are unchanged.

Compatibility-mode rule:
- Legacy and rewritten billing modules must run in dual-run mode and produce parity before traffic switch.
- Trace: [TA-16](TARGET_ARCHITECTURE_PHASE3.md#L139), [SA-01](SUBSTITUTION_AUDIT_PHASE1.md#L16), [SA-02](SUBSTITUTION_AUDIT_PHASE1.md#L17), [SA-03](SUBSTITUTION_AUDIT_PHASE1.md#L18)

## 4) Ambiguity Gate

No unresolved ambiguity blocks this billing loop scope. Billing behavior in scope is explicit in [BR-01](REDISCOVERY_SPEC.md#L8) through [BR-06](REDISCOVERY_SPEC.md#L52).
Ambiguities that remain outside this loop are governed in target architecture decision-gates: [TA-17](TARGET_ARCHITECTURE_PHASE3.md#L145).
