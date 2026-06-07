package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

public record BiInsightPlanningContext(
        Long tenantId,
        String username,
        String role,
        BiInsightRequest request,
        BiDatasetSpec dataset,
        BiQueryRequest query,
        BiQueryResult currentResult,
        BiQueryResult baselineResult
) {
    public BiInsightPlanningContext {
        tenantId = tenantId == null ? 0L : tenantId;
        username = username == null || username.isBlank() ? "system" : username;
        role = role == null || role.isBlank() ? "OPERATOR" : role;
    }
}
