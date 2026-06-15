package org.chovy.canvas.web.analytics;

import java.util.List;

import org.chovy.canvas.bi.api.AnalyticsFacade;
import org.chovy.canvas.bi.api.AnalyticsViews.AlertPreviewRequest;
import org.chovy.canvas.bi.api.AnalyticsViews.AlertPreviewView;
import org.chovy.canvas.bi.api.AnalyticsViews.AttributeDistributionView;
import org.chovy.canvas.bi.api.AnalyticsViews.EventCountView;
import org.chovy.canvas.bi.api.AnalyticsViews.EventTotalView;
import org.chovy.canvas.bi.api.AnalyticsViews.ExportJobView;
import org.chovy.canvas.bi.api.AnalyticsViews.ExportRequest;
import org.chovy.canvas.bi.api.AnalyticsViews.FunnelView;
import org.chovy.canvas.bi.api.AnalyticsViews.UserTimelineView;
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
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private static final Long DEFAULT_TENANT_ID = 7L;

    private final AnalyticsFacade facade;

    public AnalyticsController(AnalyticsFacade facade) {
        this.facade = facade;
    }

    @GetMapping({"/events/counts", "/events"})
    public Mono<CompatibilityEnvelope<List<EventCountView>>> eventCounts(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return envelope(() -> facade.eventCounts(tenantIdOrDefault(tenantId), startDate, endDate));
    }

    @GetMapping("/events/count")
    public Mono<CompatibilityEnvelope<EventTotalView>> countEvents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String eventCode) {
        return envelope(() -> facade.countEvents(tenantIdOrDefault(tenantId), startDate, endDate, eventCode));
    }

    @GetMapping("/users/{userId}/timeline")
    public Mono<CompatibilityEnvelope<UserTimelineView>> userTimeline(
            @PathVariable String userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return envelope(() -> facade.userTimeline(tenantIdOrDefault(tenantId), userId, startDate, endDate, page, size));
    }

    @GetMapping({"/events/attributes/{attribute}/distribution", "/attributes/{attribute}/distribution"})
    public Mono<CompatibilityEnvelope<List<AttributeDistributionView>>> attributeDistribution(
            @PathVariable String attribute,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return envelope(() -> facade.attributeDistribution(tenantIdOrDefault(tenantId), attribute, startDate, endDate));
    }

    @GetMapping("/funnels/{funnelKey}")
    public Mono<CompatibilityEnvelope<FunnelView>> funnelResult(
            @PathVariable String funnelKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return envelope(() -> facade.funnelResult(tenantIdOrDefault(tenantId), funnelKey, startDate, endDate));
    }

    @PostMapping("/alerts/preview")
    public Mono<CompatibilityEnvelope<AlertPreviewView>> alertPreview(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody AlertPreviewRequest request) {
        return envelope(() -> facade.alertPreview(tenantIdOrDefault(tenantId), request));
    }

    @PostMapping("/exports")
    public Mono<CompatibilityEnvelope<ExportJobView>> createExport(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody ExportRequest request) {
        return envelope(() -> facade.createExport(tenantIdOrDefault(tenantId), request));
    }

    @GetMapping("/exports/{id}")
    public Mono<CompatibilityEnvelope<ExportJobView>> exportStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.exportStatus(tenantIdOrDefault(tenantId), id));
    }

    @ExceptionHandler({IllegalArgumentException.class, ResponseStatusException.class})
    public ResponseEntity<CompatibilityEnvelope<Object>> handleBadRequest(Exception exception) {
        int status = HttpStatus.BAD_REQUEST.value();
        String message = exception.getMessage();
        if (exception instanceof ResponseStatusException responseStatusException) {
            status = responseStatusException.getStatusCode().value();
            message = responseStatusException.getReason() == null ? responseStatusException.getMessage() : responseStatusException.getReason();
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    private Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
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
