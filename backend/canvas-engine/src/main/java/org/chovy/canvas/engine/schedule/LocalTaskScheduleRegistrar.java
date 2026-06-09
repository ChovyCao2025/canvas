package org.chovy.canvas.engine.schedule;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 面向本地开发和单实例部署的任务调度注册器。
 *
 * <p>该实现只在当前 JVM 内保存调度句柄，不是生产级分布式调度器；生产部署应提供基于
 * DolphinScheduler、XXL-Job、Quartz JDBC 集群或其他持久化调度器的 ScheduleRegistrar Bean。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(ScheduleRegistrar.class)
public class LocalTaskScheduleRegistrar implements ScheduleRegistrar {

    /** Spring 本地任务调度器。 */
    private final TaskScheduler taskScheduler;
    /** 当前 JVM 内已注册调度任务的句柄映射。 */
    private final Map<ScheduleKey, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    /** 注册 cron 或一次性任务，并替换相同 key 的旧任务。 */
    @Override
    public void register(ScheduleRegistration registration) {
        // 相同业务 key 重新注册时先取消旧任务，确保本 JVM 内只有一个有效调度句柄。
        unregister(registration.key());
        ScheduledFuture<?> future = registration.cronExpression() != null && !registration.cronExpression().isBlank()
                /**
                 * 执行核心业务流程，并协调依赖组件完成处理。
                 *
                 * @param registration registration 参数，用于 scheduleCron 流程中的校验、计算或对象转换。
                 * @return 返回 scheduleCron 流程生成的业务结果。
                 */
                ? scheduleCron(registration)
                /**
                 * 执行核心业务流程，并协调依赖组件完成处理。
                 *
                 * @param registration registration 参数，用于 scheduleOnce 流程中的校验、计算或对象转换。
                 * @return 返回 scheduleOnce 流程生成的业务结果。
                 */
                : scheduleOnce(registration);
        if (future == null) {
            throw new IllegalStateException("Local TaskScheduler returned null for " + registration.key());
        }
        tasks.put(registration.key(), future);
    }

    /** 如果存在指定 key 的本地调度句柄，则取消并移除。 */
    @Override
    public void unregister(ScheduleKey key) {
        ScheduledFuture<?> future = tasks.remove(key);
        if (future != null) {
            future.cancel(false);
        }
    }

    /** 测试可见：断言内存注册表生命周期。 */
    public boolean hasTask(ScheduleKey key) {
        return tasks.containsKey(key);
    }

    /** Bean 关闭时取消所有本地任务。 */
    @PreDestroy
    void shutdown() {
        tasks.values().forEach(future -> future.cancel(false));
        tasks.clear();
    }

    /**
     * 注册 cron 周期任务。
     *
     * @param registration 调度注册信息
     * @return Spring TaskScheduler 返回的任务句柄
     */
    private ScheduledFuture<?> scheduleCron(ScheduleRegistration registration) {
        return taskScheduler.schedule(
                guardedCallback(registration),
                // CronTrigger 使用注册载荷里的时区，避免服务器默认时区影响业务触发时间。
                new CronTrigger(registration.cronExpression(),
                        TimeZone.getTimeZone(registration.timezone()))
        );
    }

    /**
     * 注册一次性定时任务。
     *
     * @param registration 调度注册信息
     * @return Spring TaskScheduler 返回的任务句柄
     */
    private ScheduledFuture<?> scheduleOnce(ScheduleRegistration registration) {
        return taskScheduler.schedule(
                guardedCallback(registration),
                // 一次性任务以业务时区解释 LocalDateTime，再转换为调度器需要的 Instant。
                registration.triggerTime().atZone(ZoneId.of(registration.timezone())).toInstant()
        );
    }

    /**
     * 包装调度回调，隔离任务异常并清理一次性任务句柄。
     *
     * <p>调度线程不能因业务回调异常退出，一次性任务结束后也不能继续占用注册表。
     *
     * @param registration 调度注册信息
     * @return 可交给 Spring TaskScheduler 执行的安全回调
     */
    private Runnable guardedCallback(ScheduleRegistration registration) {
        return () -> {
            try {
                registration.callback().run();
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException e) {
                log.error("[LOCAL_SCHEDULER] callback failed key={}: {}", registration.key(), e.getMessage(), e);
            } finally {
                if (registration.triggerTime() != null) {
                    // 一次性任务执行结束后移除句柄，避免 hasTask 误判为仍有待执行任务。
                    tasks.remove(registration.key());
                }
            }
        };
    }
}
