package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionReplayRateLimiter;
import org.chovy.canvas.engine.request.CanvasExecutionRequestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 画布执行请求 Management HTTP 控制器，根路由为 {@code /canvas/execution-requests}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/execution-requests")
@Slf4j
public class CanvasExecutionRequestManagementController {

    /** 批量重放默认条数。 */
    private static final int DEFAULT_BATCH_LIMIT = 100;
    /** 批量重放最大条数。 */
    private static final int MAX_BATCH_LIMIT = 500;
    /** 允许重放的执行请求状态集合。 */
    private static final Set<String> REPLAYABLE_STATUSES = Set.of(
            CanvasExecutionRequestStatus.FAILED,
            CanvasExecutionRequestStatus.RETRY
    );

    /** 执行请求 Mapper，用于查询和更新请求记录。 */
    private final CanvasExecutionRequestMapper mapper;
    /** Disruptor 投递服务，用于重新发布执行请求。 */
    private final CanvasDisruptorService disruptorService;
    /** 重放限流器，用于限制批量重放速率。 */
    private final CanvasExecutionReplayRateLimiter replayRateLimiter;
    /** 租户上下文解析器，用于执行请求管理查询和重放隔离。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 构造 CanvasExecutionRequestManagementController 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param mapper mapper 方法执行所需的业务参数
     * @param disruptorService disruptorService 方法执行所需的业务参数
     */
    public CanvasExecutionRequestManagementController(CanvasExecutionRequestMapper mapper,
                                                      CanvasDisruptorService disruptorService) {
        this(mapper, disruptorService, new CanvasExecutionReplayRateLimiter(0, 0), null);
    }

    /**
     * 构造 CanvasExecutionRequestManagementController 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param mapper mapper 方法执行所需的业务参数
     * @param disruptorService disruptorService 方法执行所需的业务参数
     * @param replayRateLimiter replayRateLimiter 数量、阈值或分页参数
     */
    @Autowired
    public CanvasExecutionRequestManagementController(CanvasExecutionRequestMapper mapper,
                                                      CanvasDisruptorService disruptorService,
                                                      CanvasExecutionReplayRateLimiter replayRateLimiter,
                                                      TenantContextResolver tenantContextResolver) {
        this.mapper = mapper;
        this.disruptorService = disruptorService;
        this.replayRateLimiter = replayRateLimiter;
        this.tenantContextResolver = tenantContextResolver;
    }

    /**
     * 初始化 CanvasExecutionRequestManagementController 实例。
     *
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param disruptorService 依赖组件，用于完成数据访问或外部能力调用。
     * @param replayRateLimiter replay rate limiter 参数，用于 CanvasExecutionRequestManagementController 流程中的校验、计算或对象转换。
     */
    public CanvasExecutionRequestManagementController(CanvasExecutionRequestMapper mapper,
                                                      CanvasDisruptorService disruptorService,
                                                      CanvasExecutionReplayRateLimiter replayRateLimiter) {
        this(mapper, disruptorService, replayRateLimiter, null);
    }

