package org.chovy.canvas.domain.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.chovy.canvas.dal.dataobject.AsyncTaskDO;
import org.chovy.canvas.dal.mapper.AsyncTaskMapper;
import org.chovy.canvas.dal.dataobject.AsyncTaskSubscriptionDO;
import org.chovy.canvas.dal.mapper.AsyncTaskSubscriptionMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private static final int TEXT_LIMIT = 1000;

    private final AsyncTaskMapper mapper;
    private final AsyncTaskSubscriptionMapper subscriptionMapper;

    public AsyncTaskCreateResult createOrReuseRunning(
            String taskType, String bizType, String bizId, String title, String createdBy) {
        AsyncTaskDO existing = findActive(taskType, bizType, bizId);
        if (existing != null) {
            subscribe(existing.getTaskId(), createdBy);
            return new AsyncTaskCreateResult(refresh(existing), false);
        }

        AsyncTaskDO task = new AsyncTaskDO();
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
            AsyncTaskDO concurrent = findActive(taskType, bizType, bizId);
            if (concurrent != null) {
                subscribe(concurrent.getTaskId(), createdBy);
                return new AsyncTaskCreateResult(refresh(concurrent), false);
            }
            throw e;
        }
        subscribe(task.getTaskId(), createdBy);
        return new AsyncTaskCreateResult(task, true);
    }

    public void markRunning(String taskId) {
        AsyncTaskDO task = requireByTaskId(taskId);
        task.setStatus(AsyncTaskStatus.RUNNING.name());
        task.setProgress(5);
        task.setStartedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    public void markSucceeded(String taskId, String resultSummary) {
        AsyncTaskDO task = requireByTaskId(taskId);
        task.setStatus(AsyncTaskStatus.SUCCEEDED.name());
        task.setProgress(100);
        task.setResultSummary(trimToLimit(resultSummary));
        task.setErrorMsg(null);
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    public void markFailed(String taskId, String errorMsg) {
        AsyncTaskDO task = requireByTaskId(taskId);
        task.setStatus(AsyncTaskStatus.FAILED.name());
        task.setProgress(100);
        task.setErrorMsg(trimToLimit(errorMsg));
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    public AsyncTaskDO getByTaskId(String taskId) {
        return mapper.selectOne(new LambdaQueryWrapper<AsyncTaskDO>()
                .eq(AsyncTaskDO::getTaskId, taskId)
                .last("LIMIT 1"));
    }

    public List<AsyncTaskDO> list(String taskType, String bizType, List<String> bizIds, List<String> statuses, String createdBy, boolean admin) {
        return list(taskType, bizType, bizIds, statuses, createdBy, admin, 1, 100);
    }

    public List<AsyncTaskDO> list(
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
        LambdaQueryWrapper<AsyncTaskDO> query = new LambdaQueryWrapper<AsyncTaskDO>()
                .orderByDesc(AsyncTaskDO::getCreatedAt);
        if (taskType != null && !taskType.isBlank()) {
            query.eq(AsyncTaskDO::getTaskType, taskType);
        }
        if (bizType != null && !bizType.isBlank()) {
            query.eq(AsyncTaskDO::getBizType, bizType);
        }
        if (bizIds != null && !bizIds.isEmpty()) {
            query.in(AsyncTaskDO::getBizId, bizIds);
        }
        if (statuses != null && !statuses.isEmpty()) {
            query.in(AsyncTaskDO::getStatus, statuses);
        }
        if (!admin) {
            if (subscribedTaskIds.isEmpty()) {
                query.eq(AsyncTaskDO::getCreatedBy, createdBy);
            } else {
                query.and(scope -> scope.eq(AsyncTaskDO::getCreatedBy, createdBy)
                        .or()
                        .in(AsyncTaskDO::getTaskId, subscribedTaskIds));
            }
        }
        return mapper.selectPage(new Page<>(page, size), query).getRecords();
    }

    public List<String> subscribers(String taskId) {
        if (!hasText(taskId)) {
            return List.of();
        }
        return subscriptionMapper.selectList(new LambdaQueryWrapper<AsyncTaskSubscriptionDO>()
                        .eq(AsyncTaskSubscriptionDO::getTaskId, taskId)
                        .orderByAsc(AsyncTaskSubscriptionDO::getCreatedAt))
                .stream()
                .map(AsyncTaskSubscriptionDO::getUserId)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private AsyncTaskDO findActive(String taskType, String bizType, String bizId) {
        return mapper.selectOne(new LambdaQueryWrapper<AsyncTaskDO>()
                .eq(AsyncTaskDO::getTaskType, taskType)
                .eq(AsyncTaskDO::getBizType, bizType)
                .eq(AsyncTaskDO::getBizId, bizId)
                .in(AsyncTaskDO::getStatus, AsyncTaskStatus.QUEUED.name(), AsyncTaskStatus.RUNNING.name())
                .last("LIMIT 1"));
    }

    private AsyncTaskDO requireByTaskId(String taskId) {
        AsyncTaskDO task = getByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Async task not found: " + taskId);
        }
        return task;
    }

    private AsyncTaskDO refresh(AsyncTaskDO task) {
        AsyncTaskDO latest = getByTaskId(task.getTaskId());
        return latest == null ? task : latest;
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
        AsyncTaskSubscriptionDO subscription = new AsyncTaskSubscriptionDO();
        subscription.setTaskId(taskId);
        subscription.setUserId(userId);
        try {
            subscriptionMapper.insert(subscription);
        } catch (DuplicateKeyException ignored) {
        } catch (Exception e) {
            log.error("[ASYNC_TASK] failed to subscribe user={} to task={}: {}", userId, taskId, e.getMessage(), e);
        }
    }

    private List<String> subscribedTaskIds(String userId) {
        if (!hasText(userId)) {
            return List.of();
        }
        return subscriptionMapper.selectList(new LambdaQueryWrapper<AsyncTaskSubscriptionDO>()
                        .eq(AsyncTaskSubscriptionDO::getUserId, userId))
                .stream()
                .map(AsyncTaskSubscriptionDO::getTaskId)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
