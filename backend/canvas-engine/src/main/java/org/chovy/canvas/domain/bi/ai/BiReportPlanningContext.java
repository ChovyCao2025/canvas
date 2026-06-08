package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;

import java.util.List;

/**
 * BiReportPlanningContext 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param username username 字段。
 * @param role role 字段。
 * @param request request 字段。
 * @param datasets datasets 字段。
 * @param sections sections 字段。
 */
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
