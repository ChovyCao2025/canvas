package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiSpreadsheetResourceCommand 命令。
 */
public record BiSpreadsheetResourceCommand(
        /**
         * spreadsheetKey 对应的业务键。
         */
        String spreadsheetKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * 说明文本。
         */
        String description,
        /**
         * sheets 对应的数据集合。
         */
        List<Map<String, Object>> sheets,
        /**
         * dataBinding 字段值。
         */
        Map<String, Object> dataBinding,
        /**
         * 样式配置。
         */
        Map<String, Object> style,
        String status) {

    public BiSpreadsheetResourceCommand {
        sheets = sheets == null ? List.of() : List.copyOf(sheets);
        dataBinding = dataBinding == null ? Map.of() : Map.copyOf(dataBinding);
        style = style == null ? Map.of() : Map.copyOf(style);
    }
}
