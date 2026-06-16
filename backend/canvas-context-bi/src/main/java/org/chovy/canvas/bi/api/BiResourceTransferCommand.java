package org.chovy.canvas.bi.api;
/**
 * BiResourceTransferCommand 命令。
 */
public record BiResourceTransferCommand(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        String ownerUser) {
}
