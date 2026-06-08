package org.chovy.canvas.domain.bi.dashboard;

import java.util.Map;

/**
 * BiDashboardRuntimeStateCommand 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param parameters parameters 字段。
 */
public record BiDashboardRuntimeStateCommand(Map<String, Object> parameters) {

    public BiDashboardRuntimeStateCommand {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
