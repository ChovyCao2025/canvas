package org.chovy.canvas.domain.bi.permission;

/**
 * BiPermissionRequestReviewCommand 承载 domain.bi.permission 场景中的不可变数据快照。
 * @param requestId requestId 字段。
 * @param status status 字段。
 * @param reviewComment reviewComment 字段。
 */
public record BiPermissionRequestReviewCommand(
        Long requestId,
        String status,
        String reviewComment) {
}
