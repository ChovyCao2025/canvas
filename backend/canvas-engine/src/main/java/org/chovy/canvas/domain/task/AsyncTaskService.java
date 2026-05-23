package org.chovy.canvas.domain.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private static final int TEXT_LIMIT = 1000;

    private final AsyncTaskMapper mapper;
    private final AsyncTaskSubscriptionMapper subscriptionMapper;

    public AsyncTaskCreateResult createOrReuseRunning(
            String taskType, String bizType, String bizId, String title, String createdBy) {
        AsyncTask existing = findActive(taskType, bizType, bizId);
        if (existing != null) {
            subscribe(existing.getTaskId(), createdBy);
            return new AsyncTaskCreateResult(existing, false);
        }

        AsyncTask task = new AsyncTask();
        task.setTaskId(newTaskId(taskType));
        task.setTaskType(taskType);
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setTitle(title);
        task.setStatus(AsyncTaskStatus.QUEUED.name());
        task.setProgress(0);
        task.setCreatedBy(createdBy);
        try {
            mapper.insert(task);
        } catch (DuplicateKeyException e) {
            AsyncTask concurrent = findActive(taskType, bizType, bizId);
            if (concurrent != null) {
                subscribe(concurrent.getTaskId(), createdBy);
                return new AsyncTaskCreateResult(concurrent, false);
            }
            throw e;
        }
        subscribe(task.getTaskId(), createdBy);
        return new AsyncTaskCreateResult(task, true);
    }

    public void markRunning(String taskId) {
        AsyncTask task = requireByTaskId(taskId);
        task.setStatus(AsyncTaskStatus.RUNNING.name());
        task.setProgress(5);
        task.setStartedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    public void markSucceeded(String taskId, String resultSummary) {
        AsyncTask task = requireByTaskId(taskId);
        task.setStatus(AsyncTaskStatus.SUCCEEDED.name());
        task.setProgress(100);
        task.setResultSummary(trimToLimit(resultSummary));
        task.setErrorMsg(null);
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    public void markFailed(String taskId, String errorMsg) {
        AsyncTask task = requireByTaskId(taskId);
        task.setStatus(AsyncTaskStatus.FAILED.name());
        task.setProgress(100);
        task.setErrorMsg(trimToLimit(errorMsg));
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    public AsyncTask getByTaskId(String taskId) {
        return mapper.selectOne(new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getTaskId, taskId)
                .last("LIMIT 1"));
    }

    public List<AsyncTask> list(String taskType, String bizType, List<String> bizIds, List<String> statuses, String createdBy, boolean admin) {
        return list(taskType, bizType, bizIds, statuses, createdBy, admin, 1, 100);
    }

    public List<AsyncTask> list(
            String taskType,
            String bizType,
            List<String> bizIds,
            List<String> statuses,
            String createdBy,
            boolean admin,
            int page,
            int size
    ) {
        List<String> subscribedTaskIds = admin ? List.of() : subscribedTaskIds(createdBy);
        LambdaQueryWrapper<AsyncTask> query = new LambdaQueryWrapper<AsyncTask>()
                .orderByDesc(AsyncTask::getCreatedAt);
        if (taskType != null && !taskType.isBlank()) {
            query.eq(AsyncTask::getTaskType, taskType);
        }
        if (bizType != null && !bizType.isBlank()) {
            query.eq(AsyncTask::getBizType, bizType);
        }
        if (bizIds != null && !bizIds.isEmpty()) {
            query.in(AsyncTask::getBizId, bizIds);
        }
        if (statuses != null && !statuses.isEmpty()) {
            query.in(AsyncTask::getStatus, statuses);
        }
        if (!admin) {
            if (subscribedTaskIds.isEmpty()) {
                query.eq(AsyncTask::getCreatedBy, createdBy);
            } else {
                query.and(scope -> scope.eq(AsyncTask::getCreatedBy, createdBy)
                        .or()
                        .in(AsyncTask::getTaskId, subscribedTaskIds));
            }
        }
        return mapper.selectPage(new Page<>(page, size), query).getRecords();
    }

    public List<String> subscribers(String taskId) {
        if (!hasText(taskId)) {
            return List.of();
        }
        return subscriptionMapper.selectList(new LambdaQueryWrapper<AsyncTaskSubscription>()
                        .eq(AsyncTaskSubscription::getTaskId, taskId)
                        .orderByAsc(AsyncTaskSubscription::getCreatedAt))
                .stream()
                .map(AsyncTaskSubscription::getUserId)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private AsyncTask findActive(String taskType, String bizType, String bizId) {
        return mapper.selectOne(new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getTaskType, taskType)
                .eq(AsyncTask::getBizType, bizType)
                .eq(AsyncTask::getBizId, bizId)
                .in(AsyncTask::getStatus, AsyncTaskStatus.QUEUED.name(), AsyncTaskStatus.RUNNING.name())
                .last("LIMIT 1"));
    }

    private AsyncTask requireByTaskId(String taskId) {
        AsyncTask task = getByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Async task not found: " + taskId);
        }
        return task;
    }

    private String newTaskId(String taskType) {
        return "task_" + taskType.toLowerCase(Locale.ROOT) + "_"
                + UUID.randomUUID().toString().replace("-", "");
    }

    private String trimToLimit(String value) {
        if (value == null || value.length() <= TEXT_LIMIT) {
            return value;
        }
        return value.substring(0, TEXT_LIMIT);
    }

    private void subscribe(String taskId, String userId) {
        if (!hasText(taskId) || !hasText(userId)) {
            return;
        }
        AsyncTaskSubscription subscription = new AsyncTaskSubscription();
        subscription.setTaskId(taskId);
        subscription.setUserId(userId);
        try {
            subscriptionMapper.insert(subscription);
        } catch (DuplicateKeyException ignored) {
            // Multiple clicks or racing requests can subscribe the same user twice.
        }
    }

    private List<String> subscribedTaskIds(String userId) {
        if (!hasText(userId)) {
            return List.of();
        }
        return subscriptionMapper.selectList(new LambdaQueryWrapper<AsyncTaskSubscription>()
                        .eq(AsyncTaskSubscription::getUserId, userId))
                .stream()
                .map(AsyncTaskSubscription::getTaskId)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
