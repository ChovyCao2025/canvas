# Async Task Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reusable async task and notification foundation, then use it to make audience compute results refresh automatically and notify users when work finishes.

**Architecture:** Store durable task state in `async_task` and user-facing messages in `notification`, with Redis kept for existing compute locks and bitmap storage. Audience compute becomes an enqueued task that returns `taskId`; the audience list polls only while work is active, while a global notification bell shows completion and failure messages.

**Tech Stack:** Spring Boot WebFlux, MyBatis-Plus, Flyway, Reactor, Java virtual threads, React 18, React Router 6, Ant Design 5, Axios, Vitest, Maven

---

## Scope Check

This is one vertical slice even though it touches backend and frontend: the durable async task model, notification model, audience compute task integration, audience-page polling, and global notification drawer all ship together so the user no longer has to manually refresh. The full historical task center page is intentionally not implemented in this plan.

## File Structure

### Backend

- Create: `backend/canvas-engine/src/main/resources/db/migration/V45__async_task_notification.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTask.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskStatus.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskCreateResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/Notification.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/task/AsyncTaskDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/task/ComputeTaskResp.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/notification/NotificationDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AsyncTaskController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/NotificationController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AudienceController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/task/AsyncTaskServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunnerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTaskTest.java`

### Frontend

- Create: `frontend/src/services/taskApi.ts`
- Create: `frontend/src/services/notificationApi.ts`
- Create: `frontend/src/pages/audience-list/audienceTaskPresentation.ts`
- Create: `frontend/src/pages/audience-list/audienceTaskPresentation.test.ts`
- Create: `frontend/src/components/notifications/NotificationBell.tsx`
- Create: `frontend/src/components/notifications/notificationPresentation.ts`
- Create: `frontend/src/components/notifications/notificationPresentation.test.ts`
- Modify: `frontend/src/services/audienceApi.ts`
- Modify: `frontend/src/pages/audience-list/index.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

## Task 1: Add Durable Async Task Model

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V45__async_task_notification.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTask.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskStatus.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskCreateResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/task/AsyncTaskServiceTest.java`

- [ ] **Step 1: Write the failing async task service test**

```java
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
```

- [ ] **Step 2: Run the targeted test to verify it fails**

```bash
mvn test -pl canvas-engine -Dtest=AsyncTaskServiceTest -q
```

Expected: FAIL because `AsyncTaskService`, `AsyncTask`, `AsyncTaskMapper`, `AsyncTaskStatus`, and `AsyncTaskCreateResult` do not exist.

- [ ] **Step 3: Add migration tables for async task and notification**

Create `backend/canvas-engine/src/main/resources/db/migration/V45__async_task_notification.sql`:

```sql
CREATE TABLE async_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    task_type VARCHAR(50) NOT NULL,
    biz_type VARCHAR(50) NOT NULL,
    biz_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    result_summary VARCHAR(1000),
    error_msg VARCHAR(1000),
    created_by VARCHAR(100),
    started_at DATETIME,
    finished_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_async_task_biz (biz_type, biz_id),
    INDEX idx_async_task_status (status),
    INDEX idx_async_task_creator (created_by, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000),
    target_url VARCHAR(500),
    task_id VARCHAR(64),
    read_at DATETIME,
    created_at DATETIME,
    INDEX idx_notification_user_read (user_id, read_at, created_at),
    INDEX idx_notification_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Add async task domain classes**

Create `AsyncTaskStatus.java`:

```java
package org.chovy.canvas.domain.task;

public enum AsyncTaskStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED
}
```

Create `AsyncTask.java`:

```java
package org.chovy.canvas.domain.task;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("async_task")
public class AsyncTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String taskType;
    private String bizType;
    private String bizId;
    private String title;
    private String status;
    private Integer progress;
    private String resultSummary;
    private String errorMsg;
    private String createdBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

Create `AsyncTaskMapper.java`:

```java
package org.chovy.canvas.domain.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AsyncTaskMapper extends BaseMapper<AsyncTask> {
}
```

Create `AsyncTaskCreateResult.java`:

```java
package org.chovy.canvas.domain.task;

public record AsyncTaskCreateResult(AsyncTask task, boolean created) {
}
```

- [ ] **Step 5: Implement async task service**

Create `AsyncTaskService.java`:

```java
package org.chovy.canvas.domain.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private final AsyncTaskMapper mapper;

    public AsyncTaskCreateResult createOrReuseRunning(
            String taskType,
            String bizType,
            String bizId,
            String title,
            String createdBy
    ) {
        AsyncTask existing = mapper.selectOne(new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getTaskType, taskType)
                .eq(AsyncTask::getBizType, bizType)
                .eq(AsyncTask::getBizId, bizId)
                .in(AsyncTask::getStatus, List.of(AsyncTaskStatus.QUEUED.name(), AsyncTaskStatus.RUNNING.name()))
                .last("LIMIT 1"));
        if (existing != null) {
            return new AsyncTaskCreateResult(existing, false);
        }

        AsyncTask task = new AsyncTask();
        task.setTaskId(generateTaskId(taskType));
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
        task.setErrorMsg(trim(errorMsg));
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    public AsyncTask getByTaskId(String taskId) {
        return mapper.selectOne(new LambdaQueryWrapper<AsyncTask>().eq(AsyncTask::getTaskId, taskId));
    }

    private AsyncTask requireByTaskId(String taskId) {
        AsyncTask task = getByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Async task not found: " + taskId);
        }
        return task;
    }

    private String generateTaskId(String taskType) {
        return "task_" + taskType.toLowerCase() + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
```

- [ ] **Step 6: Re-run the async task service test**

```bash
mvn test -pl canvas-engine -Dtest=AsyncTaskServiceTest -q
```

Expected: PASS.

- [ ] **Step 7: Commit the async task model slice**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V45__async_task_notification.sql backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task backend/canvas-engine/src/test/java/org/chovy/canvas/domain/task/AsyncTaskServiceTest.java
git commit -m "feat: add async task model"
```

## Task 2: Add Notification Model And Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/Notification.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationServiceTest.java`

- [ ] **Step 1: Write the failing notification service test**

