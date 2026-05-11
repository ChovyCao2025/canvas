package com.photon.canvas.engine.trigger;

import com.photon.canvas.domain.canvas.Canvas;
import com.photon.canvas.domain.canvas.CanvasMapper;
import com.photon.canvas.domain.execution.CanvasUserQuota;
import com.photon.canvas.domain.execution.CanvasUserQuotaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 触发前置检查（设计文档第 15.2 节）。
 * 检查顺序：status → 有效期 → 全局配额 → 用户每日限 → 用户总限 → 冷却期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerPreCheckService {

    private final CanvasMapper          canvasMapper;
    private final CanvasUserQuotaMapper quotaMapper;
    private final StringRedisTemplate   redis;

    private static final String GLOBAL_COUNT_KEY = "canvas:global_count:";
    private static final String QUOTA_KEY        = "canvas:quota:";

    /**
     * 执行所有前置检查。
     * @throws TriggerRejectedException 任意检查不通过时抛出（含错误码）
     */
    public void check(Canvas canvas, String userId) {
        Long canvasId = canvas.getId();

        // 1. 画布状态
        if (canvas.getStatus() != 1) {
            throw new TriggerRejectedException("QUOTA_006", "画布未发布");
        }

        // 2. 有效期
        LocalDateTime now = LocalDateTime.now();
        if (canvas.getValidStart() != null && now.isBefore(canvas.getValidStart())) {
            throw new TriggerRejectedException("QUOTA_005", "活动尚未开始");
        }
        if (canvas.getValidEnd() != null && now.isAfter(canvas.getValidEnd())) {
            throw new TriggerRejectedException("QUOTA_006", "活动已结束");
        }

        // 3. 全局执行量上限
        if (canvas.getMaxTotalExecutions() != null) {
            String key = GLOBAL_COUNT_KEY + canvasId;
            Long count = redis.opsForValue().increment(key);
            if (count == null || count > canvas.getMaxTotalExecutions()) {
                // 超配回滚
                redis.opsForValue().decrement(key);
                throw new TriggerRejectedException("QUOTA_004", "活动全局触发量已达上限");
            }
        }

        String today = LocalDate.now().toString();

        // 4. 用户每日触发上限（Redis INCR 原子扣减）
        if (canvas.getPerUserDailyLimit() != null) {
            String key = QUOTA_KEY + canvasId + ":" + userId + ":" + today;
            Long daily = redis.opsForValue().increment(key);
            redis.expire(key, Duration.ofDays(2)); // TTL 2天，自然隔离
            if (daily != null && daily > canvas.getPerUserDailyLimit()) {
                redis.opsForValue().decrement(key);
                throw new TriggerRejectedException("QUOTA_001", "用户今日触发次数已达上限");
            }
        }

        // 5. 用户总触发上限（从 MySQL 查询，允许轻微超配）
        if (canvas.getPerUserTotalLimit() != null) {
            CanvasUserQuota quota = quotaMapper.selectOne(
                    new LambdaQueryWrapper<CanvasUserQuota>()
                            .eq(CanvasUserQuota::getCanvasId, canvasId)
                            .eq(CanvasUserQuota::getUserId, userId)
                            .eq(CanvasUserQuota::getTriggerDate, LocalDate.now())
            );
            int total = quota != null ? quota.getTotalCount() : 0;
            if (total >= canvas.getPerUserTotalLimit()) {
                throw new TriggerRejectedException("QUOTA_002", "用户总触发次数已达上限");
            }
        }

        // 6. 冷却期（距上次触发间隔）
        if (canvas.getCooldownSeconds() != null) {
            CanvasUserQuota quota = quotaMapper.selectOne(
                    new LambdaQueryWrapper<CanvasUserQuota>()
                            .eq(CanvasUserQuota::getCanvasId, canvasId)
                            .eq(CanvasUserQuota::getUserId, userId)
                            .orderByDesc(CanvasUserQuota::getTriggerDate)
                            .last("LIMIT 1")
            );
            if (quota != null && quota.getLastTriggerAt() != null) {
                long elapsed = java.time.Duration.between(quota.getLastTriggerAt(), now).getSeconds();
                if (elapsed < canvas.getCooldownSeconds()) {
                    throw new TriggerRejectedException("QUOTA_003",
                            "冷却期内，距上次触发仅 " + elapsed + "s，需等待 " +
                                    canvas.getCooldownSeconds() + "s");
                }
            }
        }

        // 检查通过后异步更新用量记录
        updateQuotaAsync(canvasId, userId);
    }

    private void updateQuotaAsync(Long canvasId, String userId) {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor =
                new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        // 用虚拟线程异步写 MySQL（不阻塞触发链路）
        Thread.ofVirtual().start(() -> {
            try {
                LocalDate today = LocalDate.now();
                CanvasUserQuota existing = quotaMapper.selectOne(
                        new LambdaQueryWrapper<CanvasUserQuota>()
                                .eq(CanvasUserQuota::getCanvasId, canvasId)
                                .eq(CanvasUserQuota::getUserId, userId)
                                .eq(CanvasUserQuota::getTriggerDate, today));

                if (existing == null) {
                    CanvasUserQuota q = new CanvasUserQuota();
                    q.setCanvasId(canvasId); q.setUserId(userId);
                    q.setTriggerDate(today); q.setDailyCount(1); q.setTotalCount(1);
                    q.setLastTriggerAt(LocalDateTime.now());
                    quotaMapper.insert(q);
                } else {
                    existing.setDailyCount(existing.getDailyCount() + 1);
                    existing.setTotalCount(existing.getTotalCount() + 1);
                    existing.setLastTriggerAt(LocalDateTime.now());
                    // 复合主键：不能用 updateById()，用 update(entity, wrapper)
                    quotaMapper.update(existing,
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CanvasUserQuota>()
                                    .eq(CanvasUserQuota::getCanvasId, canvasId)
                                    .eq(CanvasUserQuota::getUserId, userId)
                                    .eq(CanvasUserQuota::getTriggerDate, today));
                }
            } catch (Exception e) {
                log.warn("[QUOTA] 用量更新失败: {}", e.getMessage());
            }
        });
    }

    /** 触发拒绝异常（含错误码，供 Controller 映射 HTTP 状态） */
    public static class TriggerRejectedException extends RuntimeException {
        private final String code;
        public TriggerRejectedException(String code, String msg) {
            super(msg);
            this.code = code;
        }
        public String getCode() { return code; }
    }
}
