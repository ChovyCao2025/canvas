package org.chovy.canvas.bi.api;
/**
 * BiSelfServiceExportReviewCommand 命令。
 */
public record BiSelfServiceExportReviewCommand(
        /**
         * 状态值。
         */
        String status,
        String reviewComment) {
}
