package org.chovy.canvas.domain.project;

/**
 * CanvasProjectRole 枚举 domain.project 场景中的固定业务取值。
 */
public enum CanvasProjectRole {
    PROJECT_ADMIN,
    EDITOR,
    EXECUTOR,
    VIEWER;

    /**
     * parse 校验或转换 domain.project 场景的数据。
     * @param raw raw 参数，用于 parse 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public static CanvasProjectRole parse(String raw) {
        try {
            return CanvasProjectRole.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported project role: " + raw);
        }
    }
}
