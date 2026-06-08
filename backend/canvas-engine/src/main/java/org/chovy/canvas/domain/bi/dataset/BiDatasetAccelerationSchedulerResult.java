package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiDatasetAccelerationSchedulerResult 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param policiesChecked policiesChecked 字段。
 * @param refreshed refreshed 字段。
 * @param skipped skipped 字段。
 * @param failed failed 字段。
 * @param items items 字段。
 */
public record BiDatasetAccelerationSchedulerResult(
        int policiesChecked,
        int refreshed,
        int skipped,
        int failed,
        List<BiDatasetAccelerationSchedulerItem> items) {

    /**
     * 创建 BiDatasetAccelerationSchedulerResult 实例并注入 domain.bi.dataset 场景依赖。
     * @param policiesChecked policies checked 参数，用于 BiDatasetAccelerationSchedulerResult 流程中的校验、计算或对象转换。
     * @param refreshed refreshed 参数，用于 BiDatasetAccelerationSchedulerResult 流程中的校验、计算或对象转换。
     * @param skipped skipped 参数，用于 BiDatasetAccelerationSchedulerResult 流程中的校验、计算或对象转换。
     * @param failed failed 参数，用于 BiDatasetAccelerationSchedulerResult 流程中的校验、计算或对象转换。
     */
    public BiDatasetAccelerationSchedulerResult(int policiesChecked, int refreshed, int skipped, int failed) {
        this(policiesChecked, refreshed, skipped, failed, List.of());
    }

    public BiDatasetAccelerationSchedulerResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
