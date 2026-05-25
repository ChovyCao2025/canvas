package org.chovy.canvas.engine.trigger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final StringRedisTemplate redis;
    private final RedisKeyUtil keys;

    @Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeoutSec;

    /**
     * JVM 本地注册表，仅供 Kill Switch 使用。
     * canvasId → { executionId → Reactor 订阅槽位 }
     */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, Disposable.Swap>> localRegistry =
            new ConcurrentHashMap<>();

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
        if (canvasLimit <= 0 || globalLimit <= 0) {
            return Optional.empty();
        }

        String canvasKey = keys.inflightCanvas(canvasId);
        String globalKey = keys.inflightGlobal();
        long nowMs = System.currentTimeMillis();
        long expiryMs = nowMs + globalTimeoutSec * 1000L;

        Long result;
        try {
            result = redis.execute(
                    ACQUIRE_SCRIPT,
                    List.of(canvasKey, globalKey),
                    String.valueOf(nowMs),
                    String.valueOf(expiryMs),
                    String.valueOf(canvasLimit),
                    String.valueOf(globalLimit),
                    executionId
            );
        } catch (Exception e) {
            log.error("[REGISTRY] Redis acquire 失败，拒绝本次执行以防雪崩 canvasId={}: {}", canvasId, e.getMessage());
            return Optional.empty();
        }

        if (result == null || result <= 0) {
            log.debug("[REGISTRY] 并发槽位不足 canvasId={} result={}", canvasId, result);
            return Optional.empty();
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

        if (!localRegistered.get()) {
            // 本地注册失败（极罕见），回滚 Redis 槽位
            releaseRedisSlot(canvasKey, globalKey, executionId);
            return Optional.empty();
        }

        log.debug("[REGISTRY] 准入执行 canvasId={} executionId={}", canvasId, executionId);
        return Optional.of(slot);
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
                releaseRedisSlot(keys.inflightCanvas(canvasId), keys.inflightGlobal(), executionId);
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
                d.dispose();
                log.info("[REGISTRY] FORCE 取消执行 canvasId={} executionId={}", canvasId, execId);
            }
            releaseRedisSlot(canvasKey, globalKey, execId);
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
            redis.opsForZSet().removeRangeByScore(key, 0, System.currentTimeMillis());
            Long count = redis.opsForZSet().zCard(key);
            return count == null ? 0 : count.intValue();
        } catch (Exception e) {
            log.warn("[REGISTRY] totalActiveCount Redis 读取失败，降级本地计数: {}", e.getMessage());
            return localRegistry.values().stream().mapToInt(ConcurrentHashMap::size).sum();
        }
    }

    // ── Redis 操作 ────────────────────────────────────────────────────

    private void releaseRedisSlot(String canvasKey, String globalKey, String executionId) {
        try {
            redis.execute(RELEASE_SCRIPT, List.of(canvasKey, globalKey), executionId);
        } catch (Exception e) {
            // ZREM 失败不影响业务，条目会在 TTL（score=expiryMs）到期后被 ZREMRANGEBYSCORE 自动清除
            log.warn("[REGISTRY] Redis ZREM 失败，等待 TTL 自愈 executionId={}: {}", executionId, e.getMessage());
        }
    }

    // ── Lua 脚本 ─────────────────────────────────────────────────────

    /**
     * 原子获取槽位脚本。
     * KEYS[1]=canvasKey, KEYS[2]=globalKey
     * ARGV[1]=nowMs, ARGV[2]=expiryMs, ARGV[3]=canvasLimit, ARGV[4]=globalLimit, ARGV[5]=memberId
     * 返回：1=成功；-1=画布维度超限；-2=全局维度超限
     */
    private static final RedisScript<Long> ACQUIRE_SCRIPT = RedisScript.of(
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])\n" +
            "redis.call('ZREMRANGEBYSCORE', KEYS[2], 0, ARGV[1])\n" +
            "local cc = tonumber(redis.call('ZCARD', KEYS[1]))\n" +
            "local gc = tonumber(redis.call('ZCARD', KEYS[2]))\n" +
            "if cc >= tonumber(ARGV[3]) then return -1 end\n" +
            "if gc >= tonumber(ARGV[4]) then return -2 end\n" +
            "redis.call('ZADD', KEYS[1], tonumber(ARGV[2]), ARGV[5])\n" +
            "redis.call('ZADD', KEYS[2], tonumber(ARGV[2]), ARGV[5])\n" +
            "redis.call('PEXPIREAT', KEYS[1], tonumber(ARGV[2]) + 60000)\n" +
            "redis.call('PEXPIREAT', KEYS[2], tonumber(ARGV[2]) + 60000)\n" +
            "return 1",
            Long.class
    );

    /**
     * 原子释放槽位脚本。
     * KEYS[1]=canvasKey, KEYS[2]=globalKey; ARGV[1]=memberId
     */
    private static final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of(
            "redis.call('ZREM', KEYS[1], ARGV[1])\n" +
            "redis.call('ZREM', KEYS[2], ARGV[1])\n" +
            "return 1",
            Long.class
    );
}
