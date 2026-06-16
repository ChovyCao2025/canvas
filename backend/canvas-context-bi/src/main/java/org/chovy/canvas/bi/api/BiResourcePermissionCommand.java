package org.chovy.canvas.bi.api;
/**
 * BiResourcePermissionCommand 命令。
 */
public record BiResourcePermissionCommand(
        /**
         * 工作空间标识。
         */
        Long workspaceId,
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
        String effect) {
    /**
     * 执行 Bi Resource Permission Command 相关处理。
     */
    public BiResourcePermissionCommand(
            String resourceType,
            String resourceKey,
            Long resourceId,
            String subjectType,
            String subjectId,
            String actionKey,
            String effect) {
        this(null, resourceType, resourceKey, resourceId, subjectType, subjectId, actionKey, effect);
    }
}
