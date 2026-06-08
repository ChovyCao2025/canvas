package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.export.BiExportCleanupResult;
import org.chovy.canvas.domain.bi.export.BiExportApprovalReviewCommand;
import org.chovy.canvas.domain.bi.export.BiExportDownload;
import org.chovy.canvas.domain.bi.export.BiExportJobDetailView;
import org.chovy.canvas.domain.bi.export.BiExportJobCommand;
import org.chovy.canvas.domain.bi.export.BiExportJobView;
import org.chovy.canvas.domain.bi.export.BiExportQueueResult;
import org.chovy.canvas.domain.bi.export.BiExportRetryResult;
import org.chovy.canvas.domain.bi.export.BiSelfServiceExportService;
import org.chovy.canvas.domain.bi.export.BiSelfServicePreviewRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
 * BiSelfServiceController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/self-service")
public class BiSelfServiceController {

    private final TenantContextResolver tenantContextResolver;
    private final BiSelfServiceExportService exportService;

    /**
     * 创建 BiSelfServiceController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param exportService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiSelfServiceController(TenantContextResolver tenantContextResolver,
                                   BiSelfServiceExportService exportService) {
        this.tenantContextResolver = tenantContextResolver;
        this.exportService = exportService;
    }
    /**
     * 预览 BI 自助分析结果接口，对应 POST /preview。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 exportService.preview 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param request 请求体。
     * @return 异步返回统一响应，包含预览 BI 自助分析结果后的业务数据。
     */
    @PostMapping("/preview")
    public Mono<R<BiQueryResult>> preview(@RequestBody BiSelfServicePreviewRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.preview(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 BI 自助分析接口，对应 POST /exports。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 exportService.createExport 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建 BI 自助分析后的业务数据。
     */
    @PostMapping("/exports")
    public Mono<R<BiExportJobView>> createExport(@RequestBody BiExportJobCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.createExport(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 自助分析列表接口，对应 GET /exports。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 exportService.listExports 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/exports")
    public Mono<R<List<BiExportJobView>>> listExports(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.listExports(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 导出 BI 自助分析接口，对应 POST /exports/{id}/review。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 exportService.reviewExport 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含导出 BI 自助分析后的业务数据。
     */
    @PostMapping("/exports/{id}/review")
    public Mono<R<BiExportJobView>> reviewExport(@PathVariable Long id,
                                                 @RequestBody BiExportApprovalReviewCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.reviewExport(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                id,
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取 BI 自助分析详情接口，对应 GET /exports/{id}。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 exportService.getExportDetail 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含获取 BI 自助分析详情后的业务数据。
     */
    @GetMapping("/exports/{id}")
    public Mono<R<BiExportJobDetailView>> getExportDetail(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.getExportDetail(context.tenantId(), id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 下载 BI 自助分析文件接口，对应 GET /exports/{id}/download。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 exportService.download 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回文件字节流响应。
     */
    @GetMapping("/exports/{id}/download")
    public Mono<ResponseEntity<byte[]>> download(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    BiExportDownload file = exportService.download(context.tenantId(), context.username(), id);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment()
                                            .filename(file.filename())
                                            .build()
                                            .toString())
                            .body(file.bytes());
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 导出 BI 自助分析接口，对应 POST /exports/{id}/cancel。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 exportService.cancelExport 完成业务处理。
     * 副作用：会取消任务。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含导出 BI 自助分析后的业务数据。
     */
    @PostMapping("/exports/{id}/cancel")
    public Mono<R<BiExportJobView>> cancelExport(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.cancelExport(
                                context.tenantId(),
                                context.username(),
                                id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 清理 BI 自助分析数据接口，对应 POST /exports/cleanup。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 exportService.cleanupExpiredExports 完成业务处理。
     * 副作用：会清理过期或无效数据。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 100。
     * @return 异步返回统一响应，包含清理 BI 自助分析数据后的业务数据。
     */
    @PostMapping("/exports/cleanup")
    public Mono<R<BiExportCleanupResult>> cleanupExports(@RequestParam(defaultValue = "100") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.cleanupExpiredExports(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 重试 BI 自助分析任务接口，对应 POST /exports/retry。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 exportService.retryFailedExports 完成业务处理。
     * 副作用：会重试待处理任务。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含重试 BI 自助分析任务后的业务数据。
     */
    @PostMapping("/exports/retry")
    public Mono<R<BiExportRetryResult>> retryExports(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.retryFailedExports(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发 BI 自助分析运行接口，对应 POST /exports/queue/run。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 exportService.processQueuedExports 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param limit 返回数量上限，默认值为 20。
     * @return 异步返回统一响应，包含触发 BI 自助分析运行后的业务数据。
     */
    @PostMapping("/exports/queue/run")
    public Mono<R<BiExportQueueResult>> runExportQueue(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(exportService.processQueuedExports(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                limit)))
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