```java
package org.chovy.canvas.domain.notification;

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
class NotificationServiceTest {

    @Mock
    private NotificationMapper mapper;

    @Test
    void createForTask_insertsUnreadNotification() {
        NotificationService service = new NotificationService(mapper);

        Notification notification = service.createForTask(
                "operator",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_1",
                "task_1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(captor.capture());
        Notification inserted = captor.getValue();
        assertThat(notification).isSameAs(inserted);
        assertThat(inserted.getNotificationId()).startsWith("ntf_");
        assertThat(inserted.getUserId()).isEqualTo("operator");
        assertThat(inserted.getReadAt()).isNull();
    }

    @Test
    void unreadCount_countsOnlyCurrentUserUnreadNotifications() {
        when(mapper.selectCount(any())).thenReturn(3L);
        NotificationService service = new NotificationService(mapper);

        assertThat(service.unreadCount("operator")).isEqualTo(3L);
    }

    @Test
    void markRead_updatesOnlyCurrentUserNotification() {
        Notification notification = new Notification();
        notification.setNotificationId("ntf_1");
        notification.setUserId("operator");
        when(mapper.selectOne(any())).thenReturn(notification);
        NotificationService service = new NotificationService(mapper);

        service.markRead("operator", "ntf_1");

        assertThat(notification.getReadAt()).isNotNull();
        verify(mapper).updateById(notification);
    }
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

```bash
mvn test -pl canvas-engine -Dtest=NotificationServiceTest -q
```

Expected: FAIL because notification domain classes do not exist.

- [ ] **Step 3: Add notification domain classes**

Create `Notification.java`:

```java
package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String notificationId;
    private String userId;
    private String type;
    private String title;
    private String content;
    private String targetUrl;
    private String taskId;
    private LocalDateTime readAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

Create `NotificationMapper.java`:

```java
package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
```

- [ ] **Step 4: Implement notification service**

Create `NotificationService.java`:

```java
package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper mapper;

    public Notification createForTask(
            String userId,
            String type,
            String title,
            String content,
            String targetUrl,
            String taskId
    ) {
        Notification notification = new Notification();
        notification.setNotificationId("ntf_" + UUID.randomUUID().toString().replace("-", ""));
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetUrl(targetUrl);
        notification.setTaskId(taskId);
        mapper.insert(notification);
        return notification;
    }

    public List<Notification> list(String userId, boolean unreadOnly, int page, int size) {
        LambdaQueryWrapper<Notification> query = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        if (unreadOnly) {
            query.isNull(Notification::getReadAt);
        }
        return mapper.selectPage(new Page<>(page, size), query).getRecords();
    }

    public long unreadCount(String userId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .isNull(Notification::getReadAt));
        return count == null ? 0 : count;
    }

    public void markRead(String userId, String notificationId) {
        Notification notification = mapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getNotificationId, notificationId));
        if (notification == null || notification.getReadAt() != null) {
            return;
        }
        notification.setReadAt(LocalDateTime.now());
        mapper.updateById(notification);
    }

    public void markAllRead(String userId) {
        List<Notification> unread = mapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .isNull(Notification::getReadAt));
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(item -> {
            item.setReadAt(now);
            mapper.updateById(item);
        });
    }
}
```

- [ ] **Step 5: Re-run the notification service test**

```bash
mvn test -pl canvas-engine -Dtest=NotificationServiceTest -q
```

Expected: PASS.

- [ ] **Step 6: Commit the notification service slice**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationServiceTest.java
git commit -m "feat: add notification service"
```

## Task 3: Convert Audience Compute Into A Task Runner

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunnerTest.java`

- [ ] **Step 1: Write the failing task runner test**

```java
package org.chovy.canvas.engine.audience;

import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudienceComputeTaskRunnerTest {

    @Mock
    private AudienceBatchComputeService computeService;
    @Mock
    private AsyncTaskService asyncTaskService;
    @Mock
    private NotificationService notificationService;

    @Test
    void runNow_marksTaskSucceededAndCreatesSuccessNotification() {
        when(computeService.compute(7L)).thenReturn(AudienceComputeResult.ready(7L, "VIP 人群", 12L, 2));
        AudienceComputeTaskRunner runner = new AudienceComputeTaskRunner(computeService, asyncTaskService, notificationService);

        runner.runNow("task_1", 7L, "VIP 人群", "operator");

        verify(asyncTaskService).markRunning("task_1");
        verify(asyncTaskService).markSucceeded("task_1", "{\"audienceId\":7,\"estimatedSize\":12,\"bitmapSizeKb\":2}");
        verify(notificationService).createForTask(
                "operator",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_1",
                "task_1");
    }

    @Test
    void runNow_marksTaskFailedAndCreatesFailureNotification() {
        when(computeService.compute(7L)).thenReturn(AudienceComputeResult.failed(7L, "VIP 人群", "JDBC timeout"));
        AudienceComputeTaskRunner runner = new AudienceComputeTaskRunner(computeService, asyncTaskService, notificationService);

        runner.runNow("task_2", 7L, "VIP 人群", "operator");

        verify(asyncTaskService).markRunning("task_2");
        verify(asyncTaskService).markFailed("task_2", "JDBC timeout");
        verify(notificationService).createForTask(
                "operator",
                "TASK_FAILED",
                "人群计算失败",
                "VIP 人群 · JDBC timeout",
                "/audiences?highlight=7&taskId=task_2",
                "task_2");
    }
}
```

- [ ] **Step 2: Run the targeted runner test to verify it fails**

```bash
mvn test -pl canvas-engine -Dtest=AudienceComputeTaskRunnerTest -q
```

Expected: FAIL because `AudienceComputeResult` and `AudienceComputeTaskRunner` do not exist, and `AudienceBatchComputeService.compute` returns `void`.

- [ ] **Step 3: Add audience compute result record**

Create `AudienceComputeResult.java`:

```java
package org.chovy.canvas.engine.audience;

public record AudienceComputeResult(
        Long audienceId,
        String audienceName,
        String status,
        Long estimatedSize,
        Integer bitmapSizeKb,
        String errorMsg
) {
    public static AudienceComputeResult ready(Long audienceId, String audienceName, Long estimatedSize, Integer bitmapSizeKb) {
        return new AudienceComputeResult(audienceId, audienceName, "READY", estimatedSize, bitmapSizeKb, null);
    }

    public static AudienceComputeResult failed(Long audienceId, String audienceName, String errorMsg) {
        return new AudienceComputeResult(audienceId, audienceName, "FAILED", null, null, errorMsg);
    }

    public boolean success() {
        return "READY".equals(status);
    }
}
```

- [ ] **Step 4: Modify `AudienceBatchComputeService.compute` to return `AudienceComputeResult`**

Change the signature from:

```java
public void compute(Long audienceId)
```

to:

```java
public AudienceComputeResult compute(Long audienceId)
```

