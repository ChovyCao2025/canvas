package org.chovy.canvas.canvas.domain;

public enum VersionStatus {
    DRAFT(0),
    PUBLISHED(1);

    private final int code;

    VersionStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static VersionStatus fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("version status is required");
        }
        for (VersionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalStateException("UNKNOWN canvas version state: " + code);
    }
}
