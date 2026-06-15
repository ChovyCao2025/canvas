package org.chovy.canvas.web.canvas;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceConfigCommand;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceConfigView;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceListQuery;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.DataSourceTableMetaView;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.PageView;
import org.chovy.canvas.canvas.api.DataSourceConfigFacade.TenantIdentity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/data-sources")
public class DataSourceConfigController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ROLE = "TENANT_ADMIN";
    private static final String DEFAULT_ACTOR = "operator-1";

    private final DataSourceConfigFacade facade;

    public DataSourceConfigController(DataSourceConfigFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<PageView<DataSourceConfigView>>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(name = "tenantId", required = false) Long tenantIdFilter) {
        return envelope(() -> facade.list(
                operator(tenantId, role, actor),
                new DataSourceListQuery(page, size, type, enabled, tenantIdFilter)));
    }

    @GetMapping("/{id}/tables")
    public Mono<CompatibilityEnvelope<List<DataSourceTableMetaView>>> tables(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.getTables(operator(tenantId, role, actor), id));
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<DataSourceConfigView>> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.create(operator(tenantId, role, actor), command(safePayload(payload))));
    }

    @PutMapping("/{id}")
    public Mono<CompatibilityEnvelope<DataSourceConfigView>> update(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.update(operator(tenantId, role, actor), id, command(safePayload(payload))));
    }

    @DeleteMapping("/{id}")
    public Mono<CompatibilityEnvelope<Void>> delete(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> {
            facade.delete(operator(tenantId, role, actor), id);
            return null;
        });
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

    private static TenantIdentity operator(Long tenantId, String role, String actor) {
        return new TenantIdentity(
                tenantId == null ? DEFAULT_TENANT_ID : tenantId,
                role == null || role.isBlank() ? DEFAULT_ROLE : role.trim(),
                actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim());
    }

    private static DataSourceConfigCommand command(Map<String, Object> payload) {
        return new DataSourceConfigCommand(
                longValue(payload.get("tenantId")),
                stringValue(payload.get("name")),
                stringValue(payload.get("type")),
                stringValue(payload.get("url")),
                stringValue(payload.get("username")),
                stringValue(payload.get("password")),
                stringValue(payload.get("driverClassName")),
                stringValue(payload.get("description")),
                integerValue(payload.get("enabled")),
                stringValue(payload.get("createdBy")));
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : Long.valueOf(text);
    }

    private static Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : Integer.valueOf(text);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
