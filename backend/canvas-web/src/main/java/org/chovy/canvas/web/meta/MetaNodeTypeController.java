package org.chovy.canvas.web.meta;

import java.util.List;

import org.chovy.canvas.execution.api.node.NodeMetadataFacade;
import org.chovy.canvas.execution.api.node.NodeMetadataView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class MetaNodeTypeController {

    private final NodeMetadataFacade nodeMetadataFacade;

    public MetaNodeTypeController(NodeMetadataFacade nodeMetadataFacade) {
        this.nodeMetadataFacade = nodeMetadataFacade;
    }

    @GetMapping("/meta/node-types")
    public Mono<CompatibilityEnvelope<List<NodeMetadataView>>> listNodeTypes() {
        return envelope(nodeMetadataFacade::listNodeTypes);
    }

    @GetMapping("/meta/node-types/{typeKey}/schema")
    public Mono<CompatibilityEnvelope<NodeMetadataView>> getNodeTypeSchema(@PathVariable String typeKey) {
        return envelope(() -> nodeMetadataFacade.getNodeTypeSchema(typeKey));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException | IllegalStateException ex) {
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
