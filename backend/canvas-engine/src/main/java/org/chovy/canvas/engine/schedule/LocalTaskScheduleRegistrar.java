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
 * This implementation is for local development and single-instance deployments.
 * It stores schedule handles in the current JVM and is not a production-grade
 * distributed scheduler. Production deployments should provide their own
 * ScheduleRegistrar bean backed by DolphinScheduler, XXL-Job, Quartz JDBC
 * cluster, or another durable scheduler.
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

    /**
     * 注册、调度或初始化 register 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param registration registration 方法执行所需的业务参数
     */
    @Override
    public void register(ScheduleRegistration registration) {
        // 相同业务 key 重新注册时先取消旧任务，确保本 JVM 内只有一个有效调度句柄。
        unregister(registration.key());
        ScheduledFuture<?> future = registration.cronExpression() != null && !registration.cronExpression().isBlank()
                ? scheduleCron(registration)
                : scheduleOnce(registration);
        if (future == null) {
            throw new IllegalStateException("Local TaskScheduler returned null for " + registration.key());
        }
        tasks.put(registration.key(), future);
    }

    /**
     * 执行 unregister 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    @Override
    public void unregister(ScheduleKey key) {
        ScheduledFuture<?> future = tasks.remove(key);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 判断 has Task 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean hasTask(ScheduleKey key) {
        return tasks.containsKey(key);
    }

    /**
     * 停止或关闭 shutdown 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    @PreDestroy
    void shutdown() {
        tasks.values().forEach(future -> future.cancel(false));
        tasks.clear();
    }

    /**
     * 注册、调度或初始化 schedule Cron 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param registration registration 方法执行所需的业务参数
     * @return 方法执行后的业务结果
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
     * 注册、调度或初始化 schedule Once 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param registration registration 方法执行所需的业务参数
     * @return 方法执行后的业务结果
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
