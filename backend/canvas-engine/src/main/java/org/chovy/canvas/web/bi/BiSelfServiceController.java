package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.export.BiExportCleanupResult;
import org.chovy.canvas.domain.bi.export.BiExportApprovalReviewCommand;
import org.chovy.canvas.domain.bi.export.BiExportDownload;
import org.chovy.canvas.domain.bi.export.BiExportJobDetailView;
import org.chovy.canvas.domain.bi.export.BiExportJobCommand;
import org.chovy.canvas.domain.bi.export.BiExportJobView;
import org.chovy.canvas.domain.bi.export.BiExportQueueResult;
import org.chovy.canvas.domain.bi.export.BiExportRetryResult;
import org.chovy.canvas.domain.bi.export.BiSelfServiceExportService;
import org.chovy.canvas.domain.bi.export.BiSelfServicePreviewRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/canvas/bi/self-service")
public class BiSelfServiceController {

    private final TenantContextResolver tenantContextResolver;
    private final BiSelfServiceExportService exportService;

    public BiSelfServiceController(TenantContextResolver tenantContextResolver,
                                   BiSelfServiceExportService exportService) {
        this.tenantContextResolver = tenantContextResolver;
        this.exportService = exportService;
    }

    @PostMapping("/preview")
    public Mono<R<BiQueryResult>> preview(@RequestBody BiSelfServicePreviewRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.preview(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/exports")
    public Mono<R<BiExportJobView>> createExport(@RequestBody BiExportJobCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.createExport(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/exports")
    public Mono<R<List<BiExportJobView>>> listExports(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.listExports(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/exports/{id}/review")
    public Mono<R<BiExportJobView>> reviewExport(@PathVariable Long id,
                                                 @RequestBody BiExportApprovalReviewCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.reviewExport(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                id,
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/exports/{id}")
    public Mono<R<BiExportJobDetailView>> getExportDetail(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.getExportDetail(context.tenantId(), id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/exports/{id}/download")
    public Mono<ResponseEntity<byte[]>> download(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    BiExportDownload file = exportService.download(context.tenantId(), context.username(), id);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment()
                                            .filename(file.filename())
                                            .build()
                                            .toString())
                            .body(file.bytes());
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/exports/{id}/cancel")
    public Mono<R<BiExportJobView>> cancelExport(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.cancelExport(
                                context.tenantId(),
                                context.username(),
                                id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/exports/cleanup")
    public Mono<R<BiExportCleanupResult>> cleanupExports(@RequestParam(defaultValue = "100") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.cleanupExpiredExports(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/exports/retry")
    public Mono<R<BiExportRetryResult>> retryExports(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.retryFailedExports(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/exports/queue/run")
    public Mono<R<BiExportQueueResult>> runExportQueue(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.processQueuedExports(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
