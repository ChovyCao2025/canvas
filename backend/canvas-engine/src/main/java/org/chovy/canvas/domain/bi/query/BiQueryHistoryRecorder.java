package org.chovy.canvas.domain.bi.query;

/**
 * BiQueryHistoryRecorder 定义 domain.bi.query 场景中的扩展契约。
 */
@FunctionalInterface
public interface BiQueryHistoryRecorder {

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param entry entry 参数，用于 record 流程中的校验、计算或对象转换。
     */
    void record(BiQueryHistoryEntry entry);

    /**
     * 执行 noop 流程，围绕 noop 完成校验、计算或结果组装。
     *
     * @return 返回 noop 流程生成的业务结果。
     */
    static BiQueryHistoryRecorder noop() {
        return ignored -> {
        };
    }
}
