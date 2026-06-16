package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiColumnPermissionCommand 命令。
 */
public record BiColumnPermissionCommand(
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * fieldKey 对应的业务键。
         */
        String fieldKey,
        /**
         * subjectType 字段值。
         */
        String subjectType,
        /**
         * subjectId 对应的标识。
         */
        String subjectId,
        /**
         * policy 字段值。
         */
        String policy,
        /**
         * mask 字段值。
         */
        Map<String, Object> mask,
        Boolean enabled) {

    public BiColumnPermissionCommand {
        mask = mask == null ? Map.of() : Map.copyOf(mask);
    }
}
