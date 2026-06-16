package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiDashboardExportPackageView 视图。
 */
public record BiDashboardExportPackageView(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * sourceDashboardKey 对应的业务键。
         */
        String sourceDashboardKey,
        /**
         * dashboard 字段值。
         */
        BiDashboardView dashboard,
        /**
         * manifest 字段值。
         */
        Map<String, Object> manifest,
        /**
         * exportedAt 对应的时间。
         */
        LocalDateTime exportedAt,
        /**
         * 导出人。
         */
        String exportedBy
) {
    public BiDashboardExportPackageView {
        manifest = manifest == null ? Map.of() : Map.copyOf(manifest);
    }
}
