package org.chovy.canvas.bi.api;
/**
 * BiPublishApprovalCommand 命令。
 */
public record BiPublishApprovalCommand(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        String reason) {
}
