package org.chovy.canvas.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime operations metrics used by alert rules and operational dashboards.
 */
@Component
@RequiredArgsConstructor
public class CanvasRuntimeMetrics {

    private final MeterRegistry registry;
    private final ConcurrentMap<String, AtomicLong> longGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicReference<Double>> doubleGauges = new ConcurrentHashMap<>();

    /**
     * recordExecutionFailure 处理 config 场景的业务逻辑。
     * @param triggerType 类型标识，用于选择对应处理分支。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     */
    public void recordExecutionFailure(String triggerType, String reason) {
        Counter.builder("canvas.runtime.execution.failures")
                .tags("triggerType", normalize(triggerType), "reason", normalize(reason))
                .register(registry)
                .increment();
    }

    /**
     * setDlqBacklog 处理 config 场景的业务逻辑。
     * @param count 分页、数量或序号参数，用于控制处理规模。
     */
    public void setDlqBacklog(long count) {
        longGauge("canvas.runtime.dlq.backlog", Tags.empty()).set(Math.max(0L, count));
    }

    /**
     * recordRouteRebuildFailure 处理 config 场景的业务逻辑。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     */
    public void recordRouteRebuildFailure(String reason) {
        Counter.builder("canvas.runtime.route.rebuild.failures")
                .tag("reason", normalize(reason))
                .register(registry)
                .increment();
    }

    /**
     * recordCacheInvalidationFailure 处理 config 场景的业务逻辑。
     * @param cacheName 名称文本，用于展示或唯一性校验。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     */
    public void recordCacheInvalidationFailure(String cacheName, String reason) {
        Counter.builder("canvas.runtime.cache.invalidation.failures")
                .tags("cache", normalize(cacheName), "reason", normalize(reason))
                .register(registry)
                .increment();
    }

    /**
     * setRedisAvailable 处理 config 场景的业务逻辑。
     * @param available available 参数，用于 setRedisAvailable 流程中的校验、计算或对象转换。
     */
    public void setRedisAvailable(boolean available) {
        longGauge("canvas.runtime.redis.available", Tags.empty()).set(available ? 1L : 0L);
    }

    /**
     * setMqAvailable 处理 config 场景的业务逻辑。
     * @param available available 参数，用于 setMqAvailable 流程中的校验、计算或对象转换。
     */
    public void setMqAvailable(boolean available) {
        longGauge("canvas.runtime.mq.available", Tags.empty()).set(available ? 1L : 0L);
    }

    /**
     * setLanePressure 处理 config 场景的业务逻辑。
     * @param lane lane 参数，用于 setLanePressure 流程中的校验、计算或对象转换。
     * @param active active 参数，用于 setLanePressure 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     */
    public void setLanePressure(String lane, long active, long limit) {
        double pressure = limit <= 0 ? 1.0d : Math.max(0d, active) / (double) limit;
        doubleGauge("canvas.runtime.lane.pressure", Tags.of("lane", normalize(lane)))
                .set(Math.min(1.0d, pressure));
    }

    /**
     * setDisruptorPressure 处理 config 场景的业务逻辑。
     * @param backlog backlog 参数，用于 setDisruptorPressure 流程中的校验、计算或对象转换。
     * @param ringBufferSize ring buffer size 参数，用于 setDisruptorPressure 流程中的校验、计算或对象转换。
     */
    public void setDisruptorPressure(long backlog, long ringBufferSize) {
        double pressure = ringBufferSize <= 0 ? 1.0d : Math.max(0d, backlog) / (double) ringBufferSize;
        doubleGauge("canvas.runtime.disruptor.pressure", Tags.empty())
                .set(Math.min(1.0d, pressure));
    }

    /**
     * recordShutdownDrainTimeout 处理 config 场景的业务逻辑。
     * @param component component 参数，用于 recordShutdownDrainTimeout 流程中的校验、计算或对象转换。
     */
    public void recordShutdownDrainTimeout(String component) {
        Counter.builder("canvas.runtime.shutdown.drain.timeout")
                .tag("component", normalize(component))
                .register(registry)
                .increment();
    }

