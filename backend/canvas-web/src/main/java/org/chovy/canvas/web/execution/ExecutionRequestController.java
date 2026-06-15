package org.chovy.canvas.web.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.function.Supplier;

import org.chovy.canvas.execution.api.ExecutionRequestFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/canvas/execution-requests")
public class ExecutionRequestController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_OPERATOR = "system";

    private final ExecutionRequestFacade facade;

    public ExecutionRequestController(ExecutionRequestFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<ExecutionRequestFacade.RequestPageView>> list(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sourceMsgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return envelope(() -> facade.list(new ExecutionRequestFacade.RequestQuery(DEFAULT_TENANT_ID, canvasId,
                status, userId, sourceMsgId, page, size)));
    }

    @PostMapping("/{id}/replay")
    public Mono<CompatibilityEnvelope<ExecutionRequestFacade.ReplayResult>> replay(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "false") boolean force) {
        return envelope(() -> facade.replay(id, new ExecutionRequestFacade.ReplayCommand(DEFAULT_TENANT_ID,
                DEFAULT_OPERATOR, reason, force)));
    }

    @PostMapping("/replay")
    public Mono<CompatibilityEnvelope<ExecutionRequestFacade.BatchReplayResult>> replayBatch(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sourceMsgId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "false") boolean force) {
        return envelope(() -> facade.replayBatch(new ExecutionRequestFacade.BatchReplayCommand(DEFAULT_TENANT_ID,
                canvasId, status, userId, sourceMsgId, limit, reason, force)));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(RuntimeException exception) {
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
