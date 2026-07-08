package com.sourcegraph.demo.bigbadmonolith.service.billing;

import com.sourcegraph.demo.bigbadmonolith.entity.BillableHour;

import java.sql.SQLException;
import java.util.Map;

public interface BillingContract {
    Map<String, Object> generateCustomerBill(Long customerId) throws SQLException;

    Map<String, Object> generateMonthlyReport(int year, int month) throws SQLException;

    String validateBillableHour(BillableHour hour);
}
