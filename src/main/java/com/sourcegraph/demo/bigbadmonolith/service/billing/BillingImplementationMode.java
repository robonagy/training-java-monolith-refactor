package com.sourcegraph.demo.bigbadmonolith.service.billing;

public enum BillingImplementationMode {
    LEGACY,
    REWRITTEN;

    public static BillingImplementationMode fromString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return LEGACY;
        }

        String normalized = raw.trim().toLowerCase();
        if ("rewritten".equals(normalized) || "new".equals(normalized)) {
            return REWRITTEN;
        }
        return LEGACY;
    }
}
