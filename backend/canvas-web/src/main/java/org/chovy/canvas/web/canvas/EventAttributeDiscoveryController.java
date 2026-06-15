package org.chovy.canvas.web.canvas;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.cdp.api.CdpEventAttributeDiscoveryFacade;
import org.chovy.canvas.cdp.api.CdpEventAttributeDiscoveryFacade.DiscoveredAttributeView;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/canvas/event-attributes")
public class EventAttributeDiscoveryController {

    private static final List<DiscoveredAttributeView> ATTRIBUTES = List.of(
            new DiscoveredAttributeView(
                    101L,
                    "USER_SIGNUP",
                    "plan",
                    "STRING",
                    "ACTIVE",
                    "pro",
                    "2026-06-01T10:00:00",
                    "2026-06-14T10:00:00"),
            new DiscoveredAttributeView(
                    102L,
                    "ORDER_PAID",
                    "amount",
                    "DECIMAL",
                    "ACTIVE",
                    "129.99",
                    "2026-06-02T10:00:00",
                    "2026-06-14T11:00:00"),
            new DiscoveredAttributeView(
                    103L,
                    "LEGACY_IMPORT",
                    "legacySource",
                    "STRING",
                    "INACTIVE",
                    "csv",
                    "2026-05-20T09:00:00",
                    "2026-06-01T09:00:00"));

    private final CdpEventAttributeDiscoveryFacade facade;

    public EventAttributeDiscoveryController() {
        this(status -> ATTRIBUTES.stream()
                .filter(attribute -> status == null || status.isBlank()
                        || attribute.status().equalsIgnoreCase(status))
                .toList());
    }

    @Autowired
    public EventAttributeDiscoveryController(CdpEventAttributeDiscoveryFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/discovered")
    public Mono<CompatibilityEnvelope<List<DiscoveredAttributeView>>> listDiscovered(
            @RequestParam(required = false) String status) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(facade.listDiscovered(status)))
                .subscribeOn(Schedulers.boundedElastic());
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
