package org.chovy.canvas.engine.schedule;

/**
 * Stable business identifier for a scheduled registration.
 */
public record ScheduleKey(String namespace, String id) {
}
