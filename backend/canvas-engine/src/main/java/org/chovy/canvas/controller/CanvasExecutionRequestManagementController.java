package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.execution.CanvasExecutionRequest;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionRequestStatus;
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

@RestController
@RequestMapping("/canvas/execution-requests")
@RequiredArgsConstructor
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

    @GetMapping
    public Mono<R<PageResult<CanvasExecutionRequest>>> list(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sourceMsgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<CanvasExecutionRequest> wrapper = new LambdaQueryWrapper<CanvasExecutionRequest>()
                    .eq(canvasId != null, CanvasExecutionRequest::getCanvasId, canvasId)
                    .eq(status != null && !status.isBlank(), CanvasExecutionRequest::getStatus, status)
                    .eq(userId != null && !userId.isBlank(), CanvasExecutionRequest::getUserId, userId)
                    .eq(sourceMsgId != null && !sourceMsgId.isBlank(),
                            CanvasExecutionRequest::getSourceMsgId, sourceMsgId)
                    .orderByDesc(CanvasExecutionRequest::getUpdatedAt);
            Page<CanvasExecutionRequest> result = mapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{id}/replay")
    public Mono<R<Map<String, Object>>> replay(@PathVariable String id,
                                               @RequestParam(required = false) String reason,
                                               @RequestParam(defaultValue = "false") boolean force) {
        return currentUsername().flatMap(operator -> Mono.fromCallable(() -> {
                    CanvasExecutionRequest request = mapper.selectById(id);
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
                            "requestId", id,
                            "status", "QUEUED",
                            "immediateDispatch", immediateDispatch
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
                    LambdaQueryWrapper<CanvasExecutionRequest> wrapper = new LambdaQueryWrapper<CanvasExecutionRequest>()
                            .eq(canvasId != null, CanvasExecutionRequest::getCanvasId, canvasId)
                            .eq(userId != null && !userId.isBlank(), CanvasExecutionRequest::getUserId, userId)
                            .eq(sourceMsgId != null && !sourceMsgId.isBlank(),
                                    CanvasExecutionRequest::getSourceMsgId, sourceMsgId)
                            .orderByAsc(CanvasExecutionRequest::getUpdatedAt)
                            .last("LIMIT " + normalizedLimit);
                    applyReplayStatusFilter(wrapper, status, force);

                    List<CanvasExecutionRequest> requests = mapper.selectList(wrapper);
                    List<String> replayed = new ArrayList<>(requests.size());
                    List<String> dispatchFailed = new ArrayList<>();
                    LocalDateTime now = LocalDateTime.now();
                    String replayReason = normalize(reason, "");
                    for (CanvasExecutionRequest request : requests) {
                        int updated = mapper.markPendingForReplay(request.getId(), now, operator, replayReason);
                        if (updated > 0) {
                            replayed.add(request.getId());
                            if (!publishRequestBestEffort(request.getId())) {
                                dispatchFailed.add(request.getId());
                            }
                        }
                    }
                    return Map.<String, Object>of(
                            "count", replayed.size(),
                            "limit", normalizedLimit,
                            "requestIds", replayed,
                            "dispatchFailureCount", dispatchFailed.size(),
                            "dispatchFailedRequestIds", dispatchFailed
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

    private void applyReplayStatusFilter(LambdaQueryWrapper<CanvasExecutionRequest> wrapper,
                                         String status,
                                         boolean force) {
        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim();
            if (!force && !REPLAYABLE_STATUSES.contains(normalizedStatus)) {
                throw new IllegalArgumentException("批量重放默认只允许 FAILED/RETRY，其他状态请使用 force=true");
            }
            wrapper.eq(CanvasExecutionRequest::getStatus, normalizedStatus);
            return;
        }
        if (!force) {
            wrapper.in(CanvasExecutionRequest::getStatus, REPLAYABLE_STATUSES);
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
