package org.chovy.canvas.risk.domain.dsl;

/**
 * 风控策略运行模式。
 */
public enum RiskRuntimeMode {
    /** 离线仿真，不影响线上结果。 */
    SIMULATION,
    /** 只打标记录，不改变线上动作。 */
    MARK,
    /** 影子运行，产出建议但不强制执行。 */
    SHADOW,
    /** 双跑模式，对比基线和候选策略。 */
    DUAL_RUN,
    /** 金丝雀模式，按流量比例选择候选策略。 */
    CANARY,
    /** 强制执行模式，决策直接影响业务流程。 */
    ENFORCE,
    /** 暂停模式，策略不再参与执行。 */
    PAUSED
}
