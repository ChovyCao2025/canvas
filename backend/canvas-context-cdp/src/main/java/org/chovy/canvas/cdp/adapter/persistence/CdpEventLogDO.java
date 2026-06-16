package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载 CdpEventLog 表的持久化字段。
 */
@TableName("cdp_event_log")
public class CdpEventLogDO {

    /**
     * 唯一标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户标识。
     */
    private Long tenantId;

    /**
     * write Key Id。
     */
    private Long writeKeyId;

    /**
     * 消息标识。
     */
    private String messageId;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 事件编码。
     */
    private String eventCode;

    /**
     * 用户标识。
     */
    private String userId;

    /**
     * anonymous Id。
     */
    private String anonymousId;

    /**
     * session Id。
     */
    private String sessionId;

    /**
     * device Id。
     */
    private String deviceId;

    /**
     * platform。
     */
    private String platform;

    /**
     * sdk Context。
     */
    private String sdkContext;

    /**
     * 扩展属性。
     */
    private String properties;

    /**
     * 幂等键。
     */
    private String idempotencyKey;

    /**
     * 事件时间。
     */
    private LocalDateTime eventTime;

    /**
     * sent At。
     */
    private LocalDateTime sentAt;

    /**
     * received At。
     */
    private LocalDateTime receivedAt;

    /**
     * 状态。
     */
    private String status;

    /**
     * error Message。
     */
    private String errorMessage;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 返回唯一标识。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置唯一标识。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 返回租户标识。
     */
    public Long getTenantId() {
        return tenantId;
    }

    /**
     * 设置租户标识。
     */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 返回write Key Id。
     */
    public Long getWriteKeyId() {
        return writeKeyId;
    }

    /**
     * 设置write Key Id。
     */
    public void setWriteKeyId(Long writeKeyId) {
        this.writeKeyId = writeKeyId;
    }

    /**
     * 返回消息标识。
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 设置消息标识。
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * 返回事件类型。
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 设置事件类型。
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * 返回事件编码。
     */
    public String getEventCode() {
        return eventCode;
    }

    /**
     * 设置事件编码。
     */
    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    /**
     * 返回用户标识。
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户标识。
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 返回anonymous Id。
     */
    public String getAnonymousId() {
        return anonymousId;
    }

    /**
     * 设置anonymous Id。
     */
    public void setAnonymousId(String anonymousId) {
        this.anonymousId = anonymousId;
    }

    /**
     * 返回session Id。
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置session Id。
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 返回device Id。
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * 设置device Id。
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * 返回platform。
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * 设置platform。
     */
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    /**
     * 返回sdk Context。
     */
    public String getSdkContext() {
        return sdkContext;
    }

    /**
     * 设置sdk Context。
     */
    public void setSdkContext(String sdkContext) {
        this.sdkContext = sdkContext;
    }

    /**
     * 返回扩展属性。
     */
    public String getProperties() {
        return properties;
    }

    /**
     * 设置扩展属性。
     */
    public void setProperties(String properties) {
        this.properties = properties;
    }

    /**
     * 返回幂等键。
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * 设置幂等键。
     */
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * 返回事件时间。
     */
    public LocalDateTime getEventTime() {
        return eventTime;
    }

    /**
     * 设置事件时间。
     */
    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    /**
     * 返回sent At。
     */
    public LocalDateTime getSentAt() {
        return sentAt;
    }

    /**
     * 设置sent At。
     */
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    /**
     * 返回received At。
     */
    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    /**
     * 设置received At。
     */
    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    /**
     * 返回状态。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态。
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 返回error Message。
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置error Message。
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 返回创建时间。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
