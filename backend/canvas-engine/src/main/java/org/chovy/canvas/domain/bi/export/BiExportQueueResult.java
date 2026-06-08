package org.chovy.canvas.domain.bi.export;

import java.util.List;

/**
 * BiExportQueueResult 承载 domain.bi.export 场景中的不可变数据快照。
 * @param checked checked 字段。
 * @param processed processed 字段。
 * @param completed completed 字段。
 * @param failed failed 字段。
 * @param jobs jobs 字段。
 */
public record BiExportQueueResult(
        int checked,
        int processed,
        int completed,
        int failed,
        List<BiExportJobView> jobs) {
}
