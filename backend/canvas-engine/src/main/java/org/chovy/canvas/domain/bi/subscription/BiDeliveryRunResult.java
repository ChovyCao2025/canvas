package org.chovy.canvas.domain.bi.subscription;

import java.util.List;

/**
 * BiDeliveryRunResult 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param jobType jobType 字段。
 * @param jobId jobId 字段。
 * @param jobKey jobKey 字段。
 * @param status status 字段。
 * @param logs logs 字段。
 */
public record BiDeliveryRunResult(
        String jobType,
        Long jobId,
        String jobKey,
        String status,
        List<BiDeliveryLogView> logs
) {
    public BiDeliveryRunResult {
        logs = logs == null ? List.of() : List.copyOf(logs);
    }
}
