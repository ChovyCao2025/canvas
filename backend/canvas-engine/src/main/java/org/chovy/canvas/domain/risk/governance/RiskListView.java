package org.chovy.canvas.domain.risk.governance;

import org.chovy.canvas.domain.risk.dsl.RiskSubjectType;
import org.chovy.canvas.domain.risk.runtime.RiskListType;

/**
 * 风控名单视图。
 *
 * @param tenantId 租户编号
 * @param listKey 名单业务键
 * @param listType 名单类型
 * @param subjectType 主体类型
 * @param status 名单状态
 * @param requiresApproval 是否需要审批
 * @param owner 负责人
 */
public record RiskListView(
        Long tenantId,
        String listKey,
        RiskListType listType,
        RiskSubjectType subjectType,
        String status,
        boolean requiresApproval,
        String owner
) {
}
