package org.chovy.canvas.domain.search;

import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;

import java.util.List;
import java.util.Map;

/**
 * SearchMarketingProviderSyncResult 承载 domain.search 场景中的不可变数据快照。
 * @param success success 字段。
 * @param providerRequestId providerRequestId 字段。
 * @param errorCode errorCode 字段。
 * @param errorMessage errorMessage 字段。
 * @param retryable retryable 字段。
 * @param requestedCount requestedCount 字段。
 * @param performanceRows performanceRows 字段。
 * @param urlInspectionRows urlInspectionRows 字段。
 * @param evidence evidence 字段。
 */
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

    /**
     * success 处理 domain.search 场景的业务逻辑。
     * @param providerRequestId 业务对象 ID，用于定位具体记录。
     * @param performanceRows performance rows 参数，用于 success 流程中的校验、计算或对象转换。
     * @param urlInspectionRows url inspection rows 参数，用于 success 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 success 流程中的校验、计算或对象转换。
     * @return 返回 success 流程生成的业务结果。
     */
    public static SearchMarketingProviderSyncResult success(String providerRequestId,
                                                            List<SearchMarketingPerformanceRow> performanceRows,
                                                            List<SearchMarketingUrlInspectionRow> urlInspectionRows,
                                                            Map<String, Object> evidence) {
        int count = (performanceRows == null ? 0 : performanceRows.size())
                + (urlInspectionRows == null ? 0 : urlInspectionRows.size());
        return new SearchMarketingProviderSyncResult(true, providerRequestId, null, null, false, count,
                performanceRows, urlInspectionRows, evidence);
    }

    /**
     * failure 处理 domain.search 场景的业务逻辑。
     * @param errorCode 业务编码，用于匹配对应类型或状态。
     * @param errorMessage error message 参数，用于 failure 流程中的校验、计算或对象转换。
     * @param retryable retryable 参数，用于 failure 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 failure 流程中的校验、计算或对象转换。
     * @return 返回 failure 流程生成的业务结果。
     */
    public static SearchMarketingProviderSyncResult failure(String errorCode,
                                                            String errorMessage,
                                                            boolean retryable,
                                                            Map<String, Object> evidence) {
        return new SearchMarketingProviderSyncResult(false, null, errorCode, errorMessage, retryable, 0,
                List.of(), List.of(), evidence);
    }
}
