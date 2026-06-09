package org.chovy.canvas.domain.risk.governance;

import java.util.List;

/**
 * 风控策略版本差异视图。
 *
 * @param strategyKey 策略业务键
 * @param leftVersion 左侧版本号
 * @param rightVersion 右侧版本号
 * @param changes 差异摘要列表
 */
public record RiskStrategyDiffView(
        String strategyKey,
        int leftVersion,
        int rightVersion,
        List<String> changes
) {
}
