package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

import java.util.Map;

/**
 * BiInterpretationRequest 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param question question 字段。
 * @param subjectType subjectType 字段。
 * @param subjectKey subjectKey 字段。
 * @param query query 字段。
 * @param result result 字段。
 * @param providerId providerId 字段。
 * @param templateId templateId 字段。
 * @param modelKey modelKey 字段。
 * @param timeoutMs timeoutMs 字段。
 * @param params params 字段。
 */
public record BiInterpretationRequest(
        String question,
        String subjectType,
        String subjectKey,
        BiQueryRequest query,
        BiQueryResult result,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        Map<String, Object> params
) {
    public BiInterpretationRequest {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
