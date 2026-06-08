package org.chovy.canvas.domain.bi.spreadsheet;

import java.time.LocalDateTime;

/**
 * BiSpreadsheetVersionView 承载 domain.bi.spreadsheet 场景中的不可变数据快照。
 * @param id id 字段。
 * @param spreadsheetKey spreadsheetKey 字段。
 * @param version version 字段。
 * @param status status 字段。
 * @param resource resource 字段。
 * @param publishedBy publishedBy 字段。
 * @param createdAt createdAt 字段。
 */
public record BiSpreadsheetVersionView(
        Long id,
        String spreadsheetKey,
        Integer version,
        String status,
        BiSpreadsheetResource resource,
        String publishedBy,
        LocalDateTime createdAt) {
}
