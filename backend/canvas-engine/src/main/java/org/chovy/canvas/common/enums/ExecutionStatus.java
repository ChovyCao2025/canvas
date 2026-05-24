package org.chovy.canvas.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 执行状态枚举（`canvas_execution.status`）。
 */
@Getter
@AllArgsConstructor
public enum ExecutionStatus {

    /** 执行中。 */
    RUNNING(0),

    /** 挂起等待（多阶段执行）。 */
    PAUSED(1),

    /** 执行成功。 */
    SUCCESS(2),

    /** 执行失败。 */
    FAILED(3);

    /** 对应数据库中的整型状态码。 */
    private final int code;

    /** 按数值状态码反查枚举。 */
    public static ExecutionStatus of(int code) {
        for (ExecutionStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown ExecutionStatus code: " + code);
    }
}
