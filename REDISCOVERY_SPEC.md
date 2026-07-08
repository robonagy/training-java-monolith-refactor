# Legacy Monolith Rediscovery Specification

Date: 2026-07-08
Scope: Behavioral rediscovery from existing code only (no assumptions beyond source evidence)

## 1) Business Rules (Plain English with concrete references and examples)

### BR-01: Customer bill generation requires an existing customer
- Behavior:
  - Billing flow loads customer by ID first.
  - If customer is missing, it throws RuntimeException with message Customer not found.
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L24)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L26)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L44)
- Example:
  - Requesting a bill for a non-existent customer ID fails immediately before totals are computed.

### BR-02: Customer bill totals are line-based from hours and category rate
- Behavior:
  - For each billable-hour row, line amount is hours multiplied by category hourly rate.
  - Totals accumulate from included lines.
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L37)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L38)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L39)
- Example:
  - 8.50 hours at 150.00 plus 4.00 hours at 200.00 yields 2075.00 total amount.

### BR-03: Rows with missing category are excluded from bill totals
- Behavior:
  - If category lookup returns null, that row is skipped from hours and amount aggregation.
- Reference:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L37)

### BR-04: Monthly report filters rows by exact year and month
- Behavior:
  - A row contributes only if date_logged year and month match requested year and month.
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L54)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L63)
- Example:
  - June rows are excluded from a July report.

### BR-05: Monthly report aggregates revenue and hours by category name
- Behavior:
  - Matching rows update totalRevenue, totalHours, and revenueByCategory keyed by category name.
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L67)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L73)

### BR-06: Billable-hour validation rules exist in service layer
- Behavior:
  - Checks invalid customer ID.
  - Checks invalid category ID.
  - Checks hours must be greater than zero.
  - Checks date logged required.
  - Checks date logged cannot be in the future.
  - Adds weekend warning when date is non-working day.
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L89)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L95)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L104)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L111)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L115)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L117)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L119)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/util/DateTimeUtils.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/util/DateTimeUtils.java#L59)
- Example:
  - Saturday entries can produce warning text rather than hard rejection in this validator path.

### BR-07: Startup chooses datasource mode by Liberty availability
- Behavior:
  - If Liberty datasource is available, initializes schema through Liberty connection manager.
  - Otherwise runs in embedded mode.
  - Then attempts sample-data initialization.
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java#L17)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java#L19)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java#L21)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java#L25)

### BR-08: Sample data initialization runs only when users table is empty
- Behavior:
  - If userDAO.findAll is non-empty, seeding exits immediately.
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/DataInitializationService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/DataInitializationService.java#L24)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/DataInitializationService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/DataInitializationService.java#L26)

### BR-09: users.jsp add flow swaps name and email constructor order
- Behavior:
  - users.jsp calls new User(name, email), but entity constructor expects User(email, name).
  - DAO persists user.getEmail into email column and user.getName into name column.
- References:
  - [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L11)
  - [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L15)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java#L10)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/UserDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/UserDAO.java#L11)
- Example:
  - Input name Alice and email alice@example.com persists email as Alice and name as alice@example.com.

### BR-10: Customer add/delete actions are driven by request action parameter
- Behavior:
  - action add creates customer from name/email/address.
  - action delete parses id and deletes by DAO.
- References:
  - [src/main/webapp/customers.jsp](src/main/webapp/customers.jsp#L10)
  - [src/main/webapp/customers.jsp](src/main/webapp/customers.jsp#L16)
  - [src/main/webapp/customers.jsp](src/main/webapp/customers.jsp#L24)
  - [src/main/webapp/customers.jsp](src/main/webapp/customers.jsp#L28)

### BR-11: Category add and update parse rates from request values
- Behavior:
  - Add parses hourlyRate and saves category.
  - Update parses newRate and updates existing category hourlyRate.
- References:
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L11)
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L17)
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L28)
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L34)
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L39)

### BR-12: Hours logging JSP validates required fields then saves directly via DAO
- Behavior:
  - Validates customerId/userId/categoryId/hours presence in JSP.
  - Parses values, constructs BillableHour, and calls DAO save.
  - Does not call BillingService.validateBillableHour.
- References:
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L17)
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L29)
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L55)
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L60)

### BR-13: Reports page has three report modes implemented with direct JDBC
- Behavior:
  - Supports customer, monthly, and revenue report types.
  - Uses direct DriverManager JDBC calls in JSP and SQL joins/aggregates.
- References:
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L69)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L20)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L136)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L154)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L244)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L333)

### BR-14: Monthly report date range in reports.jsp is built with fixed day 31
- Behavior:
  - startDate is year-month-01.
  - endDate is year-month-31.
  - Converted via Date.valueOf before query execution.
- References:
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L235)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L236)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L250)

### BR-15: Dashboard and some summary pages compute revenue using double arithmetic
- Behavior:
  - Revenue totals are computed using double math in JSP pages.
- References:
  - [src/main/webapp/index.jsp](src/main/webapp/index.jsp#L33)
  - [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L113)
  - [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L116)
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L148)

## 2) Data-Model Summary

### Entities
- User
  - Fields: id, email, name
  - References:
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java)
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L32)
- Customer
  - Fields: id, name, email, address, created_at
  - References:
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/Customer.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/Customer.java)
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L42)
- BillingCategory
  - Fields: id, name, description, hourly_rate
  - References:
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/BillingCategory.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/BillingCategory.java)
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L54)
- BillableHour
  - Fields: id, customer_id, user_id, category_id, hours, note, date_logged, created_at
  - References:
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/BillableHour.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/BillableHour.java)
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L65)

