package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionRequestExecutorTest {

    @Test
    void terminalRequestIsNotExecutedAgain() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestDO request = new CanvasExecutionRequestDO();
        request.setId("mq-10-abc");
        request.setStatus(CanvasExecutionRequestStatus.SUCCEEDED);
        when(mapper.selectById("mq-10-abc")).thenReturn(request);

        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), mock(CanvasMetrics.class),
                5_000L, 5, 300L, 60_000L);

        executor.execute("mq-10-abc").block();

        verify(mapper, never()).markRunning(anyString(), any(), any(), anyString());
        verify(executionService, never()).triggerFromExecutionRequest(
                anyLong(), nullable(String.class), anyString(), anyString(), anyString(),
                anyMap(), nullable(String.class), anyInt(), nullable(String.class));
    }

    @Test
    void missingRequestIsSkippedWithoutExecution() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);

        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), mock(CanvasMetrics.class),
                5_000L, 5, 300L, 60_000L);

        executor.execute("missing").block();

        verify(executionService, never()).triggerFromExecutionRequest(
                anyLong(), nullable(String.class), anyString(), anyString(), anyString(),
                anyMap(), nullable(String.class), anyInt(), nullable(String.class));
    }

    @Test
    void executePassesLaneOverrideToCanvasExecutionService() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestDO request = pendingRequest();
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(
                eq(1L), eq("user-1"), eq("MQ"), eq("MQ_TRIGGER"), eq("order.paid"),
                anyMap(), eq("msg-1"), eq(0), nullable(String.class), eq(ExecutionLane.HEAVY)))
                .thenReturn(Mono.just(Map.of("ok", true)));
        when(mapper.markSucceeded(eq("req-1"), anyString(), any(), anyString())).thenReturn(1);
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), mock(CanvasMetrics.class),
                1_000L, 5, 300L, 60_000L);

        executor.execute("req-1", ExecutionLane.HEAVY).block();

        verify(executionService).triggerFromExecutionRequest(
                eq(1L), eq("user-1"), eq("MQ"), eq("MQ_TRIGGER"), eq("order.paid"),
                anyMap(), eq("msg-1"), eq(0), nullable(String.class), eq(ExecutionLane.HEAVY));
    }

    @Test
    void retryDelayUsesInjectedPressureSnapshots() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestDO request = pendingRequest();
        request.setAttemptCount(1);
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markRunning(eq("req-1"), any(), any(), anyString())).thenReturn(1);
        when(executionService.triggerFromExecutionRequest(
                eq(1L), eq("user-1"), eq("MQ"), eq("MQ_TRIGGER"), eq("order.paid"),
                anyMap(), eq("msg-1"), eq(1), nullable(String.class), nullable(ExecutionLane.class)))
                .thenReturn(Mono.error(new IllegalStateException("provider down")));
        when(mapper.markRetry(eq("req-1"), anyString(), any(), any(), anyString())).thenReturn(1);
        ExecutionRequestRetryPressureSource pressureSource = () -> new ExecutionRequestRetryPressureSource.Snapshot(
                new AdaptiveRetryBackoffPolicy.LanePressureSnapshot(250, 100),
                new AdaptiveRetryBackoffPolicy.DownstreamErrorSnapshot(15.0, 5.0),
                new AdaptiveRetryBackoffPolicy.DlqGrowthSnapshot(200, 50));
        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), mock(CanvasMetrics.class),
                1_000L, 5, 300L, 10_000L, 60_000L,
                org.chovy.canvas.infrastructure.reactor.TrackedReactiveTaskRegistry.direct(),
                pressureSource);
        LocalDateTime before = LocalDateTime.now();

        executor.execute("req-1").block();

        ArgumentCaptor<LocalDateTime> retryAt = forClass(LocalDateTime.class);
        verify(mapper).markRetry(eq("req-1"), eq("provider down"), retryAt.capture(), any(), anyString());
        assertThat(retryAt.getValue()).isAfterOrEqualTo(before.plusSeconds(9));
    }

    private CanvasExecutionRequestDO pendingRequest() {
        CanvasExecutionRequestDO request = new CanvasExecutionRequestDO();
        request.setId("req-1");
        request.setCanvasId(1L);
        request.setUserId("user-1");
        request.setTriggerType("MQ");
        request.setTriggerNodeType("MQ_TRIGGER");
        request.setMatchKey("order.paid");
        request.setPayloadJson("{\"amount\":100}");
        request.setSourceMsgId("msg-1");
        request.setStatus(CanvasExecutionRequestStatus.PENDING);
        request.setAttemptCount(0);
        return request;
    }
}
