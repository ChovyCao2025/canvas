package com.photon.canvas.controller;

import com.photon.canvas.common.R;
import com.photon.canvas.domain.approval.CanvasManualApproval;
import com.photon.canvas.domain.approval.CanvasManualApprovalMapper;
import com.photon.canvas.engine.handlers.ManualApprovalHandler;
import com.photon.canvas.engine.trigger.CanvasExecutionService;
import com.photon.canvas.infra.redis.ContextPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
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

    private final CanvasManualApprovalMapper  approvalMapper;
    private final ContextPersistenceService   ctxStore;
    private final CanvasExecutionService      executionService;

    /**
     * 人工审批通过（设计文档 18.2节）。
     * 流程：更新 approval 记录 → 向 ctx 写入结果 → 触发继续执行。
     */
    @PostMapping("/{executionId}/approve")
    public Mono<R<Void>> approve(@PathVariable String executionId,
                                  @RequestParam(defaultValue = "system") String approver) {
        return Mono.<Void>fromRunnable(() -> resumeWithResult(executionId, "APPROVED", approver))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }

    /** 人工审批拒绝（设计文档 18.2节） */
    @PostMapping("/{executionId}/reject")
    public Mono<R<Void>> reject(@PathVariable String executionId,
                                 @RequestParam(required = false) String reason,
                                 @RequestParam(defaultValue = "system") String approver) {
        return Mono.<Void>fromRunnable(() -> resumeWithResult(executionId, "REJECTED", approver))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }

    // ── private ──────────────────────────────────────────────────

    private void resumeWithResult(String executionId, String result, String approver) {
        // 1. 找到对应的审批记录（executionId 对应唯一一个 PENDING 审批）
        CanvasManualApproval approval = approvalMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CanvasManualApproval>()
                        .eq(CanvasManualApproval::getExecutionId, executionId)
                        .eq(CanvasManualApproval::getStatus, "PENDING")
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);

        if (approval == null) {
            log.warn("[APPROVAL] 找不到 PENDING 审批记录 executionId={}", executionId);
            return;
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
                        "MANUAL_APPROVAL",
                        null,
                        Map.of("__approvalResult", result),
                        executionId + ":resume:" + result,
                        false)
                .subscribe(
                        r  -> log.info("[APPROVAL] 恢复执行完成 executionId={} result={}", executionId, result),
                        e  -> log.error("[APPROVAL] 恢复执行失败 executionId={}: {}", executionId, e.getMessage())
                );
    }
}
