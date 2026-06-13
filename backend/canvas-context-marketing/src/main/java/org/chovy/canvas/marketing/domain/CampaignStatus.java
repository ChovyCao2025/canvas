package org.chovy.canvas.marketing.domain;

import java.util.Locale;

public enum CampaignStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED;

    public static CampaignStatus from(String value) {
        String status = normalizeUpper(value, "DRAFT");
        return switch (status) {
            case "DRAFT" -> DRAFT;
            case "ACTIVE" -> ACTIVE;
            case "PAUSED" -> PAUSED;
            case "COMPLETED" -> COMPLETED;
            case "ARCHIVED" -> ARCHIVED;
            default -> throw new IllegalArgumentException("unsupported campaign status: " + status);
        };
    }

    public static CampaignStatus optional(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : from(trimmed);
    }

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }
}
