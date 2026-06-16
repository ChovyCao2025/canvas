package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiQuickEngineCapacitySummaryView 视图。
 */
public record BiQuickEngineCapacitySummaryView(
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * capacityLimitRows 对应的数据集合。
         */
        Long capacityLimitRows,
        /**
         * usedRows 对应的数据集合。
         */
        Long usedRows,
        /**
         * usagePercent 字段值。
         */
        Double usagePercent,
        /**
         * alertLevel 字段值。
         */
        String alertLevel,
        /**
         * alertEnabled 字段值。
         */
        Boolean alertEnabled,
        /**
         * alertPolicy 字段值。
         */
        Map<String, Object> alertPolicy,
        /**
         * tenantPoolPolicy 字段值。
         */
        BiQuickEnginePoolView tenantPoolPolicy,
        /**
         * concurrencyQueue 字段值。
         */
        Map<String, Object> concurrencyQueue,
        /**
         * categories 对应的数据集合。
         */
        List<Map<String, Object>> categories,
        /**
         * details 对应的数据集合。
         */
        List<Map<String, Object>> details,
        List<Map<String, Object>> userRankings) {
}
