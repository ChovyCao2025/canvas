package org.chovy.canvas.domain.bi.ai;

import java.util.List;
import java.util.Map;

/**
 * BiReportRequest 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param reportType reportType 字段。
 * @param title title 字段。
 * @param sections sections 字段。
 * @param providerId providerId 字段。
 * @param templateId templateId 字段。
 * @param modelKey modelKey 字段。
 * @param timeoutMs timeoutMs 字段。
 * @param params params 字段。
 */
public record BiReportRequest(
        String reportType,
        String title,
        List<BiReportSectionInput> sections,
        Long providerId,
        Long templateId,
        String modelKey,
        Integer timeoutMs,
        Map<String, Object> params
) {
    public BiReportRequest {
        sections = sections == null ? List.of() : List.copyOf(sections);
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
