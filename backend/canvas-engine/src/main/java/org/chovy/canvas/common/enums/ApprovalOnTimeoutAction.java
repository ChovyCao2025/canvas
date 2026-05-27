package org.chovy.canvas.common.enums;

/**
 * 人工审批超时策略常量（`canvas_manual_approval.on_timeout`）。
 *
 * <p>用于审批 watchdog 判断“超时后自动如何流转”。
 */
public final class ApprovalOnTimeoutAction {

    /** 超时自动通过（继续成功分支）。 */
    public static final String APPROVE      = "APPROVE";

    /** 超时自动拒绝（进入失败分支或终止）。 */
    public static final String REJECT       = "REJECT";

    /** 保持等待（由 watchdog 后续继续检查）。 */
    public static final String KEEP_WAITING = "KEEP_WAITING";

    /**
     * 构造 ApprovalOnTimeoutAction 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
// 使用字符串常量便于在审批记录表中直接阅读和排查。
    private ApprovalOnTimeoutAction() {}
}
