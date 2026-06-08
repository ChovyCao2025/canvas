package org.chovy.canvas.domain.bi.export;

import java.util.Map;

/**
 * BiExportJobDetailView 承载 domain.bi.export 场景中的不可变数据快照。
 * @param job job 字段。
 * @param request request 字段。
 * @param partition partition 字段。
 */
public record BiExportJobDetailView(
        BiExportJobView job,
        BiExportJobCommand request,
        Map<String, Object> partition) {

    /**
     * 转换为接口返回或领域视图。
     *
     * @param job job 参数，用于 BiExportJobDetailView 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     */
    public BiExportJobDetailView(BiExportJobView job, BiExportJobCommand request) {
        this(job, request, Map.of());
    }

    public BiExportJobDetailView {
        partition = partition == null ? Map.of() : Map.copyOf(partition);
    }
}
