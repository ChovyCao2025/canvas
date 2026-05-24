package org.chovy.canvas.engine.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasUserQuotaDO;
import org.chovy.canvas.dal.mapper.CanvasUserQuotaMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 触发前置检查（设计文档第 15.2 节）。
 * 检查顺序：status → 有效期 → 冷却期 → 全局配额 → 用户每日限 → 用户总限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerPreCheckService {

    private final CanvasMapper canvasMapper;
    private final CanvasUserQuotaMapper quotaMapper;
    private final StringRedisTemplate redis;

    private static final String GLOBAL_COUNT_KEY = "canvas:global_count:";
    private static final String QUOTA_KEY = "canvas:quota:";

    /**
     * 执行所有前置检查，并在通过后扣减配额。
     *
     * @throws TriggerRejectedException 任意检查不通过时抛出（含错误码）
     */
    public void check(CanvasDO canvas, String userId) {
        checkWithoutQuotaAccounting(canvas, userId);
        consumeQuotaAndRecord(canvas, userId);
    }

    /**
     * 只校验不会扣减配额的触发资格。
     * 用于溢出重试，避免同一个事件在排队期间重复消耗配额。
     */
    public void checkWithoutQuotaAccounting(CanvasDO canvas, String userId) {
        Long canvasId = canvas.getId();

        if (canvas.getStatus() != 1) {
            throw new TriggerRejectedException("QUOTA_006", "画布未发布");
        }

        LocalDateTime now = LocalDateTime.now();
        if (canvas.getValidStart() != null && now.isBefore(canvas.getValidStart())) {
            throw new TriggerRejectedException("QUOTA_005", "活动尚未开始");
        }
        if (canvas.getValidEnd() != null && now.isAfter(canvas.getValidEnd())) {
            throw new TriggerRejectedException("QUOTA_006", "活动已结束");
        }

        if (canvas.getCooldownSeconds() != null) {
            CanvasUserQuotaDO quota = quotaMapper.selectOne(
                    new LambdaQueryWrapper<CanvasUserQuotaDO>()
                            .eq(CanvasUserQuotaDO::getCanvasId, canvasId)
                            .eq(CanvasUserQuotaDO::getUserId, userId)
                            .orderByDesc(CanvasUserQuotaDO::getTriggerDate)
                            .last("LIMIT 1")
            );
            if (quota != null && quota.getLastTriggerAt() != null) {
                long elapsed = Duration.between(quota.getLastTriggerAt(), now).getSeconds();
                if (elapsed < canvas.getCooldownSeconds()) {
                    throw new TriggerRejectedException("QUOTA_003",
                            "冷却期内，距上次触发仅 " + elapsed + "s，需等待 " +
                                    canvas.getCooldownSeconds() + "s");
                }
            }
        }
    }

    /**
     * 扣减配额并记录用量。
     * CanvasExecutionService 在确认本次触发会被执行后调用，避免溢出排队事件提前刷新冷却期。
     */
    public void consumeQuotaAndRecord(CanvasDO canvas, String userId) {
        Long canvasId = canvas.getId();
        List<String> acquiredCounterKeys = new ArrayList<>();
        String acquiredCooldownKey = null;

        try {
            LocalDateTime now = LocalDateTime.now();

            if (canvas.getCooldownSeconds() != null && canvas.getCooldownSeconds() > 0) {
                String key = QUOTA_KEY + "cooldown:" + canvasId + ":" + userId;
                boolean acquired = Boolean.TRUE.equals(redis.opsForValue()
                        .setIfAbsent(key, "1", Duration.ofSeconds(canvas.getCooldownSeconds())));
                if (!acquired) {
                    throw new TriggerRejectedException("QUOTA_003", "冷却期内，请稍后重试");
                }
                acquiredCooldownKey = key;
            }

            if (canvas.getMaxTotalExecutions() != null) {
                incrementWithinLimit(GLOBAL_COUNT_KEY + canvasId, canvas.getMaxTotalExecutions(),
                        null, acquiredCounterKeys, "QUOTA_004", "活动全局触发量已达上限");
            }

            String today = LocalDate.now().toString();

            if (canvas.getPerUserDailyLimit() != null) {
                String key = QUOTA_KEY + canvasId + ":" + userId + ":" + today;
                incrementWithinLimit(key, canvas.getPerUserDailyLimit(), Duration.ofDays(2),
                        acquiredCounterKeys, "QUOTA_001", "用户今日触发次数已达上限");
            }

            if (canvas.getPerUserTotalLimit() != null) {
                String key = QUOTA_KEY + "total:" + canvasId + ":" + userId;
                Duration ttl = canvas.getValidEnd() != null && canvas.getValidEnd().isAfter(now)
                        ? Duration.between(now, canvas.getValidEnd().plusDays(1))
                        : null;
                incrementWithinLimit(key, canvas.getPerUserTotalLimit(), ttl,
                        acquiredCounterKeys, "QUOTA_002", "用户总触发次数已达上限");
            }

            updateQuotaAsync(canvasId, userId);
        } catch (RuntimeException e) {
            rollbackCounters(acquiredCounterKeys);
            if (acquiredCooldownKey != null) {
                redis.delete(acquiredCooldownKey);
            }
            throw e;
        }
    }

    private void incrementWithinLimit(String key, int limit, Duration ttl,
                                      List<String> acquiredCounterKeys,
                                      String rejectCode, String rejectMessage) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L && ttl != null) {
            redis.expire(key, ttl);
        }
        if (count == null || count > limit) {
            if (count != null) {
                redis.opsForValue().decrement(key);
            }
            throw new TriggerRejectedException(rejectCode, rejectMessage);
        }
        acquiredCounterKeys.add(key);
    }

    private void rollbackCounters(List<String> counterKeys) {
        for (String key : counterKeys) {
            redis.opsForValue().decrement(key);
        }
    }

    private void updateQuotaAsync(Long canvasId, String userId) {
        Thread.ofVirtual().start(() -> {
            try {
                quotaMapper.upsertUsage(canvasId, userId, LocalDate.now(), LocalDateTime.now());
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

        public String getCode() {
            return code;
        }
    }
}