    /**
     * recordMarketingIntegrationProbeResult 处理 config 场景的业务逻辑。
     * @param providerFamily provider family 参数，用于 recordMarketingIntegrationProbeResult 流程中的校验、计算或对象转换。
     * @param environment environment 参数，用于 recordMarketingIntegrationProbeResult 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param httpStatusCode 业务编码，用于匹配对应类型或状态。
     * @param latencyMs latency ms 参数，用于 recordMarketingIntegrationProbeResult 流程中的校验、计算或对象转换。
     * @param errorType 类型标识，用于选择对应处理分支。
     */
    public void recordMarketingIntegrationProbeResult(String providerFamily,
                                                      String environment,
                                                      String status,
                                                      Integer httpStatusCode,
                                                      Long latencyMs,
                                                      String errorType) {
        Tags tags = Tags.of(
                /**
                 * 规范化输入值。
                 *
                 * @return 返回解析、归一化或安全处理后的值。
                 */
                "provider_family", normalize(providerFamily),
                /**
                 * 规范化输入值。
                 *
                 * @return 返回解析、归一化或安全处理后的值。
                 */
                "environment", normalize(environment),
                /**
                 * 规范化输入值。
                 *
                 * @return 返回解析、归一化或安全处理后的值。
                 */
                "status", normalize(status),
                "http.response.status_code", httpStatusCode == null ? "unknown" : String.valueOf(httpStatusCode),
                "error.type", normalizeErrorType(errorType));
        Counter.builder("canvas.marketing.integration.probe.result.total")
                .tags(tags)
                .register(registry)
                .increment();
        Timer.builder("canvas.marketing.integration.probe.latency")
                .tags(tags)
                .register(registry)
                .record(Math.max(0L, latencyMs == null ? 0L : latencyMs), TimeUnit.MILLISECONDS);
    }

    /**
     * 记录一次风控在线决策请求和耗时。
     *
     * @param scene 风控场景键
     * @param action 最终决策动作
     * @param latencyMs 决策耗时毫秒数
     */
    public void recordRiskDecision(String scene, String action, long latencyMs) {
        Tags tags = Tags.of("scene", normalize(scene), "action", normalize(action));
        Counter.builder("risk_decision_requests_total")
                .tags(tags)
                .register(registry)
                .increment();
        Timer.builder("risk_decision_latency_ms")
                .tags(tags)
                .register(registry)
                .record(Math.max(0L, latencyMs), TimeUnit.MILLISECONDS);
    }

    /**
     * 记录风控决策失败次数。
     *
     * @param scene 风控场景键
     * @param reason 失败原因
     */
    public void recordRiskDecisionFailure(String scene, String reason) {
        Counter.builder("risk_decision_failures_total")
                .tags("scene", normalize(scene), "reason", normalize(reason))
                .register(registry)
                .increment();
    }

    /**
     * 记录风控规则命中次数。
     *
     * @param scene 风控场景键
     * @param group 规则组键
     * @param rule 规则键
     * @param action 命中动作
     */
    public void recordRiskRuleHit(String scene, String group, String rule, String action) {
        Counter.builder("risk_rule_hits_total")
                .tags("scene", normalize(scene),
                        /**
                         * 规范化输入值。
                         *
                         * @return 返回解析、归一化或安全处理后的值。
                         */
                        "group", normalize(group),
                        /**
                         * 规范化输入值。
                         *
                         * @return 返回解析、归一化或安全处理后的值。
                         */
                        "rule", normalize(rule),
                        /**
                         * 规范化输入值。
                         *
                         * @return 返回解析、归一化或安全处理后的值。
                         */
                        "action", normalize(action))
                .register(registry)
                .increment();
    }

    /**
     * 记录风控特征缺失次数。
     *
     * @param scene 风控场景键
     * @param feature 缺失特征键
     */
    public void recordRiskFeatureMissing(String scene, String feature) {
        Counter.builder("risk_feature_missing_total")
                .tags("scene", normalize(scene), "feature", normalize(feature))
                .register(registry)
                .increment();
    }

    /**
     * 记录风控策略激活、暂停或切换状态事件。
     *
     * @param scene 风控场景键
     * @param strategy 策略键
     * @param status 策略状态
     */
    public void recordRiskStrategyActivation(String scene, String strategy, String status) {
        Counter.builder("risk_strategy_activations_total")
                .tags("scene", normalize(scene),
                        /**
                         * 规范化输入值。
                         *
                         * @return 返回解析、归一化或安全处理后的值。
                         */
                        "strategy", normalize(strategy),
                        /**
                         * 规范化输入值。
                         *
                         * @return 返回解析、归一化或安全处理后的值。
                         */
                        "status", normalize(status))
                .register(registry)
                .increment();
    }

