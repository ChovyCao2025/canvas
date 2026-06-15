package org.chovy.canvas.web.marketing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.ProgrammaticDspFacade;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/canvas/programmatic-dsp")
public class ProgrammaticDspController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final ProgrammaticDspFacade facade;

    public ProgrammaticDspController(ProgrammaticDspFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/seats")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertSeat(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertSeat(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/campaigns")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertCampaign(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertCampaign(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/line-items")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertLineItem(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertLineItem(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/supply-paths")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertSupplyPath(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertSupplyPath(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/snapshots")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> recordSnapshot(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.recordSnapshot(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/summary")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> summary(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long seatId,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long lineItemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime evaluatedAt) {
        return envelope(() -> facade.summary(tenantIdOrDefault(tenantId), query(
                "seatId", seatId,
                "campaignId", campaignId,
                "lineItemId", lineItemId,
                "startDate", startDate,
                "endDate", endDate,
                "evaluatedAt", evaluatedAt)));
    }

    @PostMapping("/mutations")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> proposeMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.proposeMutation(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/mutations/{mutationId}/approve")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> approveMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long mutationId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.approveMutation(tenantIdOrDefault(tenantId), mutationId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/mutations/{mutationId}/execute")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> executeMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long mutationId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.executeMutation(tenantIdOrDefault(tenantId), mutationId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/mutations")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listMutations(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long seatId,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long lineItemId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listMutations(tenantIdOrDefault(tenantId), query(
                "seatId", seatId,
                "campaignId", campaignId,
                "lineItemId", lineItemId,
                "status", status,
                "approvalStatus", approvalStatus,
                "limit", limit)));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        });
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    private static Map<String, Object> query(Object... keysAndValues) {
        Map<String, Object> query = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            Object value = keysAndValues[i + 1];
            if (value != null) {
                query.put((String) keysAndValues[i], value);
            }
        }
        return query;
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    public record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}
