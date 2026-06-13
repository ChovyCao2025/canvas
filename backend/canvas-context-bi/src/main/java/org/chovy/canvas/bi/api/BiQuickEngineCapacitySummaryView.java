package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiQuickEngineCapacitySummaryView(
        Long tenantId,
        Long capacityLimitRows,
        Long usedRows,
        Double usagePercent,
        String alertLevel,
        Boolean alertEnabled,
        Map<String, Object> alertPolicy,
        BiQuickEnginePoolView tenantPoolPolicy,
        Map<String, Object> concurrencyQueue,
        List<Map<String, Object>> categories,
        List<Map<String, Object>> details,
        List<Map<String, Object>> userRankings) {
}
