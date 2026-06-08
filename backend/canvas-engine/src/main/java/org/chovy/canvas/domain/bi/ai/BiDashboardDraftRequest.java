package org.chovy.canvas.domain.bi.ai;

import java.util.Map;

/**
 * BiDashboardDraftRequest 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param prompt prompt 字段。
 * @param datasetKey datasetKey 字段。
 * @param providerId providerId 字段。
 * @param templateId templateId 字段。
 * @param modelKey modelKey 字段。
 * @param timeoutMs timeoutMs 字段。
 * @param params params 字段。
 */
public record BiDashboardDraftRequest(
        String prompt,
        String datasetKey,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        Map<String, Object> params
) {
    public BiDashboardDraftRequest {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
