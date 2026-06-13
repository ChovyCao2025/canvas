package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控运行时值解析结果。
 *
 * @param present 是否解析到值
 * @param value 解析到的值
 */
public record RiskResolvedValue(boolean present, Object value) {

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
