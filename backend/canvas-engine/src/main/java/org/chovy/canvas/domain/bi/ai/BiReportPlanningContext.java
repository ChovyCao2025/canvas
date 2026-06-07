package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;

import java.util.List;

public record BiReportPlanningContext(
        Long tenantId,
        String username,
        String role,
        BiReportRequest request,
        List<BiDatasetSpec> datasets,
        List<BiReportSectionInput> sections
) {
    public BiReportPlanningContext {
        tenantId = tenantId == null ? 0L : tenantId;
        username = username == null || username.isBlank() ? "system" : username;
        role = role == null || role.isBlank() ? "OPERATOR" : role;
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
        sections = sections == null ? List.of() : List.copyOf(sections);
    }
}
