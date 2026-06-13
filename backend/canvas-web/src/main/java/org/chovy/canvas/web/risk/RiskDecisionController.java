package org.chovy.canvas.web.risk;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.risk.api.RiskDecisionCommand;
import org.chovy.canvas.risk.api.RiskDecisionFacade;
import org.chovy.canvas.risk.api.RiskDecisionView;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionReplayMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class RiskDecisionController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final int DEFAULT_DEADLINE_MS = 50;
    private static final int MAX_DEADLINE_MS = 50;
    private static final long MAX_FUTURE_SECONDS = 24 * 60 * 60;

    private final RiskDecisionFacade facade;
    private final Clock clock;

    public RiskDecisionController(RiskDecisionFacade facade) {
        this(facade, Clock.systemUTC());
    }

    RiskDecisionController(RiskDecisionFacade facade, Clock clock) {
        this.facade = facade;
        this.clock = clock;
    }

    @PostMapping("/canvas/risk/decisions/evaluate")
    public Mono<CompatibilityEnvelope<RiskDecisionView>> evaluate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) EvaluateRequest body) {
        return Mono.fromCallable(() -> {
                    EvaluateRequest request = body == null ? EvaluateRequest.empty() : body;
                    Long authenticatedTenantId = tenantIdOrDefault(tenantId);
                    validate(request);
                    return CompatibilityEnvelope.ok(facade.evaluate(toCommand(authenticatedTenantId, request)));
                })
                .onErrorMap(RiskDecisionReplayMismatchException.class,
                        ex -> new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    private void validate(EvaluateRequest request) {
        if (isBlank(request.requestId())) {
            throw badRequest("requestId is required");
        }
        if (isBlank(request.sceneKey())) {
            throw badRequest("sceneKey is required");
        }
        Instant eventTime = parseEventTime(request.eventTime());
        if (!hasSubjectIdentifier(request.subject())) {
            throw badRequest("subject identifier is required");
        }
        if (eventTime.isAfter(clock.instant().plusSeconds(MAX_FUTURE_SECONDS))) {
            throw badRequest("eventTime must not be more than 24 hours in the future");
        }
        int deadlineMs = deadlineMs(request);
        if (deadlineMs <= 0 || deadlineMs > MAX_DEADLINE_MS) {
            throw badRequest("deadline must be between 1 and 50ms");
        }
    }

    private RiskDecisionCommand toCommand(Long tenantId, EvaluateRequest request) {
        return new RiskDecisionCommand(
                tenantId,
                request.requestId(),
                request.sceneKey(),
                parseEventTime(request.eventTime()),
                request.subject(),
                request.event(),
                request.context(),
                request.features(),
                deadlineMs(request));
    }

    private int deadlineMs(EvaluateRequest request) {
        if (request.options() == null || request.options().deadlineMs() == null) {
            return DEFAULT_DEADLINE_MS;
        }
        return request.options().deadlineMs();
    }

    private Instant parseEventTime(String eventTime) {
        if (isBlank(eventTime)) {
            throw badRequest("eventTime is required");
        }
        try {
            return Instant.parse(eventTime);
        } catch (DateTimeParseException ex) {
            throw badRequest("eventTime must be ISO-8601");
        }
    }

    private static boolean hasSubjectIdentifier(Map<String, Object> subject) {
        return hasText(subject, "userId")
                || hasText(subject, "deviceId")
                || hasText(subject, "ip")
                || hasText(subject, "email")
                || hasText(subject, "phone");
    }

    private static boolean hasText(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        return value != null && !value.toString().isBlank();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record EvaluateRequest(
            Long tenantId,
            String requestId,
            String sceneKey,
            Map<String, Object> subject,
            String eventTime,
            Map<String, Object> event,
            Map<String, Object> context,
            Map<String, Object> features,
            Options options) {

        private EvaluateRequest {
            subject = subject == null ? Map.of() : new LinkedHashMap<>(subject);
            event = event == null ? Map.of() : new LinkedHashMap<>(event);
            context = context == null ? Map.of() : new LinkedHashMap<>(context);
            features = features == null ? Map.of() : new LinkedHashMap<>(features);
        }

        private static EvaluateRequest empty() {
            return new EvaluateRequest(null, null, null, Map.of(), null, Map.of(), Map.of(), Map.of(), null);
        }
    }

    private record Options(
            String modeOverride,
            Boolean includeTrace,
            Integer deadlineMs) {
    }

    public record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }
}
