package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;

import java.util.List;

public record BiAskDataPlanningContext(
        Long tenantId,
        String username,
        String role,
        String question,
        String requestedDatasetKey,
        List<BiDatasetSpec> datasets,
        BiAskDataRequest request
) {
    public BiAskDataPlanningContext {
        tenantId = tenantId == null ? 0L : tenantId;
        username = username == null || username.isBlank() ? "system" : username;
        role = role == null || role.isBlank() ? "OPERATOR" : role;
        question = question == null ? "" : question.trim();
        requestedDatasetKey = requestedDatasetKey == null || requestedDatasetKey.isBlank()
                ? null
                : requestedDatasetKey.trim();
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
}
