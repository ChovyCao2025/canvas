package org.chovy.canvas.domain.bi.datasource;

import java.time.LocalDateTime;

/**
 * BiDatasourceConnectionTestResult 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param sourceKey sourceKey 字段。
 * @param connectorType connectorType 字段。
 * @param success success 字段。
 * @param message message 字段。
 * @param databaseProductName databaseProductName 字段。
 * @param databaseProductVersion databaseProductVersion 字段。
 * @param checkedAt checkedAt 字段。
 * @param durationMs durationMs 字段。
 */
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
