package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CdpComputedProfileAttributeDO;
import org.chovy.canvas.dal.dataobject.CdpComputedProfileRunDO;
import org.chovy.canvas.dal.dataobject.CdpProfileAttributeChangeLogDO;
import org.chovy.canvas.domain.cdp.ComputedProfileAttributeService;
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
@RequestMapping("/cdp/computed-profile-attributes")
@RequiredArgsConstructor
public class CdpComputedProfileController {
    private final TenantContextResolver tenantContextResolver;
    private final ComputedProfileAttributeService service;

    @GetMapping
    public Mono<R<List<CdpComputedProfileAttributeDO>>> list() {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.list(tenantId(ctx))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    public Mono<R<CdpComputedProfileAttributeDO>> create(@RequestBody CdpComputedProfileAttributeDO body) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.create(tenantId(ctx), body, ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/preview")
    public Mono<R<ComputedProfileAttributeService.PreviewResult>> preview(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.preview(tenantId(ctx), id)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/activate")
    public Mono<R<Void>> activate(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    service.activate(tenantId(ctx), id);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/pause")
    public Mono<R<Void>> pause(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    service.pause(tenantId(ctx), id);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/run")
    public Mono<R<ComputedProfileAttributeService.RunResult>> run(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.runNow(tenantId(ctx), id, ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}/runs")
    public Mono<R<List<CdpComputedProfileRunDO>>> runs(@PathVariable Long id,
                                                       @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.listRuns(tenantId(ctx), id, normalizeLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}/changes")
    public Mono<R<List<CdpProfileAttributeChangeLogDO>>> changes(@PathVariable Long id,
                                                                 @RequestParam(required = false) String userId,
                                                                 @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.listChangeLogs(tenantId(ctx), id, userId, normalizeLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }

    private Integer normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
    }
}
