package com.sourcegraph.demo.bigbadmonolith.service.billing;

import com.sourcegraph.demo.bigbadmonolith.entity.BillableHour;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillingTrafficSwitchServiceTest {

    @Test
    void routesToLegacyByDefaultMode() throws SQLException {
        BillingContract legacy = new StubBillingContract("legacy");
        BillingContract rewritten = new StubBillingContract("rewritten");

        BillingTrafficSwitchService service = new BillingTrafficSwitchService(
                legacy,
                rewritten,
                BillingImplementationMode.LEGACY
        );

        assertEquals("legacy", service.generateCustomerBill(1L).get("source"));
        assertEquals("legacy", service.generateMonthlyReport(2026, 7).get("source"));
        assertEquals("legacy", service.validateBillableHour(new BillableHour()));
    }

    @Test
    void routesToRewrittenWhenModeIsRewritten() throws SQLException {
        BillingContract legacy = new StubBillingContract("legacy");
        BillingContract rewritten = new StubBillingContract("rewritten");

        BillingTrafficSwitchService service = new BillingTrafficSwitchService(
                legacy,
                rewritten,
                BillingImplementationMode.REWRITTEN
        );

        assertEquals("rewritten", service.generateCustomerBill(1L).get("source"));
        assertEquals("rewritten", service.generateMonthlyReport(2026, 7).get("source"));
        assertEquals("rewritten", service.validateBillableHour(new BillableHour()));
    }

    @Test
    void canRollbackFromRewrittenToLegacyBySwitchingMode() throws SQLException {
        BillingContract legacy = new StubBillingContract("legacy");
        BillingContract rewritten = new StubBillingContract("rewritten");

        BillingTrafficSwitchService service = new BillingTrafficSwitchService(
                legacy,
                rewritten,
                BillingImplementationMode.REWRITTEN
        );

        assertEquals("rewritten", service.generateCustomerBill(1L).get("source"));

        service.switchMode(BillingImplementationMode.LEGACY);

        assertEquals("legacy", service.generateCustomerBill(1L).get("source"));
    }

    @Test
    void modeParsingFallsBackToLegacyForUnknownValues() {
        assertEquals(BillingImplementationMode.LEGACY, BillingImplementationMode.fromString(null));
        assertEquals(BillingImplementationMode.LEGACY, BillingImplementationMode.fromString(""));
        assertEquals(BillingImplementationMode.LEGACY, BillingImplementationMode.fromString("unknown"));
        assertEquals(BillingImplementationMode.REWRITTEN, BillingImplementationMode.fromString("rewritten"));
        assertEquals(BillingImplementationMode.REWRITTEN, BillingImplementationMode.fromString("new"));
    }

    private static final class StubBillingContract implements BillingContract {
        private final String source;

        private StubBillingContract(String source) {
            this.source = source;
        }

        @Override
        public Map<String, Object> generateCustomerBill(Long customerId) {
            return Map.of("source", source);
        }

        @Override
        public Map<String, Object> generateMonthlyReport(int year, int month) {
            return Map.of("source", source);
        }

        @Override
        public String validateBillableHour(BillableHour hour) {
            return source;
        }
    }
}
