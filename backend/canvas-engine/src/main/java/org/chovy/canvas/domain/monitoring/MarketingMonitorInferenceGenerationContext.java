package org.chovy.canvas.domain.monitoring;

import org.chovy.canvas.dal.dataobject.MarketingMonitorItemDO;

import java.util.Map;

/**
 * MarketingMonitorInferenceGenerationContext 承载 domain.monitoring 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param item item 字段。
 * @param command command 字段。
 * @param promptContext promptContext 字段。
 * @param inputHash inputHash 字段。
 * @param promptHash promptHash 字段。
 */
public record MarketingMonitorInferenceGenerationContext(
        Long tenantId,
        MarketingMonitorItemDO item,
        MarketingMonitorInferenceCommand command,
        Map<String, Object> promptContext,
        String inputHash,
        String promptHash) {
}
