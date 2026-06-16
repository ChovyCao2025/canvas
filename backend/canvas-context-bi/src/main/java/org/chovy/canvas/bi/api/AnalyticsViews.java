package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * AnalyticsViews 业务对象。
 */
public final class AnalyticsViews {
    /**
     * 执行 Analytics Views 相关处理。
     */
    private AnalyticsViews() {
    }
    /**
     * EventCountView 视图。
     */
    public record EventCountView(Long tenantId, String eventCode, long count) {
    }
    /**
     * EventTotalView 视图。
     */
    public record EventTotalView(Long tenantId, String eventCode, long total) {
    }
    /**
     * UserTimelineItemView 视图。
     */
    public record UserTimelineItemView(String userId, String eventCode, String eventTime) {
    }
    /**
     * UserTimelineView 视图。
     */
    public record UserTimelineView(
            /**
             * 租户标识。
             */
            Long tenantId,
            /**
             * userId 对应的标识。
             */
            String userId,
            /**
             * page 字段值。
             */
            int page,
            /**
             * size 字段值。
             */
            int size,
            /**
             * total 对应的统计数量。
             */
            long total,
            List<UserTimelineItemView> records) {
    }
    /**
     * AttributeDistributionView 视图。
     */
    public record AttributeDistributionView(Long tenantId, String attribute, String value, long count) {
    }
    /**
     * FunnelStepView 视图。
     */
    public record FunnelStepView(String stepKey, String name, long users, double conversionRate) {
    }
    /**
     * FunnelView 视图。
     */
    public record FunnelView(
            /**
             * 租户标识。
             */
            Long tenantId,
            /**
             * funnelKey 对应的业务键。
             */
            String funnelKey,
            /**
             * startDate 字段值。
             */
            String startDate,
            /**
             * endDate 字段值。
             */
            String endDate,
            List<FunnelStepView> steps) {
    }
    /**
     * AlertPreviewRequest 不可变数据载体。
     */
    public record AlertPreviewRequest(
            /**
             * ruleKey 对应的业务键。
             */
            String ruleKey,
            /**
             * eventCode 字段值。
             */
            String eventCode,
            /**
             * startDate 字段值。
             */
            String startDate,
            /**
             * endDate 字段值。
             */
            String endDate,
            Long threshold) {
    }
    /**
     * AlertPreviewView 视图。
     */
    public record AlertPreviewView(
            /**
             * 租户标识。
             */
            Long tenantId,
            /**
             * ruleKey 对应的业务键。
             */
            String ruleKey,
            /**
             * eventCode 字段值。
             */
            String eventCode,
            /**
             * startDate 字段值。
             */
            String startDate,
            /**
             * endDate 字段值。
             */
            String endDate,
            /**
             * count 对应的统计数量。
             */
            long count,
            /**
             * threshold 字段值。
             */
            long threshold,
            boolean triggered) {
    }
    /**
     * ExportRequest 不可变数据载体。
     */
    public record ExportRequest(
            /**
             * reportType 字段值。
             */
            String reportType,
            /**
             * eventCode 字段值。
             */
            String eventCode,
            /**
             * startDate 字段值。
             */
            String startDate,
            /**
             * endDate 字段值。
             */
            String endDate,
            /**
             * rowLimit 字段值。
             */
            Integer rowLimit,
            String createdBy) {
    }
    /**
     * ExportJobView 视图。
     */
    public record ExportJobView(
            /**
             * 唯一标识。
             */
            Long id,
            /**
             * 租户标识。
             */
            Long tenantId,
            /**
             * reportType 字段值。
             */
            String reportType,
            /**
             * eventCode 字段值。
             */
            String eventCode,
            /**
             * 状态值。
             */
            String status,
            /**
             * rowLimit 字段值。
             */
            int rowLimit,
            /**
             * estimatedRows 对应的数据集合。
             */
            long estimatedRows,
            Map<String, Object> query) {
    }
}
