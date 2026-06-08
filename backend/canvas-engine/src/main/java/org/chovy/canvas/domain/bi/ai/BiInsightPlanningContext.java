package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

/**
 * BiInsightPlanningContext 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param username username 字段。
 * @param role role 字段。
 * @param request request 字段。
 * @param dataset dataset 字段。
 * @param query query 字段。
 * @param currentResult currentResult 字段。
 * @param baselineResult baselineResult 字段。
 */
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
