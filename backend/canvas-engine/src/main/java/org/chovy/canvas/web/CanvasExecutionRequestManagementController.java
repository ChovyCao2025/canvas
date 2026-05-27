package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionReplayRateLimiter;
import org.chovy.canvas.engine.request.CanvasExecutionRequestStatus;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final int DEFAULT_BATCH_LIMIT = 100;
    private static final int MAX_BATCH_LIMIT = 500;
    private static final Set<String> REPLAYABLE_STATUSES = Set.of(
            CanvasExecutionRequestStatus.FAILED,
            CanvasExecutionRequestStatus.RETRY
    );

    private final CanvasExecutionRequestMapper mapper;
    private final CanvasDisruptorService disruptorService;
    private final CanvasExecutionReplayRateLimiter replayRateLimiter;

    public CanvasExecutionRequestManagementController(CanvasExecutionRequestMapper mapper,
                                                      CanvasDisruptorService disruptorService) {
        this(mapper, disruptorService, new CanvasExecutionReplayRateLimiter(0, 0));
    }

    @Autowired
    public CanvasExecutionRequestManagementController(CanvasExecutionRequestMapper mapper,
                                                      CanvasDisruptorService disruptorService,
                                                      CanvasExecutionReplayRateLimiter replayRateLimiter) {
        this.mapper = mapper;
        this.disruptorService = disruptorService;
        this.replayRateLimiter = replayRateLimiter;
    }

    @GetMapping
    public Mono<R<PageResult<CanvasExecutionRequestDO>>> list(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sourceMsgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<CanvasExecutionRequestDO> wrapper = new LambdaQueryWrapper<CanvasExecutionRequestDO>()
                    .eq(canvasId != null, CanvasExecutionRequestDO::getCanvasId, canvasId)
                    .eq(status != null && !status.isBlank(), CanvasExecutionRequestDO::getStatus, status)
                    .eq(userId != null && !userId.isBlank(), CanvasExecutionRequestDO::getUserId, userId)
                    .eq(sourceMsgId != null && !sourceMsgId.isBlank(),
                            CanvasExecutionRequestDO::getSourceMsgId, sourceMsgId)
                    .orderByDesc(CanvasExecutionRequestDO::getUpdatedAt);
            Page<CanvasExecutionRequestDO> result = mapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{id}/replay")
    public Mono<R<Map<String, Object>>> replay(@PathVariable String id,
                                               @RequestParam(required = false) String reason,
                                               @RequestParam(defaultValue = "false") boolean force) {
        return currentUsername().flatMap(operator -> Mono.fromCallable(() -> {
                    requireReplayRateLimit(replayRateLimiter.tryAcquireSingleReplay(operator),
                            "执行请求单条重放过于频繁，请稍后再试");
                    CanvasExecutionRequestDO request = mapper.selectById(id);
                    if (request == null) {
                        throw new IllegalArgumentException("执行请求不存在: " + id);
                    }
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
                    boolean immediateDispatch = publishRequestBestEffort(id);
                    return Map.<String, Object>of(
                            MapFieldKeys.REQUEST_ID, id,
                            MapFieldKeys.STATUS, "QUEUED",
                            MapFieldKeys.IMMEDIATE_DISPATCH, immediateDispatch
                    );
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok));
    }

    @PostMapping("/replay")
    public Mono<R<Map<String, Object>>> replayBatch(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sourceMsgId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "false") boolean force) {
        int normalizedLimit = normalizeLimit(limit);
        return currentUsername().flatMap(operator -> Mono.fromCallable(() -> {
                    requireReplayRateLimit(replayRateLimiter.tryAcquireBatchReplay(operator, normalizedLimit),
                            "执行请求批量重放过于频繁，请稍后再试");
                    LambdaQueryWrapper<CanvasExecutionRequestDO> wrapper = new LambdaQueryWrapper<CanvasExecutionRequestDO>()
                            .eq(canvasId != null, CanvasExecutionRequestDO::getCanvasId, canvasId)
                            .eq(userId != null && !userId.isBlank(), CanvasExecutionRequestDO::getUserId, userId)
                            .eq(sourceMsgId != null && !sourceMsgId.isBlank(),
                                    CanvasExecutionRequestDO::getSourceMsgId, sourceMsgId)
                            .orderByAsc(CanvasExecutionRequestDO::getUpdatedAt)
                            .last("LIMIT " + normalizedLimit);
                    applyReplayStatusFilter(wrapper, status, force);

                    List<CanvasExecutionRequestDO> requests = mapper.selectList(wrapper);
                    List<String> replayed = new ArrayList<>(requests.size());
                    List<String> dispatchFailed = new ArrayList<>();
                    LocalDateTime now = LocalDateTime.now();
                    String replayReason = normalize(reason, "");
                    for (CanvasExecutionRequestDO request : requests) {
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
                .map(R::ok));
    }

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

    private void requireReplayRateLimit(boolean allowed, String message) {
        if (!allowed) {
            throw new IllegalStateException(message);
        }
    }

    private void applyReplayStatusFilter(LambdaQueryWrapper<CanvasExecutionRequestDO> wrapper,
                                         String status,
                                         boolean force) {
        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim();
            if (!force && !REPLAYABLE_STATUSES.contains(normalizedStatus)) {
                throw new IllegalArgumentException("批量重放默认只允许 FAILED/RETRY，其他状态请使用 force=true");
            }
            wrapper.eq(CanvasExecutionRequestDO::getStatus, normalizedStatus);
            return;
        }
        if (!force) {
            wrapper.in(CanvasExecutionRequestDO::getStatus, REPLAYABLE_STATUSES);
        }
    }

    private void requireReplayable(String status, boolean force) {
        if (force) {
            return;
        }
        if (!REPLAYABLE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("只能重放 FAILED/RETRY 状态的执行请求，其他状态请使用 force=true");
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_BATCH_LIMIT;
        }
        return Math.min(limit, MAX_BATCH_LIMIT);
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Mono<String> currentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                .map(c -> c.get("username", String.class))
                .defaultIfEmpty("system");
    }
}
