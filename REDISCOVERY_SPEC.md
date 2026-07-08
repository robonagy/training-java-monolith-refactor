# Legacy Monolith Rediscovery Specification

Date: 2026-07-08
Scope: Behavioral rediscovery from existing code only (no assumptions beyond source evidence)

## 1) Business Rules (Plain English, with concrete references)

### Rule BR-01: A customer bill can only be generated for an existing customer
- Evidence:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L24)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L26)
- Behavior:
  - `generateCustomerBill(customerId)` loads customer by ID.
  - If missing, it throws `RuntimeException("Customer not found")`.
- Example:
  - If user requests a bill for `customerId=9999` and no row exists, billing fails immediately.

### Rule BR-02: Bill totals are computed as sum(hours * category hourly rate), only when category exists
- Evidence:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L37)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L38)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L39)
- Behavior:
  - For each billable hour row, line amount = `hours * hourlyRate`.
  - Rows with missing categories are skipped from totals.
- Example:
  - 2.00 hours in a 150.00 rate category contributes 300.00.

### Rule BR-03: Monthly report includes only entries where date_logged year/month matches input
- Evidence:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L54)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L65)
- Behavior:
  - Service iterates all billable hours and keeps only records with exact year/month match.
  - Revenue is grouped by category name.

### Rule BR-04: Billable-hour validation enforces referential existence and date/hour constraints
- Evidence:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L95)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L104)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L111)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L115)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L117)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L119)
- Behavior:
  - Invalid customer/category IDs are flagged.
  - Hours must be greater than zero.
  - Date logged is required and must not be in the future.
  - Weekend logging is returned as warning text, not hard rejection.
- Note:
  - This validator is not invoked by JSP hour logging flow.

### Rule BR-05: Sample data initialization runs only when users table is empty
- Evidence:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/DataInitializationService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/DataInitializationService.java#L26)
- Behavior:
  - If any user exists, initialization exits without inserting sample users/customers/categories/hours.

### Rule BR-06: User creation in JSP swaps name/email constructor arguments
- Evidence:
  - [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L11)
  - [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L12)
  - [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L15)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java#L9)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java#L10)
- Behavior:
  - Form reads `name`, `email`, then calls `new User(name, email)`.
  - `User(String email, String name)` means data lands in opposite fields.
- Example:
  - Entering name "Alice" and email "alice@corp.com" stores email="Alice", name="alice@corp.com".

### Rule BR-07: Customer add/update enforces non-empty name/email at DAO layer
- Evidence:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L14)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L17)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L20)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L80)
- Behavior:
  - DAO throws `IllegalArgumentException` for null customer, null ID on update, blank name/email.

### Rule BR-08: Hours logging JSP validates only required fields and numeric parsing, not business policy
- Evidence:
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L17)
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L41)
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L57)
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L60)
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L177)
- Behavior:
  - Requires customer/user/category/hours presence.
  - Parses BigDecimal and saves directly.
  - HTML input hints `min=0`, `max=24`, `step=0.25`, but server-side code does not enforce max/step.

### Rule BR-09: Category rate update allows any parseable decimal; no server-side non-negative check
- Evidence:
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L34)
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L38)
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L39)
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L113)
- Behavior:
  - UI add form uses `min=0`, but update path parses arbitrary decimal and persists it.

### Rule BR-10: Customer/category/user deletion is direct DB delete with no pre-check in JSP
- Evidence:
  - [src/main/webapp/customers.jsp](src/main/webapp/customers.jsp#L28)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L104)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/UserDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/UserDAO.java#L110)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/BillingCategoryDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/BillingCategoryDAO.java#L84)
- Behavior:
  - Delete attempts rely on DB referential constraints to allow/block operation.

### Rule BR-11: Reports page computes three report types with direct SQL against Derby (bypassing DAOs)
- Evidence:
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L20)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L136)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L137)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L154)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L244)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L333)
- Behavior:
  - Customer Bill: joins hours, users, categories.
  - Monthly Summary: filters by date range and groups by customer.
  - Revenue Summary: customer/category aggregates via left joins.

### Rule BR-12: Monthly report end date is hardcoded to day 31
- Evidence:
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L236)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L250)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L291)
- Behavior:
  - For every month, end date string is `YYYY-MM-31` and converted via `Date.valueOf`.
  - Invalid calendar dates route to error output.

### Rule BR-13: Dashboard/user/category totals use in-memory joins and doubles
- Evidence:
  - [src/main/webapp/index.jsp](src/main/webapp/index.jsp#L33)
  - [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L113)
  - [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L116)
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L148)
- Behavior:
  - Revenue totals in JSP are calculated in `double`, not `BigDecimal`.

### Rule BR-14: Startup chooses Liberty DataSource when available, else embedded Derby
- Evidence:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java#L17)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java#L19)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java#L21)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java#L11)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java#L25)
- Behavior:
  - Runtime data access mode is environment-dependent via JNDI lookup.

## 2) Data Model Summary

### Entities and key fields
- User
  - DB: `users(id, email, name)`
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L32)
- Customer
  - DB: `customers(id, name, email, address, created_at)`
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L42)
- BillingCategory
  - DB: `billing_categories(id, name, description, hourly_rate)`
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L54)
- BillableHour
  - DB: `billable_hours(id, customer_id, user_id, category_id, hours, note, date_logged, created_at)`
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L65)

