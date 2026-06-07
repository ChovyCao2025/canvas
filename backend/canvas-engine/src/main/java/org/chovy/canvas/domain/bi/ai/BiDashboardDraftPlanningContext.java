package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;

import java.util.List;

public record BiDashboardDraftPlanningContext(
        Long tenantId,
        String username,
        String role,
        BiDashboardDraftRequest request,
        List<BiDatasetSpec> datasets
) {
    public BiDashboardDraftPlanningContext {
        tenantId = tenantId == null ? 0L : tenantId;
        username = username == null || username.isBlank() ? "system" : username;
        role = role == null || role.isBlank() ? "OPERATOR" : role;
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
}
