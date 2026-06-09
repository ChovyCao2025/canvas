package org.chovy.canvas.engine.schedule;

/**
 * Stable business identifier for a scheduled registration.
 * @param namespace 调度所属命名空间.
 * @param id 命名空间内的调度业务 ID.
 */
public record ScheduleKey(
        String namespace,
        String id
) {
}
