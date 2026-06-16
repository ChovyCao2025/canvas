package org.chovy.canvas.bi.api;
/**
 * BiPermissionRequestReviewCommand 命令。
 */
public record BiPermissionRequestReviewCommand(
        /**
         * requestId 对应的标识。
         */
        Long requestId,
        /**
         * 状态值。
         */
        String status,
        String reviewComment) {
}
