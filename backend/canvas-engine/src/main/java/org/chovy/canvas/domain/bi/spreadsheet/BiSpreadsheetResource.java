package org.chovy.canvas.domain.bi.spreadsheet;

import java.util.List;
import java.util.Map;

public record BiSpreadsheetResource(
        Long id,
        String spreadsheetKey,
        String name,
        String description,
        List<Map<String, Object>> sheets,
        Map<String, Object> dataBinding,
        Map<String, Object> style,
        String status,
        Integer version,
        String source
) {
    public BiSpreadsheetResource {
        sheets = sheets == null ? List.of() : List.copyOf(sheets);
        dataBinding = dataBinding == null ? Map.of() : Map.copyOf(dataBinding);
        style = style == null ? Map.of() : Map.copyOf(style);
    }
}
