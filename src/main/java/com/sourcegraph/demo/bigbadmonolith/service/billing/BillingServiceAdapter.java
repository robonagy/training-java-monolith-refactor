package com.sourcegraph.demo.bigbadmonolith.service.billing;

import com.sourcegraph.demo.bigbadmonolith.entity.BillableHour;
import com.sourcegraph.demo.bigbadmonolith.service.BillingService;

import java.sql.SQLException;
import java.util.Map;

public class BillingServiceAdapter implements BillingContract {

    private final BillingService delegate;

    public BillingServiceAdapter(BillingService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Map<String, Object> generateCustomerBill(Long customerId) throws SQLException {
        return delegate.generateCustomerBill(customerId);
    }

    @Override
    public Map<String, Object> generateMonthlyReport(int year, int month) throws SQLException {
        return delegate.generateMonthlyReport(year, month);
    }

    @Override
    public String validateBillableHour(BillableHour hour) {
        return delegate.validateBillableHour(hour);
    }
}
