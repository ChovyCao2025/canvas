package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiQuickEngineQueueSnapshotView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param poolKey poolKey 字段。
 * @param queued queued 字段。
 * @param claimed claimed 字段。
 * @param completed completed 字段。
 * @param blocked blocked 字段。
 * @param total total 字段。
 * @param jobs jobs 字段。
 */
public record BiQuickEngineQueueSnapshotView(
        Long tenantId,
        String poolKey,
        long queued,
        long claimed,
        long completed,
        long blocked,
        long total,
        List<BiQuickEngineQueueJobView> jobs) {
}
