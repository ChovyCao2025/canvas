package org.chovy.canvas.engine.trigger;

import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneResolver;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriggerAdmissionServiceTest {

    @Test
    void duplicateMessageShortCircuitsBeforePreCheckAndAdmission() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.acquireDedup(eq(10L), eq("user-1"), eq("msg-1"), any(Duration.class)))
                .thenReturn(false);
        TriggerPreCheckService preCheckService = mock(TriggerPreCheckService.class);
        InFlightExecutionRegistry registry = mock(InFlightExecutionRegistry.class);
        TriggerAdmissionService service = service(ctxStore, preCheckService, registry,
                mock(CanvasExecutionMapper.class), mock(CanvasExecutionDlqMapper.class));

        TriggerAdmissionService.AdmissionDecision decision = service.evaluate(request(
                TriggerType.MQ, NodeType.MQ_TRIGGER, Map.of(), "msg-1",
                false, false, false, 0, 0));

        assertThat(decision.isShortCircuited()).isTrue();
        assertThat(decision.shortCircuitResponse()).containsEntry(MapFieldKeys.DEDUPLICATED, true);
        verify(preCheckService, never()).checkWithoutQuotaAccounting(any(), any());
        verify(registry, never()).activeCount(any());
    }

    @Test
    void normalMessageAcquiresDedupRunsPreCheckAndReturnsLaneAdmission() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.acquireDedup(eq(10L), eq("user-1"), eq("msg-1"), any(Duration.class)))
                .thenReturn(true);
        when(ctxStore.buildDedupKey(10L, "user-1", "msg-1")).thenReturn("dedup-key");
        TriggerPreCheckService preCheckService = mock(TriggerPreCheckService.class);
        InFlightExecutionRegistry registry = mock(InFlightExecutionRegistry.class);
        when(registry.activeCount(10L)).thenReturn(0);
        TriggerAdmissionService service = service(ctxStore, preCheckService, registry,
                mock(CanvasExecutionMapper.class), mock(CanvasExecutionDlqMapper.class));

        TriggerAdmissionService.AdmissionDecision decision = service.evaluate(request(
                TriggerType.MQ, NodeType.MQ_TRIGGER, Map.of(), "msg-1",
                false, false, false, 0, 0));

        assertThat(decision.isShortCircuited()).isFalse();
        assertThat(decision.dedupKey()).isEqualTo("dedup-key");
        assertThat(decision.admissionLimit()).isEqualTo(100);
        assertThat(decision.executionLane()).isEqualTo(ExecutionLane.STANDARD);
        assertThat(decision.quotaBypass()).isFalse();
        verify(preCheckService).checkWithoutQuotaAccounting(any(CanvasDO.class), eq("user-1"));
    }

    @Test
    void internalContinuationWithoutStoredContextMarksPausedExecutionFailed() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(10L, "user-1")).thenReturn(false);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        when(executionMapper.update(any(CanvasExecutionDO.class), any())).thenReturn(1);
        TriggerAdmissionService service = service(ctxStore, mock(TriggerPreCheckService.class),
                mock(InFlightExecutionRegistry.class), executionMapper, mock(CanvasExecutionDlqMapper.class));

        TriggerAdmissionService.AdmissionDecision decision = service.evaluate(request(
                TriggerType.WAIT_RESUME, NodeType.WAIT, Map.of(), "exec-1:wait:7:COMPLETED",
                false, false, false, 0, 0));

        assertThat(decision.isShortCircuited()).isTrue();
        assertThat(decision.shortCircuitResponse()).containsEntry(MapFieldKeys.SKIPPED, "missing-context");
        assertThat(decision.shortCircuitResponse()).containsEntry(
                MapFieldKeys.ERROR, "internal continuation context is missing");
        verify(executionMapper).update(any(CanvasExecutionDO.class), any());
    }

    @Test
    void persistentRequestOverflowReturnsRequestRetryWithoutRocketMq() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        TriggerPreCheckService preCheckService = mock(TriggerPreCheckService.class);
        InFlightExecutionRegistry registry = mock(InFlightExecutionRegistry.class);
        when(registry.activeCount(10L)).thenReturn(100);
        TriggerAdmissionService service = service(ctxStore, preCheckService, registry,
                mock(CanvasExecutionMapper.class), mock(CanvasExecutionDlqMapper.class));

        TriggerAdmissionService.AdmissionDecision decision = service.evaluate(request(
                TriggerType.MQ, NodeType.MQ_TRIGGER, Map.of(), "msg-1",
                false, false, true, 0, 0));

        assertThat(decision.isShortCircuited()).isTrue();
        assertThat(decision.shortCircuitResponse()).containsEntry(MapFieldKeys.OVERFLOW, "request_retry");
        verify(ctxStore, never()).acquireDedup(any(), any(), any(), any());
    }

    @Test
    void lowPriorityOverflowDropsAndReleasesDedupKey() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.acquireDedup(eq(10L), eq("user-1"), eq("msg-1"), any(Duration.class)))
                .thenReturn(true);
        when(ctxStore.buildDedupKey(10L, "user-1", "msg-1")).thenReturn("dedup-key");
        InFlightExecutionRegistry registry = mock(InFlightExecutionRegistry.class);
        when(registry.activeCount(10L)).thenReturn(50);
        TriggerPriorityConfig priorityConfig = new TriggerPriorityConfig();
        priorityConfig.setLowRatio(0.5);
        TriggerAdmissionService service = new TriggerAdmissionService(
                ctxStore,
                mock(TriggerPreCheckService.class),
                registry,
                priorityConfig,
                new ExecutionLaneResolver(),
                mock(RocketMQTemplate.class),
                new ObjectMapper(),
                mock(CanvasExecutionMapper.class),
                mock(CanvasExecutionDlqMapper.class),
                new Snowflake(1, 1));

        TriggerAdmissionService.AdmissionDecision decision = service.evaluate(request(
                TriggerType.SCHEDULED, NodeType.SCHEDULED_TRIGGER, Map.of(), "msg-1",
                false, false, false, 0, 0));

        assertThat(decision.isShortCircuited()).isTrue();
        assertThat(decision.shortCircuitResponse()).containsEntry(MapFieldKeys.OVERFLOW, "dropped_low_priority");
        verify(ctxStore).releaseDedup("dedup-key");
    }

    @Test
    void sanitizePayloadRemovesInternalOverflowRetryCounter() {
        TriggerAdmissionService service = service(mock(ContextPersistenceService.class),
                mock(TriggerPreCheckService.class), mock(InFlightExecutionRegistry.class),
                mock(CanvasExecutionMapper.class), mock(CanvasExecutionDlqMapper.class));

        Map<String, Object> sanitized = service.sanitizePayload(Map.of(
                "business", "value",
                org.chovy.canvas.infrastructure.mq.OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY, 3));

        assertThat(sanitized).containsEntry("business", "value");
        assertThat(sanitized).doesNotContainKey(
                org.chovy.canvas.infrastructure.mq.OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY);
    }

    private TriggerAdmissionService service(
            ContextPersistenceService ctxStore,
            TriggerPreCheckService preCheckService,
            InFlightExecutionRegistry registry,
            CanvasExecutionMapper executionMapper,
            CanvasExecutionDlqMapper dlqMapper) {
        return new TriggerAdmissionService(
                ctxStore,
                preCheckService,
                registry,
                new TriggerPriorityConfig(),
                new ExecutionLaneResolver(),
                mock(RocketMQTemplate.class),
                new ObjectMapper(),
                executionMapper,
                dlqMapper,
                new Snowflake(1, 1));
    }

    private TriggerAdmissionService.AdmissionRequest request(
            String triggerType,
            String triggerNodeType,
            Map<String, Object> payload,
            String msgId,
            boolean dryRun,
            boolean overflowRetry,
            boolean persistentRequest,
            int priorAttemptCount,
            int overflowChainRetryCount) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setTenantId(1L);
        return new TriggerAdmissionService.AdmissionRequest(
                10L,
                "user-1",
                triggerType,
                triggerNodeType,
                null,
                payload,
                msgId,
                dryRun,
                overflowRetry,
                overflowChainRetryCount,
                persistentRequest,
                priorAttemptCount,
                canvas,
                100,
                600L,
                null);
    }
}
