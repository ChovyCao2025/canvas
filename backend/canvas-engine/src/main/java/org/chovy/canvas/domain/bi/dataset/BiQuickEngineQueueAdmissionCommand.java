package org.chovy.canvas.domain.bi.dataset;

/**
 * BiQuickEngineQueueAdmissionCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param poolKey poolKey 字段。
 * @param sqlHash sqlHash 字段。
 * @param datasetKey datasetKey 字段。
 * @param requestedBy requestedBy 字段。
 * @param queueTimeoutSeconds queueTimeoutSeconds 字段。
 */
public record BiQuickEngineQueueAdmissionCommand(
        String poolKey,
        String sqlHash,
        String datasetKey,
        String requestedBy,
        Integer queueTimeoutSeconds) {
}