Move the `AudienceDefinition definition` variable outside the `try` so both success and failure paths can name the audience:

```java
AudienceDefinition definition = null;
try {
    definition = definitionMapper.selectById(audienceId);
    if (definition == null || definition.getEnabled() == null || definition.getEnabled() == 0) {
        throw new IllegalArgumentException("Audience not found or disabled: " + audienceId);
    }
    RoaringBitmap bitmap = switch (definition.getDataSourceType()) {
        case "JDBC" -> computeViaJdbc(definition);
        case "TAGGER_API" -> computeViaTaggerApi(definition);
        default -> throw new IllegalStateException("Unsupported data source: " + definition.getDataSourceType());
    };
}
```

Inside the success path, return the computed result:

```java
long estimatedSize = bitmap.getCardinality();
int bitmapSizeKb = bos.size() / 1024;
updateStat(audienceId, "READY", estimatedSize, bitmapSizeKb, null);
return AudienceComputeResult.ready(audienceId, definition.getName(), estimatedSize, bitmapSizeKb);
```

Inside the lock conflict branch, return a failed result:

```java
if (!locked) {
    log.warn("[AUDIENCE] compute skipped because lock exists audienceId={}", audienceId);
    AudienceDefinition definition = definitionMapper.selectById(audienceId);
    String name = definition == null ? "人群 " + audienceId : definition.getName();
    return AudienceComputeResult.failed(audienceId, name, "已有计算任务正在运行");
}
```

Inside the catch branch, return the failure result after updating `audience_stat`:

```java
String error = trimError(e.getMessage());
log.error("[AUDIENCE] compute failed audienceId={}: {}", audienceId, e.getMessage(), e);
updateStat(audienceId, "FAILED", null, null, error);
String name = definition == null ? "人群 " + audienceId : definition.getName();
return AudienceComputeResult.failed(audienceId, name, error);
```

Keep `finally { redis.delete(lockKey); }` only for cases where the lock was acquired. The simplest safe implementation is to return before `try` when `locked` is false, as shown above.

- [ ] **Step 5: Add the task runner**

Create `AudienceComputeTaskRunner.java`:

```java
package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AudienceComputeTaskRunner {

    private final AudienceBatchComputeService computeService;
    private final AsyncTaskService asyncTaskService;
    private final NotificationService notificationService;

    public void start(String taskId, Long audienceId, String audienceName, String operator) {
        Thread.ofVirtual().start(() -> runNow(taskId, audienceId, audienceName, operator));
    }

    public void runNow(String taskId, Long audienceId, String audienceName, String operator) {
        asyncTaskService.markRunning(taskId);
        AudienceComputeResult result = computeService.compute(audienceId);
        if (result.success()) {
            asyncTaskService.markSucceeded(taskId, successSummary(result));
            notificationService.createForTask(
                    operator,
                    "TASK_SUCCEEDED",
                    "人群计算完成",
                    result.audienceName() + " · " + result.estimatedSize() + " 人",
                    targetUrl(audienceId, taskId),
                    taskId);
            return;
        }
        String error = result.errorMsg() == null ? "计算失败" : result.errorMsg();
        asyncTaskService.markFailed(taskId, error);
        notificationService.createForTask(
                operator,
                "TASK_FAILED",
                "人群计算失败",
                audienceName + " · " + error,
                targetUrl(audienceId, taskId),
                taskId);
    }

    private String successSummary(AudienceComputeResult result) {
        return "{\"audienceId\":" + result.audienceId()
                + ",\"estimatedSize\":" + result.estimatedSize()
                + ",\"bitmapSizeKb\":" + result.bitmapSizeKb()
                + "}";
    }

    private String targetUrl(Long audienceId, String taskId) {
        return "/audiences?highlight=" + audienceId + "&taskId=" + taskId;
    }
}
```

- [ ] **Step 6: Re-run the runner test**

```bash
mvn test -pl canvas-engine -Dtest=AudienceComputeTaskRunnerTest -q
```

Expected: PASS.

- [ ] **Step 7: Run affected audience tests**

```bash
mvn test -pl canvas-engine -Dtest=AudienceComputeTaskRunnerTest,TaggerHandlerTest,SqlWhereGeneratorTest -q
```

Expected: PASS.

- [ ] **Step 8: Commit the task runner slice**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeResult.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunnerTest.java
git commit -m "feat: run audience compute as async task"
```

## Task 4: Expose Task And Notification Backend APIs

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/task/AsyncTaskDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/task/ComputeTaskResp.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/notification/NotificationDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AsyncTaskController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/NotificationController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AsyncTaskControllerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/NotificationControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AsyncTaskControllerTest.java`:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.domain.task.AsyncTask;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncTaskControllerTest {

    @Test
    void list_returnsTasksForCurrentUserWhenNoSecurityContextExists() {
        AsyncTaskService service = mock(AsyncTaskService.class);
        AsyncTask task = new AsyncTask();
        task.setTaskId("task_1");
        task.setTaskType("AUDIENCE_COMPUTE");
        task.setBizType("AUDIENCE");
        task.setBizId("7");
        task.setTitle("计算人群：VIP 人群");
        task.setStatus("RUNNING");
        task.setProgress(5);
        when(service.list("AUDIENCE_COMPUTE", "AUDIENCE", List.of("7"), List.of("RUNNING"), "system", false))
                .thenReturn(List.of(task));

        AsyncTaskController controller = new AsyncTaskController(service);

        var response = controller.list("AUDIENCE_COMPUTE", "AUDIENCE", "7", "RUNNING").block();

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().taskId()).isEqualTo("task_1");
    }
}
```

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/NotificationControllerTest.java`:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.domain.notification.NotificationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    @Test
    void unreadCount_returnsCurrentUserCountWhenNoSecurityContextExists() {
        NotificationService service = mock(NotificationService.class);
        when(service.unreadCount("system")).thenReturn(2L);

        NotificationController controller = new NotificationController(service);

        var response = controller.unreadCount().block();

        assertThat(response.getData().get("count")).isEqualTo(2L);
    }
}
```

- [ ] **Step 2: Run controller tests to verify they fail**

```bash
mvn test -pl canvas-engine -Dtest=AsyncTaskControllerTest,NotificationControllerTest -q
```

Expected: FAIL because `AsyncTaskController`, `NotificationController`, and DTOs do not exist.

- [ ] **Step 3: Extend `AsyncTaskService` with query methods**

Add these methods:

```java
public List<AsyncTask> list(String taskType, String bizType, List<String> bizIds, List<String> statuses, String createdBy, boolean admin) {
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
        query.eq(AsyncTask::getCreatedBy, createdBy);
    }
    return mapper.selectList(query);
}
```

- [ ] **Step 4: Add DTOs**

Create `AsyncTaskDTO.java`:

```java
package org.chovy.canvas.dto.task;

