package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetVersionView;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import java.util.List;

@RestController
@RequestMapping("/canvas/bi/datasets/resources")
public class BiDatasetController {

    private final TenantContextResolver tenantContextResolver;
    private final BiDatasetResourceService datasetResourceService;

    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService) {
        this.tenantContextResolver = tenantContextResolver;
        this.datasetResourceService = datasetResourceService;
    }

    @GetMapping
    public Mono<R<List<BiDatasetResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.listResources(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{datasetKey}")
    public Mono<R<BiDatasetResource>> get(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.getResource(context.tenantId(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{datasetKey}/draft")
    public Mono<R<BiDatasetResource>> saveDraft(@PathVariable String datasetKey,
                                                @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                @RequestBody BiDatasetResource resource) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!datasetKey.equals(resource.datasetKey())) {
                        throw new IllegalArgumentException("dataset key does not match request path");
                    }
                    return R.ok(datasetResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            resource,
                            lockToken));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<R<BiDatasetResource>> saveDraft(String datasetKey, BiDatasetResource resource) {
        return saveDraft(datasetKey, null, resource);
    }

    @PostMapping("/{datasetKey}/publish")
    public Mono<R<BiDatasetResource>> publish(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.publish(context.tenantId(), context.username(), context.role(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/{datasetKey}")
    public Mono<R<BiDatasetResource>> archive(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.archive(context.tenantId(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{datasetKey}/versions")
    public Mono<R<List<BiDatasetVersionView>>> listVersions(@PathVariable String datasetKey,
                                                            @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.listVersions(context.tenantId(), datasetKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{datasetKey}/versions/{version}/restore")
    public Mono<R<BiDatasetResource>> restoreVersion(@PathVariable String datasetKey,
                                                     @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                     @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                datasetKey,
                                version,
                                lockToken)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<R<BiDatasetResource>> restoreVersion(String datasetKey, int version) {
        return restoreVersion(datasetKey, null, version);
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
