package org.chovy.canvas.domain.search;

import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;

import java.util.List;
import java.util.Map;

public record SearchMarketingProviderSyncResult(
        boolean success,
        String providerRequestId,
        String errorCode,
        String errorMessage,
        boolean retryable,
        long requestedCount,
        List<SearchMarketingPerformanceRow> performanceRows,
        List<SearchMarketingUrlInspectionRow> urlInspectionRows,
        Map<String, Object> evidence) {

    public SearchMarketingProviderSyncResult {
        performanceRows = performanceRows == null ? List.of() : List.copyOf(performanceRows);
        urlInspectionRows = urlInspectionRows == null ? List.of() : List.copyOf(urlInspectionRows);
        evidence = ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    public static SearchMarketingProviderSyncResult success(String providerRequestId,
                                                            List<SearchMarketingPerformanceRow> performanceRows,
                                                            List<SearchMarketingUrlInspectionRow> urlInspectionRows,
                                                            Map<String, Object> evidence) {
        int count = (performanceRows == null ? 0 : performanceRows.size())
                + (urlInspectionRows == null ? 0 : urlInspectionRows.size());
        return new SearchMarketingProviderSyncResult(true, providerRequestId, null, null, false, count,
                performanceRows, urlInspectionRows, evidence);
    }

    public static SearchMarketingProviderSyncResult failure(String errorCode,
                                                            String errorMessage,
                                                            boolean retryable,
                                                            Map<String, Object> evidence) {
        return new SearchMarketingProviderSyncResult(false, null, errorCode, errorMessage, retryable, 0,
                List.of(), List.of(), evidence);
    }
}
