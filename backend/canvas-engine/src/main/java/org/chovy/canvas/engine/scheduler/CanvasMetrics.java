package org.chovy.canvas.engine.scheduler;

import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * 画布执行引擎核心监控指标（设计文档第 14.1 节）。
 *
 * 指标分类：
 *   - 执行层：execution.total / execution.duration / execution.paused.count
 *   - 节点层：node.execution.total / node.execution.duration / node.retry.total
 *   - DLQ：dlq.size
 */
@Component
@RequiredArgsConstructor
public class CanvasMetrics {

    /** Micrometer 指标注册中心。 */
    private final MeterRegistry registry;
    /** 执行请求积压 Gauge 的状态值缓存。 */
    private final ConcurrentMap<String, AtomicLong> executionRequestBacklog = new ConcurrentHashMap<>();
    /** 容量与资源水位 Gauge 的值缓存。 */
    private final ConcurrentMap<String, AtomicLong> capacityGauges = new ConcurrentHashMap<>();

    /**
     * 写入或记录 record Execution 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param status status 状态值或状态筛选条件
     * @param durationMs durationMs 时间、过期时间或持续时长参数
     */
// ── 执行层指标 ────────────────────────────────────────────────

    /** 记录一次画布执行完成（含 canvasId、status 标签） */
    public void recordExecution(String canvasId, String status, long durationMs) {
        Counter.builder("canvas.execution.total")
                .tag("canvasId", canvasId)
                .tag("status", status)
                .register(registry)
                .increment();

        Timer.builder("canvas.execution.duration")
                .tag("canvasId", canvasId)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /** 当前挂起中的执行数（多阶段 WAITING） */
    public void setPausedCount(String canvasId, double count) {
        Gauge.builder("canvas.execution.paused.count", () -> count)
                .tag("canvasId", canvasId)
                .register(registry);
    }

    /**
     * 写入或记录 record Node Execution 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nodeType nodeType 节点相关对象、标识或配置
     * @param status status 状态值或状态筛选条件
     * @param durationMs durationMs 时间、过期时间或持续时长参数
     */
// ── 节点层指标 ────────────────────────────────────────────────

    /** 记录一次节点执行 */
    public void recordNodeExecution(String nodeType, String status, long durationMs) {
        Counter.builder("canvas.node.execution.total")
                .tag("nodeType", nodeType)
                .tag("status", status)
                .register(registry)
                .increment();

        Timer.builder("canvas.node.execution.duration")
                .tag("nodeType", nodeType)
                .publishPercentiles(0.5, 0.99)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /** 记录节点重试次数 */
    public void recordNodeRetry(String nodeType) {
        Counter.builder("canvas.node.retry.total")
                .tag("nodeType", nodeType)
                .register(registry)
                .increment();
    }

    /** DLQ 消息写入 */
    public void recordDlq(String nodeType) {
        Counter.builder("canvas.dlq.size")
                .tag("nodeType", nodeType)
                .register(registry)
                .increment();
    }

    /**
     * 写入或记录 record Dedup 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     */
// ── 触发层指标 ────────────────────────────────────────────────

    /** 幂等拦截计数 */
    public void recordDedup(String canvasId) {
        Counter.builder("canvas.trigger.deduplicated")
                .tag("canvasId", canvasId)
                .register(registry)
                .increment();
    }

    /** 配额拦截计数 */
    public void recordQuotaReject(String canvasId, String code) {
        Counter.builder("canvas.trigger.quota.rejected")
                .tag("canvasId", canvasId)
                .tag("code", code)
                .register(registry)
                .increment();
    }

    /** Disruptor 投递成功计数 */
    public void recordDisruptorPublished(String triggerType) {
        Counter.builder("canvas.disruptor.published.total")
                .tag("triggerType", triggerType)
                .register(registry)
                .increment();
    }

    /** Disruptor Ring Buffer 满导致的溢出计数 */
    public void recordDisruptorOverflow(String triggerType) {
        Counter.builder("canvas.disruptor.overflow.total")
                .tag("triggerType", triggerType)
                .register(registry)
                .increment();
    }

    /** Disruptor 待消费积压水位 Gauge。 */
    public void registerDisruptorBacklogGauge(LongSupplier backlogSupplier) {
        Gauge.builder("canvas.disruptor.backlog", backlogSupplier,
                        supplier -> Math.max(0L, supplier.getAsLong()))
                .register(registry);
    }

    /** 执行请求状态迁移计数（MQ/行为等异步触发的可靠投递层） */
    public void recordExecutionRequestTransition(String status, String triggerType) {
        Counter.builder("canvas.execution.request.transition.total")
                .tag("status", status != null ? status : "UNKNOWN")
                .tag("triggerType", triggerType != null ? triggerType : "UNKNOWN")
                .register(registry)
                .increment();
    }

    /** 执行请求积压水位，按状态分组。 */
    public void setExecutionRequestBacklog(String status, long count) {
        String normalizedStatus = status != null ? status : "UNKNOWN";
        AtomicLong gauge = executionRequestBacklog.computeIfAbsent(normalizedStatus, key -> {
            AtomicLong value = new AtomicLong();
            // 每个状态只注册一次 Gauge，后续只更新 AtomicLong，避免重复注册同名指标。
            Gauge.builder("canvas.execution.request.backlog", value, AtomicLong::get)
                    .tag("status", key)
                    .register(registry);
            return value;
        });
        // backlog 不能为负，异常统计值在指标入口归零。
        gauge.set(Math.max(0L, count));
    }

    /** Redis 执行注册表准入结果计数，按 lane 和原因分组。 */
    public void recordExecutionRegistryAdmission(String lane, String reason) {
        Counter.builder("canvas.execution.registry.admission.total")
                .tag("lane", normalizeTag(lane))
                .tag("reason", normalizeTag(reason))
                .register(registry)
                .increment();
    }

    /** Redis 执行注册表准入延迟。 */
    public void recordExecutionRegistryLatency(long latencyMs) {
        Timer.builder("canvas.execution.registry.latency")
                .publishPercentiles(0.95, 0.99)
                .register(registry)
                .record(Math.max(0L, latencyMs), TimeUnit.MILLISECONDS);
    }

    /** 执行 lane 当前活跃执行数，供 3000 hardening protected-lane gate 读取。 */
    public void setExecutionLaneActive(String lane, long count) {
        setTaggedGauge("canvas.execution.lane.active", "lane", lane, count);
    }

    /** Trace buffer backlog，按 buffer 分组。 */
    public void setTraceBufferBacklog(String buffer, long count) {
        setTaggedGauge("canvas.trace.buffer.backlog", "buffer", buffer, count);
    }

    /** RocketMQ 触发消息被消费端拒绝的计数。 */
    public void recordMqTriggerRejected(String reason, String tag) {
        Counter.builder("canvas.mq.trigger.rejected.total")
                .tag("reason", reason != null ? reason : "UNKNOWN")
                .tag("tag", tag != null ? tag : "UNKNOWN")
                .register(registry)
                .increment();
    }

    /** MQ 路由表脏数据被消费端跳过的计数。 */
    public void recordMqRouteRejected(String reason, String tag) {
        Counter.builder("canvas.mq.route.rejected.total")
                .tag("reason", reason != null ? reason : "UNKNOWN")
                .tag("tag", tag != null ? tag : "UNKNOWN")
                .register(registry)
                .increment();
    }

    /** 执行请求派发成功计数。 */
    public void recordExecutionRequestDispatched(String canvasId) {
        Counter.builder("canvas.execution.request.dispatched.total")
                .tag("canvasId", canvasId != null ? canvasId : "UNKNOWN")
                .register(registry)
                .increment();
    }

    /** 执行请求因单画布批内限额被跳过的计数。 */
    public void recordExecutionRequestSkipped(String canvasId, String reason) {
        Counter.builder("canvas.execution.request.skipped.total")
                .tag("canvasId", canvasId != null ? canvasId : "UNKNOWN")
                .tag("reason", reason != null ? reason : "UNKNOWN")
                .register(registry)
                .increment();
    }

    /** 执行请求派发阶段失败计数。 */
    public void recordExecutionRequestDispatchFailure(String canvasId) {
        Counter.builder("canvas.execution.request.dispatch.failure.total")
                .tag("canvasId", canvasId != null ? canvasId : "UNKNOWN")
                .register(registry)
                .increment();
    }

    /** 连接池、线程池等资源饱和度百分比。 */
    public void setPoolSaturationPercent(String resource, long percent) {
        setTaggedGauge("canvas.capacity.pool.saturation.percent", "resource", resource, percent);
    }

    /** 执行 lane 的并发饱和度百分比。 */
    public void setLaneSaturationPercent(String lane, long percent) {
        setTaggedGauge("canvas.capacity.lane.saturation.percent", "lane", lane, percent);
    }

    /** 执行 lane 当前活跃执行数。 */
    public void setExecutionActiveByLane(String lane, long count) {
        setTaggedGauge("canvas.execution.active", "lane", lane, count);
    }

    /** 队列或 broker backlog 深度。 */
    public void setQueueDepth(String queue, long depth) {
        setTaggedGauge("canvas.capacity.queue.depth", "queue", queue, depth);
    }

    /** 重试积压水位，覆盖执行请求、节点重试、投递重试等队列。 */
    public void setRetryBacklog(String queue, long count) {
        setTaggedGauge("canvas.retry.backlog", "queue", queue, count);
    }

    /** Redis used_memory 字节数。 */
    public void setRedisMemoryBytes(long bytes) {
        setTaggedGauge("canvas.capacity.redis.memory.bytes", "resource", "redis", bytes);
    }

    /** DLQ 待处理积压水位。 */
    public void setDlqBacklog(String queue, long count) {
        setTaggedGauge("canvas.capacity.dlq.backlog", "queue", queue, count);
    }

    /** 投递 outbox 按状态聚合的水位。 */
    public void setDeliveryOutboxStatusCount(String status, long count) {
        setTaggedGauge("canvas.delivery.outbox.status.count", "status", status, count);
    }

    /** Redis 路由注册、刷新、恢复相关操作延迟。 */
    public void recordRedisRegistryLatency(String operation, long durationMs) {
        Timer.builder("canvas.redis.registry.latency")
                .tag("operation", normalizeTag(operation))
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(Math.max(0L, durationMs), TimeUnit.MILLISECONDS);
    }

    /** MySQL 连接池压力百分比，独立命名便于生产看板和告警直接引用。 */
    public void setMysqlPoolPressurePercent(String pool, long percent) {
        setTaggedGauge("canvas.mysql.pool.pressure.percent", "pool", pool, percent);
    }

    /** Trace 写入缓冲区待刷盘数量。 */
    public void setTraceBufferPending(long count) {
        setGlobalGauge("canvas.trace.buffer.pending", count);
    }

    /** Trace 写入被丢弃或降级的计数。 */
    public void recordTraceDropped(String reason) {
        Counter.builder("canvas.trace.dropped.total")
                .tag("reason", reason != null ? reason : "UNKNOWN")
                .register(registry)
                .increment();
    }

    /** 下游系统调用延迟。 */
    public void recordDownstreamLatency(String system, String operation, long durationMs) {
        Timer.builder("canvas.downstream.latency")
                .tag("system", normalizeTag(system))
                .tag("operation", normalizeTag(operation))
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(Math.max(0L, durationMs), TimeUnit.MILLISECONDS);
    }

    public long currentTaggedGaugeValue(String metricName, String tagName, String tagValue) {
        AtomicLong gauge = capacityGauges.get(metricName + ":" + tagName + ":" + normalizeTag(tagValue));
        return gauge == null ? 0L : Math.max(0L, gauge.get());
    }

    private void setTaggedGauge(String metricName, String tagName, String tagValue, long value) {
        String normalizedTagValue = normalizeTag(tagValue);
        String key = metricName + ":" + tagName + ":" + normalizedTagValue;
        AtomicLong gauge = capacityGauges.computeIfAbsent(key, ignored -> {
            AtomicLong holder = new AtomicLong();
            Gauge.builder(metricName, holder, AtomicLong::get)
                    .tag(tagName, normalizedTagValue)
                    .register(registry);
            return holder;
        });
        gauge.set(Math.max(0L, value));
    }

    private void setGlobalGauge(String metricName, long value) {
        AtomicLong gauge = capacityGauges.computeIfAbsent(metricName, ignored -> {
            AtomicLong holder = new AtomicLong();
            Gauge.builder(metricName, holder, AtomicLong::get)
                    .register(registry);
            return holder;
        });
        gauge.set(Math.max(0L, value));
    }

    private String normalizeTag(String value) {
        return value != null && !value.isBlank() ? value : "UNKNOWN";
    }
}
