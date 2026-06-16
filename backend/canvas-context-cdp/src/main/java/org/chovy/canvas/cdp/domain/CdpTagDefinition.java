package org.chovy.canvas.cdp.domain;

/**
 * 表示 CdpTagDefinition 的业务数据或处理组件。
 */
public final class CdpTagDefinition {

    /**
     * 标签编码。
     */
    private final String tagCode;

    /**
     * 名称。
     */
    private final String name;

    /**
     * 值类型。
     */
    private final String valueType;

    /**
     * enabled。
     */
    private final boolean enabled;

    /**
     * manual Enabled。
     */
    private final boolean manualEnabled;

    /**
     * default Ttl Days。
     */
    private final Integer defaultTtlDays;

    /**
     * 使用记录字段创建 CdpTagDefinition。
     */
    public CdpTagDefinition(
            String tagCode,
            String name,
            String valueType,
            boolean enabled,
            boolean manualEnabled,
            Integer defaultTtlDays) {
        this.tagCode = tagCode;
        this.name = name;
        this.valueType = valueType;
        this.enabled = enabled;
        this.manualEnabled = manualEnabled;
        this.defaultTtlDays = defaultTtlDays;
    }

    /**
     * 返回标签编码。
     */
    public String tagCode() {
        return tagCode;
    }

    /**
     * 返回名称。
     */
    public String name() {
        return name;
    }

    /**
     * 返回值类型。
     */
    public String valueType() {
        return valueType;
    }

    /**
     * 返回enabled。
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * 返回manual Enabled。
     */
    public boolean manualEnabled() {
        return manualEnabled;
    }

    /**
     * 返回default Ttl Days。
     */
    public Integer defaultTtlDays() {
        return defaultTtlDays;
    }

    /**
     * 按所有字段比较 CdpTagDefinition。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpTagDefinition that = (CdpTagDefinition) o;
        return java.util.Objects.equals(tagCode, that.tagCode)
                && java.util.Objects.equals(name, that.name)
                && java.util.Objects.equals(valueType, that.valueType)
                && java.util.Objects.equals(enabled, that.enabled)
                && java.util.Objects.equals(manualEnabled, that.manualEnabled)
                && java.util.Objects.equals(defaultTtlDays, that.defaultTtlDays);
    }

    /**
     * 根据所有字段计算 CdpTagDefinition 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(tagCode, name, valueType, enabled, manualEnabled, defaultTtlDays);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpTagDefinition[" + "tagCode=" + tagCode + ", name=" + name + ", valueType=" + valueType + ", enabled=" + enabled + ", manualEnabled=" + manualEnabled + ", defaultTtlDays=" + defaultTtlDays + "]";
    }
}
