package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.perf.PerfRunContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 画布执行请求 Executor 执行请求治理组件。
 *
 * <p>负责画布执行请求的排队、派发、限流、重放或积压度量，削峰高并发触发流量。
 * <p>该层位于触发入口和 DAG 执行之间，核心目标是保护执行引擎稳定性。
 */
@Service
public class CanvasExecutionRequestExecutor {

    /** 执行请求 Mapper，用于状态流转和请求载荷读取。 */
    private final CanvasExecutionRequestMapper mapper;
    /** 画布执行服务，承接实际 DAG 触发。 */
    private final CanvasExecutionService executionService;
    /** Jackson ObjectMapper，用于请求载荷反序列化和结果序列化。 */
    private final ObjectMapper objectMapper;
    /** 画布指标埋点器。 */
    private final CanvasMetrics metrics;
    /** 初始重试延迟毫秒数。 */
    private final long retryDelayMs;
    /** 执行请求最大尝试次数。 */
    private final int maxAttempts;
    /** RUNNING 请求判定为陈旧可抢占的秒数。 */
    private final long runningStaleSeconds;
    /** 指数退避重试的最大延迟毫秒数。 */
    private final long maxRetryDelayMs;
    /** RUNNING 请求心跳刷新间隔毫秒数。 */
    private final long heartbeatIntervalMs;

    /**
     * 构造 CanvasExecutionRequestExecutor 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param mapper mapper 方法执行所需的业务参数
     * @param executionService executionService 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     */
    public CanvasExecutionRequestExecutor(CanvasExecutionRequestMapper mapper,
                                          CanvasExecutionService executionService,
                                          ObjectMapper objectMapper) {
        this(mapper, executionService, objectMapper, null, 5_000L, 5, 300L, 60_000L, 60_000L);
    }

    /**
     * 构造 CanvasExecutionRequestExecutor 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param mapper mapper 方法执行所需的业务参数
     * @param executionService executionService 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     * @param retryDelayMs retryDelayMs 方法执行所需的业务参数
     * @param maxAttempts maxAttempts 方法执行所需的业务参数
     * @param runningStaleSeconds runningStaleSeconds 方法执行所需的业务参数
     */
    public CanvasExecutionRequestExecutor(CanvasExecutionRequestMapper mapper,
                                          CanvasExecutionService executionService,
                                          ObjectMapper objectMapper,
                                          long retryDelayMs,
                                          int maxAttempts,
                                          long runningStaleSeconds) {
        this(mapper, executionService, objectMapper, null, retryDelayMs, maxAttempts, runningStaleSeconds, 60_000L, 60_000L);
    }

    @Autowired
    public CanvasExecutionRequestExecutor(CanvasExecutionRequestMapper mapper,
                                          CanvasExecutionService executionService,
                                          ObjectMapper objectMapper,
                                          CanvasMetrics metrics,
                                          @Value("${canvas.execution-request.retry-delay-ms:5000}") long retryDelayMs,
                                          @Value("${canvas.execution-request.max-attempts:5}") int maxAttempts,
                                          @Value("${canvas.execution-request.running-stale-sec:300}") long runningStaleSeconds,
                                          @Value("${canvas.execution-request.max-retry-delay-ms:60000}") long maxRetryDelayMs,
                                          @Value("${canvas.execution-request.heartbeat-interval-ms:60000}") long heartbeatIntervalMs) {
        this.mapper = mapper;
        this.executionService = executionService;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.retryDelayMs = retryDelayMs;
        this.maxAttempts = maxAttempts;
        this.runningStaleSeconds = runningStaleSeconds;
        this.maxRetryDelayMs = maxRetryDelayMs;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
    }

