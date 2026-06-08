package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BiDatasetAccelerationPolicyView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param enabled enabled 字段。
 * @param accelerationMode accelerationMode 字段。
 * @param refreshMode refreshMode 字段。
 * @param refreshIntervalMinutes refreshIntervalMinutes 字段。
 * @param ttlSeconds ttlSeconds 字段。
 * @param maxRows maxRows 字段。
 * @param cronExpression cronExpression 字段。
 * @param materializedTable materializedTable 字段。
 * @param lastStatus lastStatus 字段。
 * @param lastRunId lastRunId 字段。
 * @param lastRefreshedAt lastRefreshedAt 字段。
 * @param recentRuns recentRuns 字段。
 */
public record BiDatasetAccelerationPolicyView(
        String datasetKey,
        Boolean enabled,
        String accelerationMode,
        String refreshMode,
        Long refreshIntervalMinutes,
        Long ttlSeconds,
        Long maxRows,
        String cronExpression,
        String materializedTable,
        String lastStatus,
        Long lastRunId,
        LocalDateTime lastRefreshedAt,
        List<BiDatasetExtractRefreshRunView> recentRuns) {

    public BiDatasetAccelerationPolicyView {
        recentRuns = recentRuns == null ? List.of() : List.copyOf(recentRuns);
    }
}
