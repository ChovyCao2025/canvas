package org.chovy.canvas.web.risk;

/**
 * 风控策略审计写入接口。
 */
public interface RiskStrategyAuditSink {

    /**
     * 记录策略治理事件。
     */
    void record(Long tenantId, String eventType, String strategyKey, int version, String actor);
}
