package org.chovy.canvas.domain.bi.query;

public record BiFilter(
        String field,
        Operator operator,
        Object value
) {
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
