package org.chovy.canvas.domain.marketing;

import java.util.Map;

/**
 * MarketingIntegrationContractProbeClient 定义 domain.marketing 场景中的扩展契约。
 */
public interface MarketingIntegrationContractProbeClient {

    /**
     * 执行 probe 流程，围绕 probe 完成校验、计算或结果组装。
     *
     * @param target target 参数，用于 probe 流程中的校验、计算或对象转换。
     * @return 返回 probe 流程生成的业务结果。
     */
    ProbeResult probe(ProbeTarget target);

    /**
     * ProbeTarget 数据记录。
     */
    record ProbeTarget(
            Long id,
            Long tenantId,
            String contractKey,
            String displayName,
            String providerFamily,
            String apiRoot,
            String authMode,
            Integer timeoutMs,
            Map<String, Object> schemaContract,
            Map<String, Object> metadata) {
    }

    /**
     * ProbeResult 数据记录。
     */
    record ProbeResult(
            String status,
            Integer httpStatusCode,
            Long latencyMs,
            String problemTypeUri,
            String errorMessage,
            String summary,
            Map<String, Object> evidence) {
    }
}
