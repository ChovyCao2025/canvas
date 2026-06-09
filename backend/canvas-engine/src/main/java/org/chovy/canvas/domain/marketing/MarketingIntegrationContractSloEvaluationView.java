package org.chovy.canvas.domain.marketing;

import java.util.List;

/**
 * MarketingIntegrationContractSloEvaluationView 承载 domain.marketing 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param contractId contractId 字段。
 * @param contractKey contractKey 字段。
 * @param displayName displayName 字段。
 * @param providerFamily providerFamily 字段。
 * @param probeKey probeKey 字段。
 * @param status status 字段。
 * @param severity severity 字段。
 * @param triggeredRuleKey triggeredRuleKey 字段。
 * @param targetPercent targetPercent 字段。
 * @param errorBudget errorBudget 字段。
 * @param reason reason 字段。
 * @param generatedAt generatedAt 字段。
 * @param windows windows 字段。
 */
public record MarketingIntegrationContractSloEvaluationView(
        Long tenantId,
        Long contractId,
        String contractKey,
        String displayName,
        String providerFamily,
        String probeKey,
        String status,
        String severity,
        String triggeredRuleKey,
        Double targetPercent,
        Double errorBudget,
        String reason,
        String generatedAt,
        List<MarketingIntegrationContractSloWindowView> windows) {
}
