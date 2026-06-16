package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * BiSpreadsheetResourceView 视图。
 */
public record BiSpreadsheetResourceView(
        /**
         * 租户标识。
         */
        Long tenantId,
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
        /**
         * 状态值。
         */
        String status,
        /**
         * 版本号。
         */
        Integer version,
        /**
         * 创建人。
         */
        String createdBy,
        /**
         * updatedBy 字段值。
         */
        String updatedBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public BiSpreadsheetResourceView {
        sheets = sheets == null ? List.of() : List.copyOf(sheets);
        dataBinding = dataBinding == null ? Map.of() : Map.copyOf(dataBinding);
        style = style == null ? Map.of() : Map.copyOf(style);
    }
}
