package org.chovy.canvas.web.cdp;

import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.RealtimeAudienceFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/cdp")
public class RealtimeAudienceController {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String DEFAULT_ACTOR = "system";

    private final RealtimeAudienceFacade facade;

    public RealtimeAudienceController(RealtimeAudienceFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/realtime-audiences/{audienceId}/events")
    public Mono<CompatibilityEnvelope<RealtimeAudienceFacade.EventResult>> processEvent(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long audienceId,
            @RequestBody EventPayload payload) {
        return envelope(() -> facade.processEvent(tenantIdOrDefault(tenantId), audienceId, payload.toEvent(),
                payload.removeOnNoMatchOrDefault()));
    }

    @PostMapping("/realtime-audiences/{audienceId}/snapshot")
    public Mono<CompatibilityEnvelope<RealtimeAudienceFacade.SnapshotResult>> createSnapshot(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long audienceId) {
        return envelope(() -> facade.createSnapshot(tenantIdOrDefault(tenantId), audienceId, "MANUAL",
                actorOrDefault(actor)));
    }

    @GetMapping("/realtime-audiences/{audienceId}/snapshots")
    public Mono<CompatibilityEnvelope<java.util.List<RealtimeAudienceFacade.SnapshotRow>>> listSnapshots(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long audienceId,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listSnapshots(tenantIdOrDefault(tenantId), audienceId, normalizeLimit(limit)));
    }

    @GetMapping("/audiences/{leftId}/overlap/{rightId}")
    public Mono<CompatibilityEnvelope<RealtimeAudienceFacade.OverlapResult>> overlap(@PathVariable Long leftId,
                                                                                      @PathVariable Long rightId) {
        return envelope(() -> facade.overlap(leftId, rightId));
    }

    @PostMapping("/audiences/merge")
    public Mono<CompatibilityEnvelope<RealtimeAudienceFacade.SetOperationResult>> merge(@RequestParam Long leftId,
                                                                                        @RequestParam Long rightId) {
        return envelope(() -> facade.merge(leftId, rightId));
    }

    @PostMapping("/audiences/exclude")
    public Mono<CompatibilityEnvelope<RealtimeAudienceFacade.SetOperationResult>> exclude(@RequestParam Long baseId,
                                                                                          @RequestParam Long excludedId) {
        return envelope(() -> facade.exclude(baseId, excludedId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 500);
    }

    private record EventPayload(String sourceEventId, String userId, Instant eventTime,
                                Map<String, Object> properties, Boolean removeOnNoMatch) {
        private RealtimeAudienceFacade.CdpEvent toEvent() {
            return new RealtimeAudienceFacade.CdpEvent(sourceEventId, userId,
                    eventTime == null ? Instant.now() : eventTime, properties == null ? Map.of() : properties);
        }

        private boolean removeOnNoMatchOrDefault() {
            return removeOnNoMatch == null || removeOnNoMatch;
        }
    }

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
