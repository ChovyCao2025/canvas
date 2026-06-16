package org.chovy.canvas.bi.api;
/**
 * BiResourceLockCommand 命令。
 */
public record BiResourceLockCommand(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * lockToken 字段值。
         */
        String lockToken,
        Integer ttlSeconds) {
}
