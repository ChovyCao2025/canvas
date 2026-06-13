package org.chovy.canvas.web.cdp;

import java.time.LocalDateTime;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpWarehouseReadinessFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessSectionView;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class CdpWarehouseRealtimeCutoverReadinessController {

    private static final Long DEFAULT_TENANT_ID = 7L;

    private final CdpWarehouseReadinessFacade readinessFacade;

    public CdpWarehouseRealtimeCutoverReadinessController(CdpWarehouseReadinessFacade readinessFacade) {
        this.readinessFacade = readinessFacade;
    }

    @GetMapping("/warehouse/realtime/cutover-readiness")
    public Mono<CompatibilityEnvelope<CutoverReadinessCompatibilityView>> cutoverReadiness(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "FLINK_FIRST") String targetMode,
            @RequestParam(name = "pipelineKey", required = false) List<String> pipelineKeys,
            @RequestParam(name = "contractKey", required = false) List<String> contractKeys,
            @RequestParam(defaultValue = "HYBRID") String certificationMode,
            @RequestParam(defaultValue = "60") Long maxCertificationAgeMinutes) {
        return envelope(() -> CutoverReadinessCompatibilityView.from(
                readinessFacade.readiness(tenantIdOrDefault(tenantId)),
                targetMode,
                pipelineKeys,
                contractKeys,
                certificationMode,
                maxCertificationAgeMinutes));
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

    public record CutoverReadinessCompatibilityView(
            Long tenantId,
            String targetMode,
            List<String> pipelineKeys,
            List<String> contractKeys,
            String certificationMode,
            Long maxCertificationAgeMinutes,
            String status,
            LocalDateTime generatedAt,
            List<CdpWarehouseReadinessSectionView> sections,
            boolean ready,
            boolean productionReady,
            boolean cutoverAllowed,
            int blockerCount,
            List<CutoverReadinessBlocker> blockers) {

        private static CutoverReadinessCompatibilityView from(
                CdpWarehouseReadinessView view,
                String targetMode,
                List<String> pipelineKeys,
                List<String> contractKeys,
                String certificationMode,
                Long maxCertificationAgeMinutes) {
            List<CdpWarehouseReadinessSectionView> sections = view.sections() == null ? List.of() : view.sections();
            List<CutoverReadinessBlocker> blockers = sections.stream()
                    .filter(section -> !"PASS".equals(section.status()))
                    .map(section -> new CutoverReadinessBlocker(section.key(), section.status(), section.reason()))
                    .toList();
            boolean ready = "PASS".equals(view.status());
            return new CutoverReadinessCompatibilityView(
                    view.tenantId(),
                    targetMode,
                    pipelineKeys == null ? List.of() : List.copyOf(pipelineKeys),
                    contractKeys == null ? List.of() : List.copyOf(contractKeys),
                    certificationMode,
                    maxCertificationAgeMinutes,
                    view.status(),
                    view.generatedAt(),
                    sections,
                    ready,
                    ready,
                    ready,
                    blockers.size(),
                    blockers);
        }
    }

    public record CutoverReadinessBlocker(String section, String status, String reason) {
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
