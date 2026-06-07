package org.chovy.canvas.domain.monitoring;

import org.chovy.canvas.dal.dataobject.MarketingMonitorItemDO;

import java.util.Map;

public record MarketingMonitorInferenceGenerationContext(
        Long tenantId,
        MarketingMonitorItemDO item,
        MarketingMonitorInferenceCommand command,
        Map<String, Object> promptContext,
        String inputHash,
        String promptHash) {
}
