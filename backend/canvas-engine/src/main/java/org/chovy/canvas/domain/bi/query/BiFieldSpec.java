package org.chovy.canvas.domain.bi.query;

/**
 * BiFieldSpec 承载 domain.bi.query 场景中的不可变数据快照。
 * @param fieldKey fieldKey 字段。
 * @param columnExpression columnExpression 字段。
 * @param role role 字段。
 * @param valueType valueType 字段。
 */
public record BiFieldSpec(
        String fieldKey,
        String columnExpression,
        Role role,
        String valueType
) {
    /**
     * Role 枚举类型。
     */
    public enum Role {
        DIMENSION,
        MEASURE
    }
}
