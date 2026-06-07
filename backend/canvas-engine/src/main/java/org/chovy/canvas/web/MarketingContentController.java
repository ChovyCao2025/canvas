package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.content.ContentEntryService;
import org.chovy.canvas.domain.content.ContentTemplateService;
import org.chovy.canvas.domain.content.MarketingAssetService;
import org.chovy.canvas.domain.content.MarketingAssetUploadService;
import org.chovy.canvas.domain.content.MarketingContentReleaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/marketing/content")
public class MarketingContentController {

    private final MarketingAssetService assetService;
    private final ContentTemplateService templateService;
    private final ContentEntryService entryService;
    private final MarketingAssetUploadService uploadService;
    private final MarketingContentReleaseService releaseService;
    private final TenantContextResolver tenantContextResolver;

    public MarketingContentController(MarketingAssetService assetService,
                                      ContentTemplateService templateService,
                                      ContentEntryService entryService,
                                      MarketingAssetUploadService uploadService,
                                      MarketingContentReleaseService releaseService,
                                      TenantContextResolver tenantContextResolver) {
        this.assetService = assetService;
        this.templateService = templateService;
        this.entryService = entryService;
        this.uploadService = uploadService;
        this.releaseService = releaseService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/asset-folders")
    public Mono<R<List<MarketingAssetService.FolderView>>> assetFolders() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(assetService.listFolders(context)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/asset-folders")
    public Mono<R<MarketingAssetService.FolderView>> createAssetFolder(
            @RequestBody MarketingAssetService.FolderCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(assetService.createFolder(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/assets")
    public Mono<R<List<MarketingAssetService.AssetView>>> assets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(assetService.list(context, keyword, assetType, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/assets")
    public Mono<R<MarketingAssetService.AssetView>> createAsset(@RequestBody MarketingAssetService.AssetCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(assetService.create(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/assets/upload-intents")
    public Mono<R<MarketingAssetUploadService.UploadIntentView>> createUploadIntent(
            @RequestBody MarketingAssetUploadService.UploadIntentCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(uploadService.createIntent(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/assets/upload-intents/expire-stale")
    public Mono<R<MarketingAssetUploadService.UploadIntentCleanupResult>> expireStaleUploadIntents(
            @RequestBody(required = false) MarketingAssetUploadService.UploadIntentCleanupCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(uploadService.expireStalePendingUploads(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/assets/{assetKey}/status")
    public Mono<R<MarketingAssetService.AssetView>> setAssetStatus(
            @PathVariable String assetKey,
            @RequestBody MarketingAssetService.AssetStatusCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(assetService.setStatus(context, assetKey, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/templates")
    public Mono<R<List<ContentTemplateService.TemplateView>>> templates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(templateService.list(context, keyword, channel, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/templates")
    public Mono<R<ContentTemplateService.TemplateView>> saveTemplate(
            @RequestBody ContentTemplateService.TemplateCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(templateService.save(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/templates/{templateKey}/preview")
    public Mono<R<ContentTemplateService.PreviewResult>> previewTemplate(
            @PathVariable String templateKey,
            @RequestBody Map<String, Object> context) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(templateService.preview(tenant, templateKey, context)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/templates/{templateKey}/status")
    public Mono<R<ContentTemplateService.TemplateView>> setTemplateStatus(
            @PathVariable String templateKey,
            @RequestBody ContentTemplateService.TemplateStatusCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(templateService.setStatus(tenant, templateKey, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/entries")
    public Mono<R<List<ContentEntryService.EntryView>>> entries(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(entryService.list(context, keyword, contentType, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/entries")
    public Mono<R<ContentEntryService.EntryView>> saveEntry(@RequestBody ContentEntryService.EntryCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(entryService.saveDraft(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/entries/{entryKey}/publish")
    public Mono<R<ContentEntryService.EntryView>> publishEntry(
            @PathVariable String entryKey,
            @RequestBody ContentEntryService.EntryStatusCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(entryService.publish(context, entryKey, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/entries/{entryKey}/archive")
    public Mono<R<ContentEntryService.EntryView>> archiveEntry(
            @PathVariable String entryKey,
            @RequestBody ContentEntryService.EntryStatusCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(entryService.archive(context, entryKey, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/releases/validate")
    public Mono<R<MarketingContentReleaseService.ValidationResult>> validateRelease(
            @RequestBody MarketingContentReleaseService.ValidationCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(releaseService.validate(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/releases/publish")
    public Mono<R<MarketingContentReleaseService.ReleaseView>> publishRelease(
            @RequestBody MarketingContentReleaseService.ReleaseCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(releaseService.publish(context, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/releases")
    public Mono<R<List<MarketingContentReleaseService.ReleaseView>>> releases(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceKey,
            @RequestParam(required = false) String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(releaseService.list(context, sourceType, sourceKey, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/releases/{releaseKey}/resolve")
    public Mono<R<MarketingContentReleaseService.ResolvedRelease>> resolveRelease(
            @PathVariable String releaseKey,
            @RequestBody(required = false) Map<String, Object> context) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(releaseService.resolve(
                                tenant,
                                releaseKey,
                                context == null ? Map.of() : context)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/releases/{releaseKey}/rollback")
    public Mono<R<MarketingContentReleaseService.ReleaseView>> rollbackRelease(
            @PathVariable String releaseKey,
            @RequestBody MarketingContentReleaseService.RollbackCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(releaseService.rollback(tenant, releaseKey, command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/audit-events")
    public Mono<R<List<MarketingContentReleaseService.AuditEventView>>> auditEvents(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetKey,
            @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(releaseService.auditEvents(tenant, targetType, targetKey, limit)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
