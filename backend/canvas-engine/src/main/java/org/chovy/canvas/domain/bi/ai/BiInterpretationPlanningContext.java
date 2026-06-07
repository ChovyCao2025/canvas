package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

import java.util.List;

public record BiInterpretationPlanningContext(
        Long tenantId,
        String username,
        String role,
        BiInterpretationRequest request,
        List<BiDatasetSpec> datasets,
        BiQueryRequest query,
        BiQueryResult result
) {
    public BiInterpretationPlanningContext {
        tenantId = tenantId == null ? 0L : tenantId;
        username = username == null || username.isBlank() ? "system" : username;
        role = role == null || role.isBlank() ? "OPERATOR" : role;
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
}
