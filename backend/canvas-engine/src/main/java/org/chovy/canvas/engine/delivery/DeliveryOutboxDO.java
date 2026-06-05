package org.chovy.canvas.engine.delivery;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
public class DeliveryOutboxDO {
    private Long id;
    private Long tenantId;
    private Long messageSendRecordId;
    private String executionId;
    private Long canvasId;
    private String userId;
    private String nodeId;
    private String channel;
    private String provider;
    private String payloadJson;
    private String idempotencyKey;
    private String status;
    private int attemptCount;
    private LocalDateTime nextRetryAt;
    private String lockedBy;
    private LocalDateTime lockedAt;
    private String providerMessageId;
    private String providerResponseJson;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean duplicate;

    public DeliveryOutboxDO markDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
        return this;
    }
}
