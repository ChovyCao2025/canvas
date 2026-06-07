package org.chovy.canvas.domain.bi.ai;

import java.util.List;

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
