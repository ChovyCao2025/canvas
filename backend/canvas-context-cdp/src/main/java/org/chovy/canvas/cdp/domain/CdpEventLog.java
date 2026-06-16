package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 表示 CdpEventLog 的业务数据或处理组件。
 */
public final class CdpEventLog {

    /**
     * 唯一标识。
     */
    private final Long id;

    /**
     * 租户标识。
     */
    private final Long tenantId;

    /**
     * write Key Id。
     */
    private final Long writeKeyId;

    /**
     * 消息标识。
     */
    private final String messageId;

    /**
     * 事件类型。
     */
    private final String eventType;

    /**
     * 事件编码。
     */
    private final String eventCode;

    /**
     * 用户标识。
     */
    private final String userId;

    /**
     * anonymous Id。
     */
    private final String anonymousId;

    /**
     * session Id。
     */
    private final String sessionId;

    /**
     * device Id。
     */
    private final String deviceId;

    /**
     * 平台。
     */
    private final String platform;

    /**
     * sdk Context。
     */
    private final Map<String, Object> sdkContext;

    /**
     * 扩展属性。
     */
    private final Map<String, Object> properties;

    /**
     * 幂等键。
     */
    private final String idempotencyKey;

    /**
     * 事件时间。
     */
    private final LocalDateTime eventTime;

    /**
     * 发送时间。
     */
    private final LocalDateTime sentAt;

    /**
     * 接收时间。
     */
    private final LocalDateTime receivedAt;

    /**
     * 状态。
     */
    private final String status;

    /**
     * error Message。
     */
    private final String errorMessage;

    /**
     * 创建时间。
     */
    private final LocalDateTime createdAt;

    /**
     * 使用记录字段创建 CdpEventLog。
     */
    public CdpEventLog(
            Long id,
            Long tenantId,
            Long writeKeyId,
            String messageId,
            String eventType,
            String eventCode,
            String userId,
            String anonymousId,
            String sessionId,
            String deviceId,
            String platform,
            Map<String, Object> sdkContext,
            Map<String, Object> properties,
            String idempotencyKey,
            LocalDateTime eventTime,
            LocalDateTime sentAt,
            LocalDateTime receivedAt,
            String status,
            String errorMessage,
            LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.writeKeyId = writeKeyId;
        this.messageId = messageId;
        this.eventType = eventType;
        this.eventCode = eventCode;
        this.userId = userId;
        this.anonymousId = anonymousId;
        this.sessionId = sessionId;
        this.deviceId = deviceId;
        this.platform = platform;
        this.sdkContext = sdkContext;
        this.properties = properties;
        this.idempotencyKey = idempotencyKey;
        this.eventTime = eventTime;
        this.sentAt = sentAt;
        this.receivedAt = receivedAt;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

/**
 * ACCEPTED。
 */
public static final String ACCEPTED = "ACCEPTED";

    /**
     * 返回替换Id后的副本。
     */
    public CdpEventLog withId(Long newId) {
        return new CdpEventLog(newId, tenantId, writeKeyId, messageId, eventType, eventCode, userId, anonymousId,
                sessionId, deviceId, platform, sdkContext, properties, idempotencyKey, eventTime, sentAt, receivedAt,
                status, errorMessage, createdAt);
    }

    /**
     * 返回唯一标识。
     */
    public Long id() {
        return id;
    }

    /**
     * 返回租户标识。
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回write Key Id。
     */
    public Long writeKeyId() {
        return writeKeyId;
    }

    /**
     * 返回消息标识。
     */
    public String messageId() {
        return messageId;
    }

    /**
     * 返回事件类型。
     */
    public String eventType() {
        return eventType;
    }

    /**
     * 返回事件编码。
     */
    public String eventCode() {
        return eventCode;
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
     * 返回session Id。
     */
    public String sessionId() {
        return sessionId;
    }

    /**
     * 返回device Id。
     */
    public String deviceId() {
        return deviceId;
    }

    /**
     * 返回平台。
     */
    public String platform() {
        return platform;
    }

    /**
     * 返回sdk Context。
     */
    public Map<String, Object> sdkContext() {
        return sdkContext;
    }

    /**
     * 返回扩展属性。
     */
    public Map<String, Object> properties() {
        return properties;
    }

    /**
     * 返回幂等键。
     */
    public String idempotencyKey() {
        return idempotencyKey;
    }

    /**
     * 返回事件时间。
     */
    public LocalDateTime eventTime() {
        return eventTime;
    }

    /**
     * 返回发送时间。
     */
    public LocalDateTime sentAt() {
        return sentAt;
    }

    /**
     * 返回接收时间。
     */
    public LocalDateTime receivedAt() {
        return receivedAt;
    }

    /**
     * 返回状态。
     */
    public String status() {
        return status;
    }

    /**
     * 返回error Message。
     */
    public String errorMessage() {
        return errorMessage;
    }

    /**
     * 返回创建时间。
     */
    public LocalDateTime createdAt() {
        return createdAt;
    }

    /**
     * 按所有字段比较 CdpEventLog。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpEventLog that = (CdpEventLog) o;
        return java.util.Objects.equals(id, that.id)
                && java.util.Objects.equals(tenantId, that.tenantId)
                && java.util.Objects.equals(writeKeyId, that.writeKeyId)
                && java.util.Objects.equals(messageId, that.messageId)
                && java.util.Objects.equals(eventType, that.eventType)
                && java.util.Objects.equals(eventCode, that.eventCode)
                && java.util.Objects.equals(userId, that.userId)
                && java.util.Objects.equals(anonymousId, that.anonymousId)
                && java.util.Objects.equals(sessionId, that.sessionId)
                && java.util.Objects.equals(deviceId, that.deviceId)
                && java.util.Objects.equals(platform, that.platform)
                && java.util.Objects.equals(sdkContext, that.sdkContext)
                && java.util.Objects.equals(properties, that.properties)
                && java.util.Objects.equals(idempotencyKey, that.idempotencyKey)
                && java.util.Objects.equals(eventTime, that.eventTime)
                && java.util.Objects.equals(sentAt, that.sentAt)
                && java.util.Objects.equals(receivedAt, that.receivedAt)
                && java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(errorMessage, that.errorMessage)
                && java.util.Objects.equals(createdAt, that.createdAt);
    }

    /**
     * 根据所有字段计算 CdpEventLog 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, tenantId, writeKeyId, messageId, eventType, eventCode, userId, anonymousId, sessionId, deviceId, platform, sdkContext, properties, idempotencyKey, eventTime, sentAt, receivedAt, status, errorMessage, createdAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpEventLog[" + "id=" + id + ", tenantId=" + tenantId + ", writeKeyId=" + writeKeyId + ", messageId=" + messageId + ", eventType=" + eventType + ", eventCode=" + eventCode + ", userId=" + userId + ", anonymousId=" + anonymousId + ", sessionId=" + sessionId + ", deviceId=" + deviceId + ", platform=" + platform + ", sdkContext=" + sdkContext + ", properties=" + properties + ", idempotencyKey=" + idempotencyKey + ", eventTime=" + eventTime + ", sentAt=" + sentAt + ", receivedAt=" + receivedAt + ", status=" + status + ", errorMessage=" + errorMessage + ", createdAt=" + createdAt + "]";
    }
}
