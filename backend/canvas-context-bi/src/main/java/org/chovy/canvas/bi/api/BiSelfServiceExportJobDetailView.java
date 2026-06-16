package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiSelfServiceExportJobDetailView 视图。
 */
public record BiSelfServiceExportJobDetailView(
        /**
         * job 字段值。
         */
        BiSelfServiceExportJobView job,
        /**
         * partition 字段值。
         */
        Map<String, Object> partition,
        Map<String, Object> audit) {

    public BiSelfServiceExportJobDetailView {
        partition = partition == null ? Map.of() : Map.copyOf(partition);
        audit = audit == null ? Map.of() : Map.copyOf(audit);
    }
}
