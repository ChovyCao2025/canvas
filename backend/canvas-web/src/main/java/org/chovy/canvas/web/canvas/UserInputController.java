package org.chovy.canvas.web.canvas;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.canvas.api.UserInputFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/user-input")
public class UserInputController {

    private final UserInputFacade facade;

    public UserInputController(UserInputFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/responses/{responseId}/submit")
    public Mono<CompatibilityEnvelope<UserInputFacade.SubmitResult>> submit(
            @PathVariable Long responseId,
            @RequestBody(required = false) SubmitRequest request) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(facade.submit(responseId, toCommand(request))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static UserInputFacade.SubmitCommand toCommand(SubmitRequest request) {
        SubmitRequest body = request == null ? SubmitRequest.empty() : request;
        return new UserInputFacade.SubmitCommand(body.response(), body.operator());
    }

    private record SubmitRequest(Map<String, Object> response, String operator) {
        private static SubmitRequest empty() {
            return new SubmitRequest(Map.of(), null);
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
