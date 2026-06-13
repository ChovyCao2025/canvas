package org.chovy.canvas.web.canvas;

import java.util.List;

import org.chovy.canvas.canvas.application.CanvasPublishApplicationService;
import org.chovy.canvas.canvas.application.CanvasVersionApplicationService;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class CanvasController {

    private final CanvasVersionApplicationService versionService;
    private final CanvasPublishApplicationService publishService;

    public CanvasController(CanvasVersionApplicationService versionService,
                            CanvasPublishApplicationService publishService) {
        this.versionService = versionService;
        this.publishService = publishService;
    }

    @GetMapping("/canvas/{id}/versions")
    public Mono<CompatibilityEnvelope<VersionPageResponse>> listVersions(@PathVariable Long id) {
        return envelope(() -> VersionPageResponse.from(versionService.getVersions(id)));
    }

    @GetMapping("/canvas/{id}/versions/{versionId}")
    public Mono<CompatibilityEnvelope<VersionResponse>> getVersion(
            @PathVariable Long id,
            @PathVariable Long versionId) {
        return envelope(() -> VersionResponse.from(versionService.getVersion(versionId)));
    }

    @PostMapping("/canvas/{id}/publish")
    public Mono<CompatibilityEnvelope<VersionResponse>> publish(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return envelope(() -> VersionResponse.from(publishService.publish(id, operator)));
    }

    @PostMapping("/canvas/{id}/offline")
    public Mono<CompatibilityEnvelope<Void>> offline(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return emptyEnvelope(() -> publishService.unpublish(id));
    }

    @PostMapping("/canvas/{id}/archive")
    public Mono<CompatibilityEnvelope<Void>> archive(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return emptyEnvelope(() -> publishService.archive(id));
    }

    @PostMapping("/canvas/{id}/kill")
    public Mono<CompatibilityEnvelope<Void>> kill(
            @PathVariable Long id,
            @RequestParam(required = false) String mode) {
        return emptyEnvelope(() -> publishService.kill(id));
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

    private static Mono<CompatibilityEnvelope<Void>> emptyEnvelope(ThrowingRunnable runnable) {
        return envelope(() -> {
            runnable.run();
            return null;
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

    public record VersionPageResponse(
            int total,
            List<VersionResponse> list) {

        private static VersionPageResponse from(List<CanvasVersion> versions) {
            List<VersionResponse> list = versions.stream()
                    .map(VersionResponse::from)
                    .toList();
            return new VersionPageResponse(list.size(), list);
        }
    }

    public record VersionResponse(
            Long id,
            Long canvasId,
            Long tenantId,
            Integer version,
            String graphJson,
            String status,
            String createdBy) {

        private static VersionResponse from(CanvasVersion version) {
            return new VersionResponse(
                    version.id(),
                    version.canvasId(),
                    version.tenantId(),
                    version.version(),
                    version.graphJson(),
                    version.status().name(),
                    version.createdBy());
        }
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

    private interface ThrowingRunnable {
        void run();
    }
}
