package org.chovy.canvas.engine.schedule;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Scheduler-agnostic registration payload.
 * @param key 调度任务唯一标识.
 * @param cronExpression 周期调度使用的 Cron 表达式.
 * @param triggerTime 一次性调度的触发时间.
 * @param timezone 解释 Cron 或一次性时间使用的业务时区.
 * @param callback 调度触发时执行的回调.
 * @param metadata 传递给调度后端的元数据快照.
 */
public record ScheduleRegistration(
        ScheduleKey key,
        String cronExpression,
        LocalDateTime triggerTime,
        String timezone,
        Runnable callback,
        Map<String, Object> metadata
) {
    public ScheduleRegistration {
        if (key == null) {
            throw new IllegalArgumentException("Schedule key must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Schedule callback must not be null");
        }
        boolean hasCron = cronExpression != null && !cronExpression.isBlank();
        boolean hasTriggerTime = triggerTime != null;
        if (hasCron == hasTriggerTime) {
            // 一个注册载荷只能表达周期任务或一次性任务，不能二者同时存在或同时缺失。
            throw new IllegalArgumentException("Exactly one of cronExpression or triggerTime must be set");
        }
        if (timezone == null || timezone.isBlank()) {
            timezone = "Asia/Shanghai";
        }
        // 元数据快照化后传给调度后端，避免调用方后续修改 Map 影响已注册任务。
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