import org.chovy.canvas.domain.task.AsyncTask;

import java.time.LocalDateTime;

public record AsyncTaskDTO(
        String taskId,
        String taskType,
        String bizType,
        String bizId,
        String title,
        String status,
        Integer progress,
        String resultSummary,
        String errorMsg,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AsyncTaskDTO from(AsyncTask task) {
        return new AsyncTaskDTO(
                task.getTaskId(),
                task.getTaskType(),
                task.getBizType(),
                task.getBizId(),
                task.getTitle(),
                task.getStatus(),
                task.getProgress(),
                task.getResultSummary(),
                task.getErrorMsg(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
```

Create `ComputeTaskResp.java`:

```java
package org.chovy.canvas.dto.task;

public record ComputeTaskResp(String taskId, String status) {
}
```

Create `NotificationDTO.java`:

```java
package org.chovy.canvas.dto.notification;

import org.chovy.canvas.domain.notification.Notification;

import java.time.LocalDateTime;

public record NotificationDTO(
        String notificationId,
        String type,
        String title,
        String content,
        String targetUrl,
        String taskId,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationDTO from(Notification notification) {
        return new NotificationDTO(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetUrl(),
                notification.getTaskId(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
```

- [ ] **Step 5: Add async task controller**

Create `AsyncTaskController.java`:

```java
package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.dto.task.AsyncTaskDTO;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/canvas/async-tasks")
@RequiredArgsConstructor
public class AsyncTaskController {

    private final AsyncTaskService taskService;

    @GetMapping
    public Mono<R<List<AsyncTaskDTO>>> list(
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizIds,
            @RequestParam(required = false) String statuses
    ) {
        return currentUser().flatMap(user -> currentRole().flatMap(role ->
                Mono.fromCallable(() -> R.ok(taskService.list(
                                taskType,
                                bizType,
                                splitCsv(bizIds),
                                splitCsv(statuses),
                                user,
                                "ADMIN".equals(role)).stream().map(AsyncTaskDTO::from).toList()))
                        .subscribeOn(Schedulers.boundedElastic())));
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(io.jsonwebtoken.Claims.class)
                .map(c -> c.get("username", String.class))
                .defaultIfEmpty("system");
    }

    private Mono<String> currentRole() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(io.jsonwebtoken.Claims.class)
                .map(c -> c.get("role", String.class))
                .defaultIfEmpty("OPERATOR");
    }
}
```

- [ ] **Step 6: Add notification controller**

Create `NotificationController.java`:

```java
package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.dto.notification.NotificationDTO;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/canvas/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Mono<R<List<NotificationDTO>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return currentUser().flatMap(user ->
                Mono.fromCallable(() -> R.ok(notificationService.list(user, unreadOnly, page, size)
                                .stream().map(NotificationDTO::from).toList()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/unread-count")
    public Mono<R<Map<String, Long>>> unreadCount() {
        return currentUser().flatMap(user ->
                Mono.fromCallable(() -> R.ok(Map.of("count", notificationService.unreadCount(user))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/{notificationId}/read")
    public Mono<R<Void>> markRead(@PathVariable String notificationId) {
        return currentUser().flatMap(user ->
                Mono.fromRunnable(() -> notificationService.markRead(user, notificationId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    @PutMapping("/read-all")
    public Mono<R<Void>> markAllRead() {
        return currentUser().flatMap(user ->
                Mono.fromRunnable(() -> notificationService.markAllRead(user))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(io.jsonwebtoken.Claims.class)
                .map(c -> c.get("username", String.class))
                .defaultIfEmpty("system");
    }
}
```

- [ ] **Step 7: Run backend compile and targeted API tests**

```bash
mvn test -pl canvas-engine -Dtest=AsyncTaskServiceTest,NotificationServiceTest,AsyncTaskControllerTest,NotificationControllerTest -q
```

Expected: PASS.

- [ ] **Step 8: Commit task and notification APIs**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dto/task backend/canvas-engine/src/main/java/org/chovy/canvas/dto/notification backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AsyncTaskController.java backend/canvas-engine/src/main/java/org/chovy/canvas/controller/NotificationController.java backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskService.java backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AsyncTaskControllerTest.java backend/canvas-engine/src/test/java/org/chovy/canvas/controller/NotificationControllerTest.java
git commit -m "feat: expose async task notifications api"
```

## Task 5: Wire Audience Controller To Task Enqueueing

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AudienceController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTaskTest.java`

- [ ] **Step 1: Write the failing audience controller task test**

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.chovy.canvas.domain.audience.AudienceStatMapper;
import org.chovy.canvas.domain.task.AsyncTask;
import org.chovy.canvas.domain.task.AsyncTaskCreateResult;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceControllerTaskTest {

    @Test
    void compute_returnsTaskIdAndStartsRunnerWhenTaskCreated() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceStatMapper statMapper = mock(AudienceStatMapper.class);
        AudienceBatchComputeService computeService = mock(AudienceBatchComputeService.class);
        AudienceSchedulerService schedulerService = mock(AudienceSchedulerService.class);
        AsyncTaskService taskService = mock(AsyncTaskService.class);
        AudienceComputeTaskRunner runner = mock(AudienceComputeTaskRunner.class);

        AudienceDefinition definition = new AudienceDefinition();
        definition.setId(7L);
        definition.setName("VIP 人群");
        when(definitionMapper.selectById(7L)).thenReturn(definition);

        AsyncTask task = new AsyncTask();
        task.setTaskId("task_1");
        task.setStatus("QUEUED");
        when(taskService.createOrReuseRunning("AUDIENCE_COMPUTE", "AUDIENCE", "7", "计算人群：VIP 人群", "system"))
                .thenReturn(new AsyncTaskCreateResult(task, true));

        AudienceController controller = new AudienceController(
                definitionMapper, statMapper, computeService, schedulerService, taskService, runner);

        var response = controller.compute(7L).block();

        assertThat(response.getData().taskId()).isEqualTo("task_1");
        assertThat(response.getData().status()).isEqualTo("QUEUED");
        verify(runner).start("task_1", 7L, "VIP 人群", "system");
    }

    @Test
    void compute_reusesExistingTaskWithoutStartingRunner() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceStatMapper statMapper = mock(AudienceStatMapper.class);
        AudienceBatchComputeService computeService = mock(AudienceBatchComputeService.class);
        AudienceSchedulerService schedulerService = mock(AudienceSchedulerService.class);
        AsyncTaskService taskService = mock(AsyncTaskService.class);
        AudienceComputeTaskRunner runner = mock(AudienceComputeTaskRunner.class);

        AudienceDefinition definition = new AudienceDefinition();
        definition.setId(7L);
        definition.setName("VIP 人群");
        when(definitionMapper.selectById(7L)).thenReturn(definition);

        AsyncTask task = new AsyncTask();
        task.setTaskId("task_running");
        task.setStatus("RUNNING");
        when(taskService.createOrReuseRunning(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new AsyncTaskCreateResult(task, false));

        AudienceController controller = new AudienceController(
                definitionMapper, statMapper, computeService, schedulerService, taskService, runner);

        var response = controller.compute(7L).block();

        assertThat(response.getData().taskId()).isEqualTo("task_running");
        assertThat(response.getData().status()).isEqualTo("RUNNING");
    }
}
```

- [ ] **Step 2: Run the targeted controller test to verify it fails**

```bash
mvn test -pl canvas-engine -Dtest=AudienceControllerTaskTest -q
```

Expected: FAIL because `AudienceController` has not been wired to `AsyncTaskService` and `AudienceComputeTaskRunner`.

- [ ] **Step 3: Modify `AudienceBatchComputeService.create` and `update` to persist only**

Change:

```java
public AudienceDefinition create(AudienceDefinition definition) {
    definitionMapper.insert(definition);
    compute(definition.getId());
    return definition;
}

public void update(AudienceDefinition definition) {
    definitionMapper.updateById(definition);
    compute(definition.getId());
}
```

to:

```java
public AudienceDefinition create(AudienceDefinition definition) {
    definitionMapper.insert(definition);
    return definition;
}

public void update(AudienceDefinition definition) {
    definitionMapper.updateById(definition);
}
```

The controller now owns enqueueing after persistence, so create/update no longer block on compute.

- [ ] **Step 4: Modify `AudienceController` constructor dependencies**

Add fields:

```java
private final AsyncTaskService taskService;
private final AudienceComputeTaskRunner computeTaskRunner;
```

Add imports:

```java
import org.chovy.canvas.domain.task.AsyncTaskCreateResult;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.dto.task.ComputeTaskResp;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
```

- [ ] **Step 5: Add enqueue helper to `AudienceController`**

```java
private ComputeTaskResp enqueueCompute(AudienceDefinition definition, String operator) {
    AsyncTaskCreateResult result = taskService.createOrReuseRunning(
            "AUDIENCE_COMPUTE",
            "AUDIENCE",
            String.valueOf(definition.getId()),
            "计算人群：" + definition.getName(),
            operator);
    if (result.created()) {
        computeTaskRunner.start(result.task().getTaskId(), definition.getId(), definition.getName(), operator);
    }
    return new ComputeTaskResp(result.task().getTaskId(), result.task().getStatus());
}

private Mono<String> currentUser() {
    return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getPrincipal())
            .cast(io.jsonwebtoken.Claims.class)
            .map(c -> c.get("username", String.class))
            .defaultIfEmpty("system");
}
```

- [ ] **Step 6: Change compute endpoint to return task data**

Replace the current compute method with:

```java
@PostMapping("/{id}/compute")
public Mono<R<ComputeTaskResp>> compute(@PathVariable Long id) {
    return currentUser().flatMap(operator ->
            Mono.fromCallable(() -> {
                AudienceDefinition definition = definitionMapper.selectById(id);
                if (definition == null) {
                    throw new IllegalArgumentException("Audience not found: " + id);
                }
                return R.ok(enqueueCompute(definition, operator));
            }).subscribeOn(Schedulers.boundedElastic()));
}
```

- [ ] **Step 7: Enqueue compute after create and update**

Change create to set `createdBy` and enqueue after scheduler refresh:

```java
@PostMapping
public Mono<R<AudienceDefinition>> create(@RequestBody AudienceDefinition body) {
    return currentUser().flatMap(operator ->
            Mono.fromCallable(() -> {
                body.setCreatedBy(operator);
                AudienceDefinition created = computeService.create(body);
                schedulerService.refresh(created, () -> computeService.compute(created.getId()));
                enqueueCompute(created, operator);
                return R.ok(created);
            }).subscribeOn(Schedulers.boundedElastic()));
}
```

Change update to enqueue after persistence:

```java
@PutMapping("/{id}")
public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AudienceDefinition body) {
    body.setId(id);
    return currentUser().flatMap(operator ->
            Mono.fromCallable(() -> {
                computeService.update(body);
                AudienceDefinition saved = definitionMapper.selectById(id);
                AudienceDefinition target = saved == null ? body : saved;
                schedulerService.refresh(target, () -> computeService.compute(target.getId()));
                enqueueCompute(target, operator);
                return R.<Void>ok();
            }).subscribeOn(Schedulers.boundedElastic()));
}
```

- [ ] **Step 8: Re-run the controller and runner tests**

```bash
mvn test -pl canvas-engine -Dtest=AudienceControllerTaskTest,AudienceComputeTaskRunnerTest -q
```

Expected: PASS.

- [ ] **Step 9: Commit audience task integration**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AudienceController.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTaskTest.java
git commit -m "feat: enqueue audience compute tasks"
```

## Task 6: Add Frontend APIs And Polling Presentation Helpers

**Files:**
- Create: `frontend/src/services/taskApi.ts`
- Create: `frontend/src/services/notificationApi.ts`
- Create: `frontend/src/pages/audience-list/audienceTaskPresentation.ts`
- Create: `frontend/src/pages/audience-list/audienceTaskPresentation.test.ts`
- Modify: `frontend/src/services/audienceApi.ts`

- [ ] **Step 1: Write failing frontend helper tests**

Create `frontend/src/pages/audience-list/audienceTaskPresentation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  getAudienceDisplayStatus,
  getNextAudiencePollDelay,
  hasRunningAudienceTasks,
  isTerminalTaskStatus,
} from './audienceTaskPresentation'

describe('audienceTaskPresentation', () => {
  it('treats queued and running tasks as non-terminal', () => {
    expect(isTerminalTaskStatus('QUEUED')).toBe(false)
    expect(isTerminalTaskStatus('RUNNING')).toBe(false)
  })

  it('treats success, failure, and canceled tasks as terminal', () => {
    expect(isTerminalTaskStatus('SUCCEEDED')).toBe(true)
    expect(isTerminalTaskStatus('FAILED')).toBe(true)
    expect(isTerminalTaskStatus('CANCELED')).toBe(true)
  })

  it('detects active audience tasks', () => {
    expect(hasRunningAudienceTasks([{ taskId: 'task_1', status: 'RUNNING', bizId: '7' }])).toBe(true)
    expect(hasRunningAudienceTasks([{ taskId: 'task_2', status: 'SUCCEEDED', bizId: '7' }])).toBe(false)
  })

  it('backs off polling after repeated failures', () => {
    expect(getNextAudiencePollDelay(0, false)).toBe(3000)
    expect(getNextAudiencePollDelay(2, false)).toBe(5000)
    expect(getNextAudiencePollDelay(4, false)).toBe(10000)
    expect(getNextAudiencePollDelay(0, true)).toBe(15000)
  })

  it('prefers running task state over stale stat state', () => {
    expect(getAudienceDisplayStatus({ status: 'READY' }, { taskId: 'task_1', status: 'RUNNING', bizId: '7' })).toBe('RUNNING')
    expect(getAudienceDisplayStatus({ status: 'READY' }, undefined)).toBe('READY')
  })
})
```

- [ ] **Step 2: Run the frontend helper test to verify it fails**

```bash
cd frontend && npm test -- audienceTaskPresentation.test.ts
```

Expected: FAIL because `audienceTaskPresentation.ts` does not exist.

- [ ] **Step 3: Add task and notification API services**

Create `frontend/src/services/taskApi.ts`:

```ts
import type { R } from '../types'
import http from './api'

export type AsyncTaskStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELED'

export interface AsyncTask {
  taskId: string
  taskType: string
  bizType: string
  bizId: string
  title: string
  status: AsyncTaskStatus
  progress: number
  resultSummary?: string
  errorMsg?: string
  startedAt?: string
  finishedAt?: string
  createdAt?: string
  updatedAt?: string
}

export const taskApi = {
  list: (params: { taskType?: string; bizType?: string; bizIds?: string; statuses?: string }) =>
    http.get<R<AsyncTask[]>, R<AsyncTask[]>>('/canvas/async-tasks', { params }),
}
```

Create `frontend/src/services/notificationApi.ts`:

```ts
import type { R } from '../types'
import http from './api'

export interface UserNotification {
  notificationId: string
  type: 'TASK_SUCCEEDED' | 'TASK_FAILED' | string
  title: string
  content?: string
  targetUrl?: string
  taskId?: string
  readAt?: string
  createdAt?: string
}

export const notificationApi = {
  list: (params?: { unreadOnly?: boolean; page?: number; size?: number }) =>
    http.get<R<UserNotification[]>, R<UserNotification[]>>('/canvas/notifications', { params }),
  unreadCount: () =>
    http.get<R<{ count: number }>, R<{ count: number }>>('/canvas/notifications/unread-count'),
  markRead: (notificationId: string) =>
    http.put<R<void>, R<void>>(`/canvas/notifications/${notificationId}/read`),
  markAllRead: () =>
    http.put<R<void>, R<void>>('/canvas/notifications/read-all'),
}
```

- [ ] **Step 4: Update audience compute API return type**

Modify `frontend/src/services/audienceApi.ts`:

```ts
export interface ComputeTaskResp {
  taskId: string
  status: 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELED'
}
```

Change `compute` to:

```ts
compute: (id: number) =>
  http.post<R<ComputeTaskResp>, R<ComputeTaskResp>>(`/canvas/audiences/${id}/compute`),
```

- [ ] **Step 5: Add audience task presentation helper**

Create `frontend/src/pages/audience-list/audienceTaskPresentation.ts`:

```ts
import type { AudienceStat } from '../../services/audienceApi'
import type { AsyncTask, AsyncTaskStatus } from '../../services/taskApi'

export function isTerminalTaskStatus(status: AsyncTaskStatus) {
  return status === 'SUCCEEDED' || status === 'FAILED' || status === 'CANCELED'
}

export function hasRunningAudienceTasks(tasks: Pick<AsyncTask, 'status'>[]) {
  return tasks.some(task => !isTerminalTaskStatus(task.status))
}

export function getNextAudiencePollDelay(failureCount: number, pageHidden: boolean) {
  if (pageHidden) return 15000
  if (failureCount >= 4) return 10000
  if (failureCount >= 2) return 5000
  return 3000
}

export function getAudienceDisplayStatus(
  stat?: Pick<AudienceStat, 'status'>,
  task?: Pick<AsyncTask, 'status' | 'taskId' | 'bizId'>,
) {
  if (task && !isTerminalTaskStatus(task.status)) {
    return task.status
  }
  return stat?.status ?? 'PENDING'
}
```

- [ ] **Step 6: Re-run frontend helper tests**

```bash
cd frontend && npm test -- audienceTaskPresentation.test.ts
```

Expected: PASS.

- [ ] **Step 7: Commit frontend API and helper slice**

```bash
git add frontend/src/services/taskApi.ts frontend/src/services/notificationApi.ts frontend/src/services/audienceApi.ts frontend/src/pages/audience-list/audienceTaskPresentation.ts frontend/src/pages/audience-list/audienceTaskPresentation.test.ts
git commit -m "feat: add frontend task notification APIs"
```

## Task 7: Add Audience List Auto Polling

**Files:**
- Modify: `frontend/src/pages/audience-list/index.tsx`
- Use: `frontend/src/pages/audience-list/audienceTaskPresentation.ts`
- Use: `frontend/src/services/taskApi.ts`

- [ ] **Step 1: Add imports and local task state**

In `frontend/src/pages/audience-list/index.tsx`, add:

```ts
import { useCallback, useMemo, useRef } from 'react'
import { useLocation } from 'react-router-dom'
import { taskApi, type AsyncTask } from '../../services/taskApi'
import {
  getAudienceDisplayStatus,
  getNextAudiencePollDelay,
  hasRunningAudienceTasks,
} from './audienceTaskPresentation'
```

Inside the component:

```ts
const location = useLocation()
const [tasks, setTasks] = useState<Record<number, AsyncTask>>({})
const [pollFailureCount, setPollFailureCount] = useState(0)
const pollTimerRef = useRef<number | null>(null)
const highlightedAudienceId = useMemo(() => {
  const value = new URLSearchParams(location.search).get('highlight')
  return value ? Number(value) : undefined
}, [location.search])
```

- [ ] **Step 2: Replace `fetchList` with `useCallback` and keep stats loaded**

Wrap the existing `fetchList` in `useCallback` so the polling effect can reuse it:

```ts
const fetchList = useCallback(async () => {
  setLoading(true)
  try {
    const res = await audienceApi.list()
    const list = res.data.list
    setData(list)
    const statResults = await Promise.allSettled(
      list.filter(item => item.id != null).map(item => audienceApi.stat(item.id!)),
    )
    const nextStats: Record<number, AudienceStat> = {}
    statResults.forEach((result, index) => {
      const id = list[index]?.id
      if (result.status === 'fulfilled' && id != null && result.value.data) {
        nextStats[id] = result.value.data
      }
    })
    setStats(nextStats)
  } finally {
    setLoading(false)
  }
}, [])
```

Update the initial effect:

```ts
useEffect(() => {
  fetchList()
}, [fetchList])
```

- [ ] **Step 3: Add task polling function**

Add:

```ts
const pollAudienceTasks = useCallback(async () => {
  const ids = data.map(item => item.id).filter((id): id is number => id != null)
  if (ids.length === 0) return
  const res = await taskApi.list({
    taskType: 'AUDIENCE_COMPUTE',
    bizType: 'AUDIENCE',
    bizIds: ids.join(','),
    statuses: 'QUEUED,RUNNING',
  })
  const nextTasks: Record<number, AsyncTask> = {}
  for (const task of res.data) {
    const audienceId = Number(task.bizId)
    if (!Number.isNaN(audienceId) && nextTasks[audienceId] == null) {
      nextTasks[audienceId] = task
    }
  }
  setTasks(nextTasks)
  setPollFailureCount(0)
  const stillRunning = hasRunningAudienceTasks(res.data)
  if (!stillRunning) {
    await fetchList()
  }
}, [data, fetchList])
```

- [ ] **Step 4: Add polling effect with backoff**

Add:

```ts
useEffect(() => {
  const activeTasks = Object.values(tasks).filter(task => task.status === 'QUEUED' || task.status === 'RUNNING')
  if (activeTasks.length === 0) {
    if (pollTimerRef.current != null) {
      window.clearTimeout(pollTimerRef.current)
      pollTimerRef.current = null
    }
    return
  }

  const schedule = () => {
    const delay = getNextAudiencePollDelay(pollFailureCount, document.hidden)
    pollTimerRef.current = window.setTimeout(async () => {
      try {
        await pollAudienceTasks()
      } catch {
        setPollFailureCount(count => count + 1)
      }
    }, delay)
  }

  schedule()
  return () => {
    if (pollTimerRef.current != null) {
      window.clearTimeout(pollTimerRef.current)
      pollTimerRef.current = null
    }
  }
}, [pollAudienceTasks, pollFailureCount, tasks])
```

- [ ] **Step 5: Update compute handler**

Change `handleCompute` to:

```ts
const handleCompute = async (id: number) => {
  const res = await audienceApi.compute(id)
  setTasks(prev => ({
    ...prev,
    [id]: {
      taskId: res.data.taskId,
      taskType: 'AUDIENCE_COMPUTE',
      bizType: 'AUDIENCE',
      bizId: String(id),
      title: '人群计算',
      status: res.data.status,
      progress: res.data.status === 'RUNNING' ? 5 : 0,
    },
  }))
  message.success('已开始计算，完成后会自动更新结果')
}
```

- [ ] **Step 6: Render display status from task state**

In the status column render function:

```tsx
const stat = record.id != null ? stats[record.id] : undefined
const task = record.id != null ? tasks[record.id] : undefined
const status = getAudienceDisplayStatus(stat, task)
const meta = STATUS_MAP[status] ?? { label: status, color: 'default' }
return <Tag color={meta.color}>{meta.label}</Tag>
```

Extend `STATUS_MAP`:

```ts
QUEUED: { label: '排队中', color: 'processing' },
RUNNING: { label: '计算中', color: 'processing' },
SUCCEEDED: { label: '就绪', color: 'success' },
CANCELED: { label: '已取消', color: 'default' },
```

- [ ] **Step 7: Disable compute button while active**

Inside action render:

```tsx
const task = record.id != null ? tasks[record.id] : undefined
const computing = task?.status === 'QUEUED' || task?.status === 'RUNNING'
```

Use:

```tsx
<Button size="small" icon={<ThunderboltOutlined />} disabled={computing} onClick={() => handleCompute(record.id!)}>
  {computing ? '计算中' : '计算'}
</Button>
```

- [ ] **Step 8: Add highlighted row class**

Add `rowClassName` to the table:

```tsx
rowClassName={record => record.id === highlightedAudienceId ? 'audience-row-highlight' : ''}
```

Add an inline style block near the page root:

```tsx
<style>{`
  .audience-row-highlight > td {
    background: #fff7e6 !important;
    transition: background .2s;
  }
`}</style>
```

- [ ] **Step 9: Run frontend tests and build**

```bash
cd frontend && npm test -- audienceTaskPresentation.test.ts
cd frontend && npm run build
```

Expected: tests PASS and build PASS.

- [ ] **Step 10: Commit audience polling UI**

```bash
git add frontend/src/pages/audience-list/index.tsx
git commit -m "feat: auto refresh audience compute status"
```

## Task 8: Add Global Notification Bell And Drawer

**Files:**
- Create: `frontend/src/components/notifications/NotificationBell.tsx`
- Create: `frontend/src/components/notifications/notificationPresentation.ts`
- Create: `frontend/src/components/notifications/notificationPresentation.test.ts`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Use: `frontend/src/services/notificationApi.ts`

- [ ] **Step 1: Write failing notification presentation tests**

Create `frontend/src/components/notifications/notificationPresentation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { getNotificationStatusColor, shouldShowUnreadBadge } from './notificationPresentation'

describe('notificationPresentation', () => {
  it('uses success color for successful task notifications', () => {
    expect(getNotificationStatusColor('TASK_SUCCEEDED')).toBe('success')
  })

  it('uses error color for failed task notifications', () => {
    expect(getNotificationStatusColor('TASK_FAILED')).toBe('error')
  })

  it('shows unread badge only for positive unread count', () => {
    expect(shouldShowUnreadBadge(0)).toBe(false)
    expect(shouldShowUnreadBadge(3)).toBe(true)
  })
})
```

- [ ] **Step 2: Run the notification presentation test to verify it fails**

```bash
cd frontend && npm test -- notificationPresentation.test.ts
```

Expected: FAIL because `notificationPresentation.ts` does not exist.

- [ ] **Step 3: Add notification presentation helper**

Create `frontend/src/components/notifications/notificationPresentation.ts`:

```ts
export function getNotificationStatusColor(type: string) {
  if (type === 'TASK_SUCCEEDED') return 'success'
  if (type === 'TASK_FAILED') return 'error'
  return 'default'
}

export function shouldShowUnreadBadge(count: number) {
  return count > 0
}
```

- [ ] **Step 4: Add notification bell component**

Create `frontend/src/components/notifications/NotificationBell.tsx`:

```tsx
import { useCallback, useEffect, useState } from 'react'
import { Badge, Button, Drawer, Empty, List, Space, Tag, Typography, message } from 'antd'
import { BellOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { notificationApi, type UserNotification } from '../../services/notificationApi'
import { getNotificationStatusColor, shouldShowUnreadBadge } from './notificationPresentation'

const { Text } = Typography

export default function NotificationBell() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)
  const [items, setItems] = useState<UserNotification[]>([])
  const [loading, setLoading] = useState(false)

  const fetchUnreadCount = useCallback(async () => {
    const res = await notificationApi.unreadCount()
    setUnreadCount(res.data.count)
  }, [])

  const fetchNotifications = useCallback(async () => {
    setLoading(true)
    try {
      const res = await notificationApi.list({ page: 1, size: 20 })
      setItems(res.data)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchUnreadCount().catch(() => undefined)
    const timer = window.setInterval(() => {
      fetchUnreadCount().catch(() => undefined)
    }, 30000)
    return () => window.clearInterval(timer)
  }, [fetchUnreadCount])

  const handleOpen = async () => {
    setOpen(true)
    try {
      await fetchNotifications()
    } catch {
      message.error('通知加载失败')
    }
  }

  const handleClick = async (item: UserNotification) => {
    if (!item.readAt) {
      await notificationApi.markRead(item.notificationId)
      await fetchUnreadCount()
    }
    setOpen(false)
    if (item.targetUrl) {
      navigate(item.targetUrl)
    }
  }

  const handleReadAll = async () => {
    await notificationApi.markAllRead()
    await fetchUnreadCount()
    await fetchNotifications()
  }

  return (
    <>
      <Badge count={shouldShowUnreadBadge(unreadCount) ? unreadCount : 0} size="small">
        <Button type="text" icon={<BellOutlined />} onClick={handleOpen} />
      </Badge>
      <Drawer
        title="通知"
        width={360}
        placement="right"
        open={open}
        onClose={() => setOpen(false)}
        extra={<Button size="small" onClick={handleReadAll}>全部已读</Button>}
      >
        <List
          loading={loading}
          dataSource={items}
          locale={{ emptyText: <Empty description="暂无通知" /> }}
          renderItem={item => (
            <List.Item style={{ cursor: 'pointer' }} onClick={() => handleClick(item)}>
              <List.Item.Meta
                title={
                  <Space>
                    <span>{item.title}</span>
                    <Tag color={getNotificationStatusColor(item.type)}>{item.readAt ? '已读' : '未读'}</Tag>
                  </Space>
                }
                description={
                  <Space direction="vertical" size={2}>
                    <Text type="secondary">{item.content}</Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>{item.createdAt?.replace('T', ' ').slice(0, 19)}</Text>
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Drawer>
    </>
  )
}
```

- [ ] **Step 5: Mount notification bell in layout**

Modify `frontend/src/components/layout/AppLayout.tsx`:

```ts
import NotificationBell from '../notifications/NotificationBell'
```

In the bottom user area or a compact top-right content bar, render:

```tsx
<NotificationBell />
```

Preferred placement for current layout: add it in the bottom user panel before the user dropdown when not collapsed, and keep only the icon when collapsed:

```tsx
<div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
  <NotificationBell />
  <Dropdown menu={{ items: userMenu }} placement="topLeft" trigger={['click']}>
    {/* existing user block */}
  </Dropdown>
</div>
```

Keep the existing avatar dropdown behavior unchanged.

- [ ] **Step 6: Re-run frontend notification tests and build**

```bash
cd frontend && npm test -- notificationPresentation.test.ts
cd frontend && npm run build
```

Expected: tests PASS and build PASS.

- [ ] **Step 7: Commit notification UI**

```bash
git add frontend/src/components/notifications frontend/src/components/layout/AppLayout.tsx
git commit -m "feat: add notification drawer"
```

## Task 9: End-To-End Verification And Cleanup

**Files:**
- Verify all files touched in Tasks 1-8.
- Update no additional docs unless implementation diverged from `docs/superpowers/specs/2026-05-23-async-task-notification-design.md`.

- [ ] **Step 1: Run backend targeted tests**

```bash
mvn test -pl canvas-engine -Dtest=AsyncTaskServiceTest,NotificationServiceTest,AudienceComputeTaskRunnerTest,AudienceControllerTaskTest -q
```

Expected: PASS.

- [ ] **Step 2: Run backend package tests if targeted tests pass**

```bash
mvn test -pl canvas-engine -q
```

Expected: PASS. If unrelated existing dirty-worktree tests fail, record the failing class and error before continuing.

- [ ] **Step 3: Run frontend tests**

```bash
cd frontend && npm test
```

Expected: PASS.

- [ ] **Step 4: Run frontend production build**

```bash
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 5: Manual smoke test with local services**

Start dependencies if they are not already running:

```bash
docker compose -f docker-compose.local.yml up -d
```

Start backend and frontend:

```bash
mvn spring-boot:run -pl canvas-engine
cd frontend && npm run dev
```

In the browser:

1. Log in.
2. Open `/audiences`.
3. Click “计算” on a row.
4. Confirm the row changes to “排队中” or “计算中” without page refresh.
5. Wait for completion and confirm the row updates to “就绪” or “失败”.
6. Open the notification bell and confirm a success or failure notification exists.
7. Click the notification and confirm `/audiences?highlight={audienceId}&taskId={taskId}` opens with the row highlighted.

- [ ] **Step 6: Inspect git status**

```bash
git status --short
```

Expected: only intentional implementation files are changed. Existing unrelated user changes from before this plan remain untouched.

- [ ] **Step 7: Final commit for verification fixes**

If Step 1-6 required small fixes, commit them:

```bash
git add backend frontend
git commit -m "test: verify async task notifications"
```

Skip this commit when no verification fixes were needed.
