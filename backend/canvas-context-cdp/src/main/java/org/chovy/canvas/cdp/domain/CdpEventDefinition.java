package org.chovy.canvas.cdp.domain;

/**
 * 表示 CdpEventDefinition 的业务数据或处理组件。
 */
public final class CdpEventDefinition {

    /**
     * 事件编码。
     */
    private final String eventCode;

    /**
     * auto Discover。
     */
    private final boolean autoDiscover;

    /**
     * 使用记录字段创建 CdpEventDefinition。
     */
    public CdpEventDefinition(
            String eventCode,
            boolean autoDiscover) {
        this.eventCode = eventCode;
        this.autoDiscover = autoDiscover;
    }

    /**
     * 返回事件编码。
     */
    public String eventCode() {
        return eventCode;
    }

    /**
     * 返回auto Discover。
     */
    public boolean autoDiscover() {
        return autoDiscover;
    }

    /**
     * 按所有字段比较 CdpEventDefinition。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpEventDefinition that = (CdpEventDefinition) o;
        return java.util.Objects.equals(eventCode, that.eventCode)
                && java.util.Objects.equals(autoDiscover, that.autoDiscover);
    }

    /**
     * 根据所有字段计算 CdpEventDefinition 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(eventCode, autoDiscover);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpEventDefinition[" + "eventCode=" + eventCode + ", autoDiscover=" + autoDiscover + "]";
    }
}
