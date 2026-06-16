package org.chovy.canvas.bi.api;
/**
 * BiResourceMoveCommand 命令。
 */
public record BiResourceMoveCommand(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * folderKey 对应的业务键。
         */
        String folderKey,
        Integer sortOrder) {
}
