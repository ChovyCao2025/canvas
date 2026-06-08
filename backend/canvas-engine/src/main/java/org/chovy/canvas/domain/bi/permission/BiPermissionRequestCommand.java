package org.chovy.canvas.domain.bi.permission;

/**
 * BiPermissionRequestCommand 承载 domain.bi.permission 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param requestedAction requestedAction 字段。
 * @param reason reason 字段。
 */
public record BiPermissionRequestCommand(
        String resourceType,
        String resourceKey,
        String requestedAction,
        String reason) {
}
