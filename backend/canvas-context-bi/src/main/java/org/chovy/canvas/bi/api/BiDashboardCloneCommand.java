package org.chovy.canvas.bi.api;

public record BiDashboardCloneCommand(
        String dashboardKey,
        String name,
        String description
) {
}
