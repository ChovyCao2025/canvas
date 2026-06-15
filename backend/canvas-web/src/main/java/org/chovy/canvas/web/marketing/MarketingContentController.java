package org.chovy.canvas.web.marketing;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.marketing.api.MarketingContentFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/marketing/content")
public class MarketingContentController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final MarketingContentFacade facade;

    public MarketingContentController(MarketingContentFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/asset-folders")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> assetFolders(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listAssetFolders(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/asset-folders")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createAssetFolder(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createAssetFolder(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/assets")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> assets(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.listAssets(tenantIdOrDefault(tenantId), keyword, assetType, status));
    }

    @PostMapping("/assets")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createAsset(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createAsset(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/assets/upload-intents")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createUploadIntent(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createUploadIntent(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/assets/upload-intents/expire-stale")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> expireStaleUploadIntents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.expireStaleUploadIntents(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/assets/{assetKey}/status")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> setAssetStatus(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String assetKey,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.setAssetStatus(tenantIdOrDefault(tenantId), assetKey, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/templates")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> templates(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.listTemplates(tenantIdOrDefault(tenantId), keyword, channel, status));
    }

    @PostMapping("/templates")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> saveTemplate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.saveTemplate(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/templates/{templateKey}/preview")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> previewTemplate(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String templateKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.previewTemplate(tenantIdOrDefault(tenantId), templateKey, safePayload(payload)));
    }

    @PostMapping("/templates/{templateKey}/status")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> setTemplateStatus(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String templateKey,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.setTemplateStatus(tenantIdOrDefault(tenantId), templateKey, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/entries")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> entries(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.listEntries(tenantIdOrDefault(tenantId), keyword, contentType, status));
    }

    @PostMapping("/entries")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> saveEntry(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.saveEntry(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/entries/{entryKey}/publish")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> publishEntry(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String entryKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.publishEntry(tenantIdOrDefault(tenantId), entryKey, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/entries/{entryKey}/archive")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> archiveEntry(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String entryKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.archiveEntry(tenantIdOrDefault(tenantId), entryKey, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/releases/validate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> validateRelease(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.validateRelease(tenantIdOrDefault(tenantId), safePayload(payload)));
    }

    @PostMapping("/releases/publish")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> publishRelease(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.publishRelease(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/releases")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> releases(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceKey,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.listReleases(tenantIdOrDefault(tenantId), sourceType, sourceKey, status));
    }

    @PostMapping("/releases/{releaseKey}/resolve")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> resolveRelease(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String releaseKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.resolveRelease(tenantIdOrDefault(tenantId), releaseKey, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/releases/{releaseKey}/rollback")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> rollbackRelease(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String releaseKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.rollbackRelease(tenantIdOrDefault(tenantId), releaseKey, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/audit-events")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> auditEvents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetKey,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.auditEvents(tenantIdOrDefault(tenantId), targetType, targetKey, limit));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
