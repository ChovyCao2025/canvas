package org.chovy.canvas.engine.wait;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.core.lang.Snowflake;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.config.ExecutionLaneProperties;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.dataobject.CanvasWaitSubscriptionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneAdmissionResult;
import org.chovy.canvas.engine.lane.ExecutionLaneResolver;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.engine.trigger.CanvasExecutionConfigLoader;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.ExecutionLaneDispatcher;
import org.chovy.canvas.engine.trigger.InFlightExecutionRegistry;
import org.chovy.canvas.engine.trigger.TriggerAdmissionService;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.engine.trigger.TriggerPriorityConfig;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.when;

class WaitResumeServiceTest {

    @Test
    void resumeEventWaitsSkipsSubscriptionsWhoseEventFiltersDoNotMatch() {
        WaitSubscriptionService waitSubscriptionService = mock(WaitSubscriptionService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        WaitResumeService service = new WaitResumeService(
                waitSubscriptionService, executionService, new ObjectMapper());
        CanvasWaitSubscriptionDO wait = waitRecord();
        wait.setEventFilters("{\"amount\":{\"gt\":100}}");
        when(waitSubscriptionService.findActiveEventWaits("ORDER_PAID", "user-1")).thenReturn(List.of(wait));

        int resumed = service.resumeEventWaits("ORDER_PAID", "user-1", Map.of("amount", 50), "evt-1");

        assertThat(resumed).isZero();
        verify(waitSubscriptionService, never()).completeWait(any(), any());
        verify(executionService, never()).trigger(any(), any(), any(), any(), any(), any(), any(), eq(false));
    }

    @Test
    void resumeEventWaitsCompletesAndTriggersWhenEventFiltersMatch() {
        WaitSubscriptionService waitSubscriptionService = mock(WaitSubscriptionService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        WaitResumeService service = new WaitResumeService(
                waitSubscriptionService, executionService, new ObjectMapper());
        CanvasWaitSubscriptionDO wait = waitRecord();
        wait.setEventFilters("{\"amount\":{\"gt\":100}}");
        when(waitSubscriptionService.findActiveEventWaits("ORDER_PAID", "user-1")).thenReturn(List.of(wait));
        when(waitSubscriptionService.completeWait(eq(1L), any())).thenReturn(1);
        when(executionService.trigger(any(), any(), any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(Mono.just(Map.of()));

        int resumed = service.resumeEventWaits("ORDER_PAID", "user-1", Map.of("amount", 150), "evt-2");

        assertThat(resumed).isEqualTo(1);
        verify(executionService).trigger(eq(10L), eq("user-1"), eq(TriggerType.WAIT_RESUME),
                eq(NodeType.WAIT), eq("wait-1"), any(), any(), eq(false));
    }

    @Test
    void waitResumeBypassesQuotaAndCooldown() {
        WaitSubscriptionService waitSubscriptionService = mock(WaitSubscriptionService.class);
        TriggerPreCheckService preCheckService = mock(TriggerPreCheckService.class);
        CanvasExecutionHarness harness = executionHarness(preCheckService, NodeType.WAIT, "wait-1");
        WaitResumeService service = new WaitResumeService(
                waitSubscriptionService, harness.executionService(), new ObjectMapper());
        CanvasWaitSubscriptionDO wait = waitRecord();
        when(waitSubscriptionService.findActiveEventWaits("ORDER_PAID", "user-1")).thenReturn(List.of(wait));
        when(waitSubscriptionService.completeWait(eq(1L), any())).thenReturn(1);

        int resumed = service.resumeEventWaits("ORDER_PAID", "user-1", Map.of("amount", 150), "evt-2");

        assertThat(resumed).isEqualTo(1);
        verify(harness.executionMapper(), timeout(2_000).atLeastOnce()).update(any(CanvasExecutionDO.class), any());
        verify(preCheckService, never()).checkWithoutQuotaAccounting(any(), any());
        verify(preCheckService, never()).consumeQuotaAndRecord(any(), any());
    }

    private CanvasWaitSubscriptionDO waitRecord() {
        CanvasWaitSubscriptionDO wait = new CanvasWaitSubscriptionDO();
        wait.setId(1L);
        wait.setExecutionId("exec-1");
        wait.setCanvasId(10L);
        wait.setUserId("user-1");
        wait.setNodeId("wait-1");
        wait.setWaitType(WaitSubscriptionService.WAIT_TYPE_EVENT);
        wait.setEventCode("ORDER_PAID");
        wait.setResumePayload("{}");
        return wait;
    }

    private CanvasExecutionHarness executionHarness(
            TriggerPreCheckService preCheckService,
            String nodeType,
            String nodeId
    ) {
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        CanvasEntityCache canvasEntityCache = mock(CanvasEntityCache.class);
        CanvasConfigCache configCache = mock(CanvasConfigCache.class);
        InFlightExecutionRegistry executionRegistry = mock(InFlightExecutionRegistry.class);
        DagEngine dagEngine = mock(DagEngine.class);
        CdpUserService cdpUserService = mock(CdpUserService.class);

        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setStatus(1);
        canvas.setPublishedVersionId(100L);
        when(canvasEntityCache.get(10L)).thenReturn(canvas);

        ExecutionContext storedCtx = new ExecutionContext();
        storedCtx.setExecutionId("exec-1");
        storedCtx.setCanvasId(10L);
        storedCtx.setVersionId(100L);
        storedCtx.setUserId("user-1");
        when(ctxStore.exists(10L, "user-1")).thenReturn(true);
        when(ctxStore.acquireResumeLock(eq(10L), eq("user-1"), any(), anyLong())).thenReturn(true);
        when(ctxStore.load(10L, "user-1")).thenReturn(storedCtx);

        DagParser.CanvasNode waitNode = new DagParser.CanvasNode();
        waitNode.setId(nodeId);
        waitNode.setType(nodeType);
        DagGraph graph = new DagGraph(
                Map.of(nodeId, waitNode),
                Map.of(nodeId, List.of()),
                Map.of(nodeId, List.of()),
                Map.of(nodeId, 0));
        when(configCache.get(10L, 100L)).thenReturn(graph);
        when(executionRegistry.activeCount(10L)).thenReturn(0);
        when(executionRegistry.tryAcquire(eq(10L), eq("exec-1"), eq(ExecutionLane.LIGHT),
                anyInt(), anyInt(), anyInt()))
                .thenReturn(ExecutionLaneAdmissionResult.allowed(Disposables.swap(), 1, 1, 1));
        when(dagEngine.execute(eq(graph), eq(nodeId), any(ExecutionContext.class)))
                .thenReturn(Mono.just(Map.of("resumed", true)));
        when(executionMapper.update(any(CanvasExecutionDO.class), any())).thenReturn(1);

        CanvasExecutionConfigLoader configLoaderForExecution = new CanvasExecutionConfigLoader(
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class),
                configCache,
                canvasEntityCache,
                mock(DagParser.class),
                mock(MqTriggerHandler.class));
        TriggerAdmissionService admissionService = new TriggerAdmissionService(
                ctxStore,
                preCheckService,
                executionRegistry,
                new TriggerPriorityConfig(),
                new ExecutionLaneResolver(),
                mock(RocketMQTemplate.class),
                new ObjectMapper(),
                executionMapper,
                mock(CanvasExecutionDlqMapper.class),
                new Snowflake(1, 1));
        CanvasExecutionService executionService = new CanvasExecutionService(
                executionMapper,
                configLoaderForExecution,
                ctxStore,
                dagEngine,
                preCheckService,
                executionRegistry,
                mock(CanvasExecutionStatsMapper.class),
                admissionService,
                new ExecutionLaneProperties(),
                new ExecutionLaneDispatcher(executionRegistry, ctxStore),
                new ObjectMapper(),
                cdpUserService,
                mock(CanvasDisruptorService.class),
                mock(StringRedisTemplate.class),
                mock(RedisKeyUtil.class));
        ReflectionTestUtils.setField(executionService, "ctxTtlSec", 86_400L);
        ReflectionTestUtils.setField(executionService, "globalTimeoutSec", 600L);
        ReflectionTestUtils.setField(executionService, "globalMaxConcurrency", 3_000);

        return new CanvasExecutionHarness(executionService, executionMapper);
    }

    private record CanvasExecutionHarness(
            CanvasExecutionService executionService,
            CanvasExecutionMapper executionMapper
    ) {
    }
}
