package org.chovy.canvas.domain.bi.spreadsheet;

import java.time.LocalDateTime;

public record BiSpreadsheetVersionView(
        Long id,
        String spreadsheetKey,
        Integer version,
        String status,
        BiSpreadsheetResource resource,
        String publishedBy,
        LocalDateTime createdAt) {
}
