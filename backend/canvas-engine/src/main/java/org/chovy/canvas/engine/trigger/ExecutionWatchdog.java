package org.chovy.canvas.engine.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.approval.CanvasManualApproval;
import org.chovy.canvas.domain.approval.CanvasManualApprovalMapper;
import org.chovy.canvas.domain.constant.ApprovalOnTimeoutAction;
import org.chovy.canvas.domain.constant.ApprovalStatus;
import org.chovy.canvas.domain.constant.ExecutionStatus;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.chovy.canvas.engine.handlers.ManualApprovalHandler;
import org.chovy.canvas.engine.wait.WaitResumeService;
import org.chovy.canvas.infra.redis.ContextPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Watchdog：每 30s 扫描两类异常状态
 * 1. 僵尸 ctx（Section 9.8）：PAUSED + last_dedup_key 超时 → 清理 dedup 允许 MQ 重投
 * 2. ManualApproval 超时（Section 18.2）：PENDING 审批超过 timeoutAt → 按 onTimeout 策略处理
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ExecutionWatchdog {

    private final CanvasExecutionMapper      executionMapper;
    private final CanvasManualApprovalMapper approvalMapper;
    private final ContextPersistenceService  ctxStore;
    private final CanvasExecutionService     executionService;
    private final WaitResumeService          waitResumeService;

    @Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeoutSec;

    @Value("${canvas.watchdog.zombie-resume-threshold-min:10}")
    private int zombieThresholdMin;

    @Value("${canvas.watchdog.wait-expire-batch-size:200}")
    private int waitExpireBatchSize;

    @Scheduled(fixedDelay = 30_000)
    public void scan() {
        scanZombieCtx();
        scanWaitSubscriptions();
        scanApprovalTimeout();
    }

    // ── 僵尸 ctx 清理（Section 9.8）────────────────────────────────

    private void scanZombieCtx() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(zombieThresholdMin);

        List<CanvasExecution> zombies = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecution>()
                        .eq(CanvasExecution::getStatus, ExecutionStatus.PAUSED.getCode())
                        .isNotNull(CanvasExecution::getLastDedupKey)
                        .lt(CanvasExecution::getUpdatedAt, threshold)
        );

        for (CanvasExecution exec : zombies) {
            log.warn("[WATCHDOG] 僵尸 ctx executionId={} dedupKey={}",
                    exec.getId(), exec.getLastDedupKey());
            ctxStore.releaseDedup(exec.getLastDedupKey());
            exec.setLastDedupKey(null);
            executionMapper.updateById(exec);
            log.info("[WATCHDOG] 僵尸 ctx 已清理 executionId={}", exec.getId());
        }
    }

    private void scanWaitSubscriptions() {
        int resumed = waitResumeService.resumeDueWaits(waitExpireBatchSize);
        if (resumed > 0) {
            log.info("[WATCHDOG] 恢复到期 WAIT/GOAL 订阅 count={}", resumed);
        }
    }

    // ── ManualApproval 超时处理（Section 18.2）─────────────────────

    private void scanApprovalTimeout() {
        List<CanvasManualApproval> timedOut = approvalMapper.selectList(
                new LambdaQueryWrapper<CanvasManualApproval>()
                        .eq(CanvasManualApproval::getStatus, ApprovalStatus.PENDING)
                        .lt(CanvasManualApproval::getTimeoutAt, LocalDateTime.now())
        );

        for (CanvasManualApproval approval : timedOut) {
            log.warn("[WATCHDOG] ManualApproval 超时 approvalId={} onTimeout={}",
                    approval.getId(), approval.getOnTimeout());

            switch (approval.getOnTimeout()) {
                case ApprovalOnTimeoutAction.APPROVE -> processApprovalTimeout(approval, ApprovalStatus.APPROVED);
                case ApprovalOnTimeoutAction.REJECT  -> processApprovalTimeout(approval, ApprovalStatus.REJECTED);
                case ApprovalOnTimeoutAction.KEEP_WAITING -> {
                    // 续期 ctx TTL（防止 Redis key 过期），不做任何处理
                    var ctx = ctxStore.load(approval.getCanvasId(), approval.getUserId());
                    if (ctx != null) ctxStore.save(ctx);
                    log.info("[WATCHDOG] KEEP_WAITING，续期 ctx executionId={}", approval.getExecutionId());
                }
                default -> processApprovalTimeout(approval, ApprovalStatus.REJECTED);
            }
        }
    }

    private void processApprovalTimeout(CanvasManualApproval approval, String result) {
        approval.setStatus(ApprovalStatus.TIMEOUT);
        approval.setResultBy("watchdog");
        approval.setResultAt(LocalDateTime.now());
        approvalMapper.updateById(approval);

        // 向 ctx 写入超时结果，触发恢复执行
        var ctx = ctxStore.load(approval.getCanvasId(), approval.getUserId());
        if (ctx == null) {
            log.warn("[WATCHDOG] ctx 不存在，跳过恢复 executionId={}", approval.getExecutionId());
            return;
        }

        String resultKey = ManualApprovalHandler.APPROVAL_RESULT_KEY + approval.getNodeId();
        ctx.getFlatContext().put(resultKey, result);
        ctxStore.save(ctx);

        executionService.trigger(
                        approval.getCanvasId(), approval.getUserId(),
                        TriggerType.MANUAL_APPROVAL_TIMEOUT, NodeType.MANUAL_APPROVAL,
                        null, Map.of(), approval.getExecutionId() + ":timeout", false)
                .subscribe(
                        null,
                        e -> log.error("[WATCHDOG] 超时恢复失败: {}", e.getMessage())
                );

        log.info("[WATCHDOG] 超时处理完成 approvalId={} result={}", approval.getId(), result);
    }
}
