package org.chovy.canvas.domain.bi.dashboard;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * BiDashboardRuntimeStateView 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param dashboardKey dashboardKey 字段。
 * @param username username 字段。
 * @param parameters parameters 字段。
 * @param updatedAt updatedAt 字段。
 */
public record BiDashboardRuntimeStateView(
        String dashboardKey,
        String username,
        Map<String, Object> parameters,
        LocalDateTime updatedAt) {

    public BiDashboardRuntimeStateView {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
