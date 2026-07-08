package com.sourcegraph.demo.bigbadmonolith.service;

import com.sourcegraph.demo.bigbadmonolith.dao.BillableHourDAO;
import com.sourcegraph.demo.bigbadmonolith.dao.BillingCategoryDAO;
import com.sourcegraph.demo.bigbadmonolith.dao.CustomerDAO;
import com.sourcegraph.demo.bigbadmonolith.entity.BillableHour;

import java.sql.SQLException;
import java.util.Map;

public class BillingService {

    private final BillingContract billingContract;

    public BillingService() {
        this(new LegacyCompatibleBillingModule(new BillableHourDAO(), new BillingCategoryDAO(), new CustomerDAO()));
    }

    BillingService(BillingContract billingContract) {
        this.billingContract = billingContract;
    }

    public Map<String, Object> generateCustomerBill(Long customerId) throws SQLException {
        return billingContract.generateCustomerBill(customerId);
    }

    public Map<String, Object> generateMonthlyReport(int year, int month) throws SQLException {
        return billingContract.generateMonthlyReport(year, month);
    }


    public String validateBillableHour(BillableHour hour) {
        return billingContract.validateBillableHour(hour);
    }
}
