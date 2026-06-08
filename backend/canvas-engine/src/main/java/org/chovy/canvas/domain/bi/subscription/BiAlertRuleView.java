package org.chovy.canvas.domain.bi.subscription;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * BiAlertRuleView 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param alertKey alertKey 字段。
 * @param name name 字段。
 * @param datasetKey datasetKey 字段。
 * @param datasetId datasetId 字段。
 * @param metricKey metricKey 字段。
 * @param condition condition 字段。
 * @param receivers receivers 字段。
 * @param enabled enabled 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record BiAlertRuleView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String alertKey,
        String name,
        String datasetKey,
        Long datasetId,
        String metricKey,
        Map<String, Object> condition,
        Map<String, Object> receivers,
        Boolean enabled,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BiAlertRuleView {
        condition = condition == null ? Map.of() : Map.copyOf(condition);
        receivers = receivers == null ? Map.of() : Map.copyOf(receivers);
    }
}
