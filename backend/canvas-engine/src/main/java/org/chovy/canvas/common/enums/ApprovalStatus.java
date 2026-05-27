package org.chovy.canvas.common.enums;

/**
 * 人工审批状态常量（`canvas_manual_approval.status`）。
 */
public final class ApprovalStatus {

    /** 等待审批。 */
    public static final String PENDING  = "PENDING";

    /** 审批通过。 */
    public static final String APPROVED = "APPROVED";

    /** 审批拒绝。 */
    public static final String REJECTED = "REJECTED";

    /** 审批超时。 */
    public static final String TIMEOUT  = "TIMEOUT";

    /**
     * 构造 ApprovalStatus 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
// 状态值用字符串而非数字，便于审计日志直接阅读。
    private ApprovalStatus() {}
}
