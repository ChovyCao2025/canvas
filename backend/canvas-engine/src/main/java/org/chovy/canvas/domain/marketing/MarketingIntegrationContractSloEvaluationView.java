package org.chovy.canvas.domain.marketing;

import java.util.List;

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
