package org.chovy.canvas.web.canvas;

import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionCommand;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionListQuery;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionView;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.PageView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/api-definitions")
public class ApiDefinitionController {

    private final ApiDefinitionFacade facade;

    public ApiDefinitionController(ApiDefinitionFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<PageView<ApiDefinitionView>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled) {
        return envelope(() -> facade.list(new ApiDefinitionListQuery(page, size, enabled)));
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<ApiDefinitionView>> create(
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.create(command(safePayload(payload))));
    }

    @PutMapping("/{id}")
    public Mono<CompatibilityEnvelope<ApiDefinitionView>> update(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.update(id, command(safePayload(payload))));
    }

    @DeleteMapping("/{id}")
    public Mono<CompatibilityEnvelope<Void>> delete(@PathVariable Long id) {
        return envelope(() -> {
            facade.delete(id);
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

    private static ApiDefinitionCommand command(Map<String, Object> payload) {
        return new ApiDefinitionCommand(
                stringValue(payload.get("apiKey")),
                stringValue(payload.get("url")),
                integerValue(payload.get("enabled")),
                integerValue(payload.get("includeContextPayload")),
                integerValue(payload.get("receiptEnabled")),
                integerValue(payload.get("receiptExpireMinutes")),
                stringValue(payload.get("receiptStatuses")),
                integerValue(payload.get("rateLimitPerSec")),
                payload.containsKey("rateLimitPerSec"));
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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
