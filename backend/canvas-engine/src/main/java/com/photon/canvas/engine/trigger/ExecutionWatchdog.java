package com.photon.canvas.engine.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.photon.canvas.domain.execution.CanvasExecution;
import com.photon.canvas.domain.execution.CanvasExecutionMapper;
import com.photon.canvas.infra.redis.ContextPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Watchdog：每 30s 扫描超时执行、僵尸 ctx 清理。
 * 设计文档第 9.8 节。
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ExecutionWatchdog {

    private final CanvasExecutionMapper executionMapper;
    private final ContextPersistenceService ctxStore;

    @Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeoutSec;

    @Value("${canvas.watchdog.zombie-resume-threshold-min:10}")
    private int zombieThresholdMin;

    @Scheduled(fixedDelay = 30_000)
    public void scan() {
        scanZombieCtx();
    }

    /**
     * 扫描 PAUSED 且有 last_dedup_key 且长时间无进展的执行 → 清理 dedup，允许 MQ 重投
     */
    private void scanZombieCtx() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(zombieThresholdMin);

        List<CanvasExecution> zombies = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecution>()
                        .eq(CanvasExecution::getStatus, 1)       // PAUSED
                        .isNotNull(CanvasExecution::getLastDedupKey)
                        .lt(CanvasExecution::getUpdatedAt, threshold)
        );

        for (CanvasExecution exec : zombies) {
            log.warn("[WATCHDOG] 发现僵尸 ctx executionId={} lastDedupKey={}",
                    exec.getId(), exec.getLastDedupKey());
            // 清除 dedup key，让 MQ 重投时可以正常恢复
            ctxStore.releaseDedup(exec.getLastDedupKey());
            // 重置 last_dedup_key 避免重复清理
            exec.setLastDedupKey(null);
            executionMapper.updateById(exec);
            log.info("[WATCHDOG] 已清理僵尸 ctx，等待 MQ 重投 executionId={}", exec.getId());
        }
    }
}
