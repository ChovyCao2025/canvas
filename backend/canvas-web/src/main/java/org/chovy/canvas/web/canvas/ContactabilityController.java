package org.chovy.canvas.web.canvas;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.canvas.api.ContactabilityFacade;
import org.chovy.canvas.canvas.application.ContactabilityApplicationService;
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
@RequestMapping("/canvas/contactability")
public class ContactabilityController {

    private static final String DEFAULT_QUIET_START = "22:00";
    private static final String DEFAULT_QUIET_END = "08:00";
    private static final String DEFAULT_QUIET_TIMEZONE = "USER_LOCAL";
    private static final String DEFAULT_NODE_ID = "preflight";
    private static final String DEFAULT_FREQUENCY_SCOPE = "JOURNEY";
    private static final int DEFAULT_FREQUENCY_MAX = 1;
    private static final long DEFAULT_FREQUENCY_WINDOW_SECONDS = 86_400L;

    private final ContactabilityFacade facade;

    public ContactabilityController() {
        this(new ContactabilityApplicationService());
    }

    public ContactabilityController(ContactabilityFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/explain")
    public Mono<CompatibilityEnvelope<ContactabilityFacade.Report>> explain(
            @RequestParam String userId,
            @RequestParam String channel,
            @RequestParam(required = false) Boolean requireExplicitConsent,
            @RequestParam(required = false) String quietStart,
            @RequestParam(required = false) String quietEnd,
            @RequestParam(required = false) String quietTimezone,
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String frequencyScope,
            @RequestParam(required = false) Integer frequencyMax,
            @RequestParam(required = false) Long frequencyWindowSeconds) {
        return Mono.fromCallable(() -> {
            ContactabilityFacade.Request request = new ContactabilityFacade.Request(
                    userId,
                    channel,
                    requireExplicitConsent == null || requireExplicitConsent,
                    safeTime(quietStart, DEFAULT_QUIET_START),
                    safeTime(quietEnd, DEFAULT_QUIET_END),
                    safeTimezone(quietTimezone),
                    canvasId == null ? 0L : canvasId,
                    defaultString(nodeId, DEFAULT_NODE_ID),
                    defaultString(frequencyScope, DEFAULT_FREQUENCY_SCOPE),
                    safeFrequencyMax(frequencyMax),
                    java.time.Duration.ofSeconds(safeWindowSeconds(frequencyWindowSeconds)));
            return CompatibilityEnvelope.ok(facade.explain(request));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String safeTime(String value, String fallback) {
        String candidate = defaultString(value, fallback);
        try {
            LocalTime.parse(candidate);
            return candidate;
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }

    private static String safeTimezone(String value) {
        String candidate = defaultString(value, DEFAULT_QUIET_TIMEZONE);
        if (DEFAULT_QUIET_TIMEZONE.equalsIgnoreCase(candidate)) {
            return DEFAULT_QUIET_TIMEZONE;
        }
        try {
            ZoneId.of(candidate);
            return candidate;
        } catch (RuntimeException ignored) {
            return DEFAULT_QUIET_TIMEZONE;
        }
    }

    private static int safeFrequencyMax(Integer value) {
        return value == null || value <= 0 ? DEFAULT_FREQUENCY_MAX : value;
    }

    private static long safeWindowSeconds(Long value) {
        return value == null || value <= 0 ? DEFAULT_FREQUENCY_WINDOW_SECONDS : value;
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
