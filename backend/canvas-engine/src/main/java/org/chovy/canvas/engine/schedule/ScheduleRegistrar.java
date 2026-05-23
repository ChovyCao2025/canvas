package org.chovy.canvas.engine.schedule;

/**
 * Abstraction over schedule registration backends.
 */
public interface ScheduleRegistrar {

    void register(ScheduleRegistration registration);

    void unregister(ScheduleKey key);
}
