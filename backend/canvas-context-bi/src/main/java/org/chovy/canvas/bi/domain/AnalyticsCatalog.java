package org.chovy.canvas.bi.domain;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsCatalog {

    public long countFor(String eventCode) {
        return switch (normalize(eventCode)) {
            case "signup" -> 64L;
            case "page_view" -> 320L;
            default -> 96L;
        };
    }

    public List<EventCount> eventCounts() {
        return List.of(
                new EventCount("purchase", countFor("purchase")),
                new EventCount("signup", countFor("signup")),
                new EventCount("page_view", countFor("page_view")));
    }

    public List<TimelineEvent> timeline(String userId) {
        return List.of(
                new TimelineEvent(userId, "purchase", "2026-06-01T10:00:00"),
                new TimelineEvent(userId, "signup", "2026-06-01T09:30:00"),
                new TimelineEvent(userId, "page_view", "2026-06-01T09:00:00"));
    }

    public List<AttributeBucket> distribution(String attribute) {
        return List.of(
                new AttributeBucket(attribute, "wechat", 144L),
                new AttributeBucket(attribute, "email", 96L),
                new AttributeBucket(attribute, "organic", 64L));
    }

    public List<FunnelStep> funnelSteps() {
        return List.of(
                new FunnelStep("visit", "Visit", 320L, 1.0d),
                new FunnelStep("signup", "Signup", 64L, 0.2d),
                new FunnelStep("purchase", "Purchase", 32L, 0.1d));
    }

    public Map<String, Object> query(String reportType,
                                     String eventCode,
                                     String startDate,
                                     String endDate,
                                     long estimatedRows) {
        return Map.of(
                "reportType", reportType,
                "eventCode", eventCode == null ? "" : eventCode,
                "startDate", startDate,
                "endDate", endDate,
                "estimatedRows", estimatedRows);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "purchase";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record EventCount(String eventCode, long count) {
    }

    public record TimelineEvent(String userId, String eventCode, String eventTime) {
    }

    public record AttributeBucket(String attribute, String value, long count) {
    }

    public record FunnelStep(String stepKey, String name, long users, double conversionRate) {
    }
}