### Relationships
- `billable_hours.customer_id -> customers.id`
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L75)
- `billable_hours.user_id -> users.id`
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L76)
- `billable_hours.category_id -> billing_categories.id`
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L77)

### DB-enforced invariants
- Users
  - `email` is `NOT NULL` and `UNIQUE`.
  - `name` is `NOT NULL`.
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L34)
- Customers
  - `name`, `email`, `created_at` are `NOT NULL`.
  - `address` nullable.
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L45)
- Billing categories
  - `name`, `hourly_rate` are `NOT NULL`.
  - `description` nullable.
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L56)
- Billable hours
  - FK IDs, `hours`, `date_logged`, `created_at` are `NOT NULL`.
  - `note` nullable.
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L70)

### Defensive-code invariants (application-level)
- Customer DAO blocks null/empty customer name/email and null ID on update.
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L17)
- Billable-hour validation function requires `hours > 0` and non-future date.
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L111)
- Working-day rule is warning-only when used.
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L119)

### Important non-invariants (things not enforced by DB or uniformly by service)
- No DB `CHECK` constraint for positive hours or non-negative hourly rate.
  - Evidence: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L65)
- Hours JSP does not call `BillingService.validateBillableHour`.
  - Save path evidence: [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L60)
  - Validation function location: [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L89)

## 3) Integration Inventory

### External systems and runtime platforms
- Apache Derby embedded JDBC database
  - DB URL in DAO layer: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L9)
  - Driver load in reports: [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L136)
- WebSphere/Open Liberty container and managed DataSource
  - JNDI DataSource name: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java#L11)
  - JNDI lookup call: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java#L25)
  - Liberty DataSource config: [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L53)

### Filesystem paths (hard-coded/configured)
- Embedded Derby path (local mode): `./data/bigbadmonolith`
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L9)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L20)
- Liberty output DB path: `${server.output.dir}/data/bigbadmonolith`
  - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L55)
  - [src/main/liberty/config/bootstrap.properties](src/main/liberty/config/bootstrap.properties#L12)
- Derby system/log paths
  - `${server.output.dir}/derby-system`: [src/main/liberty/config/jvm.options](src/main/liberty/config/jvm.options#L11)
  - `${server.output.dir}/logs/derby.log`: [src/main/liberty/config/jvm.options](src/main/liberty/config/jvm.options#L12)

### Hard-coded endpoints and ports
- Web app context root: `/big-bad-monolith`
  - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L26)
- HTTP/HTTPS ports: `9080` / `9443`
  - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L17)
  - [src/main/liberty/config/bootstrap.properties](src/main/liberty/config/bootstrap.properties#L4)
- Startup scripts printing endpoint URLs
  - [liberty-dev.bat](liberty-dev.bat#L19)
  - [liberty-dev.sh](liberty-dev.sh#L20)
  - [liberty-start.sh](liberty-start.sh#L30)

### Identity/security integrations and credentials (hard-coded)
- Liberty basic registry dev user: `user1/password`
  - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L36)
- Keystore password: `password`
  - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L13)
- Derby credentials in config and JDBC
  - `app/app` in Liberty dataSource: [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L58)
  - `app/app` in ConnectionManager fields: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L10)

### Build/dependency integrations
- Maven Central repository
  - [build.gradle](build.gradle#L10)
- External runtime libraries
  - Derby: [build.gradle](build.gradle#L26)
  - Joda-Time: [build.gradle](build.gradle#L33)
- Open Liberty Gradle plugin
  - [build.gradle](build.gradle#L4)

## 4) Open Questions (code alone cannot fully answer)

1. Which business flow is authoritative for bill creation in production: service-layer `BillingService.generateCustomerBill` or JSP direct SQL report generation?
- Both exist and differ in implementation style.
- References: [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L24), [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L154)

2. Is weekend logging intended to be allowed with warning, or disallowed?
- Code currently labels weekend as warning text in validator, but hour logging JSP does not invoke validator.
- References: [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L119), [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L60)

3. Are negative or very large hourly rates/hours acceptable by policy?
- DB schema has no check constraints; some UI fields have HTML `min/max` but server-side acceptance is broader.
- References: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L70), [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L177), [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L163)

4. For monthly reports, should month-end use calendar last day (28/29/30/31) rather than fixed 31?
- Current code always builds day 31 and may fail for short months.
- References: [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L236), [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L250)

5. Is the user add-form field mapping defect intentional test data behavior or an unintended bug?
- Form maps to `new User(name, email)` while constructor order is `(email, name)`.
- References: [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L15), [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java#L9)

6. What is the expected behavior when deleting customers/users/categories that have dependent billable hours?
- DAOs issue direct deletes; FK constraints may block, but user-facing expectation is not defined.
- References: [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L104), [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L75)

7. Should auth/security from Liberty basic registry be required for all pages, or is current setup for local demo only?
- Security config exists, but page-level constraints are not declared in web.xml.
- References: [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L40), [src/main/webapp/WEB-INF/web.xml](src/main/webapp/WEB-INF/web.xml#L1)

8. Which date/time standard is canonical for business meaning (timezone/cutoff), given mixed Joda `LocalDate`, `DateTime`, and SQL date usage?
- References: [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/BillableHour.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/BillableHour.java#L13), [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/BillableHourDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/BillableHourDAO.java#L24), [src/main/java/com/sourcegraph/demo/bigbadmonolith/util/DateTimeUtils.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/util/DateTimeUtils.java#L59)
