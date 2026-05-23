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

    private final TaskScheduler taskScheduler;
    private final Map<ScheduleKey, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    @Override
    public void register(ScheduleRegistration registration) {
        unregister(registration.key());
        ScheduledFuture<?> future = registration.cronExpression() != null && !registration.cronExpression().isBlank()
                ? scheduleCron(registration)
                : scheduleOnce(registration);
        if (future == null) {
            throw new IllegalStateException("Local TaskScheduler returned null for " + registration.key());
        }
        tasks.put(registration.key(), future);
    }

    @Override
    public void unregister(ScheduleKey key) {
        ScheduledFuture<?> future = tasks.remove(key);
        if (future != null) {
            future.cancel(false);
        }
    }

    public boolean hasTask(ScheduleKey key) {
        return tasks.containsKey(key);
    }

    @PreDestroy
    void shutdown() {
        tasks.values().forEach(future -> future.cancel(false));
        tasks.clear();
    }

    private ScheduledFuture<?> scheduleCron(ScheduleRegistration registration) {
        return taskScheduler.schedule(
                guardedCallback(registration),
                new CronTrigger(registration.cronExpression(),
                        TimeZone.getTimeZone(registration.timezone()))
        );
    }

    private ScheduledFuture<?> scheduleOnce(ScheduleRegistration registration) {
        return taskScheduler.schedule(
                guardedCallback(registration),
                registration.triggerTime().atZone(ZoneId.of(registration.timezone())).toInstant()
        );
    }

    private Runnable guardedCallback(ScheduleRegistration registration) {
        return () -> {
            try {
                registration.callback().run();
            } catch (RuntimeException e) {
                log.error("[LOCAL_SCHEDULER] callback failed key={}: {}", registration.key(), e.getMessage(), e);
            } finally {
                if (registration.triggerTime() != null) {
                    tasks.remove(registration.key());
                }
            }
        };
    }
}
