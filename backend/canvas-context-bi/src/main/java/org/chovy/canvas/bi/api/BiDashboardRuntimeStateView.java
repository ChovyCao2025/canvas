package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiDashboardRuntimeStateView 视图。
 */
public record BiDashboardRuntimeStateView(
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 仪表盘键。
         */
        String dashboardKey,
        /**
         * parameters 对应的数据集合。
         */
        Map<String, Object> parameters,
        /**
         * updatedBy 字段值。
         */
        String updatedBy,
        /**
         * 更新时间。
         */
        LocalDateTime updatedAt
) {
    public BiDashboardRuntimeStateView {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
