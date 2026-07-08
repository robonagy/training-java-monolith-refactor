package com.sourcegraph.demo.bigbadmonolith.service;

import com.sourcegraph.demo.bigbadmonolith.dao.BillableHourDAO;
import com.sourcegraph.demo.bigbadmonolith.dao.BillingCategoryDAO;
import com.sourcegraph.demo.bigbadmonolith.dao.CustomerDAO;
import com.sourcegraph.demo.bigbadmonolith.entity.BillableHour;
import com.sourcegraph.demo.bigbadmonolith.entity.BillingCategory;
import com.sourcegraph.demo.bigbadmonolith.entity.Customer;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyCompatibleBillingModuleTest {

    @Test
    void generateCustomerBill_returnsExpectedTotalsAndShape() throws SQLException {
        FakeCustomerDAO customerDAO = new FakeCustomerDAO();
        FakeCategoryDAO categoryDAO = new FakeCategoryDAO();
        FakeBillableHourDAO hourDAO = new FakeBillableHourDAO();

        Customer customer = new Customer(1L, "Acme", "billing@acme.com", "123 Main", DateTime.now());
        customerDAO.customersById.put(1L, customer);

        categoryDAO.categoriesById.put(10L, new BillingCategory(10L, "Dev", "Development", new BigDecimal("150.00")));
        categoryDAO.categoriesById.put(20L, new BillingCategory(20L, "Consulting", "Consulting", new BigDecimal("200.00")));

        hourDAO.hoursByCustomerId.put(1L, List.of(
            hour(100L, 1L, 5L, 10L, new BigDecimal("8.50"), LocalDate.now().minusDays(2)),
            hour(101L, 1L, 6L, 20L, new BigDecimal("4.00"), LocalDate.now().minusDays(1))
        ));

        LegacyCompatibleBillingModule module = new LegacyCompatibleBillingModule(hourDAO, categoryDAO, customerDAO);

        Map<String, Object> bill = module.generateCustomerBill(1L);

        assertEquals(customer, bill.get("customer"));
        assertTrue(bill.containsKey("billableHours"));
        assertTrue(bill.containsKey("totalHours"));
        assertTrue(bill.containsKey("totalAmount"));
        assertTrue(bill.containsKey("generatedDate"));

        BigDecimal totalHours = (BigDecimal) bill.get("totalHours");
        BigDecimal totalAmount = (BigDecimal) bill.get("totalAmount");

        assertEquals(0, totalHours.compareTo(new BigDecimal("12.50")));
        assertEquals(0, totalAmount.compareTo(new BigDecimal("2075.00")));
        assertInstanceOf(LocalDate.class, bill.get("generatedDate"));
    }

    @Test
    void generateCustomerBill_throwsWhenCustomerMissing() {
        LegacyCompatibleBillingModule module = new LegacyCompatibleBillingModule(new FakeBillableHourDAO(), new FakeCategoryDAO(), new FakeCustomerDAO());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> module.generateCustomerBill(999L));
        assertEquals("Customer not found", ex.getMessage());
    }

    @Test
    void generateCustomerBill_skipsRowsWithMissingCategoryFromTotals() throws SQLException {
        FakeCustomerDAO customerDAO = new FakeCustomerDAO();
        FakeCategoryDAO categoryDAO = new FakeCategoryDAO();
        FakeBillableHourDAO hourDAO = new FakeBillableHourDAO();

        customerDAO.customersById.put(1L, new Customer(1L, "Acme", "billing@acme.com", "123 Main", DateTime.now()));
        categoryDAO.categoriesById.put(10L, new BillingCategory(10L, "Dev", "Development", new BigDecimal("150.00")));

        hourDAO.hoursByCustomerId.put(1L, List.of(
            hour(100L, 1L, 5L, 10L, new BigDecimal("2.00"), LocalDate.now()),
            hour(101L, 1L, 5L, 99L, new BigDecimal("3.00"), LocalDate.now())
        ));

        LegacyCompatibleBillingModule module = new LegacyCompatibleBillingModule(hourDAO, categoryDAO, customerDAO);
        Map<String, Object> bill = module.generateCustomerBill(1L);

        assertEquals(0, ((BigDecimal) bill.get("totalHours")).compareTo(new BigDecimal("2.00")));
        assertEquals(0, ((BigDecimal) bill.get("totalAmount")).compareTo(new BigDecimal("300.00")));
    }

    @Test
    void generateMonthlyReport_filtersByYearAndMonthAndAggregatesByCategory() throws SQLException {
        FakeCustomerDAO customerDAO = new FakeCustomerDAO();
        FakeCategoryDAO categoryDAO = new FakeCategoryDAO();
        FakeBillableHourDAO hourDAO = new FakeBillableHourDAO();

        categoryDAO.categoriesById.put(10L, new BillingCategory(10L, "Dev", "Development", new BigDecimal("100.00")));
        categoryDAO.categoriesById.put(20L, new BillingCategory(20L, "Support", "Support", new BigDecimal("50.00")));

        LocalDate inMonth1 = new LocalDate(2026, 7, 10);
        LocalDate inMonth2 = new LocalDate(2026, 7, 11);
        LocalDate outMonth = new LocalDate(2026, 6, 15);

        hourDAO.allHours = List.of(
            hour(1L, 1L, 1L, 10L, new BigDecimal("2.00"), inMonth1),
            hour(2L, 1L, 1L, 20L, new BigDecimal("4.00"), inMonth2),
            hour(3L, 1L, 1L, 10L, new BigDecimal("8.00"), outMonth)
        );

        LegacyCompatibleBillingModule module = new LegacyCompatibleBillingModule(hourDAO, categoryDAO, customerDAO);

        Map<String, Object> report = module.generateMonthlyReport(2026, 7);

        assertEquals(2026, report.get("year"));
        assertEquals(7, report.get("month"));
        assertEquals(0, ((BigDecimal) report.get("totalHours")).compareTo(new BigDecimal("6.00")));
        assertEquals(0, ((BigDecimal) report.get("totalRevenue")).compareTo(new BigDecimal("400.00")));

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> revenueByCategory = (Map<String, BigDecimal>) report.get("revenueByCategory");
        assertEquals(0, revenueByCategory.get("Dev").compareTo(new BigDecimal("200.00")));
        assertEquals(0, revenueByCategory.get("Support").compareTo(new BigDecimal("200.00")));
        assertNotNull(report.get("generatedDate"));
    }

    @Test
    void validateBillableHour_reportsInvalidForeignKeysHoursAndDateRules() {
        LegacyCompatibleBillingModule module = new LegacyCompatibleBillingModule(new FakeBillableHourDAO(), new FakeCategoryDAO(), new FakeCustomerDAO());

        BillableHour hour = new BillableHour();
        hour.setCustomerId(99L);
        hour.setCategoryId(88L);
        hour.setHours(BigDecimal.ZERO);
        hour.setDateLogged(LocalDate.now().plusDays(1));

        String errors = module.validateBillableHour(hour);

        assertTrue(errors.contains("Invalid customer ID."));
        assertTrue(errors.contains("Invalid category ID."));
        assertTrue(errors.contains("Hours must be greater than zero."));
        assertTrue(errors.contains("Date logged cannot be in the future."));
    }

    @Test
    void validateBillableHour_reportsRequiredDateAndWeekendWarning() {
        FakeCustomerDAO customerDAO = new FakeCustomerDAO();
        FakeCategoryDAO categoryDAO = new FakeCategoryDAO();

        customerDAO.customersById.put(1L, new Customer(1L, "Acme", "billing@acme.com", "123 Main", DateTime.now()));
        categoryDAO.categoriesById.put(10L, new BillingCategory(10L, "Dev", "Development", new BigDecimal("100.00")));

        LegacyCompatibleBillingModule module = new LegacyCompatibleBillingModule(new FakeBillableHourDAO(), categoryDAO, customerDAO);

        BillableHour missingDate = new BillableHour();
        missingDate.setCustomerId(1L);
        missingDate.setCategoryId(10L);
        missingDate.setHours(new BigDecimal("1.00"));
        missingDate.setDateLogged(null);

        String missingDateErrors = module.validateBillableHour(missingDate);
        assertEquals("Date logged is required.", missingDateErrors);

        BillableHour weekend = new BillableHour();
        weekend.setCustomerId(1L);
        weekend.setCategoryId(10L);
        weekend.setHours(new BigDecimal("1.00"));
        weekend.setDateLogged(new LocalDate(2026, 7, 11));

        String weekendErrors = module.validateBillableHour(weekend);
        assertEquals("Warning: Hours logged on weekend.", weekendErrors);
    }

    @Test
    void validateBillableHour_keepsLegacyMessageOrderingAndTrim() {
        LegacyCompatibleBillingModule module = new LegacyCompatibleBillingModule(new FakeBillableHourDAO(), new FakeCategoryDAO(), new FakeCustomerDAO());

        BillableHour hour = new BillableHour();
        hour.setCustomerId(10L);
        hour.setCategoryId(20L);
        hour.setHours(BigDecimal.ZERO);
        hour.setDateLogged(null);

        String errors = module.validateBillableHour(hour);

        assertEquals("Invalid customer ID. Invalid category ID. Hours must be greater than zero. Date logged is required.", errors);
    }

    @Test
    void validateBillableHour_reportsDatabaseLookupErrors() {
        FakeCustomerDAO customerDAO = new FakeCustomerDAO();
        FakeCategoryDAO categoryDAO = new FakeCategoryDAO();

        customerDAO.throwOnFindById = true;
        categoryDAO.throwOnFindById = true;

        LegacyCompatibleBillingModule module = new LegacyCompatibleBillingModule(new FakeBillableHourDAO(), categoryDAO, customerDAO);

        BillableHour hour = new BillableHour();
        hour.setCustomerId(1L);
        hour.setCategoryId(1L);
        hour.setHours(new BigDecimal("1.00"));
        hour.setDateLogged(LocalDate.now());

        String errors = module.validateBillableHour(hour);

        assertTrue(errors.contains("Database error checking customer."));
        assertTrue(errors.contains("Database error checking category."));
    }

    private static BillableHour hour(Long id, Long customerId, Long userId, Long categoryId, BigDecimal hours, LocalDate dateLogged) {
        return new BillableHour(id, customerId, userId, categoryId, hours, "note", dateLogged, DateTime.now());
    }

    private static class FakeCustomerDAO extends CustomerDAO {
        private final Map<Long, Customer> customersById = new HashMap<>();
        private boolean throwOnFindById;

        @Override
        public Customer findById(Long id) throws SQLException {
            if (throwOnFindById) {
                throw new SQLException("boom");
            }
            return customersById.get(id);
        }
    }

    private static class FakeCategoryDAO extends BillingCategoryDAO {
        private final Map<Long, BillingCategory> categoriesById = new HashMap<>();
        private boolean throwOnFindById;

        @Override
        public BillingCategory findById(Long id) throws SQLException {
            if (throwOnFindById) {
                throw new SQLException("boom");
            }
            return categoriesById.get(id);
        }
    }

    private static class FakeBillableHourDAO extends BillableHourDAO {
        private final Map<Long, List<BillableHour>> hoursByCustomerId = new HashMap<>();
        private List<BillableHour> allHours = new ArrayList<>();

        @Override
        public List<BillableHour> findByCustomerId(Long customerId) {
            return hoursByCustomerId.getOrDefault(customerId, List.of());
        }

        @Override
        public List<BillableHour> findAll() {
            return allHours;
        }
    }
}
