package org.chovy.canvas.domain.bi.query;

public record BiSort(
        String field,
        Direction direction
) {
    public enum Direction {
        ASC,
        DESC
    }
}
