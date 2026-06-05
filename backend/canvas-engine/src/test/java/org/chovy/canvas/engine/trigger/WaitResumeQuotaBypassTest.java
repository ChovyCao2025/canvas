package org.chovy.canvas.engine.trigger;

import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneResolver;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WaitResumeQuotaBypassTest {

    @Test
    void waitResumeWithStoredContextBypassesQuotaPreCheckAndDedup() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(10L, "user-1")).thenReturn(true);
        TriggerPreCheckService preCheckService = mock(TriggerPreCheckService.class);
        InFlightExecutionRegistry registry = mock(InFlightExecutionRegistry.class);
        when(registry.activeCount(10L)).thenReturn(0);
        TriggerAdmissionService service = service(ctxStore, preCheckService, registry);

        TriggerAdmissionService.AdmissionDecision decision = service.evaluate(request(
                TriggerType.WAIT_RESUME, NodeType.WAIT, "exec-1:wait:7:COMPLETED"));

        assertThat(decision.isShortCircuited()).isFalse();
        assertThat(decision.isResume()).isTrue();
        assertThat(decision.quotaBypass()).isTrue();
        assertThat(decision.executionLane()).isEqualTo(ExecutionLane.LIGHT);
        verify(preCheckService, never()).checkWithoutQuotaAccounting(any(), any());
        verify(ctxStore, never()).acquireDedup(any(), any(), any(), any());
    }

    @Test
    void hubTimeoutWithStoredContextBypassesQuotaPreCheckAndDedup() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(10L, "user-1")).thenReturn(true);
        TriggerPreCheckService preCheckService = mock(TriggerPreCheckService.class);
        InFlightExecutionRegistry registry = mock(InFlightExecutionRegistry.class);
        when(registry.activeCount(10L)).thenReturn(0);
        TriggerAdmissionService service = service(ctxStore, preCheckService, registry);

        TriggerAdmissionService.AdmissionDecision decision = service.evaluate(request(
                TriggerType.HUB_TIMEOUT, NodeType.HUB, "exec-1:hub:timeout"));

        assertThat(decision.isShortCircuited()).isFalse();
        assertThat(decision.isResume()).isTrue();
        assertThat(decision.quotaBypass()).isTrue();
        assertThat(decision.executionLane()).isEqualTo(ExecutionLane.LIGHT);
        verify(preCheckService, never()).checkWithoutQuotaAccounting(any(), eq("user-1"));
        verify(ctxStore, never()).acquireDedup(any(), any(), any(), any());
    }

    private TriggerAdmissionService service(
            ContextPersistenceService ctxStore,
            TriggerPreCheckService preCheckService,
            InFlightExecutionRegistry registry) {
        return new TriggerAdmissionService(
                ctxStore,
                preCheckService,
                registry,
                new TriggerPriorityConfig(),
                new ExecutionLaneResolver(),
                mock(RocketMQTemplate.class),
                new ObjectMapper(),
                mock(CanvasExecutionMapper.class),
                mock(CanvasExecutionDlqMapper.class),
                new Snowflake(1, 1));
    }

    private TriggerAdmissionService.AdmissionRequest request(
            String triggerType,
            String triggerNodeType,
            String msgId) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setTenantId(1L);
        return new TriggerAdmissionService.AdmissionRequest(
                10L,
                "user-1",
                triggerType,
                triggerNodeType,
                null,
                Map.of(),
                msgId,
                false,
                false,
                0,
                false,
                0,
                canvas,
                100,
                600L,
                null);
    }
}
