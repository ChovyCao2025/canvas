package org.chovy.canvas.web.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.function.Supplier;

import org.chovy.canvas.execution.api.DlqFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/dlq")
public class DlqController {

    private final DlqFacade facade;

    public DlqController(DlqFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<DlqFacade.DlqPageView>> list(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return envelope(() -> facade.list(new DlqFacade.DlqQuery(canvasId, page, size)));
    }

    @PostMapping("/{id}/replay")
    public Mono<CompatibilityEnvelope<DlqFacade.DlqReplayResult>> replay(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean skipSuccessNodes) {
        return envelope(() -> facade.replay(id, skipSuccessNodes));
    }

    @DeleteMapping("/{id}")
    public Mono<CompatibilityEnvelope<DlqFacade.DeleteResult>> delete(@PathVariable Long id) {
        return envelope(() -> facade.delete(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
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
