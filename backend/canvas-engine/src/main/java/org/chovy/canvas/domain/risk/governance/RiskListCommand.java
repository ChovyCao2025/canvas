package org.chovy.canvas.domain.risk.governance;

import org.chovy.canvas.domain.risk.dsl.RiskSubjectType;
import org.chovy.canvas.domain.risk.runtime.RiskListType;

/**
 * 创建风控名单命令。
 *
 * @param listKey 名单业务键
 * @param listType 名单类型
 * @param subjectType 主体类型
 * @param requiresApproval 是否需要审批
 */
public record RiskListCommand(
        String listKey,
        RiskListType listType,
        RiskSubjectType subjectType,
        boolean requiresApproval
) {
}
