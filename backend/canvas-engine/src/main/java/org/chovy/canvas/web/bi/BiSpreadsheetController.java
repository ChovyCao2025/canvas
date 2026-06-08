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

/**
 * BiSpreadsheetController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/spreadsheets/resources")
public class BiSpreadsheetController {

    private final TenantContextResolver tenantContextResolver;
    private final BiSpreadsheetResourceService spreadsheetResourceService;

    /**
     * 创建 BiSpreadsheetController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param spreadsheetResourceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiSpreadsheetController(TenantContextResolver tenantContextResolver,
                                   BiSpreadsheetResourceService spreadsheetResourceService) {
        this.tenantContextResolver = tenantContextResolver;
        this.spreadsheetResourceService = spreadsheetResourceService;
    }
    /**
     * 查询 BI 电子表格列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 spreadsheetResourceService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<BiSpreadsheetResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.list(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取 BI 电子表格详情接口，对应 GET /{spreadsheetKey}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 spreadsheetResourceService.get 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param spreadsheetKey 电子表格唯一键。
     * @return 异步返回统一响应，包含获取 BI 电子表格详情后的业务数据。
     */
    @GetMapping("/{spreadsheetKey}")
    public Mono<R<BiSpreadsheetResource>> get(@PathVariable String spreadsheetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.get(context.tenantId(), spreadsheetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存 BI 电子表格配置接口，对应 POST /{spreadsheetKey}/draft。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 spreadsheetResourceService.saveDraft 完成业务处理。
     * 副作用：会保存配置或运行态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param spreadsheetKey 电子表格唯一键。
     * @param resource 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含保存 BI 电子表格配置后的业务数据。
     */
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
    /**
     * 发布 BI 电子表格接口，对应 POST /{spreadsheetKey}/publish。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 spreadsheetResourceService.publish 完成业务处理。
     * 副作用：会推进发布状态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param spreadsheetKey 电子表格唯一键。
     * @return 异步返回统一响应，包含发布 BI 电子表格后的业务数据。
     */
    @PostMapping("/{spreadsheetKey}/publish")
    public Mono<R<BiSpreadsheetResource>> publish(@PathVariable String spreadsheetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.publish(context.tenantId(), context.username(), spreadsheetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 归档 BI 电子表格接口，对应 DELETE /{spreadsheetKey}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 spreadsheetResourceService.archive 完成业务处理。
     * 副作用：会归档资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param spreadsheetKey 电子表格唯一键。
     * @return 异步返回统一响应，包含归档 BI 电子表格后的业务数据。
     */
    @DeleteMapping("/{spreadsheetKey}")
    public Mono<R<BiSpreadsheetResource>> archive(@PathVariable String spreadsheetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.archive(context.tenantId(), spreadsheetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 电子表格列表接口，对应 GET /{spreadsheetKey}/versions。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 spreadsheetResourceService.listVersions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param spreadsheetKey 电子表格唯一键。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{spreadsheetKey}/versions")
    public Mono<R<List<BiSpreadsheetVersionView>>> listVersions(@PathVariable String spreadsheetKey,
                                                                @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(spreadsheetResourceService.listVersions(context.tenantId(), spreadsheetKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 恢复 BI 电子表格 版本接口，对应 POST /{spreadsheetKey}/versions/{version}/restore。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 spreadsheetResourceService.restoreVersion 完成业务处理。
     * 副作用：会按指定版本恢复资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param spreadsheetKey 电子表格唯一键。
     * @param version 目标版本号。
     * @return 异步返回统一响应，包含恢复 BI 电子表格 版本后的业务数据。
     */
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

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
