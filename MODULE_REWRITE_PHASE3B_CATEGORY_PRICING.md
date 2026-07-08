# Phase 3b Module Rewrite Specification: Category and Pricing Module

Date: 2026-07-08
Scope: Single-module rewrite specification only
Sources:
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)
- [SUBSTITUTION_AUDIT_PHASE1.md](SUBSTITUTION_AUDIT_PHASE1.md)
- [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md)

Selected module boundary:
- Category/Pricing boundary from [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md)
- Legacy surfaces:
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/BillingCategoryDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/BillingCategoryDAO.java)

In-scope phase-1 rules:
- [BR-11](REDISCOVERY_SPEC.md#L112)
- [BR-15](REDISCOVERY_SPEC.md#L158)

Out of scope this loop:
- Reporting authority policy and month-end policy in [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L299) and [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L313)
- New bounds or normalization policies not explicit in phase-1 rules

## 1) Functional Requirements

### FR-CAT-001 Add category parses hourlyRate as decimal
- For add action, hourlyRate input is parsed as BigDecimal-equivalent decimal value.
- Invalid rate format returns legacy error text Error: Invalid hourly rate format.
- Trace: [BR-11](REDISCOVERY_SPEC.md#L112)

### FR-CAT-002 Add category persists name, description, and rate
- On valid add action payload, category is created and persisted via category DAO save path.
- Success response text remains Billing category added successfully!.
- Trace: [BR-11](REDISCOVERY_SPEC.md#L112)

### FR-CAT-003 Update flow parses category id and new rate
- For update action, id is parsed as Long and newRate is parsed as decimal value.
- Parse failure returns legacy error text Error: Invalid ID or rate format.
- Trace: [BR-11](REDISCOVERY_SPEC.md#L112)

### FR-CAT-004 Update applies rate only when category exists
- Update fetches category by id before applying new rate.
- If category is missing, response text is Error: Category not found.
- On success, response text is Hourly rate updated successfully!.
- Trace: [BR-11](REDISCOVERY_SPEC.md#L112)

### FR-CAT-005 Category list retrieval and sorting semantics
- Existing categories are retrieved through DAO findAll and rendered in name-ascending order in the page flow.
- Trace: [BR-11](REDISCOVERY_SPEC.md#L112)

### FR-CAT-006 Category totals use double arithmetic in listing view
- Category totals displayed in listing flow preserve legacy double-based accumulation:
  - totalHours accumulates as double
  - totalRevenue accumulates as hours.doubleValue multiplied by hourlyRate.doubleValue
- Display formatting remains two decimal digits for hours and revenue.
- Trace: [BR-15](REDISCOVERY_SPEC.md#L158)

### FR-CAT-007 Error propagation message pattern
- Unexpected add/update failures preserve legacy prefix patterns:
  - Error adding category:
  - Error updating rate:
- Trace: [BR-11](REDISCOVERY_SPEC.md#L112)

## 2) Behavior-Preserving Acceptance Tests

| Test ID | Given | When | Then | FR coverage | Rule trace |
|---|---|---|---|---|---|
| AT-CAT-001 | action add with valid name, description, hourlyRate 150.00 | Add category flow executes | DAO save invoked with parsed decimal rate and success message Billing category added successfully! | FR-CAT-001, FR-CAT-002 | [BR-11](REDISCOVERY_SPEC.md#L112) |
| AT-CAT-002 | action add with non-numeric hourlyRate | Add category flow executes | Message is Error: Invalid hourly rate format | FR-CAT-001 | [BR-11](REDISCOVERY_SPEC.md#L112) |
| AT-CAT-003 | action add with valid rate but DAO throws exception | Add category flow executes | Message starts with Error adding category: | FR-CAT-007 | [BR-11](REDISCOVERY_SPEC.md#L112) |
| AT-CAT-004 | action update with valid id and newRate for existing category | Update category flow executes | Existing category rate is changed and message is Hourly rate updated successfully! | FR-CAT-003, FR-CAT-004 | [BR-11](REDISCOVERY_SPEC.md#L112) |
| AT-CAT-005 | action update with id not found | Update category flow executes | Message is Error: Category not found | FR-CAT-004 | [BR-11](REDISCOVERY_SPEC.md#L112) |
| AT-CAT-006 | action update with invalid id or invalid newRate | Update category flow executes | Message is Error: Invalid ID or rate format | FR-CAT-003 | [BR-11](REDISCOVERY_SPEC.md#L112) |
| AT-CAT-007 | action update with valid parse but DAO/update path throws exception | Update category flow executes | Message starts with Error updating rate: | FR-CAT-007 | [BR-11](REDISCOVERY_SPEC.md#L112) |
| AT-CAT-008 | category list includes categories and billable-hour rows | Categories listing is rendered | Rows appear in name-ascending order from categories list | FR-CAT-005 | [BR-11](REDISCOVERY_SPEC.md#L112) |
| AT-CAT-009 | category list with multiple billable-hour rows and fractional values | Categories listing computes totals | totalHours and totalRevenue equal legacy double-based accumulation and two-decimal display format | FR-CAT-006 | [BR-15](REDISCOVERY_SPEC.md#L158) |

FR-to-test mapping:
- FR-CAT-001: AT-CAT-001, AT-CAT-002
- FR-CAT-002: AT-CAT-001
- FR-CAT-003: AT-CAT-004, AT-CAT-006
- FR-CAT-004: AT-CAT-004, AT-CAT-005
- FR-CAT-005: AT-CAT-008
- FR-CAT-006: AT-CAT-009
- FR-CAT-007: AT-CAT-003, AT-CAT-007

## 3) Legacy-Identical Interface Contract

### 3.1 DAO contract to preserve for gradual switching
The rewritten module must provide an interface identical to legacy category DAO signatures:
- BillingCategory save(BillingCategory category) throws SQLException
- BillingCategory findById(Long id) throws SQLException
- List<BillingCategory> findAll() throws SQLException
- boolean update(BillingCategory category) throws SQLException
- boolean delete(Long id) throws SQLException

Reference: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/BillingCategoryDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/BillingCategoryDAO.java)

### 3.2 Request/response behavioral contract for page callers
For compatibility with legacy traffic sources using action parameter patterns:
- action add expects name, description, hourlyRate
- action update expects id, newRate
- Message texts remain exact for known parse/not-found/success outcomes listed in FR-CAT-001 through FR-CAT-004

Reference: [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp)

### 3.3 Gradual switching rule
- Traffic switching must occur behind compatibility boundary so legacy and rewritten module can be dual-run compared.
- Cutover proceeds only when AT-CAT-001 through AT-CAT-009 produce identical outcomes.
- Trace: [TA-07](TARGET_ARCHITECTURE_PHASE3.md#L56), [TA-16](TARGET_ARCHITECTURE_PHASE3.md#L139), [SA-01](SUBSTITUTION_AUDIT_PHASE1.md#L16), [SA-02](SUBSTITUTION_AUDIT_PHASE1.md#L17), [SA-03](SUBSTITUTION_AUDIT_PHASE1.md#L18)

## Ambiguity Check

No additional behavior was invented.
This module scope is limited to explicit category/pricing behaviors in [BR-11](REDISCOVERY_SPEC.md#L112) and [BR-15](REDISCOVERY_SPEC.md#L158).
Potential future policy decisions such as official bounds for rates are intentionally excluded from this loop because they are unresolved in phase-1 open questions.
