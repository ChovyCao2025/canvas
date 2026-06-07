package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CdpComputedTagDefinitionDO;
import org.chovy.canvas.dal.dataobject.CdpComputedTagRunDO;
import org.chovy.canvas.domain.cdp.CdpLineageService;
import org.chovy.canvas.domain.cdp.ComputedTagService;
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
@RequestMapping("/cdp/computed-tags")
@RequiredArgsConstructor
public class CdpComputedTagController {
    private final TenantContextResolver tenantContextResolver;
    private final ComputedTagService tagService;
    private final CdpLineageService lineageService;

    public record ComputedTagRequest(
            String tagCode,
            String displayName,
            String valueType,
            String computeType,
            String expressionJson,
            String refreshMode,
            List<String> dependencies
    ) {
    }

    public record ImpactCheckRequest(String oldValueType, String newValueType) {
    }

    @GetMapping
    public Mono<R<List<CdpComputedTagDefinitionDO>>> list() {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.list(tenantId(ctx))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    public Mono<R<CdpComputedTagDefinitionDO>> create(@RequestBody ComputedTagRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.create(tenantId(ctx), toCommand(request), ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/preview")
    public Mono<R<ComputedTagService.PreviewResult>> preview(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.preview(tenantId(ctx), tagCode)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/activate")
    public Mono<R<Void>> activate(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    tagService.activate(tenantId(ctx), tagCode);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/pause")
    public Mono<R<Void>> pause(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    tagService.pause(tenantId(ctx), tagCode);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/run")
    public Mono<R<ComputedTagService.RunResult>> run(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.runNow(tenantId(ctx), tagCode, ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{tagCode}/runs")
    public Mono<R<List<CdpComputedTagRunDO>>> runs(@PathVariable String tagCode,
                                                   @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.listRuns(tenantId(ctx), tagCode, normalizeLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{tagCode}/lineage")
    public Mono<R<List<CdpLineageService.LineageImpact>>> lineage(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(lineageService.findTagLineage(tenantId(ctx), tagCode)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/impact-check")
    public Mono<R<CdpLineageService.ImpactCheck>> impactCheck(@PathVariable String tagCode,
                                                              @RequestBody ImpactCheckRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(lineageService.checkTypeChange(
                                tenantId(ctx),
                                tagCode,
                                request.oldValueType(),
                                request.newValueType())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private ComputedTagService.DefinitionCommand toCommand(ComputedTagRequest request) {
        return new ComputedTagService.DefinitionCommand(
                request.tagCode(),
                request.displayName(),
                request.valueType(),
                request.computeType(),
                request.expressionJson(),
                request.refreshMode(),
                request.dependencies());
    }

    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }

    private Integer normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
    }
}
