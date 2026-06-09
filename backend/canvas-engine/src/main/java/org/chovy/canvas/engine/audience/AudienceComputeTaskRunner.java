package org.chovy.canvas.engine.audience;

import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.infrastructure.concurrent.ManagedVirtualThreadExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.locks.LockSupport;

/**
 * Audience Compute Task Runner 人群计算组件。
 *
 * <p>负责把人群规则、数据源配置和计算任务转换为可执行查询或后台任务结果。
 * <p>该组件处于画布触发与 CDP 数据之间，需关注大数据量查询的边界和失败兜底。
 */
@Slf4j
@Service
public class AudienceComputeTaskRunner {

    /** 任务失败错误信息最大保留长度。 */
    private static final int ERROR_LIMIT = 1000;
    /** 人群计算锁被占用时的默认重试间隔。 */
    private static final Duration DEFAULT_LOCK_RETRY_DELAY = Duration.ofSeconds(3);

    /** 人群批量计算服务。 */
    private final AudienceBatchComputeService computeService;
    /** 异步任务状态服务，用于更新任务运行结果。 */
    private final AsyncTaskService asyncTaskService;
    /** 通知服务，用于向任务订阅者发送计算结果。 */
    private final NotificationService notificationService;
    /** 人群计算锁被占用时的实际重试间隔。 */
    private final Duration lockRetryDelay;
    private final Consumer<Duration> lockRetryWaiter;
    private ManagedVirtualThreadExecutor backgroundExecutor = ManagedVirtualThreadExecutor.direct();

    /**
     * 构造 AudienceComputeTaskRunner 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param computeService computeService 方法执行所需的业务参数
     * @param asyncTaskService asyncTaskService 方法执行所需的业务参数
     * @param notificationService notificationService 方法执行所需的业务参数
     */
    @Autowired
    public AudienceComputeTaskRunner(
            AudienceBatchComputeService computeService,
            AsyncTaskService asyncTaskService,
            NotificationService notificationService
    ) {
        this(computeService, asyncTaskService, notificationService, DEFAULT_LOCK_RETRY_DELAY);
    }

    /**
     * 构造 AudienceComputeTaskRunner 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param computeService computeService 方法执行所需的业务参数
     * @param asyncTaskService asyncTaskService 方法执行所需的业务参数
     * @param notificationService notificationService 方法执行所需的业务参数
     * @param lockRetryDelay lockRetryDelay 方法执行所需的业务参数
     */
    AudienceComputeTaskRunner(
            AudienceBatchComputeService computeService,
            AsyncTaskService asyncTaskService,
            NotificationService notificationService,
            Duration lockRetryDelay
    ) {
        this.computeService = computeService;
        this.asyncTaskService = asyncTaskService;
        this.notificationService = notificationService;
        this.lockRetryDelay = lockRetryDelay;
        this.lockRetryWaiter = null;
    }

    /**
     * 初始化 AudienceComputeTaskRunner 实例。
     *
     * @param computeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param asyncTaskService 依赖组件，用于完成数据访问或外部能力调用。
     * @param notificationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param lockRetryDelay lock retry delay 参数，用于 AudienceComputeTaskRunner 流程中的校验、计算或对象转换。
     * @param lockRetryWaiter lock retry waiter 参数，用于 AudienceComputeTaskRunner 流程中的校验、计算或对象转换。
     */
    AudienceComputeTaskRunner(
            AudienceBatchComputeService computeService,
            AsyncTaskService asyncTaskService,
            NotificationService notificationService,
            Duration lockRetryDelay,
            Consumer<Duration> lockRetryWaiter
    ) {
        this.computeService = computeService;
        this.asyncTaskService = asyncTaskService;
        this.notificationService = notificationService;
        this.lockRetryDelay = lockRetryDelay;
        this.lockRetryWaiter = lockRetryWaiter;
    }

    @Autowired(required = false)
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param backgroundExecutor 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    void setBackgroundExecutor(ManagedVirtualThreadExecutor backgroundExecutor) {
        this.backgroundExecutor = backgroundExecutor;
    }