    @GetMapping
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param sourceMsgId 业务对象 ID，用于定位具体记录。
     * @param page 分页或数量限制，避免一次处理过多数据。
     * @param size 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<PageResult<CanvasExecutionRequestDO>>> list(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sourceMsgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            // 管理查询按可选条件动态拼装，所有数据库访问都在 boundedElastic 执行。
            LambdaQueryWrapper<CanvasExecutionRequestDO> wrapper = new LambdaQueryWrapper<CanvasExecutionRequestDO>()
                    .eq(canvasId != null, CanvasExecutionRequestDO::getCanvasId, canvasId)
                    .eq(status != null && !status.isBlank(), CanvasExecutionRequestDO::getStatus, status)
                    .eq(userId != null && !userId.isBlank(), CanvasExecutionRequestDO::getUserId, userId)
                    .eq(sourceMsgId != null && !sourceMsgId.isBlank(),
                            CanvasExecutionRequestDO::getSourceMsgId, sourceMsgId)
                    .orderByDesc(CanvasExecutionRequestDO::getUpdatedAt);
            applyTenantFilter(wrapper, context);
            Page<CanvasExecutionRequestDO> result =
                    mapper.selectPage(new Page<>(Math.max(1, page), normalizePageSize(size)), wrapper);
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/replay")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param force force 参数，用于 replay 流程中的校验、计算或对象转换。
     * @return 返回 replay 流程生成的业务结果。
     */
    public Mono<R<Map<String, Object>>> replay(@PathVariable String id,
                                               @RequestParam(required = false) String reason,
                                               @RequestParam(defaultValue = "false") boolean force) {
        return currentTenant().flatMap(context -> currentUsername().flatMap(operator -> Mono.fromCallable(() -> {
                    // 重放入口先做操作人维度限流，避免管理端误操作瞬时压垮执行队列。
                    requireReplayRateLimit(replayRateLimiter.tryAcquireSingleReplay(operator),
                            "执行请求单条重放过于频繁，请稍后再试");
                    CanvasExecutionRequestDO request = mapper.selectById(id);
                    if (request == null) {
                        throw new IllegalArgumentException("执行请求不存在: " + id);
                    }
                    requireTenantAccess(request, context);
                    requireReplayable(request.getStatus(), force);
                    int updated = mapper.markPendingForReplay(
                            id,
                            LocalDateTime.now(),
                            operator,
                            normalize(reason, "")
                    );
                    if (updated <= 0) {
                        throw new IllegalStateException("执行请求重放状态更新失败: " + id);
                    }
                    // 状态落库成功后再尝试立即投递；投递失败仍保留 PENDING 供后台扫描补偿。
                    boolean immediateDispatch = publishRequestBestEffort(id);
                    return Map.<String, Object>of(
                            MapFieldKeys.REQUEST_ID, id,
                            MapFieldKeys.STATUS, "QUEUED",
                            MapFieldKeys.IMMEDIATE_DISPATCH, immediateDispatch
                    );
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok)));
    }

    @PostMapping("/replay")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param sourceMsgId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param force force 参数，用于 replayBatch 流程中的校验、计算或对象转换。
     * @return 返回 replayBatch 流程生成的业务结果。
     */
    public Mono<R<Map<String, Object>>> replayBatch(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sourceMsgId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "false") boolean force) {
        int normalizedLimit = normalizeLimit(limit);
        return currentTenant().flatMap(context -> currentUsername().flatMap(operator -> Mono.fromCallable(() -> {
                    // 批量重放的限流令牌按本次归一化 limit 消耗，和实际最大处理量保持一致。
                    requireReplayRateLimit(replayRateLimiter.tryAcquireBatchReplay(operator, normalizedLimit),
                            "执行请求批量重放过于频繁，请稍后再试");
                    LambdaQueryWrapper<CanvasExecutionRequestDO> wrapper = new LambdaQueryWrapper<CanvasExecutionRequestDO>()
                            .eq(canvasId != null, CanvasExecutionRequestDO::getCanvasId, canvasId)
                            .eq(userId != null && !userId.isBlank(), CanvasExecutionRequestDO::getUserId, userId)
                            .eq(sourceMsgId != null && !sourceMsgId.isBlank(),
                                    CanvasExecutionRequestDO::getSourceMsgId, sourceMsgId)
                            .orderByAsc(CanvasExecutionRequestDO::getUpdatedAt)
                            .last("LIMIT " + normalizedLimit);
                    applyTenantFilter(wrapper, context);
                    applyReplayStatusFilter(wrapper, status, force);

                    List<CanvasExecutionRequestDO> requests = mapper.selectList(wrapper);
                    List<String> replayed = new ArrayList<>(requests.size());
                    List<String> dispatchFailed = new ArrayList<>();
                    LocalDateTime now = LocalDateTime.now();
                    String replayReason = normalize(reason, "");
                    for (CanvasExecutionRequestDO request : requests) {
                        // 使用条件更新把状态切回 PENDING，避免重复处理已被其他请求改写的记录。
                        int updated = mapper.markPendingForReplay(request.getId(), now, operator, replayReason);
                        if (updated > 0) {
                            replayed.add(request.getId());
                            if (!publishRequestBestEffort(request.getId())) {
                                dispatchFailed.add(request.getId());
                            }
                        }
                    }
                    return Map.<String, Object>of(
                            MapFieldKeys.COUNT, replayed.size(),
                            MapFieldKeys.LIMIT, normalizedLimit,
                            MapFieldKeys.REQUEST_IDS, replayed,
                            MapFieldKeys.DISPATCH_FAILURE_COUNT, dispatchFailed.size(),
                            MapFieldKeys.DISPATCH_FAILED_REQUEST_IDS, dispatchFailed
                    );
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok)));
    }

    /**
     * 尝试立即投递重放后的执行请求。
     *
     * <p>状态已恢复为 PENDING 后再投递；即时投递失败时保留记录给后台补偿。
     *
     * @param requestId 执行请求 ID
     * @return {@code true} 表示即时投递成功
     */
    private boolean publishRequestBestEffort(String requestId) {
        try {
            disruptorService.publishRequest(requestId);
            return true;
        } catch (RuntimeException e) {
            log.warn("[EXEC_REQUEST] immediate replay dispatch failed requestId={} reason={}",
                    requestId, e.getMessage());
            return false;
        }
    }

    /**
     * 校验重放限流结果，失败时直接阻断本次管理操作。
     *
     * <p>限流按操作人维度控制，避免批量重放瞬时压垮执行队列。
     *
     * @param allowed 是否已获得重放令牌
     * @param message 限流失败时返回给调用方的错误信息
     */
    private void requireReplayRateLimit(boolean allowed, String message) {
        if (!allowed) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * 执行 apply Replay Status Filter 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param wrapper wrapper 方法执行所需的业务参数
     * @param status status 状态值或状态筛选条件
     * @param force force 方法执行所需的业务参数
     */
    private void applyReplayStatusFilter(LambdaQueryWrapper<CanvasExecutionRequestDO> wrapper,
                                         String status,
                                         boolean force) {
        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim();
            if (!force && !REPLAYABLE_STATUSES.contains(normalizedStatus)) {
                throw new IllegalArgumentException("批量重放默认只允许 FAILED/RETRY，其他状态请使用 force=true");
            }
            // 显式 status 优先；force=false 时只允许失败态，防止误重放成功请求。
            wrapper.eq(CanvasExecutionRequestDO::getStatus, normalizedStatus);
            return;
        }
        if (!force) {
            // 默认只捞可安全重放的失败/待重试记录。
            wrapper.in(CanvasExecutionRequestDO::getStatus, REPLAYABLE_STATUSES);
        }
    }

    /**
     * 执行 require Replayable 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param status status 状态值或状态筛选条件
     * @param force force 方法执行所需的业务参数
     */
    private void requireReplayable(String status, boolean force) {
        if (force) {
            return;
        }
        if (!REPLAYABLE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("只能重放 FAILED/RETRY 状态的执行请求，其他状态请使用 force=true");
        }
    }

    /**
     * 执行 normalize Limit 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param limit limit 数量、阈值或分页参数
     * @return 计算得到的数值结果
     */
    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_BATCH_LIMIT;
        }
        return Math.min(limit, MAX_BATCH_LIMIT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param size 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    /**
     * 执行 normalize 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param fallback fallback 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 执行 current Username 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    private Mono<String> currentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                // 操作人写入 replay metadata，便于审计谁触发了重放。
                .map(c -> c.get("username", String.class))
                .defaultIfEmpty("system");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(null, null, null));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(null, null, null));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param wrapper wrapper 参数，用于 applyTenantFilter 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void applyTenantFilter(LambdaQueryWrapper<CanvasExecutionRequestDO> wrapper, TenantContext context) {
        if (!isSuperAdmin(context) && context != null && context.tenantId() != null) {
            wrapper.eq(CanvasExecutionRequestDO::getTenantId, context.tenantId());
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void requireTenantAccess(CanvasExecutionRequestDO request, TenantContext context) {
        if (isSuperAdmin(context) || context == null || context.tenantId() == null) {
            return;
        }
        if (!Objects.equals(request.getTenantId(), context.tenantId())) {
            throw new AccessDeniedException("跨租户执行请求访问被拒绝");
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回布尔判断结果。
     */
    private boolean isSuperAdmin(TenantContext context) {
        return context != null && context.isSuperAdmin();
    }
}
