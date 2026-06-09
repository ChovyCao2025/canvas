package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ConversationSlaBreachView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workItemId workItemId 字段。
 * @param breachType breachType 字段。
 * @param severity severity 字段。
 * @param status status 字段。
 * @param escalationTarget escalationTarget 字段。
 * @param reason reason 字段。
 * @param dueAt dueAt 字段。
 * @param breachedAt breachedAt 字段。
 * @param resolvedBy resolvedBy 字段。
 * @param resolvedAt resolvedAt 字段。
 * @param metadata metadata 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record ConversationSlaBreachView(Long id,
                                        Long tenantId,
                                        Long workItemId,
                                        String breachType,
                                        String severity,
                                        String status,
                                        String escalationTarget,
                                        String reason,
                                        LocalDateTime dueAt,
                                        LocalDateTime breachedAt,
                                        String resolvedBy,
                                        LocalDateTime resolvedAt,
                                        Map<String, Object> metadata,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {

    public ConversationSlaBreachView {
        metadata = sanitize(metadata);
    }

    /**
     * 执行 sanitize 流程，围绕 sanitize 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 sanitize 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 sanitize 流程中的校验、计算或对象转换。
     * @return 返回 sanitize 流程生成的业务结果。
     */
    private static Map<String, Object> sanitize(Map<String, Object> metadata) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Map.copyOf(result);
    }
}
