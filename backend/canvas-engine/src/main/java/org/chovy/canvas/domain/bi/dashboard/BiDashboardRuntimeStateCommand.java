package org.chovy.canvas.domain.bi.dashboard;

import java.util.Map;

public record BiDashboardRuntimeStateCommand(Map<String, Object> parameters) {

    public BiDashboardRuntimeStateCommand {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
