package org.chovy.canvas.domain.risk.governance;

/**
 * 风控策略版本生命周期状态。
 */
public enum RiskStrategyLifecycleStatus {
    /** 草稿。 */
    DRAFT,
    /** 已校验。 */
    VALIDATED,
    /** 已仿真。 */
    SIMULATED,
    /** 等待审批。 */
    APPROVAL_PENDING,
    /** 已激活。 */
    ACTIVE,
    /** 已暂停。 */
    PAUSED,
    /** 已回滚。 */
    ROLLED_BACK,
    /** 已归档。 */
    ARCHIVED,
    /** 已拒绝。 */
    REJECTED,
    /** 校验或流程失败。 */
    FAILED
}
