package org.chovy.canvas.engine.scheduler;

import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

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
}
