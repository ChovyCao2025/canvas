package org.chovy.canvas.domain.bi.ai;

import java.util.List;

/**
 * BiInterpretationPlan 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param summary summary 字段。
 * @param keyFindings keyFindings 字段。
 * @param recommendations recommendations 字段。
 */
public record BiInterpretationPlan(
        String summary,
        List<String> keyFindings,
        List<String> recommendations
) {
    public BiInterpretationPlan {
        keyFindings = keyFindings == null ? List.of() : List.copyOf(keyFindings);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
