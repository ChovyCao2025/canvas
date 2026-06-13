package org.chovy.canvas.web.canvas;

import org.chovy.canvas.canvas.application.CanvasProjectFolderApplicationService;
import org.chovy.canvas.canvas.application.ProjectFolderMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class CanvasProjectFolderMetadataController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final CanvasProjectFolderApplicationService projectFolderService;

    public CanvasProjectFolderMetadataController(CanvasProjectFolderApplicationService projectFolderService) {
        this.projectFolderService = projectFolderService;
    }

    @GetMapping("/canvas/{id}/project-folder-metadata")
    public Mono<CompatibilityEnvelope<ProjectFolderMetadataResponse>> getProjectFolderMetadata(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> ProjectFolderMetadataResponse.from(
                projectFolderService.getMetadata(tenantIdOrDefault(tenantId), id)));
    }

    @PutMapping("/canvas/{id}/project-folder-metadata")
    public Mono<CompatibilityEnvelope<ProjectFolderMetadataResponse>> saveProjectFolderMetadata(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody ProjectFolderMetadataRequest request) {
        return envelope(() -> ProjectFolderMetadataResponse.from(projectFolderService.saveMetadata(
                tenantIdOrDefault(tenantId),
                id,
                new CanvasProjectFolderApplicationService.SaveProjectFolderCommand(
                        request.projectId(),
                        request.projectKey(),
                        request.projectName(),
                        request.folderKey(),
                        request.folderName(),
                        operatorOrDefault(request.operator(), actor)))));
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

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String operatorOrDefault(String operator, String actor) {
        if (operator != null && !operator.isBlank()) {
            return operator.trim();
        }
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    public record ProjectFolderMetadataRequest(
            Long projectId,
            String projectKey,
            String projectName,
            String folderKey,
            String folderName,
            String operator) {
    }

    public record ProjectFolderMetadataResponse(
            Long canvasId,
            Long projectId,
            String projectKey,
            String projectName,
            String folderKey,
            String folderName) {

        private static ProjectFolderMetadataResponse from(ProjectFolderMetadata metadata) {
            return new ProjectFolderMetadataResponse(
                    metadata.canvasId(),
                    metadata.projectId(),
                    metadata.projectKey(),
                    metadata.projectName(),
                    metadata.folderKey(),
                    metadata.folderName());
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
}
