package org.chovy.canvas.domain.marketing;

import java.util.Map;

/**
 * MarketingIntegrationContractProbeRunCommand 承载 domain.marketing 场景中的不可变数据快照。
 * @param probeKey probeKey 字段。
 * @param status status 字段。
 * @param httpStatusCode httpStatusCode 字段。
 * @param latencyMs latencyMs 字段。
 * @param problemTypeUri problemTypeUri 字段。
 * @param errorMessage errorMessage 字段。
 * @param summary summary 字段。
 * @param evidence evidence 字段。
 */
public record MarketingIntegrationContractProbeRunCommand(
        String probeKey,
        String status,
        Integer httpStatusCode,
        Long latencyMs,
        String problemTypeUri,
        String errorMessage,
        String summary,
        Map<String, Object> evidence) {
}
