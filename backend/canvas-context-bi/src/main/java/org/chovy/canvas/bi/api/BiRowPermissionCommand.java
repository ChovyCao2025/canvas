package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiRowPermissionCommand 命令。
 */
public record BiRowPermissionCommand(
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * ruleKey 对应的业务键。
         */
        String ruleKey,
        /**
         * subjectType 字段值。
         */
        String subjectType,
        /**
         * subjectId 对应的标识。
         */
        String subjectId,
        /**
         * 筛选条件。
         */
        List<Map<String, Object>> filters,
        /**
         * filter 字段值。
         */
        Map<String, Object> filter,
        Boolean enabled) {

    public BiRowPermissionCommand {
        filters = filters == null ? List.of() : List.copyOf(filters);
        filter = filter == null ? Map.of() : Map.copyOf(filter);
    }
}
