package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BiSpreadsheetResourceView(
        Long tenantId,
        String spreadsheetKey,
        String name,
        String description,
        List<Map<String, Object>> sheets,
        Map<String, Object> dataBinding,
        Map<String, Object> style,
        String status,
        Integer version,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public BiSpreadsheetResourceView {
        sheets = sheets == null ? List.of() : List.copyOf(sheets);
        dataBinding = dataBinding == null ? Map.of() : Map.copyOf(dataBinding);
        style = style == null ? Map.of() : Map.copyOf(style);
    }
}
