package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiDashboardRuntimeStateCommand 命令。
 */
public record BiDashboardRuntimeStateCommand(
        /**
         * 运行参数。
         */
        Map<String, Object> parameters
) {
    public BiDashboardRuntimeStateCommand {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
