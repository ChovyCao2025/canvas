package org.chovy.canvas.bi.api;

public record BiDatasourceConnectionTestResult(
        Long dataSourceConfigId,
        String sourceKey,
        boolean success,
        String message,
        long latencyMs) {
}
