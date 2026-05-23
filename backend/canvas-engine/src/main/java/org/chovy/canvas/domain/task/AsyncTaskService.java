package org.chovy.canvas.domain.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private static final int ERROR_MSG_LIMIT = 1000;

    private final AsyncTaskMapper mapper;

    public AsyncTaskCreateResult createOrReuseRunning(
            String taskType, String bizType, String bizId, String title, String createdBy) {
        AsyncTask existing = mapper.selectOne(new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getTaskType, taskType)
                .eq(AsyncTask::getBizType, bizType)
                .eq(AsyncTask::getBizId, bizId)
                .in(AsyncTask::getStatus, AsyncTaskStatus.QUEUED.name(), AsyncTaskStatus.RUNNING.name())
                .last("LIMIT 1"));
        if (existing != null) {
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
        mapper.insert(task);
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
        task.setResultSummary(resultSummary);
        task.setErrorMsg(null);
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    public void markFailed(String taskId, String errorMsg) {
        AsyncTask task = requireByTaskId(taskId);
        task.setStatus(AsyncTaskStatus.FAILED.name());
        task.setProgress(100);
        task.setErrorMsg(trimError(errorMsg));
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    public AsyncTask getByTaskId(String taskId) {
        return mapper.selectOne(new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getTaskId, taskId)
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

    private String trimError(String errorMsg) {
        if (errorMsg == null || errorMsg.length() <= ERROR_MSG_LIMIT) {
            return errorMsg;
        }
        return errorMsg.substring(0, ERROR_MSG_LIMIT);
    }
}
