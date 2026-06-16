package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiAiResponseView 视图。
 */
public record BiAiResponseView(
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 操作者。
         */
        String actor,
        /**
         * operation 字段值。
         */
        String operation,
        /**
         * assistantRunId 对应的标识。
         */
        String assistantRunId,
        /**
         * question 字段值。
         */
        String question,
        /**
         * 状态值。
         */
        String status,
        /**
         * fallbackUsed 字段值。
         */
        Boolean fallbackUsed,
        /**
         * explanation 字段值。
         */
        String explanation,
        /**
         * 扩展元数据。
         */
        Map<String, Object> metadata,
        /**
         * summary 字段值。
         */
        String summary,
        /**
         * keyFindings 对应的数据集合。
         */
        List<String> keyFindings,
        /**
         * recommendations 对应的数据集合。
         */
        List<String> recommendations,
        /**
         * 展示标题。
         */
        String title,
        /**
         * sections 对应的数据集合。
         */
        List<Map<String, Object>> sections,
        /**
         * nextActions 对应的数据集合。
         */
        List<String> nextActions,
        /**
         * dashboard 字段值。
         */
        Map<String, Object> dashboard,
        /**
         * charts 对应的数据集合。
         */
        List<Map<String, Object>> charts,
        /**
         * trends 对应的数据集合。
         */
        List<String> trends,
        /**
         * anomalies 对应的数据集合。
         */
        List<String> anomalies,
        List<String> opportunities) {
}
