package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public final class AnalyticsViews {

    private AnalyticsViews() {
    }

    public record EventCountView(Long tenantId, String eventCode, long count) {
    }

    public record EventTotalView(Long tenantId, String eventCode, long total) {
    }

    public record UserTimelineItemView(String userId, String eventCode, String eventTime) {
    }

    public record UserTimelineView(
            Long tenantId,
            String userId,
            int page,
            int size,
            long total,
            List<UserTimelineItemView> records) {
    }

    public record AttributeDistributionView(Long tenantId, String attribute, String value, long count) {
    }

    public record FunnelStepView(String stepKey, String name, long users, double conversionRate) {
    }

    public record FunnelView(
            Long tenantId,
            String funnelKey,
            String startDate,
            String endDate,
            List<FunnelStepView> steps) {
    }

    public record AlertPreviewRequest(
            String ruleKey,
            String eventCode,
            String startDate,
            String endDate,
            Long threshold) {
    }

    public record AlertPreviewView(
            Long tenantId,
            String ruleKey,
            String eventCode,
            String startDate,
            String endDate,
            long count,
            long threshold,
            boolean triggered) {
    }

    public record ExportRequest(
            String reportType,
            String eventCode,
            String startDate,
            String endDate,
            Integer rowLimit,
            String createdBy) {
    }

    public record ExportJobView(
            Long id,
            Long tenantId,
            String reportType,
            String eventCode,
            String status,
            int rowLimit,
            long estimatedRows,
            Map<String, Object> query) {
    }
}
