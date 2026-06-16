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

/**
 * CdpComputedProfileController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/cdp/computed-profile-attributes")
@RequiredArgsConstructor
public class CdpComputedProfileController {
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final ComputedProfileAttributeService service;
    /**
     * 查询 CDP 计算画像列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<CdpComputedProfileAttributeDO>>> list() {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.list(tenantId(ctx))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 CDP 计算画像接口，对应 POST 请求。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param body 请求体。
     * @return 异步返回统一响应，包含创建 CDP 计算画像后的业务数据。
     */
    @PostMapping
    public Mono<R<CdpComputedProfileAttributeDO>> create(@RequestBody CdpComputedProfileAttributeDO body) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.create(tenantId(ctx), body, ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 预览 CDP 计算画像结果接口，对应 POST /{id}/preview。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含预览 CDP 计算画像结果后的业务数据。
     */
    @PostMapping("/{id}/preview")
    public Mono<R<ComputedProfileAttributeService.PreviewResult>> preview(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.preview(tenantId(ctx), id)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 启用 CDP 计算画像接口，对应 POST /{id}/activate。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会启用资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @PostMapping("/{id}/activate")
    public Mono<R<Void>> activate(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    service.activate(tenantId(ctx), id);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 暂停 CDP 计算画像接口，对应 POST /{id}/pause。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会暂停资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @PostMapping("/{id}/pause")
    public Mono<R<Void>> pause(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    service.pause(tenantId(ctx), id);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 CDP 计算画像运行接口，对应 POST /{id}/run。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含触发 CDP 计算画像运行后的业务数据。
     */
    @PostMapping("/{id}/run")
    public Mono<R<ComputedProfileAttributeService.RunResult>> run(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.runNow(tenantId(ctx), id, ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 CDP 计算画像运行接口，对应 GET /{id}/runs。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{id}/runs")
    public Mono<R<List<CdpComputedProfileRunDO>>> runs(@PathVariable Long id,
                                                       @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.listRuns(tenantId(ctx), id, normalizeLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 CDP 计算画像 请求接口，对应 GET /{id}/changes。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param userId user ID，可选。
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{id}/changes")
    public Mono<R<List<CdpProfileAttributeChangeLogDO>>> changes(@PathVariable Long id,
                                                                 @RequestParam(required = false) String userId,
                                                                 @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.listChangeLogs(tenantId(ctx), id, userId, normalizeLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param ctx ctx 参数，用于 tenantId 流程中的校验、计算或对象转换。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }

    /**
     * 规范化输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Integer normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
    }
}
