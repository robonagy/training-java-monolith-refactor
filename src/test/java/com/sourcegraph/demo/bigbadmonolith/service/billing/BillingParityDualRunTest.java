package com.sourcegraph.demo.bigbadmonolith.service.billing;

import com.sourcegraph.demo.bigbadmonolith.dao.BillableHourDAO;
import com.sourcegraph.demo.bigbadmonolith.dao.BillingCategoryDAO;
import com.sourcegraph.demo.bigbadmonolith.dao.CustomerDAO;
import com.sourcegraph.demo.bigbadmonolith.entity.BillableHour;
import com.sourcegraph.demo.bigbadmonolith.entity.BillingCategory;
import com.sourcegraph.demo.bigbadmonolith.entity.Customer;
import com.sourcegraph.demo.bigbadmonolith.service.BillingService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingParityDualRunTest {

    @Test
    void dualRunCustomerBillShowsNoParityMismatch() throws Exception {
        BillingDualRunComparator comparator = buildComparatorForSharedBaseline();

        BillingComparisonReport report = comparator.compareCustomerBill(1L);

        assertTrue(report.isMatched(), () -> String.join(" | ", report.getDifferences()));
    }

    @Test
    void dualRunMonthlyReportShowsNoParityMismatch() throws Exception {
        BillingDualRunComparator comparator = buildComparatorForSharedBaseline();

        BillingComparisonReport report = comparator.compareMonthlyReport(2026, 7);

        assertTrue(report.isMatched(), () -> String.join(" | ", report.getDifferences()));
    }

    @Test
    void dualRunValidationShowsNoParityMismatch() throws Exception {
        BillingDualRunComparator comparator = buildComparatorForSharedBaseline();

        BillableHour invalid = new BillableHour();
        invalid.setCustomerId(999L);
        invalid.setCategoryId(999L);
        invalid.setHours(BigDecimal.ZERO);
        invalid.setDateLogged(new LocalDate(2026, 7, 6));

        BillingComparisonReport report = comparator.compareValidation(invalid);

        assertTrue(report.isMatched(), () -> String.join(" | ", report.getDifferences()));
    }

    private static BillingDualRunComparator buildComparatorForSharedBaseline() throws Exception {
        Customer customer = new Customer(1L, "Acme", "billing@acme.com", "Street", DateTime.now());
        BillingCategory development = new BillingCategory(100L, "Development", "", new BigDecimal("150.00"));

        BillableHour row1 = new BillableHour(10L, 1L, 2L, 100L, new BigDecimal("2.00"), "dev", new LocalDate(2026, 7, 4), DateTime.now());
        BillableHour row2 = new BillableHour(11L, 1L, 2L, 100L, new BigDecimal("1.50"), "dev", new LocalDate(2026, 7, 5), DateTime.now());

        StubCustomerDAO customerDAO = new StubCustomerDAO(Map.of(1L, customer));
        StubCategoryDAO categoryDAO = new StubCategoryDAO(Map.of(100L, development));
        StubBillableHourDAO billableHourDAO = new StubBillableHourDAO(List.of(row1, row2), List.of(row1, row2));

        BillingService legacyService = new BillingService();
        setField(legacyService, "customerDAO", customerDAO);
        setField(legacyService, "categoryDAO", categoryDAO);
        setField(legacyService, "billableHourDAO", billableHourDAO);

        BillingService rewrittenService = new BillingService();
        setField(rewrittenService, "customerDAO", customerDAO);
        setField(rewrittenService, "categoryDAO", categoryDAO);
        setField(rewrittenService, "billableHourDAO", billableHourDAO);

        BillingContract legacy = new BillingServiceAdapter(legacyService);
        BillingContract candidate = new LegacyCompatibleBillingModule(rewrittenService);
        return new BillingDualRunComparator(legacy, candidate);
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
}
