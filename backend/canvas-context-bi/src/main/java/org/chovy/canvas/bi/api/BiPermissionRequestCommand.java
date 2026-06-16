package org.chovy.canvas.bi.api;
/**
 * BiPermissionRequestCommand 命令。
 */
public record BiPermissionRequestCommand(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * requestedAction 字段值。
         */
        String requestedAction,
        String reason) {
}
