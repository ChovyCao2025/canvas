package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * SearchMarketingReadinessView 承载 domain.search 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param status status 字段。
 * @param blockers blockers 字段。
 * @param evidence evidence 字段。
 * @param evaluatedAt evaluatedAt 字段。
 */
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
