package org.chovy.canvas.web;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.canvas.*;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.query.CanvasListQuery;

/**
 * 画布主控制器：
 * 负责画布 CRUD、版本管理、发布下线以及运营管控入口。
 */
@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class CanvasController {

    /** 画布服务，用于处理画布 CRUD 和草稿版本。 */
    private final CanvasService canvasService;
    /** 画布运维服务，用于发布、下线和审批操作。 */
    private final CanvasOpsService opsService;
    /** 通知事件服务，用于发送画布运营通知。 */
    private final NotificationEventService notificationEventService;
    /** 当前租户上下文解析器。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建画布
     *
     * @param req 创建请求对象
     * @return 创建成功的画布信息
     */
    @PostMapping
    public Mono<R<CanvasDO>> create(@RequestBody CanvasCreateReq req) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> canvasService.create(req, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 根据 ID 获取画布详情
     *
     * @param id 画布 ID
     * @return 画布详情
     */
    @GetMapping("/{id}")
    public Mono<R<CanvasDetailDTO>> getById(@PathVariable Long id) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> canvasService.getById(id, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(detail -> detail != null ? R.ok(detail) : R.fail("画布不存在")));
    }

    /**
     * 更新画布草稿
     *
     * @param id  画布 ID
     * @param req 更新信息
     * @return 成功响应
     */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody CanvasUpdateReq req) {
        return currentTenant().flatMap(context ->
                Mono.<Void>fromRunnable(() -> canvasService.updateDraft(id, req, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    /**
     * 查询画布列表
     *
     * @param query 查询条件
     * @return 分页后的画布列表
     */
    @GetMapping("/list")
    public Mono<R<PageResult<CanvasDO>>> list(CanvasListQuery query) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> canvasService.list(query, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 发布画布
     *
     * @param id       画布 ID
     * @param operator 操作人标识
     * @return 发布后的版本信息
     */
    @PostMapping("/{id}/publish")
    public Mono<R<CanvasVersionDO>> publish(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    // 发布会修改版本状态并刷新运行配置，属于阻塞事务操作。
                    CanvasVersionDO version = canvasService.publish(id, operator, context.tenantId());
                    notifyCanvasChange("CANVAS_PUBLISHED", id, "画布已发布",
                            "operator=" + operator + " versionId=" + version.getId(),
                            "INFO", operator);
                    return version;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok));
    }

    /**
     * 下线画布
     *
     * @param id       画布 ID
     * @param operator 操作人标识
     * @return 成功响应
     */
    @PostMapping("/{id}/offline")
    public Mono<R<Void>> offline(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return currentTenant().flatMap(context -> Mono.<Void>fromRunnable(() -> {
                    canvasService.offline(id, operator, context.tenantId());
                    notifyCanvasChange("CANVAS_OFFLINE", id, "画布已下线",
                            "operator=" + operator, "WARNING", operator);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok()));
    }

    @PostMapping("/{id}/archive")
    public Mono<R<Void>> archive(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return currentTenant().flatMap(context -> Mono.<Void>fromRunnable(() -> {
                    canvasService.archive(id, operator, context.tenantId());
                    notifyCanvasChange("CANVAS_ARCHIVED", id, "画布已归档",
                            "operator=" + operator, "WARNING", operator);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok()));
    }

    /**
     * 获取画布历史版本
     *
     * @param id   画布 ID
     * @param page 页码
     * @param size 每页大小
     * @return 版本列表
     */
    @GetMapping("/{id}/versions")
    public Mono<R<PageResult<CanvasVersionDO>>> getVersions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> canvasService.getVersions(id, page, size, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 获取指定版本详情
     *
     * @param id        画布 ID
     * @param versionId 版本 ID
     * @return 版本详情
     */
    @GetMapping("/{id}/versions/{versionId}")
    public Mono<R<CanvasVersionDO>> getVersion(
            @PathVariable Long id,
            @PathVariable Long versionId) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> canvasService.getVersion(id, versionId, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(v -> v != null ? R.ok(v) : R.fail("版本不存在")));
    }


    // ── 运营管控 ─────────────────────────────────────────────────

    /**
     * 终止画布执行
     *
     * @param id   画布 ID
     * @param mode 终止模式 (GRACEFUL/FORCE)
     * @return 成功响应
     */
    @PostMapping("/{id}/kill")
    public Mono<R<Void>> kill(@PathVariable Long id,
                              @RequestParam(defaultValue = "GRACEFUL") String mode) {
        return currentTenant().zipWith(currentUser()).flatMap(tuple -> {
            TenantContext context = tuple.getT1();
            String operator = tuple.getT2();
            return
                Mono.<Void>fromRunnable(() -> {
                            opsService.kill(id, mode, context.tenantId());
                            notifyCanvasChange("CANVAS_KILLED", id, "画布执行已终止",
                                    "operator=" + operator + " mode=" + mode, "ERROR", operator);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok());
        });
    }

    /**
     * 回退画布草稿到指定历史版本（不影响已发布线上版本）
     */
    @PostMapping("/{id}/revert/{versionId}")
    public Mono<R<Void>> revertToVersion(@PathVariable Long id,
                                         @PathVariable Long versionId) {
        return currentTenant().flatMap(context ->
                Mono.<Void>fromRunnable(() -> canvasService.revertToVersion(id, versionId, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    /**
     * 启动画布灰度发布
     *
     * @param id      画布 ID
     * @param percent 灰度流量比例 (0-100)
     * @return 成功响应
     */
    @PostMapping("/{id}/canary")
    public Mono<R<Void>> startCanary(@PathVariable Long id,
                                     @RequestParam int percent) {
        return currentTenant().zipWith(currentUser()).flatMap(tuple -> {
            TenantContext context = tuple.getT1();
            String operator = tuple.getT2();
            return
                Mono.<Void>fromRunnable(() -> {
                            opsService.startCanary(id, percent, operator, context.tenantId());
                            notifyCanvasChange("CANVAS_CANARY_STARTED", id, "画布灰度已启动",
                                    "operator=" + operator + " percent=" + percent, "INFO", operator);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok());
        });
    }

    /**
     * 将灰度版本晋升为正式版本
     *
     * @param id 画布 ID
     * @return 成功响应
     */
    @PostMapping("/{id}/promote-canary")
    public Mono<R<Void>> promoteCanary(@PathVariable Long id) {
        return currentTenant().zipWith(currentUser()).flatMap(tuple -> {
            TenantContext context = tuple.getT1();
            String operator = tuple.getT2();
            return
                Mono.<Void>fromRunnable(() -> {
                            opsService.promoteCanary(id, context.tenantId());
                            notifyCanvasChange("CANVAS_CANARY_PROMOTED", id, "灰度版本已转正",
                                    "operator=" + operator, "SUCCESS", operator);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok());
        });
    }

    /**
     * 回滚灰度发布
     *
     * @param id 画布 ID
     * @return 成功响应
     */
    @PostMapping("/{id}/rollback-canary")
    public Mono<R<Void>> rollbackCanary(@PathVariable Long id) {
        return currentTenant().zipWith(currentUser()).flatMap(tuple -> {
            TenantContext context = tuple.getT1();
            String operator = tuple.getT2();
            return
                Mono.<Void>fromRunnable(() -> {
                            opsService.rollbackCanary(id, context.tenantId());
                            notifyCanvasChange("CANVAS_CANARY_ROLLED_BACK", id, "灰度发布已回滚",
                                    "operator=" + operator, "WARNING", operator);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok());
        });
    }

    /**
     * 回滚画布到上一个稳定版本
     *
     * @param id 画布 ID
     * @return 成功响应
     */
    @PostMapping("/{id}/rollback")
    public Mono<R<Void>> rollback(@PathVariable Long id) {
        return currentTenant().zipWith(currentUser()).flatMap(tuple -> {
            TenantContext context = tuple.getT1();
            String operator = tuple.getT2();
            return
                Mono.<Void>fromRunnable(() -> {
                            opsService.rollback(id, context.tenantId());
                            notifyCanvasChange("CANVAS_ROLLED_BACK", id, "画布已回滚",
                                    "operator=" + operator, "WARNING", operator);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok());
        });
    }

    /**
     * 克隆画布
     *
     * @param id 原画布 ID
     * @return 克隆后的画布信息
     */
    @PostMapping("/{id}/clone")
    public Mono<R<CanvasDO>> clone(@PathVariable Long id) {
        return currentTenant().zipWith(currentUser()).flatMap(tuple -> {
            TenantContext context = tuple.getT1();
            String operator = tuple.getT2();
            return
                Mono.fromCallable(() -> {
                            CanvasDO canvas = opsService.clone(id, operator, context.tenantId());
                            notifyCanvasChange("CANVAS_CLONED", canvas.getId(), "画布已克隆",
                                    "operator=" + operator + " sourceCanvasId=" + id, "INFO", operator);
                            return canvas;
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok);
        });
    }

    /**
     * 比较两个版本之间的差异
     *
     * @param id 画布 ID
     * @param v1 版本 ID 1
     * @param v2 版本 ID 2
     * @return 差异对比结果
     */
    @GetMapping("/{id}/versions/{v1}/diff/{v2}")
    public Mono<R<Map<String, Object>>> diff(@PathVariable Long id,
                                             @PathVariable Long v1,
                                             @PathVariable Long v2) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> opsService.diff(id, v1, v2, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * PUT /canvas/{id} 升级：支持 editVersion 乐观锁
     */
    @PutMapping("/{id}/safe")
    public Mono<R<Void>> safeUpdate(@PathVariable Long id,
                                    @RequestBody SafeUpdateReq req) {

        return currentTenant().zipWith(currentUser()).flatMap(tuple -> {
            TenantContext context = tuple.getT1();
            String operator = tuple.getT2();
            return
                Mono.<Void>fromRunnable(() -> opsService.saveWithOptimisticLock(
                                // editVersion 用于乐观锁，前端版本落后时返回可识别的冲突文案。
                                id, req.getName(), req.getDescription(),
                                req.getGraphJson(), req.getEditVersion(), operator, context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok())
                        .onErrorResume(e -> {
                            if ("CANVAS_010".equals(e.getMessage()))
                                return Mono.just(R.fail("画布已被他人修改，请刷新后重试"));
                            return Mono.error(e);
                        });
        });
    }

    /**
     * 执行 current User 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
// ── helpers ───────────────────────────────────────────────────

    /** 从安全上下文读取当前用户名，缺失时回退 system。 */
    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(io.jsonwebtoken.Claims.class)
                // 管控类操作统一记录 JWT 中的 username 作为审计操作人。
                .map(c -> c.get("username", String.class))
                .defaultIfEmpty("system");
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new IllegalStateException("tenant context is required")));
    }

    /**
     * 发布或发送 notify Canvas Change 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param type type 类型标识或分类条件
     * @param canvasId canvasId 对应的业务主键或标识
     * @param title title 方法执行所需的业务参数
     * @param content content 方法执行所需的业务参数
     * @param severity severity 方法执行所需的业务参数
     * @param operator operator 操作人标识
     */
    private void notifyCanvasChange(
            String type,
            Long canvasId,
            String title,
            String content,
            String severity,
            String operator) {
        // 控制器内只发领域事件，实际通知落库/推送由通知服务处理。
        notificationEventService.canvasChanged(type, canvasId, title, content, severity, operator);
    }

}
