package org.chovy.canvas.canvas.domain;

/**
 * 枚举CanvasStatus支持的取值。
 */
public enum CanvasStatus {

    /**
     * 表示DRAFT状态。
     */
    DRAFT(0),
    /**
     * 表示PUBLISHED状态。
     */
    PUBLISHED(1),
    /**
     * 表示OFFLINE状态。
     */
    OFFLINE(2),
    /**
     * 表示ARCHIVED状态。
     */
    ARCHIVED(3),
    /**
     * 表示KILLED状态。
     */
    KILLED(4);

    /**
     * 保存编码。
     */
    private final int code;

    /**
     * 创建当前对象实例。
     */
    CanvasStatus(int code) {
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
