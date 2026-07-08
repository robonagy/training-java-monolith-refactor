package com.sourcegraph.demo.bigbadmonolith.service.billing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BillingComparisonReport {
    private final String operation;
    private final boolean matched;
    private final List<String> differences;

    public BillingComparisonReport(String operation, List<String> differences) {
        this.operation = operation;
        this.differences = Collections.unmodifiableList(new ArrayList<>(differences));
        this.matched = differences.isEmpty();
    }

    public String getOperation() {
        return operation;
    }

    public boolean isMatched() {
        return matched;
    }

    public List<String> getDifferences() {
        return differences;
    }
}
