package org.chovy.canvas.web.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.execution.api.PluginRegistryFacade;
import org.chovy.canvas.execution.domain.PluginRegistryCatalog.Plugin;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/plugins")
public class PluginRegistryController {

    private final PluginRegistryFacade facade;

    public PluginRegistryController(PluginRegistryFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<Map<String, List<Plugin>>>> catalog() {
        return envelope(facade::groupedCatalog);
    }

    @PutMapping("/{pluginKey}/enabled")
    public Mono<CompatibilityEnvelope<Void>> setEnabled(
            @PathVariable String pluginKey,
            @RequestHeader(name = "X-Canvas-Version", defaultValue = "1.0.0") String canvasVersion,
            @RequestBody EnableRequest request) {
        return envelope(() -> {
            facade.setEnabled(pluginKey, request.enabled(), canvasVersion);
            return null;
        });
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(RuntimeException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public record EnableRequest(boolean enabled) {
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
