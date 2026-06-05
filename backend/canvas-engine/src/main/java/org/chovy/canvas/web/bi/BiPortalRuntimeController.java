package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.portal.BiPortalResource;
import org.chovy.canvas.domain.bi.portal.BiPortalRuntimeService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/bi/portals/runtime")
public class BiPortalRuntimeController {

    private final TenantContextResolver tenantContextResolver;
    private final BiPortalRuntimeService portalRuntimeService;

    public BiPortalRuntimeController(TenantContextResolver tenantContextResolver,
                                     BiPortalRuntimeService portalRuntimeService) {
        this.tenantContextResolver = tenantContextResolver;
        this.portalRuntimeService = portalRuntimeService;
    }

    @GetMapping
    public Mono<R<List<BiPortalResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalRuntimeService.listPublished(
                                context.tenantId(),
                                queryContext(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{portalKey}")
    public Mono<R<BiPortalResource>> get(@PathVariable String portalKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(portalRuntimeService.getPublished(
                                context.tenantId(),
                                portalKey,
                                queryContext(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private BiQueryContext queryContext(TenantContext context) {
        return new BiQueryContext(context.tenantId(), context.username(), context.role());
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
