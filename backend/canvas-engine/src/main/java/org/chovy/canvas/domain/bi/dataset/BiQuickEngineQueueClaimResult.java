package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiQuickEngineQueueClaimResult 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param expired expired 字段。
 * @param claimed claimed 字段。
 * @param jobs jobs 字段。
 */
public record BiQuickEngineQueueClaimResult(
        int expired,
        int claimed,
        List<BiQuickEngineQueueJobView> jobs
) {
    public BiQuickEngineQueueClaimResult {
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
    }
}
