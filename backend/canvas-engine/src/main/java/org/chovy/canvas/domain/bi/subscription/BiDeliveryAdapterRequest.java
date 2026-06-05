package org.chovy.canvas.domain.bi.subscription;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record BiDeliveryAdapterRequest(
        Long tenantId,
        Long workspaceId,
        String jobType,
        Long jobId,
        String jobKey,
        String resourceType,
        Long resourceId,
        String channel,
        Map<String, Object> receiver,
        Map<String, Object> payload,
        BigDecimal metricValue,
        String triggeredBy,
        List<BiEmailAttachment> attachments
) {
    public BiDeliveryAdapterRequest(Long tenantId,
                                    Long workspaceId,
                                    String jobType,
                                    Long jobId,
                                    String jobKey,
                                    String resourceType,
                                    Long resourceId,
                                    String channel,
                                    Map<String, Object> receiver,
                                    Map<String, Object> payload,
                                    BigDecimal metricValue,
                                    String triggeredBy) {
        this(tenantId,
                workspaceId,
                jobType,
                jobId,
                jobKey,
                resourceType,
                resourceId,
                channel,
                receiver,
                payload,
                metricValue,
                triggeredBy,
                List.of());
    }

    public BiDeliveryAdapterRequest {
        receiver = receiver == null ? Map.of() : Map.copyOf(receiver);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
