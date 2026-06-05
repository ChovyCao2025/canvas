package org.chovy.canvas.domain.ops;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OpsAuditEventService {

    private static final int MAX_EVENTS = 500;

    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentLinkedDeque<OpsAuditEvent> events = new ConcurrentLinkedDeque<>();

    public OpsAuditEvent record(Long tenantId,
                                String action,
                                Long canvasId,
                                String operator,
                                String role,
                                String reason) {
        OpsAuditEvent event = new OpsAuditEvent(
                "ops-" + sequence.incrementAndGet(),
                tenantId,
                action,
                canvasId,
                operator,
                role,
                reason,
                Instant.now());
        events.addFirst(event);
        while (events.size() > MAX_EVENTS) {
            events.pollLast();
        }
        return event;
    }

    public List<OpsAuditEvent> recent(Long tenantId, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        List<OpsAuditEvent> result = new ArrayList<>();
        for (OpsAuditEvent event : events) {
            if (tenantId == null || tenantId.equals(event.tenantId())) {
                result.add(event);
            }
            if (result.size() >= normalizedLimit) {
                break;
            }
        }
        return result;
    }

    public record OpsAuditEvent(
            String id,
            Long tenantId,
            String action,
            Long canvasId,
            String operator,
            String role,
            String reason,
            Instant createdAt) {
    }
}
