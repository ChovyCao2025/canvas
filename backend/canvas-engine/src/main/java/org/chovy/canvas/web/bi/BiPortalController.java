package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.portal.BiPortalResource;
import org.chovy.canvas.domain.bi.portal.BiPortalResourceService;
import org.chovy.canvas.domain.bi.portal.BiPortalVersionView;
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
@RequestMapping("/canvas/bi/portals/resources")
public class BiPortalController {

    private final TenantContextResolver tenantContextResolver;
    private final BiPortalResourceService portalResourceService;

    public BiPortalController(TenantContextResolver tenantContextResolver,
                              BiPortalResourceService portalResourceService) {
        this.tenantContextResolver = tenantContextResolver;
        this.portalResourceService = portalResourceService;
    }

    @GetMapping
    public Mono<R<List<BiPortalResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.list(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{portalKey}")
    public Mono<R<BiPortalResource>> get(@PathVariable String portalKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.get(context.tenantId(), portalKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{portalKey}/draft")
    public Mono<R<BiPortalResource>> saveDraft(@PathVariable String portalKey,
                                               @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                               @RequestBody BiPortalResource resource) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!portalKey.equals(resource.portalKey())) {
                        throw new IllegalArgumentException("portal key does not match request path");
                    }
                    return R.ok(portalResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            resource,
                            lockToken));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<R<BiPortalResource>> saveDraft(String portalKey, BiPortalResource resource) {
        return saveDraft(portalKey, null, resource);
    }

    @PostMapping("/{portalKey}/publish")
    public Mono<R<BiPortalResource>> publish(@PathVariable String portalKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.publish(context.tenantId(), context.username(), context.role(), portalKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/{portalKey}")
    public Mono<R<BiPortalResource>> archive(@PathVariable String portalKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.archive(context.tenantId(), portalKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{portalKey}/versions")
    public Mono<R<List<BiPortalVersionView>>> listVersions(@PathVariable String portalKey,
                                                           @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.listVersions(context.tenantId(), portalKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{portalKey}/versions/{version}/restore")
    public Mono<R<BiPortalResource>> restoreVersion(@PathVariable String portalKey,
                                                    @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                    @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                portalKey,
                                version,
                                lockToken)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<R<BiPortalResource>> restoreVersion(String portalKey, int version) {
        return restoreVersion(portalKey, null, version);
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
