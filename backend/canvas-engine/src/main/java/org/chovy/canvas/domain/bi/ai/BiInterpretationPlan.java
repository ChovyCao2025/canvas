package org.chovy.canvas.domain.bi.ai;

import java.util.List;

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
