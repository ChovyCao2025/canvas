package org.chovy.canvas.domain.bi.ai;

import java.util.List;

/**
 * BiInterpretationResponse 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param status status 字段。
 * @param fallbackUsed fallbackUsed 字段。
 * @param summary summary 字段。
 * @param keyFindings keyFindings 字段。
 * @param recommendations recommendations 字段。
 */
public record BiInterpretationResponse(
        String status,
        boolean fallbackUsed,
        String summary,
        List<String> keyFindings,
        List<String> recommendations
) {
    public BiInterpretationResponse {
        keyFindings = keyFindings == null ? List.of() : List.copyOf(keyFindings);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
