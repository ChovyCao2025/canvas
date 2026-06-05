package org.chovy.canvas.domain.bi.query;

public record BiDatasourceHealth(
        String sourceKey,
        String sourceType,
        boolean available,
        String message
) {
}
