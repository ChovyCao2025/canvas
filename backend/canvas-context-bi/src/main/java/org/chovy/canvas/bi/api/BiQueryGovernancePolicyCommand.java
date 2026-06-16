package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiQueryGovernancePolicyCommand 命令。
 */
public record BiQueryGovernancePolicyCommand(
        /**
         * defaultTimeoutMs 对应的数据集合。
         */
        Long defaultTimeoutMs,
        /**
         * defaultQuotaRows 对应的数据集合。
         */
        Integer defaultQuotaRows,
        List<Map<String, Object>> datasets) {

    public BiQueryGovernancePolicyCommand {
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
}
