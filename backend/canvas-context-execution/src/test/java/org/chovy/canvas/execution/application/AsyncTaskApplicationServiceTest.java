package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.chovy.canvas.execution.api.AsyncTaskFacade;
import org.junit.jupiter.api.Test;

class AsyncTaskApplicationServiceTest {

    @Test
    void listAppliesFiltersPagingAndNonAdminVisibility() {
        AsyncTaskFacade service = new AsyncTaskApplicationService();

        List<AsyncTaskFacade.AsyncTaskView> tasks = service.listTasks(
                new AsyncTaskFacade.AsyncTaskQuery(
                        "AUDIENCE_COMPUTE",
                        "AUDIENCE",
                        List.of("aud-1", "aud-2"),
                        List.of("RUNNING", "SUCCEEDED"),
                        "operator-1",
                        false,
                        0,
                        500));

        assertThat(tasks).extracting(AsyncTaskFacade.AsyncTaskView::taskId)
                .containsExactly("task-audience-running", "task-audience-succeeded");
        assertThat(tasks).allSatisfy(task -> assertThat(task.taskType()).isEqualTo("AUDIENCE_COMPUTE"));
        assertThat(tasks.get(0).progress()).isEqualTo(45);
        assertThat(tasks.get(1).resultSummary()).isEqualTo("matched 120 users");
    }

    @Test
    void getHonorsOwnerSubscriberAdminVisibilityAndRejectsMissingTask() {
        AsyncTaskFacade service = new AsyncTaskApplicationService();

        assertThat(service.getTask("task-owned", "operator-1", false).createdBy()).isEqualTo("operator-1");
        assertThat(service.getTask("task-subscribed", "operator-1", false).createdBy()).isEqualTo("analyst-2");
        assertThat(service.getTask("task-admin-only", "admin", true).createdBy()).isEqualTo("analyst-3");

        assertThatThrownBy(() -> service.getTask("task-admin-only", "operator-1", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Async task not found: task-admin-only");
        assertThatThrownBy(() -> service.getTask("missing", "admin", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Async task not found: missing");
    }
}
