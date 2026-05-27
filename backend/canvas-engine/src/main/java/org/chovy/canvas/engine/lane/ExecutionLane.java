package org.chovy.canvas.engine.lane;

public enum ExecutionLane {
    LIGHT,
    STANDARD,
    HEAVY,
    RETRY;

    public String key() {
        return name().toLowerCase();
    }
}
