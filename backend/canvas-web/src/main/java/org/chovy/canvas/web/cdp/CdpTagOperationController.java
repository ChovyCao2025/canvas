package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpTagOperationFacade;
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
@RequestMapping("/cdp/tag-operations")
public class CdpTagOperationController {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String DEFAULT_ACTOR = "system";

    private final CdpTagOperationFacade facade;

    public CdpTagOperationController(CdpTagOperationFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<CdpTagOperationFacade.TagOperationView>> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) CdpTagOperationFacade.BatchTagCommand command) {
        return envelope(() -> facade.create(tenantIdOrDefault(tenantId), command, actorOrDefault(actor)));
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<CdpTagOperationFacade.TagOperationView>>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        return envelope(() -> facade.listRecent(tenantIdOrDefault(tenantId), boundedLimit(limit)));
    }

    @GetMapping("/{id}")
    public Mono<CompatibilityEnvelope<CdpTagOperationFacade.TagOperationView>> get(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> facade.get(tenantIdOrDefault(tenantId), id));
    }

    @PostMapping("/{id}/retry-failed")
    public Mono<CompatibilityEnvelope<CdpTagOperationFacade.TagOperationView>> retryFailed(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.retryFailed(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(RuntimeException exception) {
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
