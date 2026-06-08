package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiQuickEngineQueueSchedulerResult 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param expired expired 字段。
 * @param recovered recovered 字段。
 * @param claimed claimed 字段。
 * @param skipped skipped 字段。
 * @param wakeupJobs wakeupJobs 字段。
 */
public record BiQuickEngineQueueSchedulerResult(
        int expired,
        int recovered,
        int claimed,
        int skipped,
        List<BiQuickEngineQueueJobView> wakeupJobs) {

    public BiQuickEngineQueueSchedulerResult(int expired, int recovered, int claimed, int skipped) {
        this(expired, recovered, claimed, skipped, List.of());
    }

    public BiQuickEngineQueueSchedulerResult {
        wakeupJobs = wakeupJobs == null ? List.of() : List.copyOf(wakeupJobs);
    }
}
