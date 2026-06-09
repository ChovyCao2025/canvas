package org.chovy.canvas.domain.warehouse;

/**
 * CdpWarehouseExternalRealtimeJobProbeClient 定义 domain.warehouse 场景中的扩展契约。
 */
public interface CdpWarehouseExternalRealtimeJobProbeClient {

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
            String pipelineKey,
            String jobKey,
            String engineType,
            String endpointUrl,
            String authRef,
            String externalJobId,
            String connectorName,
            String configJson) {
    }

    /**
     * ProbeResult 数据记录。
     */
    record ProbeResult(
            String runtimeStatus,
            String message,
            String payloadJson,
            String engineJobId) {
    }
}
