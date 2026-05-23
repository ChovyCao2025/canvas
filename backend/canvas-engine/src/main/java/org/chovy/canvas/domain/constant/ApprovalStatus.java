package org.chovy.canvas.domain.constant;

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

    // 状态值用字符串而非数字，便于审计日志直接阅读。
    private ApprovalStatus() {}
}
