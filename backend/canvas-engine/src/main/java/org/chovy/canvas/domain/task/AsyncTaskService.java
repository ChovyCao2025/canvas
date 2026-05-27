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

/**
 * 异步任务 异步任务领域组件。
 *
 * <p>负责长耗时后台任务的创建、进度更新、订阅通知和结果状态维护。
 * <p>调用方通过任务状态感知执行进展，避免同步接口长时间阻塞。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    /** 任务结果摘要和错误信息最大长度，避免数据库字段溢出。 */
    private static final int TEXT_LIMIT = 1000;

    /** 异步任务 Mapper，用于任务创建、状态更新和列表查询。 */
    private final AsyncTaskMapper mapper;
    /** 异步任务订阅 Mapper。 */
    private final AsyncTaskSubscriptionMapper subscriptionMapper;

    /** 创建异步任务，若同业务已有运行中任务则复用。 */
    public AsyncTaskCreateResult createOrReuseRunning(
            String taskType, String bizType, String bizId, String title, String createdBy) {
        AsyncTaskDO existing = findActive(taskType, bizType, bizId);
        if (existing != null) {
            // 同一业务对象已有排队/运行任务时复用，避免后台重复启动长耗时任务。
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
            // 并发创建撞唯一键时重新读取运行中任务，保证调用方拿到同一个任务 ID。
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

    /** 将异步任务标记为运行中。 */
    public void markRunning(String taskId) {
        AsyncTaskDO task = requireByTaskId(taskId);
        task.setStatus(AsyncTaskStatus.RUNNING.name());
        task.setProgress(5);
        task.setStartedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    /** 将异步任务标记为成功并写入结果摘要。 */
    public void markSucceeded(String taskId, String resultSummary) {
        AsyncTaskDO task = requireByTaskId(taskId);
        task.setStatus(AsyncTaskStatus.SUCCEEDED.name());
        task.setProgress(100);
        task.setResultSummary(trimToLimit(resultSummary));
        task.setErrorMsg(null);
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    /** 将异步任务标记为失败并写入错误信息。 */
    public void markFailed(String taskId, String errorMsg) {
        AsyncTaskDO task = requireByTaskId(taskId);
        task.setStatus(AsyncTaskStatus.FAILED.name());
        task.setProgress(100);
        task.setErrorMsg(trimToLimit(errorMsg));
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    /** 按任务 ID 查询异步任务。 */
    public AsyncTaskDO getByTaskId(String taskId) {
        return mapper.selectOne(new LambdaQueryWrapper<AsyncTaskDO>()
                .eq(AsyncTaskDO::getTaskId, taskId)
                .last("LIMIT 1"));
    }

    /** 按条件查询列表数据。 */
    public List<AsyncTaskDO> list(String taskType, String bizType, List<String> bizIds, List<String> statuses, String createdBy, boolean admin) {
        return list(taskType, bizType, bizIds, statuses, createdBy, admin, 1, 100);
    }

    /** 按条件查询列表数据。 */
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
                // 普通用户可见自己创建或已订阅的任务，避免后台任务进度只对创建者可见。
                query.and(scope -> scope.eq(AsyncTaskDO::getCreatedBy, createdBy)
                        .or()
                        .in(AsyncTaskDO::getTaskId, subscribedTaskIds));
            }
        }
        return mapper.selectPage(new Page<>(page, size), query).getRecords();
    }

    /** 查询订阅指定任务通知的用户列表。 */
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

    /** 查询同一业务对象下仍处于排队或运行中的任务。 */
    private AsyncTaskDO findActive(String taskType, String bizType, String bizId) {
        return mapper.selectOne(new LambdaQueryWrapper<AsyncTaskDO>()
                .eq(AsyncTaskDO::getTaskType, taskType)
                .eq(AsyncTaskDO::getBizType, bizType)
                .eq(AsyncTaskDO::getBizId, bizId)
                .in(AsyncTaskDO::getStatus, AsyncTaskStatus.QUEUED.name(), AsyncTaskStatus.RUNNING.name())
                .last("LIMIT 1"));
    }

    /** 按任务 ID 查询任务，不存在时抛出业务异常。 */
    private AsyncTaskDO requireByTaskId(String taskId) {
        AsyncTaskDO task = getByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Async task not found: " + taskId);
        }
        return task;
    }

    /** 重新读取任务最新状态，读取失败时返回传入对象兜底。 */
    private AsyncTaskDO refresh(AsyncTaskDO task) {
        AsyncTaskDO latest = getByTaskId(task.getTaskId());
        return latest == null ? task : latest;
    }

    /** 生成带任务类型前缀的异步任务业务 ID。 */
    private String newTaskId(String taskType) {
        return "task_" + taskType.toLowerCase(Locale.ROOT) + "_"
                + UUID.randomUUID().toString().replace("-", "");
    }

    /** 将任务文本字段截断到数据库允许长度。 */
    private String trimToLimit(String value) {
        if (value == null || value.length() <= TEXT_LIMIT) {
            return value;
        }
        return value.substring(0, TEXT_LIMIT);
    }

    /** 为用户订阅任务进度通知，重复订阅按幂等处理。 */
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
            // 重复订阅视为成功，避免同一用户刷新或并发请求产生噪音。
        } catch (Exception e) {
            log.error("[ASYNC_TASK] failed to subscribe user={} to task={}: {}", userId, taskId, e.getMessage(), e);
        }
    }

    /** 查询用户订阅过的任务 ID，用于非管理员列表可见范围过滤。 */
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

    /** 判断字符串是否包含非空白字符。 */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
