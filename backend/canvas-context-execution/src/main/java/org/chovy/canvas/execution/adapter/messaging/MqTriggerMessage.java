package org.chovy.canvas.execution.adapter.messaging;

import java.util.Map;

/**
 * 定义 MqTriggerMessage 的执行上下文数据结构或业务契约。
 * @param tenantId tenantId 对应的数据字段
 * @param canvasId canvasId 对应的数据字段
 * @param versionId versionId 对应的数据字段
 * @param triggerType triggerType 对应的数据字段
 * @param matchKey matchKey 对应的数据字段
 * @param sourceMsgId sourceMsgId 对应的数据字段
 * @param payload payload 对应的数据字段
 */
public record MqTriggerMessage(
        Long tenantId,
        Long canvasId,
        Long versionId,
        String triggerType,
        String matchKey,
        String sourceMsgId,
        Map<String, Object> payload) {

    public MqTriggerMessage {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (canvasId == null || canvasId <= 0) {
            throw new IllegalArgumentException("canvasId is required");
        }
        triggerType = triggerType == null || triggerType.isBlank() ? "MQ" : triggerType;
        matchKey = matchKey == null ? "" : matchKey;
        sourceMsgId = sourceMsgId == null ? "" : sourceMsgId;
        payload = Map.copyOf(payload == null ? Map.of() : payload);
    }
}
