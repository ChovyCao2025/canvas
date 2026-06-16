package org.chovy.canvas.execution.adapter.messaging;

/**
 * 定义 MqTriggerRejectedEvent 的执行上下文数据结构或业务契约。
 * @param sourceMsgId sourceMsgId 对应的数据字段
 * @param triggerType triggerType 对应的数据字段
 * @param matchKey matchKey 对应的数据字段
 * @param reason reason 对应的数据字段
 * @param message message 对应的数据字段
 */
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
