package com.sourcegraph.demo.bigbadmonolith.service.billing;

import com.sourcegraph.demo.bigbadmonolith.entity.BillableHour;
import com.sourcegraph.demo.bigbadmonolith.service.BillingService;

import java.sql.SQLException;
import java.util.Map;

public class BillingTrafficSwitchService implements BillingContract {

    public static final String MODE_PROPERTY = "billing.mode";

    private final BillingContract legacy;
    private final BillingContract rewritten;
    private volatile BillingImplementationMode activeMode;

    public BillingTrafficSwitchService() {
        this(
                new BillingServiceAdapter(new BillingService()),
                new LegacyCompatibleBillingModule(),
                BillingImplementationMode.fromString(System.getProperty(MODE_PROPERTY))
        );
    }

    public BillingTrafficSwitchService(BillingContract legacy, BillingContract rewritten, BillingImplementationMode activeMode) {
        this.legacy = legacy;
        this.rewritten = rewritten;
        this.activeMode = activeMode;
    }

    public BillingImplementationMode getActiveMode() {
        return activeMode;
    }

    public void switchMode(BillingImplementationMode newMode) {
        this.activeMode = newMode == null ? BillingImplementationMode.LEGACY : newMode;
    }

    @Override
    public Map<String, Object> generateCustomerBill(Long customerId) throws SQLException {
        return active().generateCustomerBill(customerId);
    }

    @Override
    public Map<String, Object> generateMonthlyReport(int year, int month) throws SQLException {
        return active().generateMonthlyReport(year, month);
    }

    @Override
    public String validateBillableHour(BillableHour hour) {
        return active().validateBillableHour(hour);
    }

    private BillingContract active() {
        return activeMode == BillingImplementationMode.REWRITTEN ? rewritten : legacy;
    }
}
