package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SearchMarketingReadinessView(
        Long tenantId,
        String status,
        List<String> blockers,
        Map<String, Object> evidence,
        LocalDateTime evaluatedAt) {

    public SearchMarketingReadinessView {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
