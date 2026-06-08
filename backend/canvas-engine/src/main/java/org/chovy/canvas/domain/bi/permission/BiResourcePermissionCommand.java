package org.chovy.canvas.domain.bi.permission;

/**
 * BiResourcePermissionCommand 承载 domain.bi.permission 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param resourceId resourceId 字段。
 * @param subjectType subjectType 字段。
 * @param subjectId subjectId 字段。
 * @param actionKey actionKey 字段。
 * @param effect effect 字段。
 */
public record BiResourcePermissionCommand(
        String resourceType,
        String resourceKey,
        Long resourceId,
        String subjectType,
        String subjectId,
        String actionKey,
        String effect) {
}
