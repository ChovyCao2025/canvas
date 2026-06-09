package org.chovy.canvas.domain.risk.governance;

/**
 * 风控策略状态流转请求。
 *
 * @param reason 流转原因
 * @param targetVersion 目标版本，回滚等操作使用
 */
public record RiskStrategyTransitionRequest(
        String reason,
        Integer targetVersion
) {
}
