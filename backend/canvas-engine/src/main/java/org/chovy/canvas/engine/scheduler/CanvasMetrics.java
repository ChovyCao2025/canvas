package org.chovy.canvas.engine.scheduler;

import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

    private final MeterRegistry registry;
    private final ConcurrentMap<String, AtomicLong> executionRequestBacklog = new ConcurrentHashMap<>();

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
            Gauge.builder("canvas.execution.request.backlog", value, AtomicLong::get)
                    .tag("status", key)
                    .register(registry);
            return value;
        });
        gauge.set(Math.max(0L, count));
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
}
