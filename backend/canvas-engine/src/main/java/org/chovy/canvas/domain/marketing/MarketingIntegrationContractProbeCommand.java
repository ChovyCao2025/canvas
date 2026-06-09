package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingIntegrationContractProbeCommand 承载 domain.marketing 场景中的不可变数据快照。
 * @param probeKey probeKey 字段。
 * @param environment environment 字段。
 * @param status status 字段。
 * @param httpStatusCode httpStatusCode 字段。
 * @param latencyMs latencyMs 字段。
 * @param errorType errorType 字段。
 * @param problemTypeUri problemTypeUri 字段。
 * @param problemTitle problemTitle 字段。
 * @param problemDetail problemDetail 字段。
 * @param observedAt observedAt 字段。
 * @param evidence evidence 字段。
 */
public record MarketingIntegrationContractProbeCommand(
        String probeKey,
        String environment,
        String status,
        Integer httpStatusCode,
        Long latencyMs,
        String errorType,
        String problemTypeUri,
        String problemTitle,
        String problemDetail,
        LocalDateTime observedAt,
        Map<String, Object> evidence) {
}
