package org.chovy.canvas.platform.domain;

/**
 * 表示技术迁移候选项当前的人工决策状态。
 */
public enum TechnicalMigrationDecisionStatus {
    /**
     * 迁移仍被阻塞，需要人工复核后才能继续。
     */
    BLOCKED_PENDING_REVIEW,

    /**
     * 迁移已通过评审，可以进入子规格执行阶段。
     */
    APPROVED_FOR_CHILD_SPEC
}
