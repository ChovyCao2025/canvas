package org.chovy.canvas.marketing.domain;

import java.util.Locale;

public enum CampaignLinkStatus {
    ACTIVE,
    MISSING,
    BLOCKED,
    ARCHIVED;

    public static CampaignLinkStatus from(String value) {
        String status = normalizeUpper(value, "ACTIVE");
        return switch (status) {
            case "ACTIVE" -> ACTIVE;
            case "MISSING" -> MISSING;
            case "BLOCKED" -> BLOCKED;
            case "ARCHIVED" -> ARCHIVED;
            default -> throw new IllegalArgumentException("unsupported link status: " + status);
        };
    }

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }
}
