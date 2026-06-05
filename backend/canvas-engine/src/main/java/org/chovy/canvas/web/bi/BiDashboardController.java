package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardCloneCommand;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardExportPackage;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardImportCommand;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardVersionView;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/bi/dashboards/resources")
public class BiDashboardController {

    private final TenantContextResolver tenantContextResolver;
    private final BiDashboardResourceService dashboardResourceService;

    public BiDashboardController(TenantContextResolver tenantContextResolver,
                                 BiDashboardResourceService dashboardResourceService) {
        this.tenantContextResolver = tenantContextResolver;
        this.dashboardResourceService = dashboardResourceService;
    }

    @GetMapping
    public Mono<R<List<BiDashboardResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.list(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{dashboardKey}")
    public Mono<R<BiDashboardResource>> get(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.get(context.tenantId(), dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{dashboardKey}/draft")
    public Mono<R<BiDashboardResource>> saveDraft(@PathVariable String dashboardKey,
                                                  @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                  @RequestBody BiDashboardPreset preset) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!dashboardKey.equals(preset.dashboardKey())) {
                        throw new IllegalArgumentException("dashboard key does not match request path");
                    }
                    return R.ok(dashboardResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            preset,
                            lockToken));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<R<BiDashboardResource>> saveDraft(String dashboardKey, BiDashboardPreset preset) {
        return saveDraft(dashboardKey, null, preset);
    }

    @PostMapping("/{dashboardKey}/publish")
    public Mono<R<BiDashboardResource>> publish(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.publish(context.tenantId(), context.username(), context.role(), dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{dashboardKey}/clone")
    public Mono<R<BiDashboardResource>> clone(@PathVariable String dashboardKey,
                                              @RequestBody BiDashboardCloneCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.cloneResource(context.tenantId(), context.username(), dashboardKey, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{dashboardKey}/export")
    public Mono<R<BiDashboardExportPackage>> exportPackage(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.exportResource(context.tenantId(), context.username(), dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{dashboardKey}/export-file")
    public Mono<ResponseEntity<byte[]>> exportPackageFile(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    BiDashboardResourceService.DashboardPackageFile file =
                            dashboardResourceService.exportResourceFile(
                                    context.tenantId(),
                                    context.username(),
                                    dashboardKey);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(file.contentType()))
                            .contentLength(file.content().length)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment().filename(file.filename()).build().toString())
                            .body(file.content());
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/import")
    public Mono<R<BiDashboardResource>> importPackage(@RequestBody BiDashboardImportCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.importResource(context.tenantId(), context.username(), command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping(value = "/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<R<BiDashboardResource>> importPackageFile(@RequestPart("file") FilePart file,
                                                          @RequestParam String dashboardKey,
                                                          @RequestParam(required = false) String title,
                                                          @RequestParam(defaultValue = "false") boolean overwrite) {
        return DataBufferUtils.join(file.content())
                .map(buffer -> {
                    try {
                        byte[] content = new byte[buffer.readableByteCount()];
                        buffer.read(content);
                        return content;
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .flatMap(content -> currentTenant().flatMap(context -> Mono.fromCallable(() ->
                                R.ok(dashboardResourceService.importResourceFile(
                                        context.tenantId(),
                                        context.username(),
                                        content,
                                        dashboardKey,
                                        title,
                                        overwrite)))
                        .subscribeOn(Schedulers.boundedElastic())));
    }

    @DeleteMapping("/{dashboardKey}")
    public Mono<R<BiDashboardResource>> archive(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.archive(context.tenantId(), dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{dashboardKey}/versions")
    public Mono<R<List<BiDashboardVersionView>>> listVersions(@PathVariable String dashboardKey,
                                                              @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.listVersions(context.tenantId(), dashboardKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{dashboardKey}/versions/{version}/restore")
    public Mono<R<BiDashboardResource>> restoreVersion(@PathVariable String dashboardKey,
                                                       @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                       @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                dashboardKey,
                                version,
                                lockToken)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<R<BiDashboardResource>> restoreVersion(String dashboardKey, int version) {
        return restoreVersion(dashboardKey, null, version);
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
