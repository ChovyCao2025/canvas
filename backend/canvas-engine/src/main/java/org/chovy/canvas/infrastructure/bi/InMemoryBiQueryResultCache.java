package org.chovy.canvas.infrastructure.bi;

import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.bi.query.BiQueryCacheStats;
import org.chovy.canvas.domain.bi.query.BiQueryResultCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * InMemoryBiQueryResultCache 封装 infrastructure.bi 场景的基础设施集成。
 */
@Component
@ConditionalOnProperty(name = "canvas.bi.query.cache.provider", havingValue = "memory", matchIfMissing = true)
public class InMemoryBiQueryResultCache implements BiQueryResultCache {

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final Duration ttl;
    private final int maxSize;
    private final Clock clock;
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong putCount = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();

    /**
     * 创建 InMemoryBiQueryResultCache 实例并注入 infrastructure.bi 场景依赖。
     * @param enabled enabled 参数，用于 InMemoryBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param ttlSeconds ttl seconds 参数，用于 InMemoryBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param maxSize max size 参数，用于 InMemoryBiQueryResultCache 流程中的校验、计算或对象转换。
     */
    @Autowired
    public InMemoryBiQueryResultCache(
            @Value("${canvas.bi.query.cache.enabled:true}") boolean enabled,
            @Value("${canvas.bi.query.cache.ttl-seconds:300}") long ttlSeconds,
            @Value("${canvas.bi.query.cache.max-size:500}") int maxSize) {
        this(enabled, Duration.ofSeconds(Math.max(1, ttlSeconds)), Math.max(1, maxSize), Clock.systemUTC());
    }

    /**
     * 执行 InMemoryBiQueryResultCache 流程，围绕 in memory bi query result cache 完成校验、计算或结果组装。
     *
     * @param enabled enabled 参数，用于 InMemoryBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param ttl ttl 参数，用于 InMemoryBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param maxSize max size 参数，用于 InMemoryBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    InMemoryBiQueryResultCache(boolean enabled, Duration ttl, int maxSize, Clock clock) {
        this.enabled = enabled;
        this.ttl = ttl;
        this.maxSize = Math.max(1, maxSize);
        this.clock = clock;
    }

    /**
     * get 查询 infrastructure.bi 场景的业务数据。
     * @param sqlHash sql hash 参数，用于 get 流程中的校验、计算或对象转换。
     * @return 返回 get 流程生成的业务结果。
     */
    @Override
    public Optional<BiQueryResult> get(String sqlHash) {
        if (!enabled || sqlHash == null || sqlHash.isBlank()) {
            return Optional.empty();
        }
        Entry entry = entries.get(sqlHash);
        if (entry == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }
        Instant now = clock.instant();
        if (entry.expiresAt().isBefore(now) || entry.expiresAt().equals(now)) {
            if (entries.remove(sqlHash, entry)) {
                evictionCount.incrementAndGet();
            }
            // Expired entries count as misses because callers must re-execute the query.
            missCount.incrementAndGet();
            return Optional.empty();
        }
        hitCount.incrementAndGet();
        return Optional.of(entry.result());
    }

    /**
     * put 处理 infrastructure.bi 场景的业务逻辑。
     * @param sqlHash sql hash 参数，用于 put 流程中的校验、计算或对象转换。
     * @param result result 参数，用于 put 流程中的校验、计算或对象转换。
     */
    @Override
    public void put(String sqlHash, BiQueryResult result) {
        put(sqlHash, result, ttl);
    }

    /**
     * put 处理 infrastructure.bi 场景的业务逻辑。
     * @param sqlHash sql hash 参数，用于 put 流程中的校验、计算或对象转换。
     * @param result result 参数，用于 put 流程中的校验、计算或对象转换。
     * @param ttl ttl 参数，用于 put 流程中的校验、计算或对象转换。
     */
    @Override
    public void put(String sqlHash, BiQueryResult result, Duration ttl) {
        if (!enabled || sqlHash == null || sqlHash.isBlank() || result == null) {
            return;
        }
        // Prune before size enforcement so natural TTL expiry is preferred over evicting still-valid entries.
        pruneExpired();
        if (entries.size() >= maxSize) {
            evictOldest();
        }
        Duration effectiveTtl = ttl == null || ttl.isNegative() || ttl.isZero() ? this.ttl : ttl;
        entries.put(sqlHash, new Entry(result, clock.instant().plus(effectiveTtl)));
        putCount.incrementAndGet();
    }

    /**
     * evict 删除或清理 infrastructure.bi 场景的业务数据。
     * @param sqlHash sql hash 参数，用于 evict 流程中的校验、计算或对象转换。
     * @return 返回 evict 的布尔判断结果。
     */
    @Override
    public boolean evict(String sqlHash) {
        if (sqlHash == null || sqlHash.isBlank()) {
            return false;
        }
        boolean removed = entries.remove(sqlHash) != null;
        if (removed) {
            evictionCount.incrementAndGet();
        }
        return removed;
    }

    /**
     * evictDataset 删除或清理 infrastructure.bi 场景的业务数据。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 evict dataset 计算得到的数量、金额或指标值。
     */
    @Override
    public int evictDataset(String datasetKey) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (datasetKey == null || datasetKey.isBlank()) {
            return 0;
        }
        int before = entries.size();
        entries.entrySet().removeIf(entry -> datasetKey.equals(entry.getValue().result().datasetKey()));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        int deleted = before - entries.size();
        evictionCount.addAndGet(Math.max(0, deleted));
        return deleted;
    }

    /**
     * clear 删除或清理 infrastructure.bi 场景的业务数据。
     * @return 返回 clear 计算得到的数量、金额或指标值。
     */
    @Override
    public int clear() {
        int before = entries.size();
        entries.clear();
        evictionCount.addAndGet(before);
        return before;
    }

    /**
     * stats 查询 infrastructure.bi 场景的业务数据。
     * @return 返回 stats 流程生成的业务结果。
     */
    @Override
    public BiQueryCacheStats stats() {
        return new BiQueryCacheStats(
                "memory",
                enabled,
                size(),
                maxSize,
                ttl.toSeconds(),
                hitCount.get(),
                missCount.get(),
                putCount.get(),
                evictionCount.get());
    }

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @return 返回统计数量。
     */
    int size() {
        pruneExpired();
        return entries.size();
    }

    /**
     * 执行 pruneExpired 流程，围绕 prune expired 完成校验、计算或结果组装。
     */
    private void pruneExpired() {
        Instant now = clock.instant();
        AtomicInteger removed = new AtomicInteger();
        entries.entrySet().removeIf(entry -> {
            boolean expired = !entry.getValue().expiresAt().isAfter(now);
            if (expired) {
                removed.incrementAndGet();
            }
            return expired;
        });
        evictionCount.addAndGet(removed.get());
    }

    /**
     * 执行 evictOldest 流程，围绕 evict oldest 完成校验、计算或结果组装。
     */
    private void evictOldest() {
        entries.entrySet().stream()
                // Expiry time is used as a lightweight age proxy; shorter-lived entries leave first under pressure.
                .min(Comparator.comparing(entry -> entry.getValue().expiresAt()))
                .ifPresent(entry -> {
                    if (entries.remove(entry.getKey(), entry.getValue())) {
                        evictionCount.incrementAndGet();
                    }
                });
    }

    /**
     * Entry 数据记录。
     */
    private record Entry(BiQueryResult result, Instant expiresAt) {
    }
}
