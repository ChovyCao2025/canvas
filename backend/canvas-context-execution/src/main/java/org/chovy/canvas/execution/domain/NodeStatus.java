package org.chovy.canvas.execution.domain;

public enum NodeStatus {
    PENDING,
    RUNNING,
    WAITING,
    SUCCESS,
    FAILED,
    TIMEOUT,
    SUPPRESSED,
    SKIPPED
}
