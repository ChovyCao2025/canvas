package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AiUsageAuditService {

    private final CopyOnWriteArrayList<AiUsageAuditEvent> events = new CopyOnWriteArrayList<>();

    public void record(AiUsageAuditEvent event) {
        events.add(event);
    }

    public List<AiUsageAuditEvent> recent() {
        return List.copyOf(events);
    }

    public record AiUsageAuditEvent(
            Instant createdAt,
            Long tenantId,
            Long canvasId,
            String executionId,
            String nodeId,
            Long providerId,
            Long templateId,
            String modelKey,
            String status,
            boolean fallbackUsed,
            long latencyMs,
            Integer promptTokens,
            Integer completionTokens,
            JsonNode output,
            String errorCode,
            String errorMessage) {
    }
}
