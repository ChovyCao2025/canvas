package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseAvailabilityFacade;
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
@RequestMapping("/warehouse/availability")
public class CdpWarehouseAvailabilityController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final CdpWarehouseAvailabilityFacade facade;

    public CdpWarehouseAvailabilityController(CdpWarehouseAvailabilityFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> availability(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "HYBRID") String mode) {
        return envelope(() -> facade.availability(tenantIdOrDefault(tenantId), from, to, mode));
    }

    @PostMapping("/assets")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> recordAssetAvailability(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.recordAssetAvailability(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/assets")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listAssetAvailability(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String assetKey,
            @RequestParam(required = false) String mode,
            @RequestParam(defaultValue = "50") Integer limit) {
        return envelope(() -> facade.listAssetAvailability(tenantIdOrDefault(tenantId), assetType, assetKey, mode,
                limit));
    }

    @PostMapping("/contracts")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertContract(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertContract(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/contracts")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listContracts(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String consumerType,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.listContracts(tenantIdOrDefault(tenantId), consumerType, status));
    }

    @PostMapping("/contracts/{contractKey}/evaluate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> evaluateContract(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String contractKey,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return envelope(() -> facade.evaluateContract(tenantIdOrDefault(tenantId), contractKey, from, to));
    }

    @PostMapping("/incidents/scan")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> scanWarehouseIncidents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.scanWarehouseIncidents(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/consumer-incidents/scan")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> scanConsumerIncidents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.scanConsumerIncidents(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
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
