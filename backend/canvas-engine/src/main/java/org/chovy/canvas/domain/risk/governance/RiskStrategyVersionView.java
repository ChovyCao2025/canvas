package org.chovy.canvas.domain.risk.governance;

/**
 * 风控策略版本视图。
 *
 * @param tenantId 租户编号
 * @param strategyKey 策略业务键
 * @param version 版本号
 * @param status 生命周期状态
 * @param definitionJson 策略定义 JSON
 * @param validationJson 校验结果 JSON
 * @param createdBy 创建人
 * @param submittedBy 提交人
 * @param approvedBy 审批人
 */
public record RiskStrategyVersionView(
        Long tenantId,
        String strategyKey,
        int version,
        RiskStrategyLifecycleStatus status,
        String definitionJson,
        String validationJson,
        String createdBy,
        String submittedBy,
        String approvedBy
) {
}
