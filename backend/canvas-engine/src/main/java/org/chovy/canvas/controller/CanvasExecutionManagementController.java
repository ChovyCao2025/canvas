package org.chovy.canvas.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.chovy.canvas.domain.constant.NodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.approval.CanvasManualApproval;
import org.chovy.canvas.domain.approval.CanvasManualApprovalMapper;
import org.chovy.canvas.domain.constant.ApprovalStatus;
import org.chovy.canvas.engine.handlers.ManualApprovalHandler;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.infra.redis.ContextPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 执行相关 API：
 * - 人工审批 approve / reject（设计文档 18.2节）
 * - 死信重放（设计文档 13.3节）
 */
@Slf4j
@RestController
@RequestMapping("/canvas/execution")
@RequiredArgsConstructor
public class CanvasExecutionManagementController {

    private final CanvasManualApprovalMapper approvalMapper;
    private final ContextPersistenceService ctxStore;
    private final CanvasExecutionService executionService;
    private final ObjectMapper objectMapper;

    /**
     * 人工审批通过（设计文档 18.2节）。
     * 安全校验：当前用户必须在 approvers 列表中（设计文档 18.2节补充说明）。
     *
     * @param executionId 执行实例 ID
     * @return 成功响应
     */
    @PostMapping("/{executionId}/approve")
    public Mono<R<Void>> approve(@PathVariable String executionId) {
        return currentUsername()
                .flatMap(username ->
                        Mono.<Void>fromRunnable(() -> resumeWithResult(executionId, ApprovalStatus.APPROVED, username,
                                        /* watchdog */ false))
                                .subscribeOn(Schedulers.boundedElastic())
                                .thenReturn(R.<Void>ok()));
    }

    /**
     * 人工审批拒绝（设计文档 18.2节）
     *
     * @param executionId 执行实例 ID
     * @param reason      拒绝原因
     * @return 成功响应
     */
    @PostMapping("/{executionId}/reject")
    public Mono<R<Void>> reject(@PathVariable String executionId,
                                @RequestParam(required = false) String reason) {
        return currentUsername()
                .flatMap(username ->
                        Mono.<Void>fromRunnable(() -> resumeWithResult(executionId, ApprovalStatus.REJECTED, username,
                                        /* watchdog */ false))
                                .subscribeOn(Schedulers.boundedElastic())
                                .thenReturn(R.<Void>ok()));
    }


    // ── private ──────────────────────────────────────────────────

    private void resumeWithResult(String executionId, String result, String approver,
                                  boolean isWatchdog) {
        CanvasManualApproval approval = approvalMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CanvasManualApproval>()
                                .eq(CanvasManualApproval::getExecutionId, executionId)
                                .eq(CanvasManualApproval::getStatus, ApprovalStatus.PENDING)
                                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);

        if (approval == null) {
            log.warn("[APPROVAL] 找不到 PENDING 审批记录 executionId={}", executionId);
            return;
        }

        // 审批人身份校验（设计文档 18.2节安全要求；Watchdog 超时处理时跳过）
        if (!isWatchdog) {
            try {
                List<String> approvers = objectMapper.readValue(
                        approval.getApprovers(), new TypeReference<>() {
                        });
                if (!approvers.contains(approver)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "AUTH_003: 当前用户不在审批人列表中，无权操作此审批");
                }
            } catch (ResponseStatusException e) {
                throw e;
            } catch (Exception ignored) {
            }
        }

        // 2. 更新审批记录
        approval.setStatus(result);
        approval.setResultBy(approver);
        approval.setResultAt(LocalDateTime.now());
        approvalMapper.updateById(approval);

        // 3. 从 Redis 恢复 ctx，写入审批结果
        var ctx = ctxStore.load(approval.getCanvasId(), approval.getUserId());
        if (ctx == null) {
            log.warn("[APPROVAL] ctx 已不存在（可能已过期）executionId={}", executionId);
            return;
        }

        // 写入审批结果到 ctx.flatContext（ManualApprovalHandler 下次执行时读取）
        String resultKey = ManualApprovalHandler.APPROVAL_RESULT_KEY + approval.getNodeId();
        ctx.getFlatContext().put(resultKey, result);

        // 4. 持久化更新后的 ctx 回 Redis
        ctxStore.save(ctx);

        // 5. 重新触发画布执行（从 MANUAL_APPROVAL 节点的触发器入口重新进入）
        executionService.trigger(
                        approval.getCanvasId(),
                        approval.getUserId(),
                        "MANUAL_APPROVAL_RESUME",
                        NodeType.MANUAL_APPROVAL,
                        null,
                        Map.of("__approvalResult", result),
                        executionId + ":resume:" + result,
                        false)
                .subscribe(
                        r -> log.info("[APPROVAL] 恢复执行完成 executionId={} result={}", executionId, result),
                        e -> log.error("[APPROVAL] 恢复执行失败 executionId={}: {}", executionId, e.getMessage())
                );
    }

    /**
     * 从 JWT SecurityContext 获取当前登录用户名
     */
    private Mono<String> currentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                .map(c -> c.get("username", String.class))
                .defaultIfEmpty("system");
    }
}
