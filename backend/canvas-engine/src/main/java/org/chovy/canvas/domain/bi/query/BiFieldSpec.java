package org.chovy.canvas.domain.bi.query;

public record BiFieldSpec(
        String fieldKey,
        String columnExpression,
        Role role,
        String valueType
) {
    public enum Role {
        DIMENSION,
        MEASURE
    }
}
