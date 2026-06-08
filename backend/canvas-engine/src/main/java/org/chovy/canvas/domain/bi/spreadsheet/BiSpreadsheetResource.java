package org.chovy.canvas.domain.bi.spreadsheet;

import java.util.List;
import java.util.Map;

/**
 * BiSpreadsheetResource 承载 domain.bi.spreadsheet 场景中的不可变数据快照。
 * @param id id 字段。
 * @param spreadsheetKey spreadsheetKey 字段。
 * @param name name 字段。
 * @param description description 字段。
 * @param sheets sheets 字段。
 * @param dataBinding dataBinding 字段。
 * @param style style 字段。
 * @param status status 字段。
 * @param version version 字段。
 * @param source source 字段。
 */
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
