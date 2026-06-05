package org.chovy.canvas.engine.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasUserQuotaDO;
import org.chovy.canvas.dal.mapper.CanvasUserQuotaMapper;
import org.chovy.canvas.domain.canvas.CanvasControlGroupService;
import org.chovy.canvas.infrastructure.concurrent.ManagedVirtualThreadExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 触发前置检查（设计文档第 15.2 节）。
 *
 * <p>检查顺序：status → 有效期 → 冷却期 → 全局配额 → 用户每日限 → 用户总限。
 *
 * <p>Redis key 说明（{@code prefix} 默认为 {@code canvas}）：
 * <pre>
 * ┌─────────────────────────────────────────────────────┬──────────────────────┬───────────────────────────────────────────────────────┬────────────────────────────────────────────┐
 * │ Key 模式                                            │ 过期时间             │ 业务场景                                              │ 下线/Kill 时清理                           │
 * ├─────────────────────────────────────────────────────┼──────────────────────┼───────────────────────────────────────────────────────┼────────────────────────────────────────────┤
 * │ canvas:quota:cooldown:{canvasId}:{userId}           │ cooldownSeconds（秒）│ 防连续触发，如"两次参与间隔至少 10 分钟"              │ 自然过期，无需主动清理                     │
 * │ canvas:global_count:{canvasId}                      │ ❌ 永不过期          │ 限量发放活动，如"仅前 1000 名用户可参与"              │ ✅ cleanupCanvasQuotas 主动 DEL             │
 * │ canvas:quota:{canvasId}:{userId}:{today}            │ 2 天                 │ 日频限制，如"每人每天最多触发 5 次"                   │ 自然过期（TTL≤2天），无需主动清理          │
 * │ canvas:quota:total:{canvasId}:{userId}              │ validEnd+1天 或 ❌  │ 总参与次数上限，如"每人最多参与 3 次"                 │ ✅ cleanupCanvasQuotas SCAN+DEL            │
 * └─────────────────────────────────────────────────────┴──────────────────────┴───────────────────────────────────────────────────────┴────────────────────────────────────────────┘
 * </pre>
 *
 * <p>双层冷却期设计（分布式场景）：
 * <ul>
 *   <li>{@link #checkWithoutQuotaAccounting} 读 DB 的 {@code last_trigger_at}——属于"软检查"，
 *       DB 由 {@code updateQuotaAsync} 异步写，多机并发时可能读到旧值（假通过）。</li>
 *   <li>{@link #consumeQuotaAndRecord} 用 Redis {@code SETNX} 做冷却 key——属于"强检查"，
 *       跨机原子互斥，是真正的分布式防线。</li>
 * </ul>
 * 两者结合：软检查用于快速短路（减少不必要的 Redis 写），强检查兜底保证正确性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerPreCheckService {

    /** 画布 Mapper，保留给配额检查扩展场景读取画布配置。 */
    private final CanvasMapper canvasMapper;
    /** 用户配额 Mapper。 */
    private final CanvasUserQuotaMapper quotaMapper;
    /** 阻塞式 Redis 模板，用于锁、去重、票据或跨实例通知。 */
    private final StringRedisTemplate redis;
    private ManagedVirtualThreadExecutor backgroundExecutor = ManagedVirtualThreadExecutor.direct();
    private CanvasControlGroupService controlGroupService;

    @Autowired(required = false)
    void setBackgroundExecutor(ManagedVirtualThreadExecutor backgroundExecutor) {
        this.backgroundExecutor = backgroundExecutor;
    }

    @Autowired(required = false)
    void setControlGroupService(CanvasControlGroupService controlGroupService) {
        this.controlGroupService = controlGroupService;
    }

    /** 画布全局触发次数 Redis key 模板。 */
    private static final String GLOBAL_COUNT_KEY = "canvas:global_count:";
    /** 用户配额 Redis key 模板。 */
    private static final String QUOTA_KEY = "canvas:quota:";

    /**
     * 执行所有前置检查，并在通过后扣减配额。
     * 用于常规触发（非溢出重试）：软检查 + 强检查一步完成。
     *
     * @throws TriggerRejectedException 任意检查不通过时抛出（含错误码）
     */
    public void check(CanvasDO canvas, String userId) {
        checkWithoutQuotaAccounting(canvas, userId);
        consumeQuotaAndRecord(canvas, userId);
    }

    /**
     * 仅执行"不扣减配额"的软校验（画布状态、有效期、冷却期 DB 软检查）。
     *
     * <p>设计意图：溢出重试场景下，原始触发已扣减过一次配额（Redis 原子操作），
     * 重试时只需验证资格（是否仍在有效期、画布是否仍发布），不再重复扣减，
     * 避免同一个业务事件因排队延迟而消耗两次用户配额。
     *
     * <p>⚠️ 分布式注意：冷却期检查读 DB（{@code last_trigger_at}），DB 由虚拟线程异步写，
     * 多机并发时有滞后，实际执行时 {@link #consumeQuotaAndRecord} 的 Redis 强检查会二次兜底。
     * WAIT 恢复场景（triggerType=WAIT_RESUME）同样会进入此方法，
     * 若画布配置了 cooldownSeconds，恢复触发可能被冷却期误拒（见 WaitResumeService）。
     */
    public void checkWithoutQuotaAccounting(CanvasDO canvas, String userId) {
        Long canvasId = canvas.getId();

        // 1. 画布状态（在 validateAndLoadCanvas 中已检查一次，此处为防御性重复校验）
        if (canvas.getStatus() != 1) {
            throw new TriggerRejectedException("QUOTA_006", "画布未发布");
        }

        // 2. 有效期：validStart/validEnd 为 null 时不限制（常驻型画布）
        LocalDateTime now = LocalDateTime.now();
        if (canvas.getValidStart() != null && now.isBefore(canvas.getValidStart())) {
            throw new TriggerRejectedException("QUOTA_005", "活动尚未开始");
        }
        if (canvas.getValidEnd() != null && now.isAfter(canvas.getValidEnd())) {
            throw new TriggerRejectedException("QUOTA_006", "活动已结束");
        }

        if (controlGroupService != null && controlGroupService.isHeldOut(canvas, userId)) {
            controlGroupService.recordHoldout(canvasId, userId, null, "CONTROL_GROUP");
            throw new TriggerRejectedException("CONTROL_001", "control group holdout");
        }

        // 3. 冷却期软检查：读 DB 最新记录（异步写，可能有滞后）
        //    真正的原子冷却锁在 consumeQuotaAndRecord 中通过 Redis SETNX 保证
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
     * 强检查并扣减配额（Redis 原子操作，跨机安全）。
     *
     * <p>扣减顺序与回滚：
     * <ol>
     *   <li>冷却 key（SETNX + EX）</li>
     *   <li>全局触发量（INCR）</li>
     *   <li>用户每日限（INCR，2天 TTL）</li>
     *   <li>用户总限（INCR）</li>
     * </ol>
     * 任一步骤失败时，回滚已成功的 INCR（DECR），冷却 key 则 DEL。
     * 最后异步写 DB 用量表（最终一致，不阻塞主流程）。
     *
     * <p>⚠️ 极端场景：Redis 节点故障时 INCR 抛异常，catch 块回滚并重抛，
     * 调用方收到异常后会释放 dedup key 和 slot，该次执行被拒绝。
     * 若 INCR 成功但回滚 DECR 失败（Redis 故障），计数会偏高但不会泄漏配额。
     */
    public void consumeQuotaAndRecord(CanvasDO canvas, String userId) {
        Long canvasId = canvas.getId();
        List<String> acquiredCounterKeys = new ArrayList<>();
        String acquiredCooldownKey = null;

        try {
            LocalDateTime now = LocalDateTime.now();

            // 冷却 key：SETNX + EX，跨机原子互斥，是冷却期的真正防线
            // 业务场景：防止同一用户短时间内重复触发，如"两次参与间隔至少 10 分钟"
            // TTL = cooldownSeconds，key 自然过期，无需下线时清理
            if (canvas.getCooldownSeconds() != null && canvas.getCooldownSeconds() > 0) {
                String key = QUOTA_KEY + "cooldown:" + canvasId + ":" + userId;
                boolean acquired = Boolean.TRUE.equals(redis.opsForValue()
                        .setIfAbsent(key, "1", Duration.ofSeconds(canvas.getCooldownSeconds())));
                if (!acquired) {
                    throw new TriggerRejectedException("QUOTA_003", "冷却期内，请稍后重试");
                }
                acquiredCooldownKey = key;
            }

            // 全局触发总量：画布生命周期内最多被触发 N 次（所有用户合计）
            // 业务场景：限量发放活动，如"仅前 1000 名用户可参与，先到先得"
            // ⚠️ TTL：永不过期。画布下线/Kill/归档时由 cleanupCanvasQuotas() 主动 DEL
            if (canvas.getMaxTotalExecutions() != null) {
                incrementWithinLimit(GLOBAL_COUNT_KEY + canvasId, canvas.getMaxTotalExecutions(),
                        null, acquiredCounterKeys, "QUOTA_004", "活动全局触发量已达上限");
            }

            String today = LocalDate.now().toString();

            // 用户每日触发限：单用户当天最多触发 N 次
            // 业务场景：控制高频骚扰，如"每人每天最多领取 5 次优惠券"
            // TTL = 2 天（按自然日分桶，+1天冗余防跨日误删），自然过期无需主动清理
            if (canvas.getPerUserDailyLimit() != null) {
                String key = QUOTA_KEY + canvasId + ":" + userId + ":" + today;
                incrementWithinLimit(key, canvas.getPerUserDailyLimit(), Duration.ofDays(2),
                        acquiredCounterKeys, "QUOTA_001", "用户今日触发次数已达上限");
            }

            // 用户全生命周期总触发限：单用户在该画布整个运行期内最多触发 N 次
            // 业务场景：防止刷活动，如"每人累计最多参与 3 次，不限时间"
            // ⚠️ TTL：validEnd 存在时 = validEnd+1天；validEnd=null 时永不过期
            //    → 画布下线/Kill/归档时由 cleanupCanvasQuotas() SCAN+DEL
            if (canvas.getPerUserTotalLimit() != null) {
                String key = QUOTA_KEY + "total:" + canvasId + ":" + userId;
                Duration ttl = canvas.getValidEnd() != null && canvas.getValidEnd().isAfter(now)
                        ? Duration.between(now, canvas.getValidEnd().plusDays(1))
                        : null;
                incrementWithinLimit(key, canvas.getPerUserTotalLimit(), ttl,
                        acquiredCounterKeys, "QUOTA_002", "用户总触发次数已达上限");
            }

            // 异步写 DB 用量表（最终一致，用于 checkWithoutQuotaAccounting 的软检查和统计看板）
            updateQuotaAsync(canvasId, userId);
        } catch (RuntimeException e) {
            // 任一步骤失败：回滚已递增的计数器，并释放冷却 key
            rollbackCounters(acquiredCounterKeys);
            if (acquiredCooldownKey != null) {
                redis.delete(acquiredCooldownKey);
            }
            throw e;
        }
    }

    /**
     * 原子递增计数器并检查上限（Lua 脚本，INCR + 首次写入时 PEXPIRE 原子完成）。
     *
     * <p>修复：原两步（INCR → EXPIRE）之间机器崩溃会导致 key 永无 TTL。
     * 现用 Lua 脚本一次 roundtrip 完成：INCR 后若 count==1 且 ttlMs>0 则立即 PEXPIRE。
     * 超限时回滚 DECR 并抛出拒绝异常；INCR 返回 null（Redis 异常）同样视为超限。
     */
    private void incrementWithinLimit(String key, int limit, Duration ttl,
                                      List<String> acquiredCounterKeys,
                                      String rejectCode, String rejectMessage) {
        long ttlMs = ttl != null ? ttl.toMillis() : 0;
        Long count = redis.execute(INCR_WITH_TTL_SCRIPT, List.of(key), String.valueOf(ttlMs));
        if (count == null || count > limit) {
            if (count != null) {
                // 超限时回滚本次 INCR，保持计数器与真实准入次数一致。
                redis.opsForValue().decrement(key);
            }
            throw new TriggerRejectedException(rejectCode, rejectMessage);
        }
        // 记录已成功递增的 key，后续任一步失败时统一回滚。
        acquiredCounterKeys.add(key);
    }

    /**
     * Lua 原子 INCR + 首次写入时 PEXPIRE。
     * KEYS[1]=counter key；ARGV[1]=TTL 毫秒（0 表示不设过期）。
     * 返回 INCR 后的计数值。
     */
    private static final RedisScript<Long> INCR_WITH_TTL_SCRIPT = RedisScript.of(
            "local v = redis.call('INCR', KEYS[1])\n" +
            "if v == 1 and tonumber(ARGV[1]) > 0 then\n" +
            "    redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[1]))\n" +
            "end\n" +
            "return v",
            Long.class
    );

    /** 回滚所有已成功递增的计数器（任一配额检查失败时调用）。 */
    private void rollbackCounters(List<String> counterKeys) {
        for (String key : counterKeys) {
            redis.opsForValue().decrement(key);
        }
    }

    /**
     * 异步写 DB 用量记录（虚拟线程，不阻塞主流程）。
     * DB 用量数据用于：
     * 1. {@link #checkWithoutQuotaAccounting} 的冷却期软检查（最终一致）；
     * 2. 运营后台统计看板（无需强一致）。
     */
    private void updateQuotaAsync(Long canvasId, String userId) {
        backgroundExecutor.submit("quota-usage-update-" + canvasId, () -> {
            try {
                quotaMapper.upsertUsage(canvasId, userId, LocalDate.now(), LocalDateTime.now());
            } catch (Exception e) {
                log.warn("[QUOTA] 用量更新失败: {}", e.getMessage());
            }
        });
    }

    /** 定期修复非发布画布残留的永久配额 Redis key。 */
    @Scheduled(cron = "${canvas.quota.reconcile-cron:0 30 3 * * *}")
    public void reconcileInactiveCanvasQuotasJob() {
        int count = reconcileInactiveCanvasQuotas();
        log.info("[QUOTA] inactive canvas quota reconciliation completed canvases={}", count);
    }

    /**
     * 清理所有非发布画布的永久配额 key。
     *
     * <p>该方法是幂等 repair 入口，用于修复 offline/kill/archive 外部清理失败后遗留的
     * {@code canvas:global_count:*} 与 {@code canvas:quota:total:*} key。
     */
    public int reconcileInactiveCanvasQuotas() {
        List<CanvasDO> inactive = canvasMapper.selectList(
                new LambdaQueryWrapper<CanvasDO>()
                        .ne(CanvasDO::getStatus, CanvasStatusEnum.PUBLISHED.getCode()));
        if (inactive == null || inactive.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (CanvasDO canvas : inactive) {
            if (canvas.getId() == null) {
                continue;
            }
            try {
                cleanupCanvasQuotas(canvas.getId());
                count++;
            } catch (Exception e) {
                log.error("[QUOTA] inactive canvas quota reconciliation failed canvasId={}: {}",
                        canvas.getId(), e.getMessage());
            }
        }
        return count;
    }

    /**
     * 画布下线 / Kill / 归档时清理永不过期的配额 key。
     *
     * <p>清理范围：
     * <ul>
     *   <li>{@code canvas:global_count:{canvasId}}（全局触发总量）——单个 key，同步 DEL；</li>
     *   <li>{@code canvas:quota:total:{canvasId}:*}（所有用户总触发量）——异步 SCAN+DEL，
     *       大体量活动可能有百万 key，不阻塞调用方。</li>
     * </ul>
     *
     * <p>不清理的 key（自然过期）：
     * <ul>
     *   <li>冷却 key：TTL = cooldownSeconds，通常为分钟级，自然过期；</li>
     *   <li>每日限额 key：TTL = 2天，自然过期。</li>
     * </ul>
     *
     * <p>调用时机：{@link org.chovy.canvas.domain.canvas.CanvasService#offline}、
     * {@link org.chovy.canvas.domain.canvas.CanvasService#archive}、
     * {@link org.chovy.canvas.domain.canvas.CanvasOpsService#kill}
     */
    public void cleanupCanvasQuotas(Long canvasId) {
        // 1. 同步删除全局触发总量（1 个 key，快速完成）
        redis.delete(GLOBAL_COUNT_KEY + canvasId);
        log.info("[QUOTA] 已删除全局触发计数 canvasId={}", canvasId);

        // 2. 异步 SCAN + DEL 用户总触发量（可能是海量 key，不阻塞调用方）
        backgroundExecutor.submit("quota-cleanup-" + canvasId, () -> {
            String pattern = QUOTA_KEY + "total:" + canvasId + ":*";
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
            int deleted = 0;
            List<String> batch = new ArrayList<>(500);
            try (Cursor<String> cursor = redis.scan(options)) {
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= 500) {
                        // 分批 DEL，避免一次性删除海量 key 阻塞 Redis。
                        redis.delete(batch);
                        deleted += batch.size();
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    redis.delete(batch);
                    deleted += batch.size();
                }
            } catch (Exception e) {
                log.error("[QUOTA] 清理用户总配额 key 失败 canvasId={}: {}", canvasId, e.getMessage());
                return;
            }
            log.info("[QUOTA] 已清理用户总配额 key canvasId={} count={}", canvasId, deleted);
        });
    }

    /** 触发拒绝异常，携带错误码供 Controller 映射 HTTP 状态。 */
    public static class TriggerRejectedException extends RuntimeException {
        /** 触发拒绝错误码。 */
        private final String code;

        /** 创建带业务错误码和提示信息的触发拒绝异常。 */
        public TriggerRejectedException(String code, String msg) {
            super(msg);
            this.code = code;
        }

        /** 返回触发拒绝错误码。 */
        public String getCode() {
            return code;
        }
    }
}
