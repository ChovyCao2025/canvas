package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.spreadsheet.BiSpreadsheetResource;
import org.chovy.canvas.domain.bi.spreadsheet.BiSpreadsheetResourceService;
import org.chovy.canvas.domain.bi.spreadsheet.BiSpreadsheetVersionView;
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
@RequestMapping("/canvas/bi/spreadsheets/resources")
public class BiSpreadsheetController {

    private final TenantContextResolver tenantContextResolver;
    private final BiSpreadsheetResourceService spreadsheetResourceService;

    public BiSpreadsheetController(TenantContextResolver tenantContextResolver,
                                   BiSpreadsheetResourceService spreadsheetResourceService) {
        this.tenantContextResolver = tenantContextResolver;
        this.spreadsheetResourceService = spreadsheetResourceService;
    }

    @GetMapping
    public Mono<R<List<BiSpreadsheetResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.list(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{spreadsheetKey}")
    public Mono<R<BiSpreadsheetResource>> get(@PathVariable String spreadsheetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.get(context.tenantId(), spreadsheetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{spreadsheetKey}/draft")
    public Mono<R<BiSpreadsheetResource>> saveDraft(@PathVariable String spreadsheetKey,
                                                    @RequestBody BiSpreadsheetResource resource) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!spreadsheetKey.equals(resource.spreadsheetKey())) {
                        throw new IllegalArgumentException("spreadsheet key does not match request path");
                    }
                    return R.ok(spreadsheetResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            resource));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{spreadsheetKey}/publish")
    public Mono<R<BiSpreadsheetResource>> publish(@PathVariable String spreadsheetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.publish(context.tenantId(), context.username(), spreadsheetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/{spreadsheetKey}")
    public Mono<R<BiSpreadsheetResource>> archive(@PathVariable String spreadsheetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.archive(context.tenantId(), spreadsheetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{spreadsheetKey}/versions")
    public Mono<R<List<BiSpreadsheetVersionView>>> listVersions(@PathVariable String spreadsheetKey,
                                                                @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.listVersions(context.tenantId(), spreadsheetKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{spreadsheetKey}/versions/{version}/restore")
    public Mono<R<BiSpreadsheetResource>> restoreVersion(@PathVariable String spreadsheetKey,
                                                         @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                spreadsheetKey,
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
