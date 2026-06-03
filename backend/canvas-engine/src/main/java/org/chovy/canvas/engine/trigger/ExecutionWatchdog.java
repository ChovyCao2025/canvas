package org.chovy.canvas.engine.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.common.enums.ApprovalOnTimeoutAction;
import org.chovy.canvas.common.enums.ApprovalStatus;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.engine.handlers.ManualApprovalHandler;
import org.chovy.canvas.engine.wait.WaitResumeService;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
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
 *
 * 设计意图：
 * - 不依赖人工介入即可自动修复“卡死”执行；
 * - 把恢复动作统一收敛到 trigger() 主链路，复用幂等/并发保护。
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ExecutionWatchdog {

    /** 画布执行 Mapper，用于扫描和更新执行状态。 */
    private final CanvasExecutionMapper      executionMapper;
    /** 人工审批 Mapper，用于扫描待超时审批。 */
    private final CanvasManualApprovalMapper approvalMapper;
    /** 执行上下文 Redis 持久化服务。 */
    private final ContextPersistenceService  ctxStore;
    /** 画布执行服务，用于触发超时恢复。 */
    private final CanvasExecutionService     executionService;
    /** WAIT/GOAL 恢复服务，用于批量恢复到期订阅。 */
    private final WaitResumeService          waitResumeService;

    /** 单次画布执行全局超时时间。 */
    @Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeoutSec;

    /** 僵尸执行上下文判定阈值分钟数。 */
    @Value("${canvas.watchdog.zombie-resume-threshold-min:10}")
    private int zombieThresholdMin;

    /** 单次扫描恢复 WAIT/GOAL 到期订阅的批量上限。 */
    @Value("${canvas.watchdog.wait-expire-batch-size:200}")
    private int waitExpireBatchSize;

    @Scheduled(fixedDelay = 30_000)
    public void scan() {
        // 两类扫描彼此独立，避免一类异常阻断另一类修复
        // watchdog 只做补偿动作，真正恢复仍回到 trigger 主链路复用幂等和并发保护。
        scanZombieCtx();
        scanWaitSubscriptions();
        scanApprovalTimeout();
    }

// ── 僵尸 ctx 清理（Section 9.8）────────────────────────────────

    private void scanZombieCtx() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(zombieThresholdMin);

        List<CanvasExecutionDO> zombies = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecutionDO>()
                        .eq(CanvasExecutionDO::getStatus, ExecutionStatus.PAUSED.getCode())
                        .isNotNull(CanvasExecutionDO::getLastDedupKey)
                        .lt(CanvasExecutionDO::getUpdatedAt, threshold)
        );

        for (CanvasExecutionDO exec : zombies) {
            log.warn("[WATCHDOG] 僵尸 ctx executionId={} dedupKey={}",
                    exec.getId(), exec.getLastDedupKey());
            // 释放 dedup 后，相同消息可再次触发，避免永久卡死
            ctxStore.releaseDedup(exec.getLastDedupKey());
            exec.setLastDedupKey(null);
            // 清空 lastDedupKey 后落库，避免下个扫描周期重复释放同一个 key。
            executionMapper.updateById(exec);
            log.info("[WATCHDOG] 僵尸 ctx 已清理 executionId={}", exec.getId());
        }
    }

    /**
     * 执行 scan Wait Subscriptions 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private void scanWaitSubscriptions() {
        // WAIT/GOAL 到期恢复由 WaitResumeService 批量处理，避免 watchdog 自己拼接恢复语义。
        int resumed = waitResumeService.resumeDueWaits(waitExpireBatchSize);
        if (resumed > 0) {
            log.info("[WATCHDOG] 恢复到期 WAIT/GOAL 订阅 count={}", resumed);
        }
    }

// ── ManualApproval 超时处理（Section 18.2）─────────────────────

    private void scanApprovalTimeout() {
        List<CanvasManualApprovalDO> timedOut = approvalMapper.selectList(
                new LambdaQueryWrapper<CanvasManualApprovalDO>()
                        .eq(CanvasManualApprovalDO::getStatus, ApprovalStatus.PENDING)
                        .lt(CanvasManualApprovalDO::getTimeoutAt, LocalDateTime.now())
        );

        for (CanvasManualApprovalDO approval : timedOut) {
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

    /**
     * 执行 process Approval Timeout 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param approval approval 方法执行所需的业务参数
     * @param result result 方法执行所需的业务参数
     */
    private void processApprovalTimeout(CanvasManualApprovalDO approval, String result) {
        // 1) 审批实例落 TIMEOUT，审计可追踪
        approval.setStatus(ApprovalStatus.TIMEOUT);
        approval.setResultBy("watchdog");
        approval.setResultAt(LocalDateTime.now());
        approvalMapper.updateById(approval);

        // 2) 向 ctx 写入超时判定，让 DAG 在 MANUAL_APPROVAL 节点继续路由
        var ctx = ctxStore.load(approval.getCanvasId(), approval.getUserId());
        if (ctx == null) {
            log.warn("[WATCHDOG] ctx 不存在，跳过恢复 executionId={}", approval.getExecutionId());
            return;
        }

        String resultKey = ManualApprovalHandler.APPROVAL_RESULT_KEY + approval.getNodeId();
        // 通过约定 key 把“超时决策结果”传给审批节点后续路由逻辑
        ctx.putRuntimeContextValue(resultKey, result);
        // 先持久化 ctx 再触发恢复，确保恢复链路能读到审批超时决策。
        ctxStore.save(ctx);

        // 3) 复用 trigger() 恢复执行，走统一幂等/并发保护链路
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
