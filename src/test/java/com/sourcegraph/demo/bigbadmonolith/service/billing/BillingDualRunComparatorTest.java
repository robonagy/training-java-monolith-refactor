package com.sourcegraph.demo.bigbadmonolith.service.billing;

import com.sourcegraph.demo.bigbadmonolith.entity.BillableHour;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingDualRunComparatorTest {

    @Test
    void compareCustomerBillMatchesWhenOutputsEquivalent() throws SQLException {
        BillingContract legacy = new StubBillingContract(
                Map.of("totalAmount", new BigDecimal("100.00"), "items", List.of("a", "b")),
                Map.of(),
                ""
        );

        BillingContract candidate = new StubBillingContract(
                Map.of("totalAmount", new BigDecimal("100.000"), "items", List.of("a", "b")),
                Map.of(),
                ""
        );

        BillingDualRunComparator comparator = new BillingDualRunComparator(legacy, candidate);
        BillingComparisonReport report = comparator.compareCustomerBill(1L);

        assertTrue(report.isMatched());
        assertTrue(report.getDifferences().isEmpty());
    }

    @Test
    void compareMonthlyReportCapturesMapDifference() throws SQLException {
        BillingContract legacy = new StubBillingContract(
                Map.of(),
                Map.of("totalRevenue", new BigDecimal("650.00"), "month", 7),
                ""
        );

        BillingContract candidate = new StubBillingContract(
                Map.of(),
                Map.of("totalRevenue", new BigDecimal("600.00"), "month", 7),
                ""
        );

        BillingDualRunComparator comparator = new BillingDualRunComparator(legacy, candidate);
        BillingComparisonReport report = comparator.compareMonthlyReport(2026, 7);

        assertFalse(report.isMatched());
        assertEquals("generateMonthlyReport", report.getOperation());
        assertTrue(report.getDifferences().stream().anyMatch(msg -> msg.contains("totalRevenue")));
    }

    @Test
    void compareValidationCapturesStringDifference() {
        BillingContract legacy = new StubBillingContract(Map.of(), Map.of(), "Invalid customer ID.");
        BillingContract candidate = new StubBillingContract(Map.of(), Map.of(), "");

        BillingDualRunComparator comparator = new BillingDualRunComparator(legacy, candidate);

        BillableHour hour = new BillableHour();
        hour.setDateLogged(new LocalDate(2026, 7, 8));

        BillingComparisonReport report = comparator.compareValidation(hour);

        assertFalse(report.isMatched());
        assertEquals("validateBillableHour", report.getOperation());
        assertTrue(report.getDifferences().get(0).contains("legacy=Invalid customer ID."));
    }

    private static final class StubBillingContract implements BillingContract {
        private final Map<String, Object> customerBill;
        private final Map<String, Object> monthlyReport;
        private final String validation;

        private StubBillingContract(Map<String, Object> customerBill, Map<String, Object> monthlyReport, String validation) {
            this.customerBill = customerBill;
            this.monthlyReport = monthlyReport;
            this.validation = validation;
        }

        @Override
        public Map<String, Object> generateCustomerBill(Long customerId) {
            return customerBill;
        }

        @Override
        public Map<String, Object> generateMonthlyReport(int year, int month) {
            return monthlyReport;
        }

        @Override
        public String validateBillableHour(BillableHour hour) {
            return validation;
        }
    }
}
