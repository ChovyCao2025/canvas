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

    private final CanvasExecutionRequestMapper mapper;
    private final CanvasExecutionService executionService;
    private final ObjectMapper objectMapper;
    private final CanvasMetrics metrics;
    private final long retryDelayMs;
    private final int maxAttempts;
    private final long runningStaleSeconds;
    private final long maxRetryDelayMs;
    private final long heartbeatIntervalMs;

    public CanvasExecutionRequestExecutor(CanvasExecutionRequestMapper mapper,
                                          CanvasExecutionService executionService,
                                          ObjectMapper objectMapper) {
        this(mapper, executionService, objectMapper, null, 5_000L, 5, 300L, 60_000L, 60_000L);
    }

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
                .filter(request -> request != null && !isTerminal(request.getStatus()));
    }

    /**
     * 尝试标记为运行中（基于乐观锁）
     */
    private Mono<RunningContext> tryMarkAsRunning(CanvasExecutionRequestDO request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusSeconds(runningStaleSeconds);
        String runToken = UUID.randomUUID().toString();

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
        Map<String, Object> payload = preparePayload(ctx.request);

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
                ))
                .flatMap(result -> finish(ctx.request, result, ctx.runToken))
                .onErrorResume(e -> handleExecutionError(ctx, e));
    }

    /**
     * 准备执行载荷
     */
    private Map<String, Object> preparePayload(CanvasExecutionRequestDO request) {
        Map<String, Object> payload = parsePayload(request.getPayloadJson());

        // 回填 perfRunId（如果 payload 中没有）
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
            return fail(ctx.request, e.getMessage(), ctx.runToken);
        }
        return retryOrFail(ctx.request, e.getMessage(), ctx.runToken);
    }

    /**
     * 运行中的上下文
     */
    private static class RunningContext {
        final CanvasExecutionRequestDO request;
        final String runToken;

        RunningContext(CanvasExecutionRequestDO request, String runToken) {
            this.request = request;
            this.runToken = runToken;
        }
    }

    private <T> Mono<T> withRunningHeartbeat(String requestId, String runToken, Mono<T> source) {
        return Mono.defer(() -> {
            Disposable heartbeat = startHeartbeat(requestId, runToken);
            return source.doFinally(signal -> heartbeat.dispose());
        });
    }

    private Disposable startHeartbeat(String requestId, String runToken) {
        long intervalMs = Math.max(1L, heartbeatIntervalMs);
        return Flux.interval(Duration.ofMillis(intervalMs))
                .flatMap(ignored -> Mono.fromCallable(() ->
                                mapper.touchRunning(requestId, LocalDateTime.now(), runToken))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> Mono.empty()))
                .subscribe();
    }

    private Mono<Void> finish(CanvasExecutionRequestDO request, Map<String, Object> result, String runToken) {
        if (result.containsKey(MapFieldKeys.OVERFLOW)) {
            return retryOrFail(request, String.valueOf(result.get(MapFieldKeys.OVERFLOW)), runToken);
        }
        if (result.containsKey(MapFieldKeys.ERROR)) {
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

    private Mono<Void> retryOrFail(CanvasExecutionRequestDO request, String error, String runToken) {
        int nextAttempt = request.getAttemptCount() == null ? 1 : request.getAttemptCount() + 1;
        LocalDateTime now = LocalDateTime.now();
        if (nextAttempt >= maxAttempts) {
            return fail(request, error, runToken);
        }
        LocalDateTime nextRetryAt = now.plusNanos(calculateRetryDelayMs(nextAttempt) * 1_000_000L);
        return Mono.fromCallable(() -> mapper.markRetry(request.getId(), trim(error), nextRetryAt, now, runToken))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(updated -> {
                    if (updated > 0) {
                        recordTransition(CanvasExecutionRequestStatus.RETRY, request.getTriggerType());
                    }
                })
                .then();
    }

    private Mono<Void> fail(CanvasExecutionRequestDO request, String error, String runToken) {
        return Mono.fromCallable(() -> mapper.markFailed(request.getId(), trim(error), LocalDateTime.now(), runToken))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(updated -> {
                    if (updated > 0) {
                        recordTransition(CanvasExecutionRequestStatus.FAILED, request.getTriggerType());
                    }
                })
                .then();
    }

    private boolean isNonRecoverable(Throwable error) {
        return error instanceof IllegalArgumentException;
    }

    private boolean isTerminal(String status) {
        return CanvasExecutionRequestStatus.SUCCEEDED.equals(status)
                || CanvasExecutionRequestStatus.FAILED.equals(status);
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        try {
            if (payloadJson == null || payloadJson.isBlank()) {
                return new HashMap<>();
            }
            return new HashMap<>(objectMapper.readValue(payloadJson, new TypeReference<>() {
            }));
        } catch (Exception e) {
            throw new IllegalArgumentException("Execution request payload parse failed", e);
        }
    }

    private String toJson(Map<String, Object> result) {
        try {
            return objectMapper.writeValueAsString(result != null ? result : Map.of());
        } catch (Exception e) {
            return "{}";
        }
    }

    private String trim(String error) {
        if (error == null) {
            return "unknown";
        }
        return error.length() <= 500 ? error : error.substring(0, 500);
    }

    private long calculateRetryDelayMs(int nextAttempt) {
        long base = Math.max(1L, retryDelayMs);
        long cap = Math.max(base, maxRetryDelayMs);
        int exponent = Math.max(0, nextAttempt - 1);
        long delay = base;
        for (int i = 0; i < exponent; i++) {
            if (delay >= cap / 2) {
                return cap;
            }
            delay *= 2;
        }
        return Math.min(delay, cap);
    }

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
