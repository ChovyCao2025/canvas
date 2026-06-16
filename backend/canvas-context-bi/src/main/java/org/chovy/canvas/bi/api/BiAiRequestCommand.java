package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiAiRequestCommand 命令。
 */
public record BiAiRequestCommand(
        /**
         * question 字段值。
         */
        String question,
        /**
         * prompt 字段值。
         */
        String prompt,
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * reportType 字段值。
         */
        String reportType,
        /**
         * 展示标题。
         */
        String title,
        /**
         * 返回数量上限。
         */
        Integer limit,
        /**
         * providerId 对应的标识。
         */
        Long providerId,
        /**
         * templateId 对应的标识。
         */
        Long templateId,
        /**
         * modelKey 对应的业务键。
         */
        String modelKey,
        /**
         * timeoutMs 对应的数据集合。
         */
        Integer timeoutMs,
        /**
         * params 对应的数据集合。
         */
        Map<String, Object> params,
        /**
         * sections 对应的数据集合。
         */
        List<Map<String, Object>> sections,
        /**
         * subject 字段值。
         */
        Map<String, Object> subject,
        /**
         * result 字段值。
         */
        Map<String, Object> result,
        /**
         * 指标列表。
         */
        Map<String, Object> metrics,
        Map<String, Object> context) {
}
