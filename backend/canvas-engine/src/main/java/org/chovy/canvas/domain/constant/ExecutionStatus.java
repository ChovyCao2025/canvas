package org.chovy.canvas.domain.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** canvas_execution.status */
@Getter
@AllArgsConstructor
public enum ExecutionStatus {

    RUNNING(0),
    PAUSED(1),
    SUCCESS(2),
    FAILED(3);

    private final int code;

    public static ExecutionStatus of(int code) {
        for (ExecutionStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown ExecutionStatus code: " + code);
    }
}
