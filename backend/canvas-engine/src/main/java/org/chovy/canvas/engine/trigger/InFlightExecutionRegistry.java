package org.chovy.canvas.engine.trigger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneAdmissionResult;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 画布执行实例注册表（分布式并发控制 + 本地 Kill Switch）。
 *
 * <p>双层设计：
 * <ul>
 *   <li><b>Redis ZSET（分布式）</b>：跨机原子并发计数，解决多机部署下并发限制失效问题。
 *       每个执行以 score=过期时间戳 写入 ZSET，自然具备崩溃自愈能力——
 *       机器宕机后，其注册的条目会在 globalTimeoutSec 内被 ZREMRANGEBYSCORE 自动清除。</li>
 *   <li><b>JVM 本地 Map</b>：仅用于 Kill Switch，持有 Reactor {@link Disposable.Swap}
 *       引用以便随时取消订阅。Disposable 本质是 JVM 对象，无法跨机传递。</li>
 * </ul>
 *
 * <p>崩溃自愈：Lua 脚本在每次 acquire 时先执行 {@code ZREMRANGEBYSCORE(0, now)}，
 * 将过期的僵尸条目清除出计数，下次 acquire 时即可看到正确的并发数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InFlightExecutionRegistry {

    /** 阻塞式 Redis 模板，用于分布式执行槽位计数。 */
    private final StringRedisTemplate redis;
    /** Redis key 工具，集中生成执行槽位相关 key。 */
    private final RedisKeyUtil keys;

    /** 执行槽位过期和崩溃自愈使用的全局超时秒数。 */
    @Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeoutSec;

    /**
     * JVM 本地注册表，仅供 Kill Switch 使用。
     * canvasId → { executionId → Reactor 订阅槽位 }
     */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, Disposable.Swap>> localRegistry =
            new ConcurrentHashMap<>();
    /** executionId → lane，用于结束或 Kill Switch 时释放对应 lane ZSET。 */
    private final ConcurrentHashMap<String, ExecutionLane> localExecutionLanes = new ConcurrentHashMap<>();

    /**
     * 原子获取分布式执行槽位。
     *
     * <p>Lua 脚本一次性完成（原子）：
     * <ol>
     *   <li>ZREMRANGEBYSCORE 清除已过期的僵尸条目（崩溃自愈）；</li>
     *   <li>ZCARD 读取当前活跃数，双重检查（画布维度 + 全局维度）；</li>
     *   <li>双重检查通过后 ZADD 写入，score = 当前时间 + TTL（ms）；</li>
     *   <li>PEXPIREAT 为 ZSET key 本身设置过期，防 key 永久残留。</li>
     * </ol>
     *
     * <p>返回 empty 时调用方负责溢出处理（入队重试或丢弃）。
     * Redis 异常时降级为本地计数（保守拒绝：返回 empty 以防止雪崩）。
     */
    public Optional<Disposable.Swap> tryAcquire(Long canvasId, String executionId,
                                                int canvasLimit, int globalLimit) {
        ExecutionLaneAdmissionResult result = tryAcquire(
                canvasId, executionId, ExecutionLane.STANDARD, canvasLimit, globalLimit, globalLimit);
        return result.allowed() ? Optional.of(result.slot()) : Optional.empty();
    }

    /**
     * 原子获取分布式执行槽位（画布 + lane + 全局三层预算）。
     */
    public ExecutionLaneAdmissionResult tryAcquire(Long canvasId, String executionId,
                                                   ExecutionLane lane,
                                                   int canvasLimit, int laneLimit, int globalLimit) {
        if (canvasLimit <= 0 || laneLimit <= 0 || globalLimit <= 0) {
            return ExecutionLaneAdmissionResult.rejected(
                    ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE, 0, 0, 0);
        }
        ExecutionLane effectiveLane = lane != null ? lane : ExecutionLane.STANDARD;
        String canvasKey = keys.inflightCanvas(canvasId);
        String laneKey = keys.inflightLane(effectiveLane);
        String globalKey = keys.inflightGlobal();
        long nowMs = System.currentTimeMillis();
        long expiryMs = nowMs + globalTimeoutSec * 1000L;

        Long result;
        try {
            // Lua 内一次完成清僵尸、计数检查和 ZADD，避免多实例并发超卖 slot。
            result = redis.execute(
                    ACQUIRE_SCRIPT,
                    List.of(canvasKey, laneKey, globalKey),
                    String.valueOf(nowMs),
                    String.valueOf(expiryMs),
                    String.valueOf(canvasLimit),
                    String.valueOf(laneLimit),
                    String.valueOf(globalLimit),
                    executionId
            );
        } catch (Exception e) {
            log.error("[REGISTRY] Redis acquire 失败，拒绝本次执行以防雪崩 canvasId={}: {}", canvasId, e.getMessage());
            return ExecutionLaneAdmissionResult.rejected(
                    ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE, 0, 0, 0);
        }

        if (result == null || result <= 0) {
            log.debug("[REGISTRY] 并发槽位不足 canvasId={} lane={} result={}", canvasId, effectiveLane, result);
            return ExecutionLaneAdmissionResult.rejected(
                    mapReason(result),
                    activeCount(canvasId),
                    laneActiveCount(effectiveLane),
                    totalActiveCount());
        }

        // Redis 准入成功，再注册本地槽位（用于 Kill Switch）
        AtomicBoolean localRegistered = new AtomicBoolean(false);
        Disposable.Swap slot = Disposables.swap();
        localRegistry.compute(canvasId, (id, current) -> {
            ConcurrentHashMap<String, Disposable.Swap> map =
                    current != null ? current : new ConcurrentHashMap<>();
            map.put(executionId, slot);
            localRegistered.set(true);
            return map;
        });
        localExecutionLanes.put(localExecutionKey(canvasId, executionId), effectiveLane);

        if (!localRegistered.get()) {
            // 本地注册失败（极罕见），回滚 Redis 槽位
            localExecutionLanes.remove(localExecutionKey(canvasId, executionId));
            releaseRedisSlot(canvasKey, laneKey, globalKey, executionId);
            return ExecutionLaneAdmissionResult.rejected(
                    ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE, 0, 0, 0);
        }

        log.debug("[REGISTRY] 准入执行 canvasId={} lane={} executionId={}",
                canvasId, effectiveLane, executionId);
        return ExecutionLaneAdmissionResult.allowed(slot, 0, 0, 0);
    }

    /**
     * 执行结束时注销。
     * 先移除本地槽位，再释放 Redis ZSET 中的条目（两步顺序不影响正确性，Redis 崩溃最多留僵尸条目，TTL 自愈）。
     */
    public void deregister(Long canvasId, String executionId) {
        ConcurrentHashMap<String, Disposable.Swap> map = localRegistry.get(canvasId);
        if (map != null) {
            Disposable.Swap removed = map.remove(executionId);
            if (removed != null) {
                if (map.isEmpty()) localRegistry.remove(canvasId);
                ExecutionLane lane = localExecutionLanes.remove(localExecutionKey(canvasId, executionId));
                // 执行结束释放 Redis slot；若失败，ZSET score 到期会兜底清理。
                releaseRedisSlot(
                        keys.inflightCanvas(canvasId),
                        keys.inflightLane(lane != null ? lane : ExecutionLane.STANDARD),
                        keys.inflightGlobal(),
                        executionId);
            }
        }
    }

    /**
     * FORCE Kill：取消指定画布所有正在进行的执行，同时释放 Redis 槽位。
     * Reactor Disposable.dispose() 触发 Mono 取消信号。
     */
    public int cancelAll(Long canvasId) {
        ConcurrentHashMap<String, Disposable.Swap> map = localRegistry.remove(canvasId);
        if (map == null) return 0;
        String canvasKey = keys.inflightCanvas(canvasId);
        String globalKey = keys.inflightGlobal();
        map.forEach((execId, d) -> {
            if (!d.isDisposed()) {
                // 本地 Disposable 只能取消当前 JVM 内的执行，跨机执行依赖各实例自己的 registry。
                d.dispose();
                log.info("[REGISTRY] FORCE 取消执行 canvasId={} executionId={}", canvasId, execId);
            }
            ExecutionLane lane = localExecutionLanes.remove(localExecutionKey(canvasId, execId));
            releaseRedisSlot(
                    canvasKey,
                    keys.inflightLane(lane != null ? lane : ExecutionLane.STANDARD),
                    globalKey,
                    execId);
        });
        return map.size();
    }

    /**
     * 当前画布维度活跃数（读 Redis ZSET，先清过期条目）。
     * 非原子操作，允许轻微误差（仅用于监控日志和软准入决策）。
     * Redis 不可用时降级返回本地计数。
     */
    public int activeCount(Long canvasId) {
        try {
            String key = keys.inflightCanvas(canvasId);
            // 读计数前先清理过期 score，减少崩溃残留对准入判断的影响。
            redis.opsForZSet().removeRangeByScore(key, 0, System.currentTimeMillis());
            Long count = redis.opsForZSet().zCard(key);
            return count == null ? 0 : count.intValue();
        } catch (Exception e) {
            log.warn("[REGISTRY] activeCount Redis 读取失败，降级本地计数 canvasId={}: {}", canvasId, e.getMessage());
            ConcurrentHashMap<String, Disposable.Swap> map = localRegistry.get(canvasId);
            return map == null ? 0 : map.size();
        }
    }

    /**
     * 全局活跃数（读 Redis ZSET，先清过期条目）。
     * Redis 不可用时降级返回本地所有画布计数之和。
     */
    public int totalActiveCount() {
        try {
            String key = keys.inflightGlobal();
            // 全局计数同样先清僵尸，保持监控和准入的水位接近真实值。
            redis.opsForZSet().removeRangeByScore(key, 0, System.currentTimeMillis());
            Long count = redis.opsForZSet().zCard(key);
            return count == null ? 0 : count.intValue();
        } catch (Exception e) {
            log.warn("[REGISTRY] totalActiveCount Redis 读取失败，降级本地计数: {}", e.getMessage());
            return localRegistry.values().stream().mapToInt(ConcurrentHashMap::size).sum();
        }
    }

    /** 当前 lane 维度活跃数（读 Redis ZSET，先清过期条目）。 */
    public int laneActiveCount(ExecutionLane lane) {
        try {
            String key = keys.inflightLane(lane != null ? lane : ExecutionLane.STANDARD);
            redis.opsForZSet().removeRangeByScore(key, 0, System.currentTimeMillis());
            Long count = redis.opsForZSet().zCard(key);
            return count == null ? 0 : count.intValue();
        } catch (Exception e) {
            log.warn("[REGISTRY] laneActiveCount Redis 读取失败，降级本地计数 lane={}: {}",
                    lane, e.getMessage());
            ExecutionLane effectiveLane = lane != null ? lane : ExecutionLane.STANDARD;
            return (int) localExecutionLanes.values().stream().filter(effectiveLane::equals).count();
        }
    }

    // ── Redis 操作 ────────────────────────────────────────────────────

    private void releaseRedisSlot(String canvasKey, String laneKey, String globalKey, String executionId) {
        try {
            redis.execute(RELEASE_SCRIPT, List.of(canvasKey, laneKey, globalKey), executionId);
        } catch (Exception e) {
            // ZREM 失败不影响业务，条目会在 TTL（score=expiryMs）到期后被 ZREMRANGEBYSCORE 自动清除
            log.warn("[REGISTRY] Redis ZREM 失败，等待 TTL 自愈 executionId={}: {}", executionId, e.getMessage());
        }
    }

    private static String localExecutionKey(Long canvasId, String executionId) {
        return canvasId + ":" + executionId;
    }

    private static ExecutionLaneAdmissionResult.Reason mapReason(Long result) {
        if (result == null) {
            return ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE;
        }
        return switch (result.intValue()) {
            case -1 -> ExecutionLaneAdmissionResult.Reason.CANVAS_LIMIT;
            case -2 -> ExecutionLaneAdmissionResult.Reason.LANE_LIMIT;
            case -3 -> ExecutionLaneAdmissionResult.Reason.GLOBAL_LIMIT;
            default -> ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE;
        };
    }

    // ── Lua 脚本 ─────────────────────────────────────────────────────

    /**
     * 原子获取槽位脚本。
     * KEYS[1]=canvasKey, KEYS[2]=laneKey, KEYS[3]=globalKey
     * ARGV[1]=nowMs, ARGV[2]=expiryMs, ARGV[3]=canvasLimit, ARGV[4]=laneLimit, ARGV[5]=globalLimit, ARGV[6]=memberId
     * 返回：1=成功；-1=画布维度超限；-2=lane 维度超限；-3=全局维度超限
     */
    private static final RedisScript<Long> ACQUIRE_SCRIPT = RedisScript.of(
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])\n" +
            "redis.call('ZREMRANGEBYSCORE', KEYS[2], 0, ARGV[1])\n" +
            "redis.call('ZREMRANGEBYSCORE', KEYS[3], 0, ARGV[1])\n" +
            "local cc = tonumber(redis.call('ZCARD', KEYS[1]))\n" +
            "local lc = tonumber(redis.call('ZCARD', KEYS[2]))\n" +
            "local gc = tonumber(redis.call('ZCARD', KEYS[3]))\n" +
            "if cc >= tonumber(ARGV[3]) then return -1 end\n" +
            "if lc >= tonumber(ARGV[4]) then return -2 end\n" +
            "if gc >= tonumber(ARGV[5]) then return -3 end\n" +
            "redis.call('ZADD', KEYS[1], tonumber(ARGV[2]), ARGV[6])\n" +
            "redis.call('ZADD', KEYS[2], tonumber(ARGV[2]), ARGV[6])\n" +
            "redis.call('ZADD', KEYS[3], tonumber(ARGV[2]), ARGV[6])\n" +
            "redis.call('PEXPIREAT', KEYS[1], tonumber(ARGV[2]) + 60000)\n" +
            "redis.call('PEXPIREAT', KEYS[2], tonumber(ARGV[2]) + 60000)\n" +
            "redis.call('PEXPIREAT', KEYS[3], tonumber(ARGV[2]) + 60000)\n" +
            "return 1",
            Long.class
    );

    /**
     * 原子释放槽位脚本。
     * KEYS[1]=canvasKey, KEYS[2]=laneKey, KEYS[3]=globalKey; ARGV[1]=memberId
     */
    private static final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of(
            "redis.call('ZREM', KEYS[1], ARGV[1])\n" +
            "redis.call('ZREM', KEYS[2], ARGV[1])\n" +
            "redis.call('ZREM', KEYS[3], ARGV[1])\n" +
            "return 1",
            Long.class
    );
}
