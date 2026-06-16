package org.chovy.canvas.bi.api;
/**
 * BiPermissionGrantCommand 命令。
 */
public record BiPermissionGrantCommand(
        /**
         * 工作空间标识。
         */
        Long workspaceId,
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源标识。
         */
        Long resourceId,
        /**
         * subjectType 字段值。
         */
        String subjectType,
        /**
         * subjectId 对应的标识。
         */
        String subjectId,
        /**
         * actionKey 对应的业务键。
         */
        String actionKey,
        /**
         * effect 字段值。
         */
        String effect
) {
}
