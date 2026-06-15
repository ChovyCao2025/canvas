package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseMetricChangeReviewFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseMetricChangeReviewFacade.MetricChangeCommand;
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
@RequestMapping("/warehouse/metric-change-reviews")
public class CdpWarehouseMetricChangeReviewController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWarehouseMetricChangeReviewFacade facade;

    public CdpWarehouseMetricChangeReviewController(CdpWarehouseMetricChangeReviewFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<Object>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String datasetKey,
            @RequestParam(required = false) String metricKey,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.list(tenantIdOrDefault(tenantId), datasetKey, metricKey, status));
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<Object>> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestBody(required = false) MetricChangeRequest request) {
        MetricChangeRequest safeRequest = request == null ? new MetricChangeRequest() : request;
        return envelope(() -> facade.create(tenantIdOrDefault(tenantId), usernameOrDefault(username),
                safeRequest.toCommand()));
    }

    @PostMapping("/{reviewId}/approve")
    public Mono<CompatibilityEnvelope<Object>> approve(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @PathVariable Long reviewId,
            @RequestBody(required = false) ReviewDecisionRequest request) {
        ReviewDecisionRequest safeRequest = request == null ? new ReviewDecisionRequest() : request;
        return envelope(() -> facade.approve(tenantIdOrDefault(tenantId), usernameOrDefault(username), reviewId,
                safeRequest.reviewNote));
    }

    @PostMapping("/{reviewId}/reject")
    public Mono<CompatibilityEnvelope<Object>> reject(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @PathVariable Long reviewId,
            @RequestBody(required = false) ReviewDecisionRequest request) {
        ReviewDecisionRequest safeRequest = request == null ? new ReviewDecisionRequest() : request;
        return envelope(() -> facade.reject(tenantIdOrDefault(tenantId), usernameOrDefault(username), reviewId,
                safeRequest.reviewNote));
    }

    @PostMapping("/{reviewId}/apply")
    public Mono<CompatibilityEnvelope<Object>> apply(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @PathVariable Long reviewId) {
        return envelope(() -> facade.apply(tenantIdOrDefault(tenantId), usernameOrDefault(username), reviewId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.error(400, "API_001", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> conflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(CompatibilityEnvelope.error(409, "API_004", exception.getMessage()));
    }

    private static Mono<CompatibilityEnvelope<Object>> envelope(Supplier<Object> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String usernameOrDefault(String username) {
        return username == null || username.isBlank() ? "system" : username.trim();
    }

    public static class MetricChangeRequest {
        public String datasetKey;
        public String metricKey;
        public String proposedExpression;
        public List<String> proposedAllowedDimensions;
        public String reason;

        MetricChangeCommand toCommand() {
            return new MetricChangeCommand(datasetKey, metricKey, proposedExpression, proposedAllowedDimensions, reason);
        }
    }

    public static class ReviewDecisionRequest {
        public String reviewNote;
    }

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> error(int code, String errorCode, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }
}
