package org.chovy.canvas.infrastructure.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
/**
 * RedisDistributedRateLimiter 封装本模块的核心职责、输入输出结构和协作边界。
 */
public class RedisDistributedRateLimiter implements DistributedRateLimiter {

    private final StringRedisTemplate redis;
    private final RedisKeyUtil keys;
    private final Clock clock;

    @Autowired
    /**
     * 初始化 RedisDistributedRateLimiter 实例。
     *
     * @param redis redis 参数，用于 RedisDistributedRateLimiter 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 RedisDistributedRateLimiter 流程中的校验、计算或对象转换。
     */
    public RedisDistributedRateLimiter(StringRedisTemplate redis, RedisKeyUtil keys) {
        this(redis, keys, Clock.systemUTC());
    }

    /**
     * 初始化 RedisDistributedRateLimiter 实例。
     *
     * @param redis redis 参数，用于 RedisDistributedRateLimiter 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 RedisDistributedRateLimiter 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public RedisDistributedRateLimiter(StringRedisTemplate redis, RedisKeyUtil keys, Clock clock) {
        this.redis = redis;
        this.keys = keys;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param scope scope 参数，用于 tryAcquire 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param cost cost 参数，用于 tryAcquire 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param window window 参数，用于 tryAcquire 流程中的校验、计算或对象转换。
     * @return 返回 try acquire 的布尔判断结果。
     */
    public boolean tryAcquire(String scope, String operator, int cost, int limit, Duration window) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (limit <= 0) {
            return true;
        }
        int normalizedCost = Math.max(1, cost);
        Duration normalizedWindow = window == null || window.isNegative() || window.isZero()
                ? Duration.ofMinutes(1)
                : window;
        long windowIndex = clock.millis() / Math.max(1L, normalizedWindow.toMillis());
        String key = keys.executionRequestReplayRateLimit(scope, operator, windowIndex);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        Long count = redis.opsForValue().increment(key, normalizedCost);
        if (count == null) {
            return false;
        }
        if (count <= normalizedCost) {
            redis.expire(key, normalizedWindow.plusSeconds(1));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return count <= limit;
    }
}
