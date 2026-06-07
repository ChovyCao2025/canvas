package org.chovy.canvas.domain.bi.datasource;

import java.time.LocalDateTime;

public record BiDatasourceConnectionTestResult(
        Long id,
        String sourceKey,
        String connectorType,
        boolean success,
        String message,
        String databaseProductName,
        String databaseProductVersion,
        LocalDateTime checkedAt,
        Long durationMs) {
}
