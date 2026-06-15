package org.chovy.canvas.web.canvas;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.canvas.api.CanvasTriggerFacade;
import org.chovy.canvas.canvas.application.CanvasTriggerApplicationService;
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
@RequestMapping("/canvas/trigger")
public class CanvasTriggerController {

    private final CanvasTriggerFacade facade;

    public CanvasTriggerController() {
        this(new CanvasTriggerApplicationService());
    }

    public CanvasTriggerController(CanvasTriggerFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/behavior")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> behavior(
            @RequestBody(required = false) BehaviorTriggerRequest request) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(facade.triggerBehavior(toCommand(request)).data()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static CanvasTriggerFacade.BehaviorTriggerCommand toCommand(BehaviorTriggerRequest request) {
        BehaviorTriggerRequest body = request == null ? BehaviorTriggerRequest.empty() : request;
        return new CanvasTriggerFacade.BehaviorTriggerCommand(
                body.canvasId(),
                body.userId(),
                body.eventCode(),
                body.eventId(),
                body.behaviorData());
    }

    private record BehaviorTriggerRequest(
            Long canvasId,
            String userId,
            String eventCode,
            String eventId,
            Map<String, Object> behaviorData) {

        private static BehaviorTriggerRequest empty() {
            return new BehaviorTriggerRequest(null, null, null, null, Map.of());
        }
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
