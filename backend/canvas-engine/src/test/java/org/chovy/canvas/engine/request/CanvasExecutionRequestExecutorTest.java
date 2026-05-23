package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasExecutionRequest;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionRequestExecutorTest {

    @Test
    void executeMarksRequestSucceededWhenCanvasTriggerCompletes() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper());

        CanvasExecutionRequest request = request();
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1")))
                .thenReturn(Mono.just(Map.of("executionId", "exec-1")));

        executor.execute("req-1").block();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).markRunning(eq("req-1"), any(), any(), token.capture());
        verify(mapper).markSucceeded(eq("req-1"), anyString(), any(), eq(token.getValue()));
        verify(mapper, never()).markRetry(anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void executeMarksRequestRetryWhenCanvasTriggerReportsOverflow() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), metrics, 1_000L, 5, 300L, 60_000L);

        CanvasExecutionRequest request = request();
        request.setAttemptCount(2);
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(mapper.markRetry(eq("req-1"), anyString(), any(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1")))
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
    void executeDoesNotTriggerCanvasWhenAnotherWorkerAlreadyClaimedRequest() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper());

        when(mapper.selectById("req-1")).thenReturn(request());
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(0);

        executor.execute("req-1").block();

        verify(executionService, never()).triggerFromExecutionRequest(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void executeMarksFailedWhenStoredPayloadCannotBeParsed() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper());

        CanvasExecutionRequest request = request();
        request.setPayloadJson("{");
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);

        executor.execute("req-1").block();

        verify(mapper).markFailed(eq("req-1"), org.mockito.ArgumentMatchers.contains("payload parse failed"),
                any(), anyString());
        verify(mapper, never()).markRetry(anyString(), anyString(), any(), any(), anyString());
        verify(executionService, never()).triggerFromExecutionRequest(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void executeRefreshesRunningLeaseWhileTriggerIsStillRunning() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), null, 1_000L, 5, 300L, 60_000L, 10L);

        CanvasExecutionRequest request = request();
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(mapper.touchRunning(eq("req-1"), any(), anyString())).thenReturn(1);
        when(mapper.markSucceeded(eq("req-1"), anyString(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1")))
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

        CanvasExecutionRequest request = request();
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(mapper.markSucceeded(eq("req-1"), anyString(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1")))
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

        CanvasExecutionRequest request = request();
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(mapper.markSucceeded(eq("req-1"), anyString(), any(), anyString())).thenReturn(0);
        when(executionService.triggerFromExecutionRequest(eq(10L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("order.paid"), eq(Map.of("orderId", "O-1")),
                eq("MSG-1")))
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

        CanvasExecutionRequest request = request();
        request.setAttemptCount(2);
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new IllegalStateException("downstream down")));

        executor.execute("req-1").block();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).markRunning(eq("req-1"), any(), any(), token.capture());
        verify(mapper).markFailed(eq("req-1"), eq("downstream down"), any(), eq(token.getValue()));
    }

    private CanvasExecutionRequest request() {
        CanvasExecutionRequest request = new CanvasExecutionRequest();
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
