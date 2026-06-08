package org.chovy.canvas.domain.bi.query;

/**
 * BiFilter 承载 domain.bi.query 场景中的不可变数据快照。
 * @param field field 字段。
 * @param operator operator 字段。
 * @param value value 字段。
 */
public record BiFilter(
        String field,
        Operator operator,
        Object value
) {
    /**
     * Operator 枚举类型。
     */
    public enum Operator {
        EQ,
        NEQ,
        GT,
        GTE,
        LT,
        LTE,
        BETWEEN,
        IN,
        CONTAINS
    }
}
