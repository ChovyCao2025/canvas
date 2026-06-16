package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiSelfServiceExportQueueResult 结果。
 */
public record BiSelfServiceExportQueueResult(
        /**
         * checked 字段值。
         */
        int checked,
        /**
         * processed 字段值。
         */
        int processed,
        /**
         * skipped 字段值。
         */
        int skipped,
        List<BiSelfServiceExportJobView> jobs) {

    public BiSelfServiceExportQueueResult {
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
    }
}
