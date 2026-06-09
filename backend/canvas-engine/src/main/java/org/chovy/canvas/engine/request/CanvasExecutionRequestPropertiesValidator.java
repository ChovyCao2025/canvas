package org.chovy.canvas.engine.request;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 画布执行请求 Properties Validator 执行请求治理组件。
 *
 * <p>负责画布执行请求的排队、派发、限流、重放或积压度量，削峰高并发触发流量。
 * <p>该层位于触发入口和 DAG 执行之间，核心目标是保护执行引擎稳定性。
 */
@Component
public class CanvasExecutionRequestPropertiesValidator {

    /** 每轮执行请求派发批量。 */
    private final int dispatchBatchSize;
    /** 执行请求派发定时任务间隔毫秒数。 */
    private final long dispatchFixedDelayMs;
    /** 执行请求初始重试延迟毫秒数。 */
    private final long retryDelayMs;
    /** 执行请求最大尝试次数。 */
    private final int maxAttempts;
    /** RUNNING 请求陈旧判定秒数。 */
    private final long runningStaleSeconds;
    /** 指数退避重试最大延迟毫秒数。 */
    private final long maxRetryDelayMs;
    /** RUNNING 请求心跳刷新间隔毫秒数。 */
    private final long heartbeatIntervalMs;
    /** 单画布单轮派发上限，0 表示不限制。 */
    private final int perCanvasBatchLimit;
    /** 单条重放每分钟限额，0 表示不限制。 */
    private final int replaySinglePerMinute;
    /** 批量重放请求每分钟限额，0 表示不限制。 */
    private final int replayBatchRequestsPerMinute;

    /**
     * 创建 CanvasExecutionRequestPropertiesValidator 实例并注入 engine.request 场景依赖。
     * @param dispatchBatchSize dispatch batch size 参数，用于 CanvasExecutionRequestPropertiesValidator 流程中的校验、计算或对象转换。
     * @param dispatchFixedDelayMs dispatch fixed delay ms 参数，用于 CanvasExecutionRequestPropertiesValidator 流程中的校验、计算或对象转换。
     * @param retryDelayMs retry delay ms 参数，用于 CanvasExecutionRequestPropertiesValidator 流程中的校验、计算或对象转换。
     * @param maxAttempts max attempts 参数，用于 CanvasExecutionRequestPropertiesValidator 流程中的校验、计算或对象转换。
     * @param runningStaleSeconds running stale seconds 参数，用于 CanvasExecutionRequestPropertiesValidator 流程中的校验、计算或对象转换。
     * @param maxRetryDelayMs max retry delay ms 参数，用于 CanvasExecutionRequestPropertiesValidator 流程中的校验、计算或对象转换。
     * @param heartbeatIntervalMs heartbeat interval ms 参数，用于 CanvasExecutionRequestPropertiesValidator 流程中的校验、计算或对象转换。
     * @param perCanvasBatchLimit per canvas batch limit 参数，用于 CanvasExecutionRequestPropertiesValidator 流程中的校验、计算或对象转换。
     * @param replaySinglePerMinute replay single per minute 参数，用于 CanvasExecutionRequestPropertiesValidator 流程中的校验、计算或对象转换。
     * @param replayBatchRequestsPerMinute replay batch requests per minute 参数，用于 CanvasExecutionRequestPropertiesValidator 流程中的校验、计算或对象转换。
     */
    public CanvasExecutionRequestPropertiesValidator(
            @Value("${canvas.execution-request.dispatch-batch-size:200}") int dispatchBatchSize,
            @Value("${canvas.execution-request.dispatch-fixed-delay-ms:1000}") long dispatchFixedDelayMs,
            @Value("${canvas.execution-request.retry-delay-ms:5000}") long retryDelayMs,
            @Value("${canvas.execution-request.max-attempts:5}") int maxAttempts,
            @Value("${canvas.execution-request.running-stale-sec:300}") long runningStaleSeconds,
            @Value("${canvas.execution-request.max-retry-delay-ms:60000}") long maxRetryDelayMs,
            @Value("${canvas.execution-request.heartbeat-interval-ms:60000}") long heartbeatIntervalMs,
            @Value("${canvas.execution-request.per-canvas-batch-limit:0}") int perCanvasBatchLimit,
            @Value("${canvas.execution-request.replay.single-per-minute:60}") int replaySinglePerMinute,
            @Value("${canvas.execution-request.replay.batch-requests-per-minute:1000}") int replayBatchRequestsPerMinute) {
        this.dispatchBatchSize = dispatchBatchSize;
        this.dispatchFixedDelayMs = dispatchFixedDelayMs;
        this.retryDelayMs = retryDelayMs;
        this.maxAttempts = maxAttempts;
        this.runningStaleSeconds = runningStaleSeconds;
        this.maxRetryDelayMs = maxRetryDelayMs;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.perCanvasBatchLimit = perCanvasBatchLimit;
        this.replaySinglePerMinute = replaySinglePerMinute;
        this.replayBatchRequestsPerMinute = replayBatchRequestsPerMinute;
    }

    /**
     * 校验 validate 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    @PostConstruct
    public void validate() {
        requirePositive("dispatch-batch-size", dispatchBatchSize);
        requirePositive("dispatch-fixed-delay-ms", dispatchFixedDelayMs);
        requirePositive("retry-delay-ms", retryDelayMs);
        requirePositive("max-attempts", maxAttempts);
        requirePositive("running-stale-sec", runningStaleSeconds);
        requirePositive("max-retry-delay-ms", maxRetryDelayMs);
        requirePositive("heartbeat-interval-ms", heartbeatIntervalMs);

        if (maxRetryDelayMs < retryDelayMs) {
            throw new IllegalStateException(
                    "canvas.execution-request.max-retry-delay-ms must be greater than or equal to "
                            + "canvas.execution-request.retry-delay-ms");
        }
        long runningStaleMs = runningStaleSeconds * 1000L;
        // 心跳刷新必须快于 stale 判定窗口，否则 RUNNING 请求会被误判为超时。
        if (heartbeatIntervalMs >= runningStaleMs) {
            throw new IllegalStateException(
                    "canvas.execution-request.heartbeat-interval-ms must be smaller than "
                            + "canvas.execution-request.running-stale-sec");
        }
        if (perCanvasBatchLimit < 0) {
            throw new IllegalStateException(
                    "canvas.execution-request.per-canvas-batch-limit must be greater than or equal to 0");
        }
        if (replaySinglePerMinute < 0) {
            throw new IllegalStateException(
                    "canvas.execution-request.replay.single-per-minute must be greater than or equal to 0");
        }
        if (replayBatchRequestsPerMinute < 0) {
            throw new IllegalStateException(
                    "canvas.execution-request.replay.batch-requests-per-minute must be greater than or equal to 0");
        }
    }

    /**
     * 执行 require Positive 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param property property 方法执行所需的业务参数
     * @param value value 待写入、比较或转换的业务值
     */
    private void requirePositive(String property, long value) {
        if (value <= 0) {
            throw new IllegalStateException("canvas.execution-request." + property + " must be greater than 0");
        }
    }
}
