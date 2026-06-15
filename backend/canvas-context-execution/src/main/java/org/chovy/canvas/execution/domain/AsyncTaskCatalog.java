package org.chovy.canvas.execution.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.chovy.canvas.execution.api.AsyncTaskFacade.AsyncTaskQuery;
import org.chovy.canvas.execution.api.AsyncTaskFacade.AsyncTaskView;

public class AsyncTaskCatalog {

    private static final LocalDateTime BASE_TIME = LocalDateTime.parse("2026-06-15T06:00:00");

    private final List<TaskEntry> tasks = List.of(
            task("task-audience-running", "AUDIENCE_COMPUTE", "AUDIENCE", "aud-1",
                    "Compute audience aud-1", "RUNNING", 45, null, null, "operator-1", List.of("analyst-2")),
            task("task-audience-succeeded", "AUDIENCE_COMPUTE", "AUDIENCE", "aud-2",
                    "Compute audience aud-2", "SUCCEEDED", 100, "matched 120 users", null, "analyst-2", List.of("operator-1")),
            task("task-owned", "TAG_IMPORT", "TAG_IMPORT", "import-1",
                    "Import tags", "QUEUED", 0, null, null, "operator-1", List.of()),
            task("task-subscribed", "TAG_IMPORT", "TAG_IMPORT", "import-2",
                    "Import subscribed tags", "RUNNING", 10, null, null, "analyst-2", List.of("operator-1")),
            task("task-admin-only", "WAREHOUSE_SYNC", "WAREHOUSE", "warehouse-1",
                    "Warehouse sync", "FAILED", 100, null, "timeout", "analyst-3", List.of()));

    public List<AsyncTaskView> listTasks(AsyncTaskQuery query) {
        AsyncTaskQuery safeQuery = query == null
                ? new AsyncTaskQuery(null, null, List.of(), List.of(), "system", false, 1, 100)
                : query;
        int page = Math.max(1, safeQuery.page());
        int size = Math.max(1, Math.min(200, safeQuery.size()));
        return tasks.stream()
                .filter(task -> matches(safeQuery.taskType(), task.view().taskType()))
                .filter(task -> matches(safeQuery.bizType(), task.view().bizType()))
                .filter(task -> safeQuery.bizIds() == null || safeQuery.bizIds().isEmpty()
                        || safeQuery.bizIds().contains(task.view().bizId()))
                .filter(task -> safeQuery.statuses() == null || safeQuery.statuses().isEmpty()
                        || safeQuery.statuses().contains(task.view().status()))
                .filter(task -> safeQuery.admin() || canView(task, safeQuery.username()))
                .sorted(Comparator.comparing((TaskEntry task) -> task.view().createdAt()).reversed())
                .skip((long) (page - 1) * size)
                .limit(size)
                .map(TaskEntry::view)
                .toList();
    }

    public AsyncTaskView getTask(String taskId, String username, boolean admin) {
        return tasks.stream()
                .filter(task -> Objects.equals(task.view().taskId(), taskId))
                .filter(task -> admin || canView(task, username))
                .findFirst()
                .map(TaskEntry::view)
                .orElseThrow(() -> new IllegalArgumentException("Async task not found: " + taskId));
    }

    private static boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || Objects.equals(expected, actual);
    }

    private static boolean canView(TaskEntry task, String username) {
        String actor = username == null || username.isBlank() ? "system" : username;
        return actor.equals(task.view().createdBy()) || task.subscribers().contains(actor);
    }

    private static TaskEntry task(
            String taskId,
            String taskType,
            String bizType,
            String bizId,
            String title,
            String status,
            Integer progress,
            String resultSummary,
            String errorMsg,
            String createdBy,
            List<String> subscribers) {
        LocalDateTime createdAt = switch (taskId) {
            case "task-audience-running" -> BASE_TIME.plusMinutes(5);
            case "task-audience-succeeded" -> BASE_TIME.plusMinutes(4);
            case "task-owned" -> BASE_TIME.plusMinutes(3);
            case "task-subscribed" -> BASE_TIME.plusMinutes(2);
            default -> BASE_TIME;
        };
        return new TaskEntry(
                new AsyncTaskView(
                        taskId,
                        taskType,
                        bizType,
                        bizId,
                        title,
                        status,
                        progress,
                        resultSummary,
                        errorMsg,
                        startedAt(status, createdAt),
                        finishedAt(status, createdAt),
                        createdAt,
                        createdAt.plusMinutes(1),
                        createdBy),
                subscribers);
    }

    private static LocalDateTime startedAt(String status, LocalDateTime createdAt) {
        return "QUEUED".equals(status) ? null : createdAt.plusSeconds(30);
    }

    private static LocalDateTime finishedAt(String status, LocalDateTime createdAt) {
        return switch (status) {
            case "SUCCEEDED", "FAILED", "CANCELED" -> createdAt.plusMinutes(2);
            default -> null;
        };
    }

    private record TaskEntry(AsyncTaskView view, List<String> subscribers) {
    }
}
