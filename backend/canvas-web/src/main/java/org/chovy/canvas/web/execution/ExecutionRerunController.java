package org.chovy.canvas.web.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.execution.api.ExecutionRerunFacade;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/execution-reruns")
public class ExecutionRerunController {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String DEFAULT_OPERATOR = "system";

    private final ExecutionRerunFacade facade;

    public ExecutionRerunController(ExecutionRerunFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/canvas/{canvasId}")
    public Mono<CompatibilityEnvelope<ExecutionRerunFacade.RerunResult>> rerun(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Admin", required = false) Boolean admin,
            @PathVariable Long canvasId,
            @RequestBody(required = false) ExecutionRerunFacade.RerunCommand command) {
        return envelope(() -> facade.rerun(tenantIdOrDefault(tenantId), actorOrDefault(actor),
                Boolean.TRUE.equals(admin), canvasId, command));
    }

    @GetMapping("/{id}")
    public Mono<CompatibilityEnvelope<ExecutionRerunFacade.AuditRow>> status(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> facade.audit(tenantIdOrDefault(tenantId), id));
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<ExecutionRerunFacade.AuditRow>>> audits(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long canvasId) {
        return envelope(() -> facade.audits(tenantIdOrDefault(tenantId), canvasId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_OPERATOR : actor.trim();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }
}
