package org.chovy.canvas.execution.adapter.messaging;

import java.util.Map;

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
