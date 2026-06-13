package org.chovy.canvas.canvas.domain;

public enum CanvasStatus {
    DRAFT(0),
    PUBLISHED(1),
    OFFLINE(2),
    ARCHIVED(3),
    KILLED(4);

    private final int code;

    CanvasStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static CanvasStatus fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("canvas status is required");
        }
        for (CanvasStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalStateException("UNKNOWN canvas state: " + code);
    }
}
