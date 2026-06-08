package org.chovy.canvas.domain.bi.dashboard;

import java.util.List;
import java.util.Map;

/**
 * BiDashboardFilterCascade 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param parentFilterKeys parentFilterKeys 字段。
 * @param parentFieldMapping parentFieldMapping 字段。
 * @param mode mode 字段。
 */
public record BiDashboardFilterCascade(
        List<String> parentFilterKeys,
        Map<String, String> parentFieldMapping,
        String mode
) {
    public BiDashboardFilterCascade {
        parentFilterKeys = parentFilterKeys == null ? List.of() : List.copyOf(parentFilterKeys);
        parentFieldMapping = parentFieldMapping == null ? Map.of() : Map.copyOf(parentFieldMapping);
        mode = mode == null || mode.isBlank() ? "SAME_SOURCE" : mode;
    }
}
