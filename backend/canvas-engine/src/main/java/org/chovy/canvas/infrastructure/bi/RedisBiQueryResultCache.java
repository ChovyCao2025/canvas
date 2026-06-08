package org.chovy.canvas.infrastructure.bi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.bi.query.BiQueryCacheStats;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.bi.query.BiQueryResultCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RedisBiQueryResultCache 封装 infrastructure.bi 场景的基础设施集成。
 */
@Component
@ConditionalOnProperty(name = "canvas.bi.query.cache.provider", havingValue = "redis")
public class RedisBiQueryResultCache implements BiQueryResultCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Duration ttl;
    private final String keyPrefix;
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong putCount = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();

    /**
     * 创建 RedisBiQueryResultCache 实例并注入 infrastructure.bi 场景依赖。
     * @param redis redis 参数，用于 RedisBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 RedisBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param ttlSeconds ttl seconds 参数，用于 RedisBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param keyPrefix key prefix 参数，用于 RedisBiQueryResultCache 流程中的校验、计算或对象转换。
     */
    @Autowired
    public RedisBiQueryResultCache(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${canvas.bi.query.cache.enabled:true}") boolean enabled,
            @Value("${canvas.bi.query.cache.ttl-seconds:300}") long ttlSeconds,
            @Value("${canvas.bi.query.cache.redis.key-prefix:canvas:bi:query-cache:}") String keyPrefix) {
        this(redis, objectMapper, enabled, Duration.ofSeconds(Math.max(1, ttlSeconds)), keyPrefix);
    }

    /**
     * 执行 RedisBiQueryResultCache 流程，围绕 redis bi query result cache 完成校验、计算或结果组装。
     *
     * @param redis redis 参数，用于 RedisBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 RedisBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param ttl ttl 参数，用于 RedisBiQueryResultCache 流程中的校验、计算或对象转换。
     * @param keyPrefix key prefix 参数，用于 RedisBiQueryResultCache 流程中的校验、计算或对象转换。
     */
    RedisBiQueryResultCache(StringRedisTemplate redis,
                            ObjectMapper objectMapper,
                            boolean enabled,
                            Duration ttl,
                            String keyPrefix) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.ttl = ttl == null || ttl.isNegative() || ttl.isZero() ? Duration.ofSeconds(1) : ttl;
        this.keyPrefix = normalizePrefix(keyPrefix);
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
        String key = key(sqlHash);
        try {
            String payload = redis.opsForValue().get(key);
            if (payload == null || payload.isBlank()) {
                missCount.incrementAndGet();
                return Optional.empty();
            }
            BiQueryResult result = objectMapper.readValue(payload, BiQueryResult.class);
            hitCount.incrementAndGet();
            return Optional.of(result);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            missCount.incrementAndGet();
            // Corrupt cache payloads are treated as misses and removed so the next query can repopulate them.
            if (Boolean.TRUE.equals(redis.delete(key))) {
                evictionCount.incrementAndGet();
            }
            return Optional.empty();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            missCount.incrementAndGet();
            return Optional.empty();
        }
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
        Duration effectiveTtl = ttl == null || ttl.isNegative() || ttl.isZero() ? this.ttl : ttl;
        try {
            redis.opsForValue().set(key(sqlHash), objectMapper.writeValueAsString(result), effectiveTtl);
            putCount.incrementAndGet();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("BI query result is not JSON serializable: " + sqlHash, e);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            // Cache availability must not block BI query execution.
        }
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
        try {
            Boolean deleted = redis.delete(key(sqlHash));
            boolean removed = Boolean.TRUE.equals(deleted);
            if (removed) {
                evictionCount.incrementAndGet();
            }
            return removed;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * evictDataset 删除或清理 infrastructure.bi 场景的业务数据。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 evict dataset 计算得到的数量、金额或指标值。
     */
    @Override
    public int evictDataset(String datasetKey) {
        if (datasetKey == null || datasetKey.isBlank()) {
            return 0;
        }
        int deleted = 0;
        // Dataset eviction scans cache values because sqlHash keys do not encode dataset ownership.
        for (String redisKey : keys()) {
            try {
                String payload = redis.opsForValue().get(redisKey);
                if (payload == null || payload.isBlank()) {
                    continue;
                }
                BiQueryResult result = objectMapper.readValue(payload, BiQueryResult.class);
                if (datasetKey.equals(result.datasetKey()) && Boolean.TRUE.equals(redis.delete(redisKey))) {
                    deleted++;
                    evictionCount.incrementAndGet();
                }
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (JsonProcessingException e) {
                if (Boolean.TRUE.equals(redis.delete(redisKey))) {
                    evictionCount.incrementAndGet();
                }
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException ignored) {
                // Cache invalidation visibility should not break BI administration calls.
            }
        }
        return deleted;
    }

    /**
     * clear 删除或清理 infrastructure.bi 场景的业务数据。
     * @return 返回 clear 计算得到的数量、金额或指标值。
     */
    @Override
    public int clear() {
        Set<String> keys = keys();
        if (keys.isEmpty()) {
            return 0;
        }
        try {
            Long deleted = redis.delete((Collection<String>) keys);
            int count = deleted == null ? 0 : deleted.intValue();
            evictionCount.addAndGet(count);
            return count;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * stats 查询 infrastructure.bi 场景的业务数据。
     * @return 返回 stats 流程生成的业务结果。
     */
    @Override
    public BiQueryCacheStats stats() {
        return new BiQueryCacheStats(
                "redis",
                enabled,
                /**
                 * 执行 keys 流程，围绕 keys 完成校验、计算或结果组装。
                 *
                 * @return 返回 keys 流程生成的业务结果。
                 */
                enabled ? keys().size() : 0,
                -1,
                ttl.toSeconds(),
                hitCount.get(),
                missCount.get(),
                putCount.get(),
                evictionCount.get());
    }

    /**
     * 执行 key 流程，围绕 key 完成校验、计算或结果组装。
     *
     * @param sqlHash sql hash 参数，用于 key 流程中的校验、计算或对象转换。
     * @return 返回 key 生成的文本或业务键。
     */
    private String key(String sqlHash) {
        return keyPrefix + sqlHash.trim();
    }

    /**
     * 执行 keys 流程，围绕 keys 完成校验、计算或结果组装。
     *
     * @return 返回 keys 汇总后的集合、分页或映射视图。
     */
    private Set<String> keys() {
        try {
            // This provider is intended for bounded BI query caches; callers can switch to memory/provider-specific stats if KEYS is too expensive.
            Set<String> keys = redis.keys(keyPrefix + "*");
            return keys == null ? Set.of() : keys;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            return Set.of();
        }
    }

    /**
     * 规范化输入值。
     *
     * @param keyPrefix key prefix 参数，用于 normalizePrefix 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizePrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return "canvas:bi:query-cache:";
        }
        return keyPrefix;
    }
}
