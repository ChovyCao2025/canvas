package org.chovy.canvas.web.execution;

import java.util.Arrays;
import java.util.List;

import org.chovy.canvas.execution.api.AsyncTaskFacade;
import org.chovy.canvas.execution.api.AsyncTaskFacade.AsyncTaskQuery;
import org.chovy.canvas.execution.api.AsyncTaskFacade.AsyncTaskView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class AsyncTaskController {

    private static final String DEFAULT_USER = "system";
    private static final String ADMIN_ROLE = "ADMIN";

    private final AsyncTaskFacade asyncTaskFacade;

    public AsyncTaskController(AsyncTaskFacade asyncTaskFacade) {
        this.asyncTaskFacade = asyncTaskFacade;
    }

    @GetMapping("/canvas/async-tasks")
    public Mono<CompatibilityEnvelope<List<AsyncTaskView>>> list(
            @RequestHeader(value = "X-User", required = false) String username,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizIds,
            @RequestParam(required = false) String statuses,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        AsyncTaskQuery query = new AsyncTaskQuery(
                taskType,
                bizType,
                parseCsv(bizIds),
                parseCsv(statuses),
                usernameOrDefault(username),
                isAdmin(role),
                Math.max(1, page),
                clamp(size, 1, 200));
        return envelope(() -> asyncTaskFacade.listTasks(query));
    }

    @GetMapping("/canvas/async-tasks/{taskId}")
    public Mono<CompatibilityEnvelope<AsyncTaskView>> get(
            @RequestHeader(value = "X-User", required = false) String username,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable String taskId) {
        return envelope(() -> asyncTaskFacade.getTask(taskId, usernameOrDefault(username), isAdmin(role)));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        });
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

    private static List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String usernameOrDefault(String username) {
        return username == null || username.isBlank() ? DEFAULT_USER : username.trim();
    }

    private static boolean isAdmin(String role) {
        return ADMIN_ROLE.equals(role);
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

    private interface ThrowingSupplier<T> {
        T get();
    }
}
