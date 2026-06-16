package org.chovy.canvas.bi.api;
/**
 * BiResourceLocationCommand 命令。
 */
public record BiResourceLocationCommand(
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
