package org.chovy.canvas.domain.bi.dataset;

/**
 * BiQuickEngineQueueRecoveryResult 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param expired expired 字段。
 * @param recovered recovered 字段。
 */
public record BiQuickEngineQueueRecoveryResult(
        int expired,
        int recovered) {
}
