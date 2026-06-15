package org.chovy.canvas.bi.api;

import java.util.Map;

public record BiDashboardRuntimeStateCommand(
        Map<String, Object> parameters
) {
    public BiDashboardRuntimeStateCommand {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
