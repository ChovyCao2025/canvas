package org.chovy.canvas.domain.risk.dsl;

/**
 * 风控特征在线可用性。
 */
public enum RiskFeatureAvailability {
    /** 在线决策链路可实时读取。 */
    ONLINE,
    /** 仅离线分析或仿真可用。 */
    OFFLINE_ONLY
}
