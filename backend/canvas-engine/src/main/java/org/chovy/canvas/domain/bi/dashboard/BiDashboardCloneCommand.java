package org.chovy.canvas.domain.bi.dashboard;

public record BiDashboardCloneCommand(
        String dashboardKey,
        String title,
        String description) {
}
