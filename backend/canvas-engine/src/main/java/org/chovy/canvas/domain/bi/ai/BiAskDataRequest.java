package org.chovy.canvas.domain.bi.ai;

import java.util.Map;

/**
 * BiAskDataRequest 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param question question 字段。
 * @param datasetKey datasetKey 字段。
 * @param providerId providerId 字段。
 * @param templateId templateId 字段。
 * @param modelKey modelKey 字段。
 * @param timeoutMs timeoutMs 字段。
 * @param limit limit 字段。
 * @param params params 字段。
 */
public record BiAskDataRequest(
        String question,
        String datasetKey,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        int limit,
        Map<String, Object> params
) {
    public BiAskDataRequest {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