    /**
     * 记录风控模型网关失败次数。
     *
     * @param scene 风控场景键
     * @param model 模型键
     * @param reason 失败原因
     */
    public void recordRiskModelGatewayFailure(String scene, String model, String reason) {
        Counter.builder("risk_model_gateway_failures_total")
                .tags("scene", normalize(scene),
                        /**
                         * 规范化输入值。
                         *
                         * @return 返回解析、归一化或安全处理后的值。
                         */
                        "model", normalize(model),
                        /**
                         * 规范化输入值。
                         *
                         * @return 返回解析、归一化或安全处理后的值。
                         */
                        "reason", normalize(reason))
                .register(registry)
                .increment();
    }

    /**
     * 记录风控名单导入行数。
     *
     * @param listKey 名单键
     * @param status 导入状态
     * @param rows 行数
     */
    public void recordRiskListImport(String listKey, String status, long rows) {
        Counter.builder("risk_list_import_rows_total")
                .tags("list", normalize(listKey), "status", normalize(status))
                .register(registry)
                .increment(Math.max(0L, rows));
    }

    /**
     * 记录风控仿真运行次数、样本数和耗时。
     *
     * @param scene 风控场景键
     * @param status 仿真状态
     * @param sampleCount 样本数
     * @param latencyMs 仿真耗时毫秒数
     */
    public void recordRiskSimulationRun(String scene, String status, int sampleCount, long latencyMs) {
        Tags tags = Tags.of("scene", normalize(scene), "status", normalize(status));
        Counter.builder("risk_simulation_runs_total")
                .tags(tags)
                .register(registry)
                .increment();
        Counter.builder("risk_simulation_samples_total")
                .tags(tags)
                .register(registry)
                .increment(Math.max(0, sampleCount));
        Timer.builder("risk_simulation_latency_ms")
                .tags(tags)
                .register(registry)
                .record(Math.max(0L, latencyMs), TimeUnit.MILLISECONDS);
    }

    /**
     * 获取或注册长整型 Gauge，避免相同指标标签重复注册。
     *
     * @param name 指标名
     * @param tags 指标标签
     * @return Gauge 绑定的可变长整型值
     */
    private AtomicLong longGauge(String name, Tags tags) {
        String key = gaugeKey(name, tags);
        return longGauges.computeIfAbsent(key, ignored -> {
            AtomicLong value = new AtomicLong();
            Gauge.builder(name, value, AtomicLong::get)
                    .tags(tags)
                    .register(registry);
            return value;
        });
    }

    /**
     * 获取或注册浮点型 Gauge，避免相同指标标签重复注册。
     *
     * @param name 指标名
     * @param tags 指标标签
     * @return Gauge 绑定的可变浮点值
     */
    private AtomicReference<Double> doubleGauge(String name, Tags tags) {
        String key = gaugeKey(name, tags);
        return doubleGauges.computeIfAbsent(key, ignored -> {
            AtomicReference<Double> value = new AtomicReference<>(0d);
            Gauge.builder(name, value, ref -> ref.get() == null ? 0d : ref.get())
                    .tags(tags)
                    .register(registry);
            return value;
        });
    }

    /**
     * 根据指标名和标签构造本地 Gauge 注册键。
     *
     * @param name 指标名
     * @param tags 指标标签
     * @return 本地去重键
     */
    private String gaugeKey(String name, Iterable<Tag> tags) {
        StringJoiner joiner = new StringJoiner("|", name + "|", "");
        for (Tag tag : tags) {
            joiner.add(tag.getKey() + "=" + tag.getValue());
        }
        return joiner.toString();
    }

    /**
     * 将指标标签值规范化为小写非空字符串。
     *
     * @param value 原始标签值
     * @return 规范化后的标签值
     */
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化错误类型标签，空值使用 none。
     *
     * @param value 原始错误类型
     * @return 规范化后的错误类型
     */
    private String normalizeErrorType(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        return normalize(value);
    }
}
