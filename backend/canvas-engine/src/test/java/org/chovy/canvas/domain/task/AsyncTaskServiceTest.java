package org.chovy.canvas.domain.task;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncTaskServiceTest {

    @Mock
    private AsyncTaskMapper mapper;
    @Mock
    private AsyncTaskSubscriptionMapper subscriptionMapper;

    @Test
    void createOrReuseRunningTask_returnsExistingRunningTask() {
        AsyncTask existing = new AsyncTask();
        existing.setTaskId("task_existing");
        existing.setStatus(AsyncTaskStatus.RUNNING.name());
        when(mapper.selectOne(any())).thenReturn(existing);

        AsyncTaskService service = service();

        AsyncTaskCreateResult result = service.createOrReuseRunning(
                "AUDIENCE_COMPUTE", "AUDIENCE", "7", "计算人群：VIP", "operator");

        assertThat(result.created()).isFalse();
        assertThat(result.task().getTaskId()).isEqualTo("task_existing");
    }

    @Test
    void createOrReuseRunningTask_createsQueuedTaskWhenNoneRunning() {
        when(mapper.selectOne(any())).thenReturn(null);
        AsyncTaskService service = service();

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
        assertSubscriptionInserted(inserted.getTaskId(), "operator");
    }

    @Test
    void createOrReuseRunningTask_returnsExistingWhenConcurrentInsertWins() {
        AsyncTask existing = new AsyncTask();
        existing.setTaskId("task_existing");
        existing.setStatus(AsyncTaskStatus.RUNNING.name());
        when(mapper.selectOne(any())).thenReturn(null, existing);
        when(mapper.insert(any(AsyncTask.class))).thenThrow(new DuplicateKeyException("duplicate active task"));
        AsyncTaskService service = service();

        AsyncTaskCreateResult result = service.createOrReuseRunning(
                "AUDIENCE_COMPUTE", "AUDIENCE", "7", "计算人群：VIP", "operator");

        assertThat(result.created()).isFalse();
        assertThat(result.task()).isSameAs(existing);
        assertSubscriptionInserted("task_existing", "operator");
    }

    @Test
    void createOrReuseRunningTask_subscribesRequesterToExistingTaskOwnedByAnotherUser() {
        AsyncTask existing = new AsyncTask();
        existing.setTaskId("task_existing");
        existing.setStatus(AsyncTaskStatus.RUNNING.name());
        existing.setCreatedBy("alice");
        when(mapper.selectOne(any())).thenReturn(existing);
        AsyncTaskService service = service();

        AsyncTaskCreateResult result = service.createOrReuseRunning(
                "AUDIENCE_COMPUTE", "AUDIENCE", "7", "计算人群：VIP", "bob");

        assertThat(result.created()).isFalse();
        assertThat(result.task()).isSameAs(existing);
        assertSubscriptionInserted("task_existing", "bob");
    }

    @Test
    void list_includesSubscribedTasksForNonAdminUsers() {
        AsyncTaskSubscription subscription = new AsyncTaskSubscription();
        subscription.setTaskId("task_alice");
        subscription.setUserId("bob");
        AsyncTask task = new AsyncTask();
        task.setTaskId("task_alice");
        Page<AsyncTask> page = new Page<>();
        page.setRecords(List.of(task));
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription));
        when(mapper.selectPage(any(Page.class), any())).thenReturn(page);
        AsyncTaskService service = service();

        List<AsyncTask> result = service.list(
                "AUDIENCE_COMPUTE", "AUDIENCE", List.of("7"), List.of("RUNNING"), "bob", false, 1, 100);

        assertThat(result).containsExactly(task);
        verify(subscriptionMapper).selectList(any());
        verify(mapper).selectPage(any(Page.class), any());
    }

    @Test
    void markSucceeded_setsFinishedFields() {
        AsyncTask task = new AsyncTask();
        task.setTaskId("task_1");
        when(mapper.selectOne(any())).thenReturn(task);
        AsyncTaskService service = service();

        service.markSucceeded("task_1", "{\"estimatedSize\":12}");

        assertThat(task.getStatus()).isEqualTo(AsyncTaskStatus.SUCCEEDED.name());
        assertThat(task.getProgress()).isEqualTo(100);
        assertThat(task.getResultSummary()).isEqualTo("{\"estimatedSize\":12}");
        assertThat(task.getFinishedAt()).isNotNull();
        verify(mapper).updateById(task);
    }

    @Test
    void markSucceeded_trimsResultSummaryToColumnLimit() {
        AsyncTask task = new AsyncTask();
        task.setTaskId("task_1");
        when(mapper.selectOne(any())).thenReturn(task);
        AsyncTaskService service = service();

        service.markSucceeded("task_1", "x".repeat(1001));

        assertThat(task.getResultSummary()).hasSize(1000);
        verify(mapper).updateById(task);
    }

    @Test
    void markFailed_setsErrorFields() {
        AsyncTask task = new AsyncTask();
        task.setTaskId("task_2");
        when(mapper.selectOne(any())).thenReturn(task);
        AsyncTaskService service = service();

        service.markFailed("task_2", "JDBC timeout");

        assertThat(task.getStatus()).isEqualTo(AsyncTaskStatus.FAILED.name());
        assertThat(task.getProgress()).isEqualTo(100);
        assertThat(task.getErrorMsg()).isEqualTo("JDBC timeout");
        assertThat(task.getFinishedAt()).isNotNull();
        verify(mapper).updateById(task);
    }

    private AsyncTaskService service() {
        return new AsyncTaskService(mapper, subscriptionMapper);
    }

    private void assertSubscriptionInserted(String taskId, String userId) {
        ArgumentCaptor<AsyncTaskSubscription> captor = ArgumentCaptor.forClass(AsyncTaskSubscription.class);
        verify(subscriptionMapper).insert(captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo(taskId);
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }
}
