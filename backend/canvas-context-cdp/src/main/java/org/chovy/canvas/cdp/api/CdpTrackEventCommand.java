package org.chovy.canvas.cdp.api;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 表示 CdpTrackEventCommand 的业务数据或处理组件。
 */
public final class CdpTrackEventCommand {

    /**
     * 消息标识。
     */
    private final String messageId;

    /**
     * type。
     */
    private final String type;

    /**
     * event。
     */
    private final String event;

    /**
     * 用户标识。
     */
    private final String userId;

    /**
     * anonymous Id。
     */
    private final String anonymousId;

    /**
     * 幂等键。
     */
    private final String idempotencyKey;

    /**
     * 扩展属性。
     */
    private final Map<String, Object> properties;

    /**
     * context。
     */
    private final Map<String, Object> context;

    /**
     * timestamp。
     */
    private final OffsetDateTime timestamp;

    /**
     * 发送时间。
     */
    private final OffsetDateTime sentAt;

    /**
     * 使用记录字段创建 CdpTrackEventCommand。
     */
    public CdpTrackEventCommand(
            String messageId,
            String type,
            String event,
            String userId,
            String anonymousId,
            String idempotencyKey,
            Map<String, Object> properties,
            Map<String, Object> context,
            OffsetDateTime timestamp,
            OffsetDateTime sentAt) {
        this.messageId = messageId;
        this.type = type;
        this.event = event;
        this.userId = userId;
        this.anonymousId = anonymousId;
        this.idempotencyKey = idempotencyKey;
        this.properties = properties;
        this.context = context;
        this.timestamp = timestamp;
        this.sentAt = sentAt;
    }

    /**
     * 返回消息标识。
     */
    public String messageId() {
        return messageId;
    }

    /**
     * 返回type。
     */
    public String type() {
        return type;
    }

    /**
     * 返回event。
     */
    public String event() {
        return event;
    }

    /**
     * 返回用户标识。
     */
    public String userId() {
        return userId;
    }

    /**
     * 返回anonymous Id。
     */
    public String anonymousId() {
        return anonymousId;
    }

    /**
     * 返回幂等键。
     */
    public String idempotencyKey() {
        return idempotencyKey;
    }

    /**
     * 返回扩展属性。
     */
    public Map<String, Object> properties() {
        return properties;
    }

    /**
     * 返回context。
     */
    public Map<String, Object> context() {
        return context;
    }

    /**
     * 返回timestamp。
     */
    public OffsetDateTime timestamp() {
        return timestamp;
    }

    /**
     * 返回发送时间。
     */
    public OffsetDateTime sentAt() {
        return sentAt;
    }

    /**
     * 按所有字段比较 CdpTrackEventCommand。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpTrackEventCommand that = (CdpTrackEventCommand) o;
        return java.util.Objects.equals(messageId, that.messageId)
                && java.util.Objects.equals(type, that.type)
                && java.util.Objects.equals(event, that.event)
                && java.util.Objects.equals(userId, that.userId)
                && java.util.Objects.equals(anonymousId, that.anonymousId)
                && java.util.Objects.equals(idempotencyKey, that.idempotencyKey)
                && java.util.Objects.equals(properties, that.properties)
                && java.util.Objects.equals(context, that.context)
                && java.util.Objects.equals(timestamp, that.timestamp)
                && java.util.Objects.equals(sentAt, that.sentAt);
    }

    /**
     * 根据所有字段计算 CdpTrackEventCommand 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(messageId, type, event, userId, anonymousId, idempotencyKey, properties, context, timestamp, sentAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpTrackEventCommand[" + "messageId=" + messageId + ", type=" + type + ", event=" + event + ", userId=" + userId + ", anonymousId=" + anonymousId + ", idempotencyKey=" + idempotencyKey + ", properties=" + properties + ", context=" + context + ", timestamp=" + timestamp + ", sentAt=" + sentAt + "]";
    }
}
