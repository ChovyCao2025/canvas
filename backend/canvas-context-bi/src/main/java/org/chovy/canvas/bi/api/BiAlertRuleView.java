package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiAlertRuleView 视图。
 */
public record BiAlertRuleView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工作空间标识。
         */
        Long workspaceId,
        /**
         * alertKey 对应的业务键。
         */
        String alertKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * 数据集标识。
         */
        Long datasetId,
        /**
         * 指标键。
         */
        String metricKey,
        /**
         * condition 字段值。
         */
        Map<String, Object> condition,
        /**
         * receivers 对应的数据集合。
         */
        Map<String, Object> receivers,
        /**
         * enabled 字段值。
         */
        Boolean enabled,
        /**
         * 创建人。
         */
        String createdBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public BiAlertRuleView {
        condition = condition == null ? Map.of() : Map.copyOf(condition);
        receivers = receivers == null ? Map.of() : Map.copyOf(receivers);
    }
}
