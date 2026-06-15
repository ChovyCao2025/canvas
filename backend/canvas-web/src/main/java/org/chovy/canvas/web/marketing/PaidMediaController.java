package org.chovy.canvas.web.marketing;

import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.marketing.api.PaidMediaFacade;
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
@RequestMapping("/canvas/paid-media/audience-sync")
public class PaidMediaController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final PaidMediaFacade facade;

    public PaidMediaController(PaidMediaFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/destinations")
    public Mono<CompatibilityEnvelope<PaidMediaFacade.DestinationView>> upsertDestination(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) PaidMediaFacade.DestinationCommand command) {
        return envelope(() -> facade.upsertDestination(tenantIdOrDefault(tenantId), command, actorOrDefault(actor)));
    }

    @PostMapping("/runs")
    public Mono<CompatibilityEnvelope<PaidMediaFacade.SyncRunView>> syncAudience(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) PaidMediaFacade.SyncCommand command) {
        return envelope(() -> facade.syncAudience(tenantIdOrDefault(tenantId), command, actorOrDefault(actor)));
    }

    @GetMapping("/runs")
    public Mono<CompatibilityEnvelope<List<PaidMediaFacade.SyncRunView>>> runs(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long destinationId,
            @RequestParam(required = false) Long audienceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.runs(tenantIdOrDefault(tenantId),
                new PaidMediaFacade.RunQuery(destinationId, audienceId, status, boundedLimit(limit))));
    }

    @GetMapping("/runs/{runId}/members")
    public Mono<CompatibilityEnvelope<List<PaidMediaFacade.MemberView>>> members(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long runId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return envelope(() -> facade.members(tenantIdOrDefault(tenantId),
                new PaidMediaFacade.MemberQuery(runId, status, boundedLimit(limit))));
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
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor;
    }

    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
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
