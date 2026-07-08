package com.sourcegraph.demo.bigbadmonolith.service;

import com.sourcegraph.demo.bigbadmonolith.dao.BillableHourDAO;
import com.sourcegraph.demo.bigbadmonolith.dao.BillingCategoryDAO;
import com.sourcegraph.demo.bigbadmonolith.dao.CustomerDAO;
import com.sourcegraph.demo.bigbadmonolith.entity.BillableHour;
import com.sourcegraph.demo.bigbadmonolith.entity.BillingCategory;
import com.sourcegraph.demo.bigbadmonolith.entity.Customer;
import com.sourcegraph.demo.bigbadmonolith.service.billing.LegacyCompatibleBillingModule;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BillingServiceContractTest {

    @Test
    void generateCustomerBillThrowsWhenCustomerMissing() throws Exception {
        BillingService service = new BillingService();
        setField(service, "customerDAO", new StubCustomerDAO(Map.of()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.generateCustomerBill(999L));

        assertEquals("Customer not found", ex.getMessage());
    }

    @Test
    void generateCustomerBillCalculatesTotalsAndSkipsMissingCategory() throws Exception {
        BillingService service = new BillingService();
        Customer customer = new Customer(1L, "Acme", "billing@acme.com", "Street", DateTime.now());

        BillableHour included = new BillableHour(10L, 1L, 2L, 100L, new BigDecimal("8.50"), "dev", new LocalDate(2026, 7, 1), DateTime.now());
        BillableHour skipped = new BillableHour(11L, 1L, 2L, 999L, new BigDecimal("3.00"), "missing", new LocalDate(2026, 7, 1), DateTime.now());

        setField(service, "customerDAO", new StubCustomerDAO(Map.of(1L, customer)));
        setField(service, "billableHourDAO", new StubBillableHourDAO(List.of(included, skipped), List.of()));
        setField(service, "categoryDAO", new StubCategoryDAO(Map.of(100L, new BillingCategory(100L, "Development", "", new BigDecimal("150.00")))));

        Map<String, Object> bill = service.generateCustomerBill(1L);

        assertEquals(customer, bill.get("customer"));
        assertEquals(new BigDecimal("8.50"), bill.get("totalHours"));
        assertEquals(new BigDecimal("1275.0000"), bill.get("totalAmount"));
        assertNotNull(bill.get("generatedDate"));
    }

    @Test
    void generateMonthlyReportFiltersAndAggregatesByCategory() throws Exception {
        BillingService service = new BillingService();

        BillableHour julyDev = new BillableHour(20L, 1L, 2L, 100L, new BigDecimal("2.00"), "july-dev", new LocalDate(2026, 7, 5), DateTime.now());
        BillableHour julySupport = new BillableHour(21L, 1L, 2L, 200L, new BigDecimal("3.50"), "july-sup", new LocalDate(2026, 7, 8), DateTime.now());
        BillableHour juneDev = new BillableHour(22L, 1L, 2L, 100L, new BigDecimal("9.00"), "june-dev", new LocalDate(2026, 6, 30), DateTime.now());

        setField(service, "billableHourDAO", new StubBillableHourDAO(List.of(), List.of(julyDev, julySupport, juneDev)));
        setField(service, "categoryDAO", new StubCategoryDAO(Map.of(
                100L, new BillingCategory(100L, "Development", "", new BigDecimal("150.00")),
                200L, new BillingCategory(200L, "Support", "", new BigDecimal("100.00"))
        )));

        Map<String, Object> report = service.generateMonthlyReport(2026, 7);

        assertEquals(new BigDecimal("650.0000"), report.get("totalRevenue"));
        assertEquals(new BigDecimal("5.50"), report.get("totalHours"));

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> revenueByCategory = (Map<String, BigDecimal>) report.get("revenueByCategory");
        assertEquals(new BigDecimal("300.0000"), revenueByCategory.get("Development"));
        assertEquals(new BigDecimal("350.0000"), revenueByCategory.get("Support"));
    }

    @Test
    void validateBillableHourReturnsCombinedErrorsAndTrimmedOutput() throws Exception {
        BillingService service = new BillingService();
        setField(service, "customerDAO", new StubCustomerDAO(Map.of()));
        setField(service, "categoryDAO", new StubCategoryDAO(Map.of()));

        BillableHour hour = new BillableHour();
        hour.setCustomerId(9L);
        hour.setCategoryId(8L);
        hour.setHours(BigDecimal.ZERO);
        hour.setDateLogged(null);

        String result = service.validateBillableHour(hour);

        assertEquals("Invalid customer ID. Invalid category ID. Hours must be greater than zero. Date logged is required.", result);
    }

    @Test
    void validateBillableHourReportsDatabaseLookupErrors() throws Exception {
        BillingService service = new BillingService();
        setField(service, "customerDAO", new ErrorCustomerDAO());
        setField(service, "categoryDAO", new ErrorCategoryDAO());

        BillableHour hour = new BillableHour();
        hour.setCustomerId(9L);
        hour.setCategoryId(8L);
        hour.setHours(new BigDecimal("1.00"));
        hour.setDateLogged(new LocalDate(2026, 7, 1));

        String result = service.validateBillableHour(hour);

        assertEquals("Database error checking customer. Database error checking category.", result);
    }

    @Test
    void validateBillableHourWarnsOnWeekend() throws Exception {
        BillingService service = new BillingService();

        Customer customer = new Customer(1L, "Acme", "billing@acme.com", "Street", DateTime.now());
        BillingCategory category = new BillingCategory(2L, "Development", "", new BigDecimal("150.00"));

        setField(service, "customerDAO", new StubCustomerDAO(Map.of(1L, customer)));
        setField(service, "categoryDAO", new StubCategoryDAO(Map.of(2L, category)));

        BillableHour hour = new BillableHour();
        hour.setCustomerId(1L);
        hour.setCategoryId(2L);
        hour.setHours(new BigDecimal("2.50"));
        hour.setDateLogged(new LocalDate(2026, 7, 4));

        String result = service.validateBillableHour(hour);

        assertEquals("Warning: Hours logged on weekend.", result);
    }

    @Test
    void legacyCompatibleBillingModuleDelegatesToBillingService() throws Exception {
        BillingService delegate = new BillingService() {
            @Override
            public Map<String, Object> generateCustomerBill(Long customerId) {
                Map<String, Object> result = new HashMap<>();
                result.put("customerId", customerId);
                return result;
            }

            @Override
            public Map<String, Object> generateMonthlyReport(int year, int month) {
                Map<String, Object> result = new HashMap<>();
                result.put("year", year);
                result.put("month", month);
                return result;
            }

            @Override
            public String validateBillableHour(BillableHour hour) {
                return "ok";
            }
        };

        LegacyCompatibleBillingModule module = new LegacyCompatibleBillingModule(delegate);

        assertEquals(10L, module.generateCustomerBill(10L).get("customerId"));
        assertEquals(2026, module.generateMonthlyReport(2026, 7).get("year"));
        assertEquals("ok", module.validateBillableHour(new BillableHour()));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = BillingService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class StubBillableHourDAO extends BillableHourDAO {
        private final List<BillableHour> customerHours;
        private final List<BillableHour> allHours;

        private StubBillableHourDAO(List<BillableHour> customerHours, List<BillableHour> allHours) {
            this.customerHours = customerHours;
            this.allHours = allHours;
        }

        @Override
        public List<BillableHour> findByCustomerId(Long customerId) {
            return customerHours;
        }

        @Override
        public List<BillableHour> findAll() {
            return allHours;
        }
    }

    private static final class StubCategoryDAO extends BillingCategoryDAO {
        private final Map<Long, BillingCategory> categories;

        private StubCategoryDAO(Map<Long, BillingCategory> categories) {
            this.categories = categories;
        }

        @Override
        public BillingCategory findById(Long id) {
            return categories.get(id);
        }
    }

    private static final class StubCustomerDAO extends CustomerDAO {
        private final Map<Long, Customer> customers;

        private StubCustomerDAO(Map<Long, Customer> customers) {
            this.customers = customers;
        }

        @Override
        public Customer findById(Long id) {
            return customers.get(id);
        }
    }

    private static final class ErrorCategoryDAO extends BillingCategoryDAO {
        @Override
        public BillingCategory findById(Long id) throws SQLException {
            throw new SQLException("lookup failed");
        }
    }

    private static final class ErrorCustomerDAO extends CustomerDAO {
        @Override
        public Customer findById(Long id) throws SQLException {
            throw new SQLException("lookup failed");
        }
    }
}
