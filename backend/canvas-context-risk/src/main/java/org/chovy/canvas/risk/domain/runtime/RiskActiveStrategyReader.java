package org.chovy.canvas.risk.domain.runtime;

/**
 * 活跃风控策略读取器，供在线决策链路按租户和场景获取当前策略。
 */
@FunctionalInterface
public interface RiskActiveStrategyReader {

    /**
     * 查找指定租户和场景的活跃编译策略，未配置时返回空值。
     */
    RiskCompiledStrategy findActiveStrategy(Long tenantId, String sceneKey);
}
