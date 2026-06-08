package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

import java.util.Map;

/**
 * BiInsightRequest 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param question question 字段。
 * @param query query 字段。
 * @param currentResult currentResult 字段。
 * @param baselineResult baselineResult 字段。
 * @param providerId providerId 字段。
 * @param templateId templateId 字段。
 * @param modelKey modelKey 字段。
 * @param timeoutMs timeoutMs 字段。
 * @param params params 字段。
 */
public record BiInsightRequest(
        String question,
        BiQueryRequest query,
        BiQueryResult currentResult,
        BiQueryResult baselineResult,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        Map<String, Object> params
) {
    public BiInsightRequest {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
