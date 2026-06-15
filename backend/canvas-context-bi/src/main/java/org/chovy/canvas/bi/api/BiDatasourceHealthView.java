package org.chovy.canvas.bi.api;

public record BiDatasourceHealthView(
        String sourceKey,
        String sourceType,
        boolean available,
        String message) {
}
