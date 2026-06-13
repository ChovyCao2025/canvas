package org.chovy.canvas.marketing.domain;

import java.util.Locale;

public record CampaignKey(String value) implements Comparable<CampaignKey> {

    public CampaignKey {
        value = normalize(value, "campaignKey");
    }

    public static CampaignKey of(String value, String field) {
        return new CampaignKey(normalize(value, field));
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(CampaignKey other) {
        return value.compareTo(other.value);
    }

    private static String normalize(String value, String field) {
        String normalized = required(value, field)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
