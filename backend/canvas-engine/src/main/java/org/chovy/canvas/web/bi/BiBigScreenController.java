package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.bigscreen.BiBigScreenResource;
import org.chovy.canvas.domain.bi.bigscreen.BiBigScreenResourceService;
import org.chovy.canvas.domain.bi.bigscreen.BiBigScreenVersionView;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/canvas/bi/big-screens/resources")
public class BiBigScreenController {

    private final TenantContextResolver tenantContextResolver;
    private final BiBigScreenResourceService bigScreenResourceService;

    public BiBigScreenController(TenantContextResolver tenantContextResolver,
                                 BiBigScreenResourceService bigScreenResourceService) {
        this.tenantContextResolver = tenantContextResolver;
        this.bigScreenResourceService = bigScreenResourceService;
    }

    @GetMapping
    public Mono<R<List<BiBigScreenResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.list(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{screenKey}")
    public Mono<R<BiBigScreenResource>> get(@PathVariable String screenKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.get(context.tenantId(), screenKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{screenKey}/draft")
    public Mono<R<BiBigScreenResource>> saveDraft(@PathVariable String screenKey,
                                                  @RequestBody BiBigScreenResource resource) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!screenKey.equals(resource.screenKey())) {
                        throw new IllegalArgumentException("big screen key does not match request path");
                    }
                    return R.ok(bigScreenResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            resource));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{screenKey}/publish")
    public Mono<R<BiBigScreenResource>> publish(@PathVariable String screenKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.publish(context.tenantId(), context.username(), screenKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/{screenKey}")
    public Mono<R<BiBigScreenResource>> archive(@PathVariable String screenKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.archive(context.tenantId(), screenKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{screenKey}/versions")
    public Mono<R<List<BiBigScreenVersionView>>> listVersions(@PathVariable String screenKey,
                                                              @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.listVersions(context.tenantId(), screenKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{screenKey}/versions/{version}/restore")
    public Mono<R<BiBigScreenResource>> restoreVersion(@PathVariable String screenKey,
                                                       @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(bigScreenResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                screenKey,
                                version)))
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
