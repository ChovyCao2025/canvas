package org.chovy.canvas.execution.adapter.messaging;

public record MqTriggerRejectedEvent(
        String sourceMsgId,
        String triggerType,
        String matchKey,
        String reason,
        MqTriggerMessage message) {

    public MqTriggerRejectedEvent {
        sourceMsgId = sourceMsgId == null ? "" : sourceMsgId;
        triggerType = triggerType == null ? "" : triggerType;
        matchKey = matchKey == null ? "" : matchKey;
        reason = reason == null ? "" : reason;
    }
}
