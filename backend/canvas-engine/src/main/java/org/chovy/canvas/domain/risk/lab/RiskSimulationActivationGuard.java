package org.chovy.canvas.domain.risk.lab;

/**
 * 风控仿真激活保护接口，用于显式隔离仿真和线上激活动作。
 */
public interface RiskSimulationActivationGuard {

    /**
     * 激活指定策略版本；仿真流程不应调用该方法。
     */
    void activate(Long tenantId, String strategyKey, int version);
}
