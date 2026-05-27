package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 画布执行请求 Executor 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasExecutionRequestExecutorTest {

    @Test
    void executeMarksRequestSucceededWhenCanvasTriggerCompletes() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper());

        CanvasExecutionRequestDO request = request();
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1"), eq(0), isNull()))
                .thenReturn(Mono.just(Map.of("executionId", "exec-1")));

        executor.execute("req-1").block();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).markRunning(eq("req-1"), any(), any(), token.capture());
        verify(mapper).markSucceeded(eq("req-1"), anyString(), any(), eq(token.getValue()));
        verify(mapper, never()).markRetry(anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void executeAddsRequestPerfRunIdToPayloadWhenStoredPayloadLacksIt() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper());

        CanvasExecutionRequestDO request = request();
        request.setPerfRunId("perf_20260523_001");
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(any(), any(), any(), any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(Mono.just(Map.of("executionId", "exec-1")));

        executor.execute("req-1").block();

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executionService).triggerFromExecutionRequest(
                eq(10L),
                eq("user-7"),
                eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER),
                eq("order.paid"),
                payloadCaptor.capture(),
                eq("MSG-1"),
                eq(0),
                isNull());
        assertThat(payloadCaptor.getValue())
                .containsEntry("orderId", "O-1")
                .containsEntry("perfRunId", "perf_20260523_001");
    }

    @Test
    void executeMarksRequestRetryWhenCanvasTriggerReportsOverflow() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), metrics, 1_000L, 5, 300L, 60_000L);

        CanvasExecutionRequestDO request = request();
        request.setAttemptCount(2);
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(mapper.markRetry(eq("req-1"), anyString(), any(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1"), eq(2), isNull()))
                .thenReturn(Mono.just(Map.of("overflow", "concurrency_limit_reached")));

        LocalDateTime before = LocalDateTime.now();
        executor.execute("req-1").block();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> retryAt = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).markRunning(eq("req-1"), any(), any(), token.capture());
        verify(mapper).markRetry(eq("req-1"), eq("concurrency_limit_reached"), retryAt.capture(), any(), eq(token.getValue()));
        assertThat(retryAt.getValue()).isAfterOrEqualTo(before.plusSeconds(4));
        assertThat(retryAt.getValue()).isBeforeOrEqualTo(after.plusSeconds(5));
        verify(mapper, never()).markSucceeded(anyString(), anyString(), any(), anyString());
        verify(metrics).recordExecutionRequestTransition(CanvasExecutionRequestStatus.RETRY, TriggerType.MQ);
    }

    @Test
    void executeRetriesWhenCanvasTriggerReturnsErrorMap() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), metrics, 1_000L, 5, 300L, 60_000L);

        CanvasExecutionRequestDO request = request();
        request.setAttemptCount(2);
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(mapper.markRetry(eq("req-1"), anyString(), any(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1"), eq(2), isNull()))
                .thenReturn(Mono.just(Map.of("error", "node failed")));

        executor.execute("req-1").block();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).markRunning(eq("req-1"), any(), any(), token.capture());
        verify(mapper).markRetry(eq("req-1"), eq("node failed"), any(), any(), eq(token.getValue()));
        verify(mapper, never()).markSucceeded(anyString(), anyString(), any(), anyString());
        verify(metrics).recordExecutionRequestTransition(CanvasExecutionRequestStatus.RETRY, TriggerType.MQ);
    }

    @Test
    void executeDoesNotTriggerCanvasWhenAnotherWorkerAlreadyClaimedRequest() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper());

        when(mapper.selectById("req-1")).thenReturn(request());
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(0);

        executor.execute("req-1").block();

        verify(executionService, never()).triggerFromExecutionRequest(any(), any(), any(), any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void executeMarksFailedWhenStoredPayloadCannotBeParsed() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper());

        CanvasExecutionRequestDO request = request();
        request.setPayloadJson("{");
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);

        executor.execute("req-1").block();

        verify(mapper).markFailed(eq("req-1"), org.mockito.ArgumentMatchers.contains("payload parse failed"),
                any(), anyString());
        verify(mapper, never()).markRetry(anyString(), anyString(), any(), any(), anyString());
        verify(executionService, never()).triggerFromExecutionRequest(any(), any(), any(), any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void executeRefreshesRunningLeaseWhileTriggerIsStillRunning() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), null, 1_000L, 5, 300L, 60_000L, 10L);

        CanvasExecutionRequestDO request = request();
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(mapper.touchRunning(eq("req-1"), any(), anyString())).thenReturn(1);
        when(mapper.markSucceeded(eq("req-1"), anyString(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1"), eq(0), isNull()))
                .thenReturn(Mono.delay(Duration.ofMillis(50)).thenReturn(Map.of("executionId", "exec-1")));

        executor.execute("req-1").block();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).markRunning(eq("req-1"), any(), any(), token.capture());
        verify(mapper, atLeastOnce()).touchRunning(eq("req-1"), any(), eq(token.getValue()));
    }

    @Test
    void executeRecordsSucceededRequestMetric() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), metrics, 1_000L, 5, 300L, 60_000L);

        CanvasExecutionRequestDO request = request();
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(mapper.markSucceeded(eq("req-1"), anyString(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1"), eq(0), isNull()))
                .thenReturn(Mono.just(Map.of("executionId", "exec-1")));

        executor.execute("req-1").block();

        verify(metrics).recordExecutionRequestTransition(CanvasExecutionRequestStatus.SUCCEEDED, TriggerType.MQ);
    }

    @Test
    void executeDoesNotRecordSucceededMetricWhenRunTokenNoLongerOwnsRequest() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), metrics, 1_000L, 5, 300L, 60_000L);

        CanvasExecutionRequestDO request = request();
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(mapper.markSucceeded(eq("req-1"), anyString(), any(), anyString())).thenReturn(0);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1"), eq(0), isNull()))
                .thenReturn(Mono.just(Map.of("executionId", "exec-1")));

        executor.execute("req-1").block();

        verify(metrics, never()).recordExecutionRequestTransition(anyString(), anyString());
    }

    @Test
    void executeMarksFailedWithRunTokenWhenAttemptLimitIsReached() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), 1_000L, 3, 300L);

        CanvasExecutionRequestDO request = request();
        request.setAttemptCount(2);
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(any(), any(), any(), any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(Mono.error(new IllegalStateException("downstream down")));

        executor.execute("req-1").block();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).markRunning(eq("req-1"), any(), any(), token.capture());
        verify(mapper).markFailed(eq("req-1"), eq("downstream down"), any(), eq(token.getValue()));
    }

    private CanvasExecutionRequestDO request() {
        CanvasExecutionRequestDO request = new CanvasExecutionRequestDO();
        request.setId("req-1");
        request.setCanvasId(10L);
        request.setUserId("user-7");
        request.setTriggerType(TriggerType.MQ);
        request.setTriggerNodeType(NodeType.MQ_TRIGGER);
        request.setMatchKey("order.paid");
        request.setPayloadJson("{\"orderId\":\"O-1\"}");
        request.setSourceMsgId("MSG-1");
        request.setStatus(CanvasExecutionRequestStatus.PENDING);
        request.setAttemptCount(0);
        return request;
    }
}
