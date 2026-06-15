package org.chovy.canvas.web.channels;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.platform.api.ChannelConnectorFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/channels/connectors")
public class ChannelConnectorController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final ChannelConnectorFacade facade;

    public ChannelConnectorController(ChannelConnectorFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> connectors(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.connectors(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/limits")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> limits(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.limits(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/{id}/mode")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> updateMode(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.updateMode(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/{id}/health-test")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> healthTest(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> facade.healthTest(tenantIdOrDefault(tenantId), id));
    }

    @PostMapping("/fallback/validate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> validateFallback(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.validateFallback(tenantIdOrDefault(tenantId), safePayload(payload)));
    }

    @GetMapping("/fallback/decisions")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> fallbackDecisions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.fallbackDecisions(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/dedupe-records")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> dedupeRecords(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.dedupeRecords(tenantIdOrDefault(tenantId)));
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

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
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
