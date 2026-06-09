package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingIntegrationContractProbeRunView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param contractId contractId 字段。
 * @param contractKey contractKey 字段。
 * @param providerFamily providerFamily 字段。
 * @param environment environment 字段。
 * @param probeKey probeKey 字段。
 * @param status status 字段。
 * @param httpStatusCode httpStatusCode 字段。
 * @param latencyMs latencyMs 字段。
 * @param problemTypeUri problemTypeUri 字段。
 * @param errorMessage errorMessage 字段。
 * @param summary summary 字段。
 * @param evidence evidence 字段。
 * @param observedAt observedAt 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record MarketingIntegrationContractProbeRunView(
        Long id,
        Long tenantId,
        Long contractId,
        String contractKey,
        String providerFamily,
        String environment,
        String probeKey,
        String status,
        Integer httpStatusCode,
        Long latencyMs,
        String problemTypeUri,
        String errorMessage,
        String summary,
        Map<String, Object> evidence,
        String observedAt,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