    /**
     * 构造 CanvasExecutionRequestExecutor 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param mapper mapper 方法执行所需的业务参数
     * @param executionService executionService 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     * @param metrics metrics 方法执行所需的业务参数
     * @param retryDelayMs retryDelayMs 方法执行所需的业务参数
     * @param maxAttempts maxAttempts 方法执行所需的业务参数
     * @param runningStaleSeconds runningStaleSeconds 方法执行所需的业务参数
     * @param maxRetryDelayMs maxRetryDelayMs 方法执行所需的业务参数
     */
    public CanvasExecutionRequestExecutor(CanvasExecutionRequestMapper mapper,
                                          CanvasExecutionService executionService,
                                          ObjectMapper objectMapper,
                                          CanvasMetrics metrics,
                                          long retryDelayMs,
                                          int maxAttempts,
                                          long runningStaleSeconds,
                                          long maxRetryDelayMs) {
        this(mapper, executionService, objectMapper, metrics, retryDelayMs, maxAttempts,
                runningStaleSeconds, maxRetryDelayMs, 60_000L);
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param requestId requestId 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    public Mono<Void> execute(String requestId) {
        return loadRequest(requestId)
                .flatMap(this::tryMarkAsRunning)
                .flatMap(this::executeCanvas)
                .then();
    }

    /**
     * 加载执行请求
     */
    private Mono<CanvasExecutionRequestDO> loadRequest(String requestId) {
        return Mono.fromCallable(() -> mapper.selectById(requestId))
                .subscribeOn(Schedulers.boundedElastic())
                // 终态请求不再进入执行链，避免已完成记录被重复消费。
                .filter(request -> request != null && !isTerminal(request.getStatus()));
    }

    /**
     * 尝试标记为运行中（基于乐观锁）
     */
    private Mono<RunningContext> tryMarkAsRunning(CanvasExecutionRequestDO request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusSeconds(runningStaleSeconds);
        String runToken = UUID.randomUUID().toString();

        // 通过带 stale 窗口的乐观锁把请求抢占到 RUNNING，只有一条执行链能成功。
        return Mono.fromCallable(() -> mapper.markRunning(
                        request.getId(), now, staleBefore, runToken))
                .subscribeOn(Schedulers.boundedElastic())
                .filter(updated -> updated > 0)
                .map(updated -> new RunningContext(request, runToken));
    }

    /**
     * 执行画布逻辑
     */
    private Mono<Void> executeCanvas(RunningContext ctx) {
        return Mono.defer(() -> {
                    Map<String, Object> payload = preparePayload(ctx.request);
                    // 执行时继续透传压测上下文，确保指标、日志和请求记录能互相关联。
                    return withRunningHeartbeat(
                            ctx.request.getId(),
                            ctx.runToken,
                            executionService.triggerFromExecutionRequest(
                                    ctx.request.getCanvasId(),
                                    ctx.request.getUserId(),
                                    ctx.request.getTriggerType(),
                                    ctx.request.getTriggerNodeType(),
                                    ctx.request.getMatchKey(),
                                    payload,
                                    ctx.request.getSourceMsgId(),
                                    ctx.request.getAttemptCount() == null ? 0 : ctx.request.getAttemptCount(),
                                    ctx.request.getLastError()
                            ));
                })
                .flatMap(result -> finish(ctx.request, result, ctx.runToken))
                .onErrorResume(e -> handleExecutionError(ctx, e));
    }

    /**
     * 准备执行载荷
     */
    private Map<String, Object> preparePayload(CanvasExecutionRequestDO request) {
        Map<String, Object> payload = parsePayload(request.getPayloadJson());

        // 请求入库时如果已单独写过 perfRunId，这里把它补回执行载荷，保证下游读取一致。
        if (request.getPerfRunId() != null && PerfRunContext.extract(payload) == null) {
            payload.put("perfRunId", request.getPerfRunId());
        }

        return payload;
    }

    /**
     * 处理执行异常
     */
    private Mono<Void> handleExecutionError(RunningContext ctx, Throwable e) {
        if (isNonRecoverable(e)) {
            // 参数/配置类错误直接失败，不再占用重试窗口。
            return fail(ctx.request, e.getMessage(), ctx.runToken);
        }
        // 可恢复错误走重试或最终失败路径，避免临时抖动直接丢单。
        return retryOrFail(ctx.request, e.getMessage(), ctx.runToken);
    }

    /**
     * 运行中的上下文
     */
    private static class RunningContext {
        /** 当前抢占到的执行请求。 */
        final CanvasExecutionRequestDO request;
        /** 本次 RUNNING 状态的乐观锁令牌。 */
        final String runToken;

        /**
         * 构造 RunningContext 实例，并根据入参初始化依赖、配置或内部状态。
         *
         * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
         *
         * @param request 请求对象，承载调用方提交的业务参数
         * @param runToken runToken 方法执行所需的业务参数
         */
        RunningContext(CanvasExecutionRequestDO request, String runToken) {
            this.request = request;
            this.runToken = runToken;
        }
    }

    /**
     * 执行 with Running Heartbeat 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param requestId requestId 对应的业务主键或标识
     * @param runToken runToken 方法执行所需的业务参数
     * @param source source 方法执行所需的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    private <T> Mono<T> withRunningHeartbeat(String requestId, String runToken, Mono<T> source) {
        return Mono.defer(() -> {
            Disposable heartbeat = startHeartbeat(requestId, runToken);
            // 事件完成或失败后都要停掉心跳，避免后台定时刷新继续占用资源。
            return source.doFinally(signal -> heartbeat.dispose());
        });
    }

    /**
     * 注册、调度或初始化 start Heartbeat 相关的业务数据。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param requestId requestId 对应的业务主键或标识
     * @param runToken runToken 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private Disposable startHeartbeat(String requestId, String runToken) {
        long intervalMs = Math.max(1L, heartbeatIntervalMs);
        // 心跳只负责刷新 RUNNING 时间戳，供调度器识别健康执行中的请求。
        return Flux.interval(Duration.ofMillis(intervalMs))
                .flatMap(ignored -> Mono.fromCallable(() ->
                                mapper.touchRunning(requestId, LocalDateTime.now(), runToken))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> Mono.empty()))
                .subscribe();
    }

    /**
     * 执行 finish 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param request 请求对象，承载调用方提交的业务参数
     * @param result result 方法执行所需的业务参数
     * @param runToken runToken 方法执行所需的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    private Mono<Void> finish(CanvasExecutionRequestDO request, Map<String, Object> result, String runToken) {
        if (result.containsKey(MapFieldKeys.OVERFLOW)) {
            // 溢出表示当前执行结果需要回压到重试链路，不直接标记成功。
            return retryOrFail(request, String.valueOf(result.get(MapFieldKeys.OVERFLOW)), runToken);
        }
        if (result.containsKey(MapFieldKeys.ERROR)) {
            // 业务错误同样进入重试/失败决策，保留原始错误信息。
            return retryOrFail(request, String.valueOf(result.get(MapFieldKeys.ERROR)), runToken);
        }
        return Mono.fromCallable(() -> mapper.markSucceeded(
                        request.getId(),
                        toJson(result),
                        LocalDateTime.now(),
                        runToken))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(updated -> {
                    if (updated > 0) {
                        recordTransition(CanvasExecutionRequestStatus.SUCCEEDED, request.getTriggerType());
                    }
                })
                .then();
    }

    /**
     * 执行 retry Or Fail 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param request 请求对象，承载调用方提交的业务参数
     * @param error error 方法执行所需的业务参数
     * @param runToken runToken 方法执行所需的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    private Mono<Void> retryOrFail(CanvasExecutionRequestDO request, String error, String runToken) {
        int nextAttempt = request.getAttemptCount() == null ? 1 : request.getAttemptCount() + 1;
        LocalDateTime now = LocalDateTime.now();
        if (nextAttempt >= maxAttempts) {
            // 已达到最大次数后直接落失败，避免无限重试拖死队列。
            return fail(request, error, runToken);
        }
        LocalDateTime nextRetryAt = now.plusNanos(calculateRetryDelayMs(nextAttempt) * 1_000_000L);
        // 通过指数退避把失败请求重新放回队列，给系统留出恢复时间。
        return Mono.fromCallable(() -> mapper.markRetry(request.getId(), trim(error), nextRetryAt, now, runToken))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(updated -> {
                    if (updated > 0) {
                        recordTransition(CanvasExecutionRequestStatus.RETRY, request.getTriggerType());
                    }
                })
                .then();
    }

    /**
     * 执行 fail 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param request 请求对象，承载调用方提交的业务参数
     * @param error error 方法执行所需的业务参数
     * @param runToken runToken 方法执行所需的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    private Mono<Void> fail(CanvasExecutionRequestDO request, String error, String runToken) {
        // 最终失败会把错误和运行令牌一起落库，便于排查最后一次执行上下文。
        return Mono.fromCallable(() -> mapper.markFailed(request.getId(), trim(error), LocalDateTime.now(), runToken))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(updated -> {
                    if (updated > 0) {
                        recordTransition(CanvasExecutionRequestStatus.FAILED, request.getTriggerType());
                    }
                })
                .then();
    }

    /**
     * 判断 is Non Recoverable 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param error error 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean isNonRecoverable(Throwable error) {
        return error instanceof IllegalArgumentException;
    }

    /**
     * 判断 is Terminal 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param status status 状态值或状态筛选条件
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean isTerminal(String status) {
        return CanvasExecutionRequestStatus.SUCCEEDED.equals(status)
                || CanvasExecutionRequestStatus.FAILED.equals(status);
    }

    /**
     * 构建、解析或转换 parse Payload 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param payloadJson payloadJson 请求体、消息体或事件载荷
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> parsePayload(String payloadJson) {
        try {
            if (payloadJson == null || payloadJson.isBlank()) {
                return new HashMap<>();
            }
            // 使用可变 Map 是为了后续补回 perfRunId 等执行期字段。
            return new HashMap<>(objectMapper.readValue(payloadJson, new TypeReference<>() {
            }));
        } catch (Exception e) {
            throw new IllegalArgumentException("Execution request payload parse failed", e);
        }
    }

    /**
     * 构建、解析或转换 to Json 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param result result 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String toJson(Map<String, Object> result) {
        try {
            return objectMapper.writeValueAsString(result != null ? result : Map.of());
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 执行 trim 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param error error 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String trim(String error) {
        if (error == null) {
            return "unknown";
        }
        return error.length() <= 500 ? error : error.substring(0, 500);
    }

    /**
     * 计算或统计 calculate Retry Delay Ms 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nextAttempt nextAttempt 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    private long calculateRetryDelayMs(int nextAttempt) {
        long base = Math.max(1L, retryDelayMs);
        long cap = Math.max(base, maxRetryDelayMs);
        int exponent = Math.max(0, nextAttempt - 1);
        long delay = base;
        for (int i = 0; i < exponent; i++) {
            if (delay >= cap / 2) {
                return cap;
            }
            // 指数退避逐步放大间隔，避免同一批失败请求同步回压。
            delay *= 2;
        }
        return Math.min(delay, cap);
    }

    /**
     * 写入或记录 record Transition 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param status status 状态值或状态筛选条件
     * @param triggerType triggerType 类型标识或分类条件
     */
    private void recordTransition(String status, String triggerType) {
        if (metrics == null) {
            return;
        }
        try {
            metrics.recordExecutionRequestTransition(status, triggerType);
        } catch (RuntimeException ignored) {
            // Metrics must not change execution-request state transitions.
        }
    }
}
