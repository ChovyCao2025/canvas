package org.chovy.canvas.web.canvas;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.canvas.api.CanvasEventReportFacade;
import org.chovy.canvas.canvas.application.CanvasEventReportApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/events")
public class CanvasEventReportController {

    private final CanvasEventReportFacade facade;

    public CanvasEventReportController() {
        this(new CanvasEventReportApplicationService());
    }

    public CanvasEventReportController(CanvasEventReportFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/report")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> report(
            @RequestBody(required = false) Mono<String> rawBody) {
        Mono<String> body = rawBody == null ? Mono.just("") : rawBody.defaultIfEmpty("");
        return body.flatMap(value -> Mono.fromCallable(() -> CompatibilityEnvelope.ok(facade.report(value)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
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
