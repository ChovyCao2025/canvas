package org.chovy.canvas.cdp.api;

/**
 * 表示 CdpIngestionError 的业务数据或处理组件。
 */
public final class CdpIngestionError {

    /**
     * 消息标识。
     */
    private final String messageId;

    /**
     * 编码。
     */
    private final String code;

    /**
     * 消息。
     */
    private final String message;

    /**
     * 使用记录字段创建 CdpIngestionError。
     */
    public CdpIngestionError(
            String messageId,
            String code,
            String message) {
        this.messageId = messageId;
        this.code = code;
        this.message = message;
    }

    /**
     * 返回消息标识。
     */
    public String messageId() {
        return messageId;
    }

    /**
     * 返回编码。
     */
    public String code() {
        return code;
    }

    /**
     * 返回消息。
     */
    public String message() {
        return message;
    }

    /**
     * 按所有字段比较 CdpIngestionError。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpIngestionError that = (CdpIngestionError) o;
        return java.util.Objects.equals(messageId, that.messageId)
                && java.util.Objects.equals(code, that.code)
                && java.util.Objects.equals(message, that.message);
    }

    /**
     * 根据所有字段计算 CdpIngestionError 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(messageId, code, message);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpIngestionError[" + "messageId=" + messageId + ", code=" + code + ", message=" + message + "]";
    }
}
