package org.chovy.canvas.web.canvas;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.canvas.api.CanvasStatsFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/{id}")
public class CanvasStatsController {

    private final CanvasStatsFacade facade;

    public CanvasStatsController(CanvasStatsFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/execution/{executionId}/trace")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> trace(
            @PathVariable("id") Long canvasId,
            @PathVariable String executionId) {
        return envelope(() -> facade.trace(canvasId, executionId));
    }

    @GetMapping("/executions")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> recentExecutions(
            @PathVariable("id") Long canvasId,
            @RequestParam(defaultValue = "20") int size) {
        return envelope(() -> facade.recentExecutions(canvasId, size));
    }

    @GetMapping("/stats")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> stats(
            @PathVariable("id") Long canvasId,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        return envelope(() -> facade.stats(canvasId, days, since, until));
    }

    @GetMapping("/funnel")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> funnel(@PathVariable("id") Long canvasId) {
        return envelope(() -> facade.funnel(canvasId));
    }

    @GetMapping("/trend")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> trend(
            @PathVariable("id") Long canvasId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        return envelope(() -> facade.trend(canvasId, days, since, until));
    }

    @GetMapping("/receipts")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> receipts(@PathVariable("id") Long canvasId) {
        return envelope(() -> facade.receipts(canvasId));
    }

    @GetMapping("/attribution-summary")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> attributionSummary(@PathVariable("id") Long canvasId) {
        return envelope(() -> facade.attributionSummary(canvasId));
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
