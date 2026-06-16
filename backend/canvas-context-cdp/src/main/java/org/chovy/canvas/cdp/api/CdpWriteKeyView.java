package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;

/**
 * 表示 CdpWriteKeyView 的业务数据或处理组件。
 */
public final class CdpWriteKeyView {

    /**
     * write Key Id。
     */
    private final Long writeKeyId;

    /**
     * 租户标识。
     */
    private final Long tenantId;

    /**
     * write Key。
     */
    private final String writeKey;

    /**
     * 平台。
     */
    private final String platform;

    /**
     * rate Limit Per Minute。
     */
    private final Integer rateLimitPerMinute;

    /**
     * expires At。
     */
    private final LocalDateTime expiresAt;

    /**
     * 使用记录字段创建 CdpWriteKeyView。
     */
    public CdpWriteKeyView(
            Long writeKeyId,
            Long tenantId,
            String writeKey,
            String platform,
            Integer rateLimitPerMinute,
            LocalDateTime expiresAt) {
        this.writeKeyId = writeKeyId;
        this.tenantId = tenantId;
        this.writeKey = writeKey;
        this.platform = platform;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.expiresAt = expiresAt;
    }

    /**
     * 返回write Key Id。
     */
    public Long writeKeyId() {
        return writeKeyId;
    }

    /**
     * 返回租户标识。
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回write Key。
     */
    public String writeKey() {
        return writeKey;
    }

    /**
     * 返回平台。
     */
    public String platform() {
        return platform;
    }

    /**
     * 返回rate Limit Per Minute。
     */
    public Integer rateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    /**
     * 返回expires At。
     */
    public LocalDateTime expiresAt() {
        return expiresAt;
    }

    /**
     * 按所有字段比较 CdpWriteKeyView。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpWriteKeyView that = (CdpWriteKeyView) o;
        return java.util.Objects.equals(writeKeyId, that.writeKeyId)
                && java.util.Objects.equals(tenantId, that.tenantId)
                && java.util.Objects.equals(writeKey, that.writeKey)
                && java.util.Objects.equals(platform, that.platform)
                && java.util.Objects.equals(rateLimitPerMinute, that.rateLimitPerMinute)
                && java.util.Objects.equals(expiresAt, that.expiresAt);
    }

    /**
     * 根据所有字段计算 CdpWriteKeyView 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(writeKeyId, tenantId, writeKey, platform, rateLimitPerMinute, expiresAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpWriteKeyView[" + "writeKeyId=" + writeKeyId + ", tenantId=" + tenantId + ", writeKey=" + writeKey + ", platform=" + platform + ", rateLimitPerMinute=" + rateLimitPerMinute + ", expiresAt=" + expiresAt + "]";
    }
}