    /**
     * 注册、调度或初始化 start 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param taskId taskId 对应的业务主键或标识
     * @param audienceId audienceId 对应的业务主键或标识
     * @param audienceName audienceName 方法执行所需的业务参数
     * @param operator operator 操作人标识
     */
    public void start(String taskId, Long audienceId, String audienceName, String operator) {
        start(taskId, audienceId, audienceName, operator, null);
    }

    /** 注册指定租户内的人群计算任务。 */
    public void start(String taskId, Long audienceId, String audienceName, String operator, Long tenantId) {
        backgroundExecutor.submit("audience-compute-" + taskId,
                () -> runNow(taskId, audienceId, audienceName, operator, tenantId));
    }

    /**
     * 同步执行一次人群计算任务并落地任务状态。
     *
     * <p>成功、失败和计算锁等待都会写回异步任务，并按订阅关系发送结果通知。
     *
     * @param taskId 异步任务 ID
     * @param audienceId 人群定义 ID
     * @param audienceName 人群展示名
     * @param operator 发起计算的操作人
     */
    public void runNow(String taskId, Long audienceId, String audienceName, String operator) {
        runNow(taskId, audienceId, audienceName, operator, null);
    }

    /** 同步执行指定租户内的人群计算任务并落地任务状态。 */
    public void runNow(String taskId, Long audienceId, String audienceName, String operator, Long tenantId) {
        // 准备本次处理所需的上下文和中间变量。
        asyncTaskService.markRunning(taskId);
        AudienceComputeResult result;
        try {
            result = computeWithLockRetry(audienceId, taskId);
        } catch (Exception e) {
            String error = errorMessage(e);
            log.error("[AUDIENCE] compute task failed taskId={} audienceId={}: {}", taskId, audienceId, error, e);
            markFailedBestEffort(taskId, error);
            createNotificationsBestEffort(
                    tenantId,
                    taskId,
                    operator,
                    "TASK_FAILED",
                    "人群计算失败",
                    displayName(null, audienceName, audienceId) + " · " + error,
                    targetUrl(audienceId, taskId));
            return;
        }
        if (result.success()) {
            asyncTaskService.markSucceeded(taskId, successSummary(result));
            createNotificationsBestEffort(
                    tenantId,
                    taskId,
                    operator,
                    "TASK_SUCCEEDED",
                    "人群计算完成",
                    displayName(result, audienceName, audienceId) + " · " + result.estimatedSize() + " 人",
                    targetUrl(audienceId, taskId));
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        String error = result.errorMsg() == null ? "计算失败" : result.errorMsg();
        asyncTaskService.markFailed(taskId, error);
        createNotificationsBestEffort(
                tenantId,
                taskId,
                operator,
                "TASK_FAILED",
                "人群计算失败",
                displayName(result, audienceName, audienceId) + " · " + error,
                targetUrl(audienceId, taskId));
    }

    /**
     * 执行 success Summary 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param result result 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String successSummary(AudienceComputeResult result) {
        return "{\"audienceId\":" + result.audienceId()
                + ",\"estimatedSize\":" + result.estimatedSize()
                + ",\"bitmapSizeKb\":" + result.bitmapSizeKb()
                + "}";
    }

    /**
     * 执行 target Url 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param audienceId audienceId 对应的业务主键或标识
     * @param taskId taskId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
    private String targetUrl(Long audienceId, String taskId) {
        return "/audiences?highlight=" + audienceId + "&taskId=" + taskId;
    }

    /**
     * 尝试把任务标记为失败。
     *
     * <p>异常处理链路中不能再次抛出状态写入错误，否则会掩盖原始计算异常。
     *
     * @param taskId 异步任务 ID
     * @param error 已截断的失败原因
     */
    private void markFailedBestEffort(String taskId, String error) {
        try {
            asyncTaskService.markFailed(taskId, error);
        } catch (Exception markException) {
            log.error("[AUDIENCE] failed to mark compute task failed taskId={}: {}",
                    taskId, markException.getMessage(), markException);
        }
    }

    /**
     * 计算或统计 compute With Lock Retry 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param audienceId audienceId 对应的业务主键或标识
     * @param taskId taskId 对应的业务主键或标识
     * @return 方法执行后的业务结果
     */
    private AudienceComputeResult computeWithLockRetry(Long audienceId, String taskId) throws InterruptedException {
        while (true) {
            AudienceComputeResult result = computeService.compute(audienceId);
            if (!result.inProgress()) {
                return result;
            }
            log.info("[AUDIENCE] compute task waiting for active lock taskId={} audienceId={}: {}",
                    taskId, audienceId, result.errorMsg());
            sleepBeforeLockRetry();
        }
    }

    /**
     * 执行 sleep Before Lock Retry 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private void sleepBeforeLockRetry() throws InterruptedException {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (lockRetryDelay.isZero() || lockRetryDelay.isNegative()) {
            return;
        }
        if (lockRetryWaiter != null) {
            lockRetryWaiter.accept(lockRetryDelay);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        LockSupport.parkNanos(lockRetryDelay.toNanos());
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Interrupted while waiting for audience compute lock retry");
        }
    }

    /**
     * 尝试创建单个任务结果通知。
     *
     * <p>通知失败只记录日志，不影响任务最终状态。
     *
     * @param operator 通知接收人
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param targetUrl 前端跳转地址
     * @param taskId 关联异步任务 ID
     */
    private void createNotificationBestEffort(
            Long tenantId, String operator, String type, String title, String content, String targetUrl, String taskId) {
        try {
            if (tenantId == null) {
                notificationService.createForTask(operator, type, title, content, targetUrl, taskId);
            } else {
                notificationService.createForTask(tenantId, operator, type, title, content, targetUrl, taskId);
            }
        } catch (Exception notificationException) {
            log.error("[AUDIENCE] failed to create compute task notification taskId={}: {}",
                    taskId, notificationException.getMessage(), notificationException);
        }
    }

    /**
     * 创建或新增 create Notifications Best Effort 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param taskId taskId 对应的业务主键或标识
     * @param fallbackOperator fallbackOperator 操作人标识
     * @param type type 类型标识或分类条件
     * @param title title 方法执行所需的业务参数
     * @param content content 方法执行所需的业务参数
     * @param targetUrl targetUrl 方法执行所需的业务参数
     */
    private void createNotificationsBestEffort(
            Long tenantId, String taskId, String fallbackOperator, String type, String title, String content, String targetUrl) {
        for (String recipient : notificationRecipients(taskId, fallbackOperator)) {
            createNotificationBestEffort(tenantId, recipient, type, title, content, targetUrl, taskId);
        }
    }

    /**
     * 汇总人群计算任务的通知接收人。
     *
     * <p>优先使用任务订阅者；订阅读取失败或为空时回退到本次操作人。
     *
     * @param taskId 异步任务 ID
     * @param fallbackOperator 订阅为空时使用的操作人
     * @return 去重后的通知接收人列表
     */
    private List<String> notificationRecipients(String taskId, String fallbackOperator) {
        LinkedHashSet<String> recipients = new LinkedHashSet<>();
        try {
            List<String> subscribers = asyncTaskService.subscribers(taskId);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (subscribers != null) {
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                subscribers.stream()
                        .filter(this::hasText)
                        .forEach(recipients::add);
            }
        } catch (Exception e) {
            log.error("[AUDIENCE] failed to load compute task subscribers taskId={}: {}", taskId, e.getMessage(), e);
        }
        if (recipients.isEmpty() && hasText(fallbackOperator)) {
            recipients.add(fallbackOperator);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(recipients);
    }

    /**
     * 执行 display Name 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param result result 方法执行所需的业务参数
     * @param audienceName audienceName 方法执行所需的业务参数
     * @param audienceId audienceId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
    private String displayName(AudienceComputeResult result, String audienceName, Long audienceId) {
        if (result != null && hasText(result.audienceName())) {
            return result.audienceName();
        }
        if (hasText(audienceName)) {
            return audienceName;
        }
        return "人群 " + audienceId;
    }

    /**
     * 提取并截断任务失败原因。
     *
     * <p>异步任务表只保存摘要，完整异常仍通过日志定位。
     *
     * @param e 人群计算抛出的异常
     * @return 可写入任务结果的失败摘要
     */
    private String errorMessage(Exception e) {
        String message = e.getMessage();
        if (!hasText(message)) {
            message = "计算失败";
        }
        return message.length() <= ERROR_LIMIT ? message : message.substring(0, ERROR_LIMIT);
    }

    /**
     * 判断 has Text 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
