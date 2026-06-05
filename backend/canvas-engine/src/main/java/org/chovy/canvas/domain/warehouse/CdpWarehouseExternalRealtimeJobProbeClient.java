package org.chovy.canvas.domain.warehouse;

public interface CdpWarehouseExternalRealtimeJobProbeClient {

    ProbeResult probe(ProbeTarget target);

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

    record ProbeResult(
            String runtimeStatus,
            String message,
            String payloadJson,
            String engineJobId) {
    }
}
