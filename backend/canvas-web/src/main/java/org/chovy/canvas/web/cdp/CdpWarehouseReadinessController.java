package org.chovy.canvas.web.cdp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseReadinessFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessSectionView;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class CdpWarehouseReadinessController {

    private static final Long DEFAULT_TENANT_ID = 7L;

    private final CdpWarehouseReadinessFacade readinessFacade;

    public CdpWarehouseReadinessController(CdpWarehouseReadinessFacade readinessFacade) {
        this.readinessFacade = readinessFacade;
    }

    @GetMapping("/warehouse/readiness")
    public Mono<CompatibilityEnvelope<WarehouseReadinessCompatibilityView>> readiness(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> WarehouseReadinessCompatibilityView.from(
                readinessFacade.readiness(tenantIdOrDefault(tenantId))));
    }

    @PostMapping("/warehouse/readiness/incidents/scan")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> scanIncidents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> readinessFacade.scanIncidents(tenantIdOrDefault(tenantId)));
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

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    public record WarehouseReadinessCompatibilityView(
            Long tenantId,
            String status,
            LocalDateTime generatedAt,
            List<CdpWarehouseReadinessSectionView> sections,
            boolean productionReady,
            int blockerCount,
            List<WarehouseReadinessBlocker> blockers) {

        private static WarehouseReadinessCompatibilityView from(CdpWarehouseReadinessView view) {
            List<WarehouseReadinessBlocker> blockers = view.sections().stream()
                    .filter(section -> !"PASS".equals(section.status()))
                    .map(section -> new WarehouseReadinessBlocker(section.key(), section.status(), section.reason()))
                    .toList();
            return new WarehouseReadinessCompatibilityView(
                    view.tenantId(),
                    view.status(),
                    view.generatedAt(),
                    view.sections(),
                    "PASS".equals(view.status()),
                    blockers.size(),
                    blockers);
        }
    }

    public record WarehouseReadinessBlocker(String section, String status, String reason) {
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

    private interface ThrowingSupplier<T> {
        T get();
    }
}
