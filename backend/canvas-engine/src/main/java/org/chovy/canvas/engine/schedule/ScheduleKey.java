package org.chovy.canvas.engine.schedule;

/**
 * Stable business identifier for a scheduled registration.
 */
public record ScheduleKey(
        /** 调度所属命名空间。 */
        String namespace,
        /** 命名空间内的调度业务 ID。 */
        String id
) {
}
