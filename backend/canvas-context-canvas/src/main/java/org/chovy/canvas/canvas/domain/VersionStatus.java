package org.chovy.canvas.canvas.domain;

/**
 * 枚举VersionStatus支持的取值。
 */
public enum VersionStatus {

    /**
     * 表示DRAFT状态。
     */
    DRAFT(0),
    /**
     * 表示PUBLISHED状态。
     */
    PUBLISHED(1);

    /**
     * 保存编码。
     */
    private final int code;

    /**
     * 创建当前对象实例。
     */
    VersionStatus(int code) {
        this.code = code;
    }

    /**
     * 处理编码。
     */
    public int code() {
        return code;
    }

    /**
     * 处理fromCode。
     */
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
