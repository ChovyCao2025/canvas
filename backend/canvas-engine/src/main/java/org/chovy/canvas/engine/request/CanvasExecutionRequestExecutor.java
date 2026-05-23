package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.domain.execution.CanvasExecutionRequest;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
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
        return Mono.fromCallable(() -> mapper.selectById(requestId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(request -> {
                    if (request == null || isTerminal(request.getStatus())) {
                        return Mono.empty();
                    }
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime staleBefore = now.minusSeconds(runningStaleSeconds);
                    String runToken = UUID.randomUUID().toString();
                    return Mono.fromCallable(() -> mapper.markRunning(requestId, now, staleBefore, runToken))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(updated -> {
                                if (updated <= 0) {
                                    return Mono.empty();
                                }
                                return Mono.defer(() -> {
                                            Map<String, Object> payload = parsePayload(request.getPayloadJson());
                                            if (request.getPerfRunId() != null
                                                    && PerfRunContext.extract(payload) == null) {
                                                payload.put("perfRunId", request.getPerfRunId());
                                            }
                                            return withRunningHeartbeat(request.getId(), runToken,
                                                    executionService.triggerFromExecutionRequest(
                                                request.getCanvasId(),
                                                request.getUserId(),
                                                request.getTriggerType(),
                                                request.getTriggerNodeType(),
                                                request.getMatchKey(),
                                                payload,
                                                request.getSourceMsgId()
                                            ));
                                        })
                                        .flatMap(result -> finish(request, result, runToken))
                                        .onErrorResume(e -> {
                                            if (isNonRecoverable(e)) {
                                                return fail(request, e.getMessage(), runToken);
                                            }
                                            return retryOrFail(request, e.getMessage(), runToken);
                                        });
                            });
                })
                .then();
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

    private Mono<Void> finish(CanvasExecutionRequest request, Map<String, Object> result, String runToken) {
        if (result.containsKey(MapFieldKeys.OVERFLOW)) {
            return retryOrFail(request, String.valueOf(result.get(MapFieldKeys.OVERFLOW)), runToken);
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

    private Mono<Void> retryOrFail(CanvasExecutionRequest request, String error, String runToken) {
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

    private Mono<Void> fail(CanvasExecutionRequest request, String error, String runToken) {
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
