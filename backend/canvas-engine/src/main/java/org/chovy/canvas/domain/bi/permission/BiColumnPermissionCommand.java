package org.chovy.canvas.domain.bi.permission;

import java.util.Map;

/**
 * BiColumnPermissionCommand 承载 domain.bi.permission 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param fieldKey fieldKey 字段。
 * @param subjectType subjectType 字段。
 * @param subjectId subjectId 字段。
 * @param policy policy 字段。
 * @param mask mask 字段。
 * @param enabled enabled 字段。
 */
public record BiColumnPermissionCommand(
        String datasetKey,
        String fieldKey,
        String subjectType,
        String subjectId,
        String policy,
        Map<String, Object> mask,
        Boolean enabled) {
    public BiColumnPermissionCommand {
        mask = mask == null ? Map.of() : Map.copyOf(mask);
    }
}
