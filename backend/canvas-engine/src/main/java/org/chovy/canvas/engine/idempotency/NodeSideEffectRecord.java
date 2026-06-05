package org.chovy.canvas.engine.idempotency;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NodeSideEffectRecord {
    private Long id;
    private Long tenantId;
    private String executionId;
    private Long canvasId;
    private String nodeId;
    private String nodeType;
    private String operationKey;
    private String idempotencyKey;
    private String status;
    private int attemptCount;
    private String outputJson;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
