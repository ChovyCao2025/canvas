package org.chovy.canvas.domain.bi.permission;

import org.chovy.canvas.domain.bi.query.BiFilter;

import java.util.List;
import java.util.Map;

/**
 * BiRowPermissionCommand 承载 domain.bi.permission 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param ruleKey ruleKey 字段。
 * @param subjectType subjectType 字段。
 * @param subjectId subjectId 字段。
 * @param filters filters 字段。
 * @param filter filter 字段。
 * @param enabled enabled 字段。
 */
public record BiRowPermissionCommand(
        String datasetKey,
        String ruleKey,
        String subjectType,
        String subjectId,
        List<BiFilter> filters,
        Map<String, Object> filter,
        Boolean enabled) {
    public BiRowPermissionCommand {
        filters = filters == null ? List.of() : List.copyOf(filters);
        filter = filter == null ? Map.of() : Map.copyOf(filter);
    }
}
