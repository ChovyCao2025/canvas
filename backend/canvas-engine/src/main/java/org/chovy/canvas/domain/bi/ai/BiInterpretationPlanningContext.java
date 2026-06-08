package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

import java.util.List;

/**
 * BiInterpretationPlanningContext 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param username username 字段。
 * @param role role 字段。
 * @param request request 字段。
 * @param datasets datasets 字段。
 * @param query query 字段。
 * @param result result 字段。
 */
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