### Relationships
- billable_hours.customer_id references customers.id
- billable_hours.user_id references users.id
- billable_hours.category_id references billing_categories.id
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L75)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L76)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L77)

### DB-enforced invariants
- users.email is NOT NULL and UNIQUE.
- users.name is NOT NULL.
- customers.name, customers.email, customers.created_at are NOT NULL.
- billing_categories.name and hourly_rate are NOT NULL.
- billable_hours customer_id, user_id, category_id, hours, date_logged, created_at are NOT NULL.
- Numeric precision:
  - hourly_rate DECIMAL(10,2)
  - hours DECIMAL(8,2)
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L34)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L45)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L56)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L70)

### Defensive-code invariants
- CustomerDAO save/update rejects null customer and empty name/email; update requires non-null ID.
- BillingService validator enforces positive hours, required/non-future date, referential checks, weekend warning.
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L14)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L77)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L89)

### Important non-uniform invariants
- No explicit DB CHECK constraints for positive hours or non-negative rate.
- Hours JSP path writes directly and does not invoke BillingService validator.
- References:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L65)
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L60)

## 3) Integration Inventory

### External systems and runtime integrations
- Apache Derby embedded JDBC database.
  - References:
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L9)
    - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L20)
- Liberty managed datasource via JNDI jdbc/DefaultDataSource with fallback to embedded manager.
  - References:
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java#L11)
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java#L25)
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/LibertyConnectionManager.java#L34)
- Open Liberty runtime via Gradle plugin.
  - Reference:
    - [build.gradle](build.gradle#L4)

### Filesystem paths
- Embedded DB path: ./data/bigbadmonolith
  - References:
    - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L9)
    - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L20)
- Liberty DB path: ${server.output.dir}/data/bigbadmonolith
  - References:
    - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L55)
    - [src/main/liberty/config/bootstrap.properties](src/main/liberty/config/bootstrap.properties#L12)
- Derby system/log paths:
  - ${server.output.dir}/derby-system
  - ${server.output.dir}/logs/derby.log
  - References:
    - [src/main/liberty/config/jvm.options](src/main/liberty/config/jvm.options#L11)
    - [src/main/liberty/config/jvm.options](src/main/liberty/config/jvm.options#L12)

### Hard-coded endpoints and network values
- Context root: /big-bad-monolith
  - Reference:
    - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L26)
- Ports: HTTP 9080, HTTPS 9443
  - References:
    - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L17)
    - [src/main/liberty/config/bootstrap.properties](src/main/liberty/config/bootstrap.properties#L4)
- Script-printed local URLs including API path:
  - References:
    - [liberty-dev.bat](liberty-dev.bat#L19)
    - [liberty-dev.bat](liberty-dev.bat#L20)
    - [liberty-dev.sh](liberty-dev.sh#L20)
    - [liberty-start.sh](liberty-start.sh#L30)
- README-declared URLs:
  - References:
    - [README.md](README.md#L108)
    - [README.md](README.md#L113)
    - [README.md](README.md#L114)

### Hard-coded credentials and security config
- Keystore password: password
- Basic registry credential: user1/password
- DB credentials: app/app
- References:
  - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L13)
  - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L36)
  - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L58)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L10)

### Dependency-level integrations
- Derby runtime libraries.
- Joda-Time.
- Jakarta APIs.
- Commons DBCP/Pool.
- JUnit.
- Reference:
  - [build.gradle](build.gradle#L22)

## 4) Open Questions (code alone cannot answer)

1. Which reporting behavior is authoritative when service logic and reports.jsp SQL differ?
- Evidence:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L24)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L154)

2. Is weekend logging intended as warning-only or hard rejection?
- Evidence:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/BillingService.java#L119)
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L60)

3. What are official business bounds for hours and rates?
- Evidence:
  - [src/main/webapp/hours.jsp](src/main/webapp/hours.jsp#L177)
  - [src/main/webapp/categories.jsp](src/main/webapp/categories.jsp#L163)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L65)

4. Should monthly reporting use fixed day-31 behavior or true calendar month end?
- Evidence:
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L236)
  - [src/main/webapp/reports.jsp](src/main/webapp/reports.jsp#L250)

5. Is users.jsp name/email constructor order intentional or a defect?
- Evidence:
  - [src/main/webapp/users.jsp](src/main/webapp/users.jsp#L15)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/User.java#L10)

6. What is intended delete policy for entities referenced by billable_hours?
- Evidence:
  - [src/main/webapp/customers.jsp](src/main/webapp/customers.jsp#L28)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/CustomerDAO.java#L104)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/ConnectionManager.java#L75)

7. What routes/pages are intended to require authentication?
- Evidence:
  - [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml#L40)
  - [src/main/webapp/WEB-INF/web.xml](src/main/webapp/WEB-INF/web.xml#L1)

8. What is canonical business time standard (timezone/cutoff) across mixed temporal usage?
- Evidence:
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/BillableHour.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/entity/BillableHour.java#L13)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/BillableHourDAO.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/dao/BillableHourDAO.java#L24)
  - [src/main/java/com/sourcegraph/demo/bigbadmonolith/util/DateTimeUtils.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/util/DateTimeUtils.java#L59)
