package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiSelfServiceExportRetryResult 结果。
 */
public record BiSelfServiceExportRetryResult(
        /**
         * checked 字段值。
         */
        int checked,
        /**
         * retried 字段值。
         */
        int retried,
        List<BiSelfServiceExportJobView> jobs) {

    public BiSelfServiceExportRetryResult {
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
    }
}
