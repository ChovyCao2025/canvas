package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiSelfServiceExportCommand 命令。
 */
public record BiSelfServiceExportCommand(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * 资源标识。
         */
        Long resourceId,
        /**
         * exportFormat 对应的时间。
         */
        String exportFormat,
        /**
         * 查询定义。
         */
        Map<String, Object> query,
        /**
         * rowLimit 字段值。
         */
        Integer rowLimit,
        /**
         * approvalRequired 字段值。
         */
        Boolean approvalRequired,
        /**
         * sensitive 字段值。
         */
        Boolean sensitive,
        String approvalReason) {

    public BiSelfServiceExportCommand {
        query = query == null ? Map.of() : Map.copyOf(query);
    }
}
