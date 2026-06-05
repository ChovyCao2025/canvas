package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.chart.BiChartResourceService;
import org.chovy.canvas.domain.bi.chart.BiChartVersionView;
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
@RequestMapping("/canvas/bi/charts/resources")
public class BiChartController {

    private final TenantContextResolver tenantContextResolver;
    private final BiChartResourceService chartResourceService;

    public BiChartController(TenantContextResolver tenantContextResolver,
                             BiChartResourceService chartResourceService) {
        this.tenantContextResolver = tenantContextResolver;
        this.chartResourceService = chartResourceService;
    }

    @GetMapping
    public Mono<R<List<BiChartResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.list(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{chartKey}")
    public Mono<R<BiChartResource>> get(@PathVariable String chartKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.get(context.tenantId(), chartKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{chartKey}/draft")
    public Mono<R<BiChartResource>> saveDraft(@PathVariable String chartKey,
                                              @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                              @RequestBody BiChartResource resource) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!chartKey.equals(resource.chartKey())) {
                        throw new IllegalArgumentException("chart key does not match request path");
                    }
                    return R.ok(chartResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            resource,
                            lockToken));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<R<BiChartResource>> saveDraft(String chartKey, BiChartResource resource) {
        return saveDraft(chartKey, null, resource);
    }

    @PostMapping("/{chartKey}/publish")
    public Mono<R<BiChartResource>> publish(@PathVariable String chartKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.publish(context.tenantId(), context.username(), context.role(), chartKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/{chartKey}")
    public Mono<R<BiChartResource>> archive(@PathVariable String chartKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.archive(context.tenantId(), chartKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{chartKey}/versions")
    public Mono<R<List<BiChartVersionView>>> listVersions(@PathVariable String chartKey,
                                                          @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.listVersions(context.tenantId(), chartKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{chartKey}/versions/{version}/restore")
    public Mono<R<BiChartResource>> restoreVersion(@PathVariable String chartKey,
                                                   @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                   @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(chartResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                chartKey,
                                version,
                                lockToken)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<R<BiChartResource>> restoreVersion(String chartKey, int version) {
        return restoreVersion(chartKey, null, version);
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
