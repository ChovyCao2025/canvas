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
/**
 * CdpComputedTagController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class CdpComputedTagController {
    private final TenantContextResolver tenantContextResolver;
    private final ComputedTagService tagService;
    private final CdpLineageService lineageService;

    /**
     * ComputedTagRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
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

    /**
     * ImpactCheckRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record ImpactCheckRequest(String oldValueType, String newValueType) {
    }

    @GetMapping
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<CdpComputedTagDefinitionDO>>> list() {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.list(tenantId(ctx))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<CdpComputedTagDefinitionDO>> create(@RequestBody ComputedTagRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.create(tenantId(ctx), toCommand(request), ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/preview")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 preview 流程生成的业务结果。
     */
    public Mono<R<ComputedTagService.PreviewResult>> preview(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.preview(tenantId(ctx), tagCode)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/activate")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 activate 流程生成的业务结果。
     */
    public Mono<R<Void>> activate(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    tagService.activate(tenantId(ctx), tagCode);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/pause")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 pause 流程生成的业务结果。
     */
    public Mono<R<Void>> pause(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    tagService.pause(tenantId(ctx), tagCode);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/run")
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<ComputedTagService.RunResult>> run(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.runNow(tenantId(ctx), tagCode, ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{tagCode}/runs")
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<List<CdpComputedTagRunDO>>> runs(@PathVariable String tagCode,
                                                   @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.listRuns(tenantId(ctx), tagCode, normalizeLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{tagCode}/lineage")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 lineage 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<CdpLineageService.LineageImpact>>> lineage(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(lineageService.findTagLineage(tenantId(ctx), tagCode)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/impact-check")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 impactCheck 流程生成的业务结果。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param ctx ctx 参数，用于 tenantId 流程中的校验、计算或对象转换。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Integer normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
    }
}
