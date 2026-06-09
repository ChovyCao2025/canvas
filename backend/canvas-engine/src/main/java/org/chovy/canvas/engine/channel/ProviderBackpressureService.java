package org.chovy.canvas.engine.channel;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
/**
 * ProviderBackpressureService 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class ProviderBackpressureService {

    private final CounterStore counters;
    private final LimitRepository limits;
    private final Clock clock;

    @Autowired
    /**
     * 初始化 ProviderBackpressureService 实例。
     *
     * @param counters counters 参数，用于 ProviderBackpressureService 流程中的校验、计算或对象转换。
     * @param limits limits 参数，用于 ProviderBackpressureService 流程中的校验、计算或对象转换。
     */
    public ProviderBackpressureService(CounterStore counters, LimitRepository limits) {
        this(counters, limits, Clock.systemUTC());
    }

    /**
     * 初始化 ProviderBackpressureService 实例。
     *
     * @param counters counters 参数，用于 ProviderBackpressureService 流程中的校验、计算或对象转换。
     * @param limits limits 参数，用于 ProviderBackpressureService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public ProviderBackpressureService(CounterStore counters, LimitRepository limits, Clock clock) {
        this.counters = counters;
        this.limits = limits;
        this.clock = clock;
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param channel channel 参数，用于 decide 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 decide 流程中的校验、计算或对象转换。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param sandboxMode sandbox mode 参数，用于 decide 流程中的校验、计算或对象转换。
     * @return 返回 decide 流程生成的业务结果。
     */
    public Decision decide(Long tenantId, String channel, String provider, String operation, boolean sandboxMode) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sandboxMode) {
            return Decision.allowed();
        }
        LimitKey key = new LimitKey(
                ChannelConnectorRegistry.tenant(tenantId),
                ChannelConnectorRegistry.normalize(channel),
                ChannelConnectorRegistry.normalizeProvider(provider),
                ChannelConnectorRegistry.normalize(operation == null ? "SEND" : operation));
        ProviderLimit limit = limits.find(key);
        ProviderLimit effectiveLimit = limit == null ? ProviderLimit.defaults() : limit;
        try {
            long secondCount = counters.increment(key.toKey("s", clock.instant().getEpochSecond()));
            if (effectiveLimit.perSecondLimit() != null
                    && effectiveLimit.perSecondLimit() > 0
                    && secondCount > effectiveLimit.perSecondLimit()) {
                return new Decision("THROTTLED_RETRY", "per-second provider limit exceeded", 1L);
            }
            if (effectiveLimit.dailyLimit() != null && effectiveLimit.dailyLimit() > 0) {
                LocalDate day = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
                long dailyCount = counters.increment(key.toKey("d", day));
                if (dailyCount > effectiveLimit.dailyLimit()) {
                    return new Decision("THROTTLED_SKIP", "daily provider limit exceeded", null);
                }
            }
            return Decision.allowed();
        } catch (RuntimeException ex) {
            if (effectiveLimit.failClosed()) {
                return new Decision("REGISTRY_UNAVAILABLE", ex.getMessage(), null);
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return Decision.allowed();
        }
    }

    @FunctionalInterface
    /**
     * CounterStore 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public interface CounterStore {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param key 业务键，用于在同一租户下定位资源。
         * @return 返回 increment 计算得到的数量、金额或指标值。
         */
        long increment(String key);
    }

    /**
     * LimitRepository 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public interface LimitRepository {
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param key 业务键，用于在同一租户下定位资源。
         * @return 返回符合条件的数据列表或视图。
         */
        ProviderLimit find(LimitKey key);
    }

    /**
     * LimitKey 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record LimitKey(Long tenantId, String channel, String provider, String operation) {
        /**
         * 组装输出结构或完成对象转换。
         *
         * @param bucket bucket 参数，用于 toKey 流程中的校验、计算或对象转换。
         * @param value 待处理值，用于规则计算或转换。
         * @return 返回组装或转换后的结果对象。
         */
        String toKey(String bucket, Object value) {
            return "channel:provider:" + tenantId + ":" + channel + ":" + provider + ":" + operation + ":" + bucket + ":" + value;
        }
    }

    /**
     * ProviderLimit 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record ProviderLimit(Integer perSecondLimit, Long dailyLimit, boolean failClosed) {
        /**
         * 生成默认值或兜底结果，保证调用链稳定。
         *
         * @return 返回 defaults 流程生成的业务结果。
         */
        static ProviderLimit defaults() {
            return new ProviderLimit(100, null, true);
        }
    }

    /**
     * Decision 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record Decision(String status, String reason, Long retryAfterSeconds) {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 allowed 流程生成的业务结果。
         */
        static Decision allowed() {
            return new Decision("ALLOWED", null, null);
        }
    }

    /**
     * InMemoryCounterStore 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public static class InMemoryCounterStore implements CounterStore {

        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

        @Override
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param key 业务键，用于在同一租户下定位资源。
         * @return 返回 increment 计算得到的数量、金额或指标值。
         */
        public long increment(String key) {
            return counters.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
        }
    }
}
