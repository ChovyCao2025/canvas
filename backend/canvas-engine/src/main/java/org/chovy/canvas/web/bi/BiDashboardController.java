package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardCloneCommand;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardExportPackage;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardImportCommand;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateCommand;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateView;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardVersionView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * BiDashboardController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/dashboards/resources")
public class BiDashboardController {

    private final TenantContextResolver tenantContextResolver;
    private final BiDashboardResourceService dashboardResourceService;
    private final BiDashboardRuntimeStateService runtimeStateService;

    /**
     * 创建 BiDashboardController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param dashboardResourceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDashboardController(TenantContextResolver tenantContextResolver,
                                 BiDashboardResourceService dashboardResourceService) {
        this(tenantContextResolver, dashboardResourceService, null);
    }

    /**
     * 创建 BiDashboardController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param dashboardResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runtimeStateService 时间参数，用于计算窗口、过期或审计时间。
     */
    @Autowired
    public BiDashboardController(TenantContextResolver tenantContextResolver,
                                 BiDashboardResourceService dashboardResourceService,
                                 BiDashboardRuntimeStateService runtimeStateService) {
        this.tenantContextResolver = tenantContextResolver;
        this.dashboardResourceService = dashboardResourceService;
        this.runtimeStateService = runtimeStateService;
    }
    /**
     * 查询 BI 看板列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 dashboardResourceService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<BiDashboardResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.list(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取 BI 看板详情接口，对应 GET /{dashboardKey}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 dashboardResourceService.get 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @return 异步返回统一响应，包含获取 BI 看板详情后的业务数据。
     */
    @GetMapping("/{dashboardKey}")
    public Mono<R<BiDashboardResource>> get(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.get(context.tenantId(), dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存 BI 看板配置接口，对应 POST /{dashboardKey}/draft。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 dashboardResourceService.saveDraft 完成业务处理。
     * 副作用：会保存配置或运行态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @param lockToken 请求头参数，可选。
     * @param preset 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含保存 BI 看板配置后的业务数据。
     */
    @PostMapping("/{dashboardKey}/draft")
    public Mono<R<BiDashboardResource>> saveDraft(@PathVariable String dashboardKey,
                                                  @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                  @RequestBody BiDashboardPreset preset) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!dashboardKey.equals(preset.dashboardKey())) {
                        throw new IllegalArgumentException("dashboard key does not match request path");
                    }
                    return R.ok(dashboardResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            preset,
                            lockToken));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 保存 BI 看板配置控制器公开辅助方法，供同类 endpoint 复用。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会保存配置或运行态。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param dashboardKey 看板唯一键。
     * @param preset preset。
     * @return 异步返回统一响应，包含保存 BI 看板配置后的业务数据。
     */
    public Mono<R<BiDashboardResource>> saveDraft(String dashboardKey, BiDashboardPreset preset) {
        return saveDraft(dashboardKey, null, preset);
    }
    /**
     * 发布 BI 看板接口，对应 POST /{dashboardKey}/publish。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 dashboardResourceService.publish 完成业务处理。
     * 副作用：会推进发布状态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @return 异步返回统一响应，包含发布 BI 看板后的业务数据。
     */
    @PostMapping("/{dashboardKey}/publish")
    public Mono<R<BiDashboardResource>> publish(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.publish(context.tenantId(), context.username(), context.role(), dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 克隆 BI 看板接口，对应 POST /{dashboardKey}/clone。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 dashboardResourceService.cloneResource 完成业务处理。
     * 副作用：会复制资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含克隆 BI 看板后的业务数据。
     */
    @PostMapping("/{dashboardKey}/clone")
    public Mono<R<BiDashboardResource>> clone(@PathVariable String dashboardKey,
                                              @RequestBody BiDashboardCloneCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.cloneResource(context.tenantId(), context.username(), dashboardKey, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 导出 BI 看板接口，对应 GET /{dashboardKey}/export。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 dashboardResourceService.exportResource 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @return 异步返回统一响应，包含导出 BI 看板后的业务数据。
     */
    @GetMapping("/{dashboardKey}/export")
    public Mono<R<BiDashboardExportPackage>> exportPackage(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.exportResource(context.tenantId(), context.username(), dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 导出 BI 看板接口，对应 GET /{dashboardKey}/export-file。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 dashboardResourceService.exportResourceFile 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @return 异步返回文件字节流响应。
     */
    @GetMapping("/{dashboardKey}/export-file")
    public Mono<ResponseEntity<byte[]>> exportPackageFile(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    BiDashboardResourceService.DashboardPackageFile file =
                            dashboardResourceService.exportResourceFile(
                                    context.tenantId(),
                                    context.username(),
                                    dashboardKey);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(file.contentType()))
                            .contentLength(file.content().length)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment().filename(file.filename()).build().toString())
                            .body(file.content());
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 导入 BI 看板接口，对应 POST /import。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 dashboardResourceService.importResource 完成业务处理。
     * 副作用：会导入并落库资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含导入 BI 看板后的业务数据。
     */
    @PostMapping("/import")
    public Mono<R<BiDashboardResource>> importPackage(@RequestBody BiDashboardImportCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.importResource(context.tenantId(), context.username(), command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 导入 BI 看板接口，对应 POST /import-file。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 dashboardResourceService.importResourceFile 完成业务处理。
     * 副作用：会导入并落库资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param file 上传文件。
     * @param dashboardKey 看板唯一键。
     * @param title 请求参数，可选。
     * @param overwrite 请求参数，默认值为 false。
     * @return 异步返回统一响应，包含导入 BI 看板后的业务数据。
     */
    @PostMapping(value = "/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<R<BiDashboardResource>> importPackageFile(@RequestPart("file") FilePart file,
                                                          @RequestParam String dashboardKey,
                                                          @RequestParam(required = false) String title,
                                                          @RequestParam(defaultValue = "false") boolean overwrite) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return DataBufferUtils.join(file.content())
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(buffer -> {
                    try {
                        byte[] content = new byte[buffer.readableByteCount()];
                        buffer.read(content);
                        return content;
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .flatMap(content -> currentTenant().flatMap(context -> Mono.fromCallable(() ->
                                R.ok(dashboardResourceService.importResourceFile(
                                        context.tenantId(),
                                        context.username(),
                                        content,
                                        dashboardKey,
                                        title,
                                        overwrite)))
                        .subscribeOn(Schedulers.boundedElastic())));
    }
    /**
     * 归档 BI 看板接口，对应 DELETE /{dashboardKey}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 dashboardResourceService.archive 完成业务处理。
     * 副作用：会归档资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @return 异步返回统一响应，包含归档 BI 看板后的业务数据。
     */
    @DeleteMapping("/{dashboardKey}")
    public Mono<R<BiDashboardResource>> archive(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.archive(context.tenantId(), dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 看板列表接口，对应 GET /{dashboardKey}/versions。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 dashboardResourceService.listVersions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{dashboardKey}/versions")
    public Mono<R<List<BiDashboardVersionView>>> listVersions(@PathVariable String dashboardKey,
                                                              @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.listVersions(context.tenantId(), dashboardKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 恢复 BI 看板 版本接口，对应 POST /{dashboardKey}/versions/{version}/restore。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 dashboardResourceService.restoreVersion 完成业务处理。
     * 副作用：会按指定版本恢复资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @param lockToken 请求头参数，可选。
     * @param version 目标版本号。
     * @return 异步返回统一响应，包含恢复 BI 看板 版本后的业务数据。
     */
    @PostMapping("/{dashboardKey}/versions/{version}/restore")
    public Mono<R<BiDashboardResource>> restoreVersion(@PathVariable String dashboardKey,
                                                       @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                       @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                dashboardKey,
                                version,
                                lockToken)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 恢复 BI 看板 版本控制器公开辅助方法，供同类 endpoint 复用。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会按指定版本恢复资源。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param dashboardKey 看板唯一键。
     * @param version 目标版本号。
     * @return 异步返回统一响应，包含恢复 BI 看板 版本后的业务数据。
     */
    public Mono<R<BiDashboardResource>> restoreVersion(String dashboardKey, int version) {
        return restoreVersion(dashboardKey, null, version);
    }
    /**
     * 获取 BI 看板详情接口，对应 GET /{dashboardKey}/runtime-state。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @return 异步返回统一响应，包含获取 BI 看板详情后的业务数据。
     */
    @GetMapping("/{dashboardKey}/runtime-state")
    public Mono<R<BiDashboardRuntimeStateView>> getRuntimeState(@PathVariable String dashboardKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeStateService().get(context.tenantId(), context.username(), dashboardKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 BI 看板运行接口，对应 POST /{dashboardKey}/runtime-state。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 副作用：会触发一次运行流程，会保存配置或运行态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param dashboardKey 看板唯一键。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含触发 BI 看板运行后的业务数据。
     */
    @PostMapping("/{dashboardKey}/runtime-state")
    public Mono<R<BiDashboardRuntimeStateView>> saveRuntimeState(@PathVariable String dashboardKey,
                                                                 @RequestBody BiDashboardRuntimeStateCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeStateService().save(context.tenantId(), context.username(), dashboardKey, command)))
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

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireRuntimeStateService 流程生成的业务结果。
     */
    private BiDashboardRuntimeStateService requireRuntimeStateService() {
        if (runtimeStateService == null) {
            throw new IllegalStateException("BI dashboard runtime state service is required");
        }
        return runtimeStateService;
    }
}
