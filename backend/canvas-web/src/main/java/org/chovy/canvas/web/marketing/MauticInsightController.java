package org.chovy.canvas.web.marketing;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.marketing.api.MauticInsightFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/mautic-insights")
public class MauticInsightController {

    private final MauticInsightFacade facade;

    public MauticInsightController(MauticInsightFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/audience-membership")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> audienceMembership(
            @RequestParam Long audienceId,
            @RequestParam String userId) {
        return envelope(() -> facade.audienceMembership(audienceId, userId));
    }

    @GetMapping("/journey-path")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> journeyPath(@RequestParam String executionId) {
        return envelope(() -> facade.journeyPath(executionId));
    }

    @GetMapping("/channel-preference")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> channelPreference(
            @RequestParam String userId,
            @RequestParam(required = false) String preferredChannel) {
        return envelope(() -> facade.channelPreference(userId, preferredChannel));
    }

    @GetMapping("/suppression-timeline")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> suppressionTimeline(@RequestParam String userId) {
        return envelope(() -> facade.suppressionTimeline(userId));
    }

    @GetMapping("/publish-health")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> publishHealth(@RequestParam Long canvasId) {
        return envelope(() -> facade.publishHealth(canvasId));
    }

    @GetMapping("/frequency-templates")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> frequencyTemplates() {
        return envelope(facade::frequencyTemplates);
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
