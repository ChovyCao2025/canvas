package org.chovy.canvas.engine.trigger;

import cn.hutool.core.lang.Snowflake;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.config.ExecutionLaneProperties;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.lane.ExecutionLaneResolver;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.engine.scheduler.NodeStatePersistenceException;
import org.chovy.canvas.engine.scheduler.SpecialNodeTimeoutFailureException;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionServiceResumeTest {

    @Test
    void directCallDoesNotResumeExistingContext() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(11L, "user-1")).thenReturn(true);
        CanvasExecutionService service = service(mock(CanvasExecutionMapper.class), ctxStore);

        Boolean shouldResume = ReflectionTestUtils.invokeMethod(
                service,
                "shouldResumeExistingContext",
                11L,
                "user-1",
                false,
                false);

        assertThat(shouldResume).isFalse();
    }

    @Test
    void internalContinuationCanResumeExistingContext() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(11L, "user-1")).thenReturn(true);
        CanvasExecutionService service = service(mock(CanvasExecutionMapper.class), ctxStore);

        Boolean shouldResume = ReflectionTestUtils.invokeMethod(
                service,
                "shouldResumeExistingContext",
                11L,
                "user-1",
                false,
                true);

        assertThat(shouldResume).isTrue();
    }

    @Test
    void resumeStartUpdatesPausedExecutionInsteadOfInsertingDuplicateId() {
        CanvasExecutionMapper mapper = mock(CanvasExecutionMapper.class);
        when(mapper.update(any(), any())).thenReturn(1);
        CanvasExecutionService service = service(mapper, mock(ContextPersistenceService.class));
        CanvasExecutionDO exec = new CanvasExecutionDO();
        exec.setId("existing-exec-id");
        exec.setStatus(ExecutionStatus.RUNNING.getCode());

        Mono<Void> mono = ReflectionTestUtils.invokeMethod(service, "persistExecutionStart", exec, true);
        mono.block();

        verify(mapper, never()).insert(any(CanvasExecutionDO.class));
        verify(mapper).update(any(CanvasExecutionDO.class), any());
    }

    @Test
    void normalAdmissionUsesConfiguredPerCanvasLimitInsteadOfGlobalLimit() {
        InFlightExecutionRegistry registry = mock(InFlightExecutionRegistry.class);
        when(registry.activeCount(11L)).thenReturn(0);
        CanvasExecutionService service = service(
                mock(CanvasExecutionMapper.class),
                mock(ContextPersistenceService.class),
                registry);
        ReflectionTestUtils.setField(service, "globalMaxConcurrency", 3000);

        Object admission = ReflectionTestUtils.invokeMethod(
                service,
                "resolveAdmission",
                11L,
                "user-1",
                "MQ",
                "MQ_TRIGGER",
                "topic-a",
                Map.of(),
                "msg-1",
                new CanvasDO(),
                0,
                false);

        assertThat(ReflectionTestUtils.getField(admission, "admissionLimit")).isEqualTo(50);
    }

    @Test
    void nodeStatePersistenceErrorWithWaitingNodeDoesNotSavePausedContext() {
        CanvasExecutionMapper mapper = mock(CanvasExecutionMapper.class);
        when(mapper.update(any(CanvasExecutionDO.class), any())).thenReturn(1);
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        CanvasExecutionService service = service(mapper, ctxStore);
        CanvasExecutionDO exec = new CanvasExecutionDO();
        exec.setId("exec-paused-fail");
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-paused-fail");
        ctx.setCanvasId(11L);
        ctx.setUserId("user-1");
        ctx.setNodeStatus("wait", NodeStatus.WAITING);
        Throwable error = new NodeStatePersistenceException(
                "Failed to persist node state before releasing Redis gate",
                new RuntimeException("redis down"));

        Mono<Map<String, Object>> mono = ReflectionTestUtils.invokeMethod(
                service, "handleError", error, exec, ctx, mock(DagGraph.class), false, false);
        Map<String, Object> response = mono.block();

        assertThat(response).containsEntry("executionId", "exec-paused-fail");
        verify(ctxStore, never()).save(ctx);
        verify(ctxStore).delete(11L, "user-1");
        ArgumentCaptor<CanvasExecutionDO> update = ArgumentCaptor.forClass(CanvasExecutionDO.class);
        verify(mapper).update(update.capture(), any());
        assertThat(update.getValue().getStatus()).isEqualTo(ExecutionStatus.FAILED.getCode());
    }

    @Test
    void specialTimeoutTerminalFailureWithWaitingNodeDoesNotSavePausedContext() {
        CanvasExecutionMapper mapper = mock(CanvasExecutionMapper.class);
        when(mapper.update(any(CanvasExecutionDO.class), any())).thenReturn(1);
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        CanvasExecutionService service = service(mapper, ctxStore);
        CanvasExecutionDO exec = new CanvasExecutionDO();
        exec.setId("exec-timeout-fail");
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-timeout-fail");
        ctx.setCanvasId(11L);
        ctx.setUserId("user-1");
        ctx.setNodeStatus("hub", NodeStatus.TIMEOUT);
        ctx.setNodeStatus("other", NodeStatus.WAITING);
        Throwable error = new SpecialNodeTimeoutFailureException("HUB 等待超时且未配置超时分支 nodeId=hub");

        Mono<Map<String, Object>> mono = ReflectionTestUtils.invokeMethod(
                service, "handleError", error, exec, ctx, mock(DagGraph.class), false, false);
        Map<String, Object> response = mono.block();

        assertThat(response).containsEntry("executionId", "exec-timeout-fail");
        verify(ctxStore, never()).save(ctx);
        verify(ctxStore).delete(11L, "user-1");
        ArgumentCaptor<CanvasExecutionDO> update = ArgumentCaptor.forClass(CanvasExecutionDO.class);
        verify(mapper).update(update.capture(), any());
        assertThat(update.getValue().getStatus()).isEqualTo(ExecutionStatus.FAILED.getCode());
    }

    @Test
    void specialTimeoutWithoutExistingContextIsSkipped() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(11L, "user-1")).thenReturn(false);
        CanvasConfigCache configCache = mock(CanvasConfigCache.class);
        CanvasEntityCache canvasEntityCache = mock(CanvasEntityCache.class);
        when(canvasEntityCache.get(11L)).thenReturn(publishedCanvas());
        CanvasExecutionService service = service(mock(CanvasExecutionMapper.class), ctxStore,
                mock(InFlightExecutionRegistry.class), configCache, canvasEntityCache);

        Map<String, Object> prep = prepareHubTimeout(service, timeoutPayload("old-exec", 1L));

        assertThat(prep).containsEntry(MapFieldKeys.SKIPPED, "stale-timeout");
        assertThat(prep).doesNotContainKey(MapFieldKeys.CTX);
        verify(ctxStore).exists(11L, "user-1");
        verify(ctxStore, never()).acquireResumeLock(any(), anyString(), anyString(), anyLong());
        verify(ctxStore, never()).load(any(), anyString());
        verify(configCache, never()).get(any(), any());
    }

    @Test
    void specialTimeoutWithExecutionIdMismatchIsSkipped() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(11L, "user-1")).thenReturn(true);
        when(ctxStore.acquireResumeLock(any(), anyString(), anyString(), anyLong())).thenReturn(true);
        when(ctxStore.load(11L, "user-1")).thenReturn(context("current-exec", 1L));
        CanvasConfigCache configCache = mock(CanvasConfigCache.class);
        CanvasEntityCache canvasEntityCache = mock(CanvasEntityCache.class);
        when(canvasEntityCache.get(11L)).thenReturn(publishedCanvas());
        CanvasExecutionService service = service(mock(CanvasExecutionMapper.class), ctxStore,
                mock(InFlightExecutionRegistry.class), configCache, canvasEntityCache);

        Map<String, Object> prep = prepareHubTimeout(service, timeoutPayload("old-exec", 1L));

        assertThat(prep).containsEntry(MapFieldKeys.SKIPPED, "stale-timeout");
        assertThat(prep).doesNotContainKey(MapFieldKeys.CTX);
        verify(ctxStore).load(11L, "user-1");
        verify(ctxStore).releaseResumeLock(any(), anyString(), anyString());
        verify(configCache, never()).get(any(), any());
    }

    @Test
    void specialTimeoutWithVersionIdMismatchIsSkipped() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(11L, "user-1")).thenReturn(true);
        when(ctxStore.acquireResumeLock(any(), anyString(), anyString(), anyLong())).thenReturn(true);
        when(ctxStore.load(11L, "user-1")).thenReturn(context("exec-timeout", 2L));
        CanvasConfigCache configCache = mock(CanvasConfigCache.class);
        CanvasEntityCache canvasEntityCache = mock(CanvasEntityCache.class);
        when(canvasEntityCache.get(11L)).thenReturn(publishedCanvas());
        CanvasExecutionService service = service(mock(CanvasExecutionMapper.class), ctxStore,
                mock(InFlightExecutionRegistry.class), configCache, canvasEntityCache);

        Map<String, Object> prep = prepareHubTimeout(service, timeoutPayload("exec-timeout", 1L));

        assertThat(prep).containsEntry(MapFieldKeys.SKIPPED, "stale-timeout");
        assertThat(prep).doesNotContainKey(MapFieldKeys.CTX);
        verify(ctxStore).load(11L, "user-1");
        verify(ctxStore).releaseResumeLock(any(), anyString(), anyString());
        verify(configCache, never()).get(any(), any());
    }

    private CanvasExecutionService service(CanvasExecutionMapper mapper, ContextPersistenceService ctxStore) {
        return service(mapper, ctxStore, mock(InFlightExecutionRegistry.class));
    }

    private CanvasExecutionService service(CanvasExecutionMapper mapper,
                                           ContextPersistenceService ctxStore,
                                           InFlightExecutionRegistry registry) {
        return service(mapper, ctxStore, registry, mock(CanvasConfigCache.class), mock(CanvasEntityCache.class));
    }

    private CanvasExecutionService service(CanvasExecutionMapper mapper,
                                           ContextPersistenceService ctxStore,
                                           InFlightExecutionRegistry registry,
                                           CanvasConfigCache configCache,
                                           CanvasEntityCache canvasEntityCache) {
        CanvasExecutionService service = new CanvasExecutionService(
                mock(org.chovy.canvas.dal.mapper.CanvasMapper.class),
                mock(org.chovy.canvas.dal.mapper.CanvasVersionMapper.class),
                mapper,
                configCache,
                mock(DagParser.class),
                ctxStore,
                mock(DagEngine.class),
                mock(TriggerPreCheckService.class),
                registry,
                mock(org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper.class),
                canvasEntityCache,
                mock(MqTriggerHandler.class),
                mock(org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper.class),
                new TriggerPriorityConfig(),
                new ExecutionLaneResolver(),
                new ExecutionLaneProperties(),
                mock(RocketMQTemplate.class),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(CdpUserService.class),
                mock(CanvasDisruptorService.class),
                mock(org.springframework.data.redis.core.StringRedisTemplate.class),
                mock(org.chovy.canvas.infrastructure.redis.RedisKeyUtil.class),
                new Snowflake(1, 1));
        ReflectionTestUtils.setField(service, "globalMaxConcurrency", 3000);
        ReflectionTestUtils.setField(service, "globalTimeoutSec", 600L);
        return service;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> prepareHubTimeout(CanvasExecutionService service, Map<String, Object> payload) {
        return (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                service,
                "prepareExecution",
                11L,
                "user-1",
                TriggerType.HUB_TIMEOUT,
                NodeType.HUB,
                "hub",
                payload,
                "timeout-msg",
                false,
                false,
                0,
                false,
                0,
                null,
                null);
    }

    private Map<String, Object> timeoutPayload(String executionId, Long versionId) {
        return Map.of(
                MapFieldKeys.EXECUTION_ID, executionId,
                MapFieldKeys.VERSION_ID, versionId,
                MapFieldKeys.TIMEOUT_TIMER_KEY, "hub",
                MapFieldKeys.TIMEOUT_SCHEDULED_AT_EPOCH_MS, 1_000L,
                MapFieldKeys.TIMEOUT_FIRE_AT_EPOCH_MS, 8_000L,
                MapFieldKeys.TIMEOUT_SECONDS, 7L);
    }

    private ExecutionContext context(String executionId, Long versionId) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId(executionId);
        ctx.setCanvasId(11L);
        ctx.setVersionId(versionId);
        ctx.setUserId("user-1");
        return ctx;
    }

    private CanvasDO publishedCanvas() {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(11L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(1L);
        return canvas;
    }
}
