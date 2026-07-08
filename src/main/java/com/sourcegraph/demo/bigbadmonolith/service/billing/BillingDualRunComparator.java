package com.sourcegraph.demo.bigbadmonolith.service.billing;

import com.sourcegraph.demo.bigbadmonolith.entity.BillableHour;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BillingDualRunComparator {

    private final BillingContract legacy;
    private final BillingContract candidate;

    public BillingDualRunComparator(BillingContract legacy, BillingContract candidate) {
        this.legacy = legacy;
        this.candidate = candidate;
    }

    public BillingComparisonReport compareCustomerBill(Long customerId) throws SQLException {
        Map<String, Object> legacyResult = legacy.generateCustomerBill(customerId);
        Map<String, Object> candidateResult = candidate.generateCustomerBill(customerId);

        List<String> differences = new ArrayList<>();
        compareValues("generateCustomerBill", "result", legacyResult, candidateResult, differences);
        return new BillingComparisonReport("generateCustomerBill", differences);
    }

    public BillingComparisonReport compareMonthlyReport(int year, int month) throws SQLException {
        Map<String, Object> legacyResult = legacy.generateMonthlyReport(year, month);
        Map<String, Object> candidateResult = candidate.generateMonthlyReport(year, month);

        List<String> differences = new ArrayList<>();
        compareValues("generateMonthlyReport", "result", legacyResult, candidateResult, differences);
        return new BillingComparisonReport("generateMonthlyReport", differences);
    }

    public BillingComparisonReport compareValidation(BillableHour hour) {
        String legacyResult = legacy.validateBillableHour(hour);
        String candidateResult = candidate.validateBillableHour(hour);

        List<String> differences = new ArrayList<>();
        compareValues("validateBillableHour", "result", legacyResult, candidateResult, differences);
        return new BillingComparisonReport("validateBillableHour", differences);
    }

    private static void compareValues(String operation, String path, Object left, Object right, List<String> differences) {
        if (left == right) {
            return;
        }
        if (left == null || right == null) {
            differences.add(buildDiff(operation, path, left, right));
            return;
        }

        if (left instanceof BigDecimal leftDecimal && right instanceof BigDecimal rightDecimal) {
            if (leftDecimal.compareTo(rightDecimal) != 0) {
                differences.add(buildDiff(operation, path, leftDecimal, rightDecimal));
            }
            return;
        }

        if (left instanceof Map<?, ?> leftMap && right instanceof Map<?, ?> rightMap) {
            compareMaps(operation, path, leftMap, rightMap, differences);
            return;
        }

        if (left instanceof List<?> leftList && right instanceof List<?> rightList) {
            compareLists(operation, path, leftList, rightList, differences);
            return;
        }

        if (!Objects.equals(left, right)) {
            differences.add(buildDiff(operation, path, left, right));
        }
    }

    private static void compareMaps(String operation, String path, Map<?, ?> left, Map<?, ?> right, List<String> differences) {
        for (Object key : left.keySet()) {
            String keyPath = path + "." + key;
            if (!right.containsKey(key)) {
                differences.add(operation + " mismatch at " + keyPath + ": missing in candidate");
                continue;
            }
            compareValues(operation, keyPath, left.get(key), right.get(key), differences);
        }

        for (Object key : right.keySet()) {
            if (!left.containsKey(key)) {
                differences.add(operation + " mismatch at " + path + "." + key + ": missing in legacy");
            }
        }
    }

    private static void compareLists(String operation, String path, List<?> left, List<?> right, List<String> differences) {
        if (left.size() != right.size()) {
            differences.add(operation + " mismatch at " + path + ": list size legacy=" + left.size() + ", candidate=" + right.size());
            return;
        }

        for (int i = 0; i < left.size(); i++) {
            compareValues(operation, path + "[" + i + "]", left.get(i), right.get(i), differences);
        }
    }

    private static String buildDiff(String operation, String path, Object legacyValue, Object candidateValue) {
        return operation + " mismatch at " + path + ": legacy=" + legacyValue + ", candidate=" + candidateValue;
    }
}
