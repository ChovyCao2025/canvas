package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.permission.BiColumnPermissionCommand;
import org.chovy.canvas.domain.bi.permission.BiColumnPermissionView;
import org.chovy.canvas.domain.bi.permission.BiPermissionAuditEntry;
import org.chovy.canvas.domain.bi.permission.BiPermissionAdminService;
import org.chovy.canvas.domain.bi.permission.BiPermissionRequestCommand;
import org.chovy.canvas.domain.bi.permission.BiPermissionRequestReviewCommand;
import org.chovy.canvas.domain.bi.permission.BiPermissionRequestService;
import org.chovy.canvas.domain.bi.permission.BiPermissionRequestView;
import org.chovy.canvas.domain.bi.permission.BiResourcePermissionCommand;
import org.chovy.canvas.domain.bi.permission.BiResourcePermissionView;
import org.chovy.canvas.domain.bi.permission.BiRowPermissionCommand;
import org.chovy.canvas.domain.bi.permission.BiRowPermissionView;
import org.springframework.beans.factory.annotation.Autowired;
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
 * BiPermissionController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/permissions")
public class BiPermissionController {

    private final TenantContextResolver tenantContextResolver;
    private final BiPermissionAdminService permissionAdminService;
    private final BiPermissionRequestService permissionRequestService;

    /**
     * 创建 BiPermissionController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param permissionAdminService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiPermissionController(TenantContextResolver tenantContextResolver,
                                  BiPermissionAdminService permissionAdminService) {
        this(tenantContextResolver, permissionAdminService, null);
    }

    /**
     * 创建 BiPermissionController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param permissionAdminService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionRequestService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiPermissionController(TenantContextResolver tenantContextResolver,
                                  BiPermissionAdminService permissionAdminService,
                                  BiPermissionRequestService permissionRequestService) {
        this.tenantContextResolver = tenantContextResolver;
        this.permissionAdminService = permissionAdminService;
        this.permissionRequestService = permissionRequestService;
    }
    /**
     * 查询 BI 权限列表接口，对应 GET /resources。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 permissionAdminService.listResourcePermissions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param resourceType 请求参数，可选。
     * @param resourceKey resource 唯一键，可选。
     * @param resourceId resource ID，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/resources")
    public Mono<R<List<BiResourcePermissionView>>> listResourcePermissions(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceKey,
            @RequestParam(required = false) Long resourceId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.listResourcePermissions(
                                context.tenantId(),
                                resourceType,
                                resourceKey,
                                resourceId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 BI 权限接口，对应 POST /resources。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 permissionAdminService.upsertResourcePermission 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 BI 权限后的业务数据。
     */
    @PostMapping("/resources")
    public Mono<R<BiResourcePermissionView>> upsertResourcePermission(
            @RequestBody BiResourcePermissionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.upsertResourcePermission(
                                context.tenantId(),
                                context.username(),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 删除 BI 权限接口，对应 DELETE /resources/{id}。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 permissionAdminService.deleteResourcePermission 完成业务处理。
     * 副作用：会删除或逻辑移除记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @DeleteMapping("/resources/{id}")
    public Mono<R<Void>> deleteResourcePermission(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionAdminService.deleteResourcePermission(context.tenantId(), context.username(), id);
                    return R.<Void>ok(null);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 权限列表接口，对应 GET /rows。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 permissionAdminService.listRowPermissions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/rows")
    public Mono<R<List<BiRowPermissionView>>> listRowPermissions(
            @RequestParam(required = false) String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.listRowPermissions(context.tenantId(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 BI 权限接口，对应 POST /rows。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 permissionAdminService.upsertRowPermission 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 BI 权限后的业务数据。
     */
    @PostMapping("/rows")
    public Mono<R<BiRowPermissionView>> upsertRowPermission(
            @RequestBody BiRowPermissionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.upsertRowPermission(context.tenantId(), context.username(), command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 删除 BI 权限接口，对应 DELETE /rows/{id}。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 permissionAdminService.deleteRowPermission 完成业务处理。
     * 副作用：会删除或逻辑移除记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @DeleteMapping("/rows/{id}")
    public Mono<R<Void>> deleteRowPermission(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionAdminService.deleteRowPermission(context.tenantId(), context.username(), id);
                    return R.<Void>ok(null);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 权限列表接口，对应 GET /columns。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 permissionAdminService.listColumnPermissions 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/columns")
    public Mono<R<List<BiColumnPermissionView>>> listColumnPermissions(
            @RequestParam(required = false) String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.listColumnPermissions(context.tenantId(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 权限审计接口，对应 GET /audit。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 permissionAdminService.recentAudit 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/audit")
    public Mono<R<List<BiPermissionAuditEntry>>> permissionAudit(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.recentAudit(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 权限列表接口，对应 GET /requests。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 permissionRequestService.listPermissionRequests 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param resourceType 请求参数，可选。
     * @param resourceKey resource 唯一键，可选。
     * @param status 状态过滤条件，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/requests")
    public Mono<R<List<BiPermissionRequestView>>> listPermissionRequests(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceKey,
            @RequestParam(required = false) String status) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionRequestService.listPermissionRequests(
                                context.tenantId(),
                                resourceType,
                                resourceKey,
                                status)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 BI 权限 请求接口，对应 POST /requests。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 permissionRequestService.requestPermission 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 BI 权限 请求后的业务数据。
     */
    @PostMapping("/requests")
    public Mono<R<BiPermissionRequestView>> requestPermission(
            @RequestBody BiPermissionRequestCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionRequestService.requestPermission(
                                context.tenantId(),
                                context.username(),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 BI 权限 请求接口，对应 POST /requests/{id}/review。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 permissionRequestService.reviewPermissionRequest 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 BI 权限 请求后的业务数据。
     */
    @PostMapping("/requests/{id}/review")
    public Mono<R<BiPermissionRequestView>> reviewPermissionRequest(
            @PathVariable Long id,
            @RequestBody BiPermissionRequestReviewCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionRequestService.reviewPermissionRequest(
                                context.tenantId(),
                                context.username(),
                                new BiPermissionRequestReviewCommand(id, command.status(), command.reviewComment()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 BI 权限接口，对应 POST /columns。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 permissionAdminService.upsertColumnPermission 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 BI 权限后的业务数据。
     */
    @PostMapping("/columns")
    public Mono<R<BiColumnPermissionView>> upsertColumnPermission(
            @RequestBody BiColumnPermissionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.upsertColumnPermission(context.tenantId(), context.username(), command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 删除 BI 权限接口，对应 DELETE /columns/{id}。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 permissionAdminService.deleteColumnPermission 完成业务处理。
     * 副作用：会删除或逻辑移除记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @DeleteMapping("/columns/{id}")
    public Mono<R<Void>> deleteColumnPermission(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionAdminService.deleteColumnPermission(context.tenantId(), context.username(), id);
                    return R.<Void>ok(null);
                })
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
