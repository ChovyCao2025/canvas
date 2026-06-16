package org.chovy.canvas.risk.domain.runtime;

import java.util.Objects;

/**
 * 风控运行时值解析结果。
 *
 * @param present 是否解析到值
 * @param value 解析到的值
 */
public final class RiskResolvedValue {

    /**
     * RiskResolvedValue 的 present 字段。
     */
    private final boolean present;


    /**
     * RiskResolvedValue 的 value 字段。
     */
    private final Object value;


    /**
     * 创建 RiskResolvedValue。
     *
     * @param present RiskResolvedValue 的 present 字段
     * @param value RiskResolvedValue 的 value 字段
     */
    public RiskResolvedValue(boolean present, Object value) {
        this.present = present;
        this.value = value;
    }

    /**
     * 返回 RiskResolvedValue 的 present 字段。
     *
     * @return present 字段值
     */
    public boolean present() {
        return present;
    }

    /**
     * 返回 RiskResolvedValue 的 value 字段。
     *
     * @return value 字段值
     */
    public Object value() {
        return value;
    }

    /**
     * 比较当前 RiskResolvedValue 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskResolvedValue other)) {
            return false;
        }
        return present == other.present
                && Objects.equals(value, other.value);
    }

    /**
     * 计算 RiskResolvedValue 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(present, value);
    }

    /**
     * 返回 RiskResolvedValue 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskResolvedValue[present=" + present + ", value=" + value + "]";
    }

    /**
         * 创建存在值的解析结果。
         */
        public static RiskResolvedValue present(Object value) {
            return new RiskResolvedValue(true, value);
        }

        /**
         * 创建缺失值的解析结果。
         */
        public static RiskResolvedValue missing() {
            return new RiskResolvedValue(false, null);
        }
}
