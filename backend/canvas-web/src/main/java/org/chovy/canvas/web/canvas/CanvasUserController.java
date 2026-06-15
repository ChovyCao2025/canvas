package org.chovy.canvas.web.canvas;

import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.canvas.api.CanvasUserFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/{id}/users")
public class CanvasUserController {

    private final CanvasUserFacade facade;

    public CanvasUserController(CanvasUserFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<CanvasUserFacade.CanvasUserView>>> list(@PathVariable Long id) {
        return envelope(() -> facade.listUsers(id));
    }

    @GetMapping("/{userId}")
    public Mono<CompatibilityEnvelope<CanvasUserFacade.CanvasUserView>> get(
            @PathVariable Long id,
            @PathVariable String userId) {
        return envelope(() -> facade.getUserInCanvas(id, userId));
    }

    @GetMapping("/{userId}/executions")
    public Mono<CompatibilityEnvelope<List<CanvasUserFacade.CanvasExecutionView>>> executions(
            @PathVariable Long id,
            @PathVariable String userId) {
        return envelope(() -> facade.listExecutions(id, userId));
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

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
