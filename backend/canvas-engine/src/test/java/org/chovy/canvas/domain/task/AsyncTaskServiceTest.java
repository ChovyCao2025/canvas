package org.chovy.canvas.domain.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncTaskServiceTest {

    @Mock
    private AsyncTaskMapper mapper;

    @Test
    void createOrReuseRunningTask_returnsExistingRunningTask() {
        AsyncTask existing = new AsyncTask();
        existing.setTaskId("task_existing");
        existing.setStatus(AsyncTaskStatus.RUNNING.name());
        when(mapper.selectOne(any())).thenReturn(existing);

        AsyncTaskService service = new AsyncTaskService(mapper);

        AsyncTaskCreateResult result = service.createOrReuseRunning(
                "AUDIENCE_COMPUTE", "AUDIENCE", "7", "计算人群：VIP", "operator");

        assertThat(result.created()).isFalse();
        assertThat(result.task().getTaskId()).isEqualTo("task_existing");
    }

    @Test
    void createOrReuseRunningTask_createsQueuedTaskWhenNoneRunning() {
        when(mapper.selectOne(any())).thenReturn(null);
        AsyncTaskService service = new AsyncTaskService(mapper);

        AsyncTaskCreateResult result = service.createOrReuseRunning(
                "AUDIENCE_COMPUTE", "AUDIENCE", "7", "计算人群：VIP", "operator");

        ArgumentCaptor<AsyncTask> captor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(mapper).insert(captor.capture());
        AsyncTask inserted = captor.getValue();
        assertThat(result.created()).isTrue();
        assertThat(result.task()).isSameAs(inserted);
        assertThat(inserted.getTaskId()).startsWith("task_audience_compute_");
        assertThat(inserted.getStatus()).isEqualTo(AsyncTaskStatus.QUEUED.name());
        assertThat(inserted.getProgress()).isEqualTo(0);
        assertThat(inserted.getCreatedBy()).isEqualTo("operator");
    }

    @Test
    void markSucceeded_setsFinishedFields() {
        AsyncTask task = new AsyncTask();
        task.setTaskId("task_1");
        when(mapper.selectOne(any())).thenReturn(task);
        AsyncTaskService service = new AsyncTaskService(mapper);

        service.markSucceeded("task_1", "{\"estimatedSize\":12}");

        assertThat(task.getStatus()).isEqualTo(AsyncTaskStatus.SUCCEEDED.name());
        assertThat(task.getProgress()).isEqualTo(100);
        assertThat(task.getResultSummary()).isEqualTo("{\"estimatedSize\":12}");
        assertThat(task.getFinishedAt()).isNotNull();
        verify(mapper).updateById(task);
    }

    @Test
    void markFailed_setsErrorFields() {
        AsyncTask task = new AsyncTask();
        task.setTaskId("task_2");
        when(mapper.selectOne(any())).thenReturn(task);
        AsyncTaskService service = new AsyncTaskService(mapper);

        service.markFailed("task_2", "JDBC timeout");

        assertThat(task.getStatus()).isEqualTo(AsyncTaskStatus.FAILED.name());
        assertThat(task.getProgress()).isEqualTo(100);
        assertThat(task.getErrorMsg()).isEqualTo("JDBC timeout");
        assertThat(task.getFinishedAt()).isNotNull();
        verify(mapper).updateById(task);
    }
}
