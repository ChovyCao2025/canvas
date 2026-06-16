package org.chovy.canvas.web.canvas;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService;
import org.chovy.canvas.canvas.application.CanvasPublishApplicationService;
import org.chovy.canvas.canvas.application.CanvasVersionApplicationService;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class CanvasController {

    private final CanvasVersionApplicationService versionService;
    private final CanvasPublishApplicationService publishService;
    private final CanvasCompatibilityApplicationService compatibilityService;

    public CanvasController(CanvasVersionApplicationService versionService,
                            CanvasPublishApplicationService publishService) {
        this(versionService, publishService, new CanvasCompatibilityApplicationService());
    }

    @Autowired
    public CanvasController(CanvasVersionApplicationService versionService,
                            CanvasPublishApplicationService publishService,
                            CanvasCompatibilityApplicationService compatibilityService) {
        this.versionService = versionService;
        this.publishService = publishService;
        this.compatibilityService = compatibilityService;
    }

    @PostMapping("/canvas")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasView>> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> request) {
        return envelope(() -> compatibilityService.create(tenantId(tenantId), actor(actor), request));
    }

    @GetMapping("/canvas/{id}")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasView>> get(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> compatibilityService.get(tenantId(tenantId), id));
    }

    @PutMapping("/canvas/{id}")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasView>> update(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request) {
        return envelope(() -> compatibilityService.update(tenantId(tenantId), actor(actor), id, request));
    }

    @GetMapping("/canvas/list")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.PageView<CanvasCompatibilityApplicationService.CanvasView>>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> compatibilityService.list(tenantId(tenantId)));
    }

    @GetMapping("/canvas/templates")
    public Mono<CompatibilityEnvelope<List<CanvasCompatibilityApplicationService.TemplateView>>> listTemplates(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String category) {
        return envelope(() -> compatibilityService.listTemplates(tenantId(tenantId), category));
    }

    @PostMapping("/canvas/{id}/save-as-template")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.TemplateView>> saveAsTemplate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request) {
        return envelope(() -> compatibilityService.saveAsTemplate(tenantId(tenantId), id, request));
    }

    @PostMapping("/canvas/from-template/{templateId}")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasView>> createFromTemplate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long templateId,
            @RequestBody(required = false) Map<String, Object> request) {
        return envelope(() -> compatibilityService.createFromTemplate(tenantId(tenantId), templateId, request,
                actor(actor)));
    }

    @GetMapping("/canvas/pending-reviews")
    public Mono<CompatibilityEnvelope<List<CanvasCompatibilityApplicationService.PendingReviewView>>> pendingReviews(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> compatibilityService.pendingReviews(tenantId(tenantId)));
    }

    @PostMapping("/canvas/{id}/submit-review")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.ReviewView>> submitReview(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request) {
        return envelope(() -> compatibilityService.submitReview(tenantId(tenantId), actor(actor), id, request));
    }

    @GetMapping("/canvas/{id}/approval-status")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.ApprovalStatusView>> approvalStatus(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> compatibilityService.approvalStatusView(tenantId(tenantId), id));
    }

    @GetMapping("/canvas/{id}/pre-publish-checks")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.PrePublishCheckView>> prePublishChecks(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> compatibilityService.prePublishChecksView(tenantId(tenantId), id));
    }

    @PostMapping("/canvas/{id}/revert/{versionId}")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.OperationView>> revert(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @PathVariable Long versionId) {
        return envelope(() -> compatibilityService.revert(tenantId(tenantId), actor(actor), id, versionId));
    }

    @PostMapping("/canvas/{id}/canary")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasView>> startCanary(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestParam int percent) {
        return envelope(() -> compatibilityService.startCanary(tenantId(tenantId), actor(actor), id, percent));
    }

    @PostMapping("/canvas/{id}/promote-canary")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasView>> promoteCanary(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> compatibilityService.promoteCanary(tenantId(tenantId), actor(actor), id));
    }

    @PostMapping("/canvas/{id}/rollback-canary")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasView>> rollbackCanary(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> compatibilityService.rollbackCanary(tenantId(tenantId), actor(actor), id));
    }

    @PostMapping("/canvas/{id}/rollback")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasView>> rollback(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> compatibilityService.rollback(tenantId(tenantId), actor(actor), id));
    }

    @PostMapping("/canvas/{id}/clone")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasView>> cloneCanvas(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> compatibilityService.cloneCanvas(tenantId(tenantId), actor(actor), id));
    }

    @GetMapping("/canvas/{id}/versions/{left}/diff/{right}")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.DiffView>> diff(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id,
            @PathVariable Long left,
            @PathVariable Long right) {
        return envelope(() -> compatibilityService.diff(tenantId(tenantId), id, left, right));
    }

    @PutMapping("/canvas/{id}/safe")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasView>> safeUpdate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(compatibilityService.safeUpdate(tenantId(tenantId), actor(actor),
                        id, request));
            } catch (IllegalStateException exception) {
                if ("CANVAS_010".equals(exception.getMessage())) {
                    return CompatibilityEnvelope.<CanvasCompatibilityApplicationService.CanvasView>fail(
                            null,
                            1,
                            "画布已被他人修改，请刷新后重试");
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
            }
        });
    }

    @PostMapping("/canvas/{id}/message-preview")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.MessagePreviewView>> previewMessage(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request) {
        return envelope(() -> compatibilityService.previewMessage(tenantId(tenantId), id, request));
    }

    @GetMapping("/canvas/{id}/export")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasExportView>> exportCanvas(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id,
            @RequestParam Long versionId) {
        return envelope(() -> compatibilityService.exportCanvasView(tenantId(tenantId), id, versionId));
    }

    @PostMapping("/canvas/import")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.CanvasImportView>> importCanvas(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> request) {
        return envelope(() -> compatibilityService.importCanvas(tenantId(tenantId), request));
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

    private static Long tenantId(Long tenantId) {
        return tenantId == null ? 7L : tenantId;
    }

    private static String actor(String actor) {
        return actor == null || actor.isBlank() ? "operator-1" : actor;
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
