package org.chovy.canvas.domain.bi.export;

import java.util.List;

/**
 * BiExportRetryResult 承载 domain.bi.export 场景中的不可变数据快照。
 * @param checked checked 字段。
 * @param retried retried 字段。
 * @param completed completed 字段。
 * @param failed failed 字段。
 * @param jobs jobs 字段。
 */
public record BiExportRetryResult(
        int checked,
        int retried,
        int completed,
        int failed,
        List<BiExportJobView> jobs
) {
    public BiExportRetryResult {
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
    }
}
