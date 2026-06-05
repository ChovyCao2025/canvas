package org.chovy.canvas.engine.trigger;

import cn.hutool.core.lang.Snowflake;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.config.ExecutionLaneProperties;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.lane.ExecutionLaneResolver;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

class CanvasExecutionServiceResumeTest {

    @Test
    void directCallDoesNotResumeExistingContext() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(11L, "user-1")).thenReturn(true);
        TriggerAdmissionService admissionService = admissionService(
                ctxStore,
                mock(CanvasExecutionMapper.class),
                mock(TriggerPreCheckService.class),
                mock(InFlightExecutionRegistry.class));

        boolean shouldResume = admissionService.shouldResumeExistingContext(
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
        TriggerAdmissionService admissionService = admissionService(
                ctxStore,
                mock(CanvasExecutionMapper.class),
                mock(TriggerPreCheckService.class),
                mock(InFlightExecutionRegistry.class));

        boolean shouldResume = admissionService.shouldResumeExistingContext(
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
    void internalContinuationWithoutRedisContextIsSkippedAndMarksPausedExecutionFailed() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(11L, "user-1")).thenReturn(false);
        CanvasExecutionMapper mapper = mock(CanvasExecutionMapper.class);
        when(mapper.update(any(), any())).thenReturn(1);
        CanvasExecutionService service = preparedService(mapper, ctxStore);

        Map<String, ?> prep = ReflectionTestUtils.invokeMethod(service, "prepareExecution",
                11L, "user-1", TriggerType.WAIT_RESUME, NodeType.WAIT, "wait-1", Map.of(),
                "exec-1:wait:7:COMPLETED", false, false, 0, false, 0, null, null);

        assertThat(prep.get("skipped")).isEqualTo("missing-context");
        verify(mapper).update(any(CanvasExecutionDO.class), any());
        verify(ctxStore, never()).acquireResumeLock(any(), any(), any(), anyLong());
    }

    @Test
    void internalContinuationReleasesResumeLockWhenRedisContextDisappearsAfterExistsCheck() {
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        when(ctxStore.exists(11L, "user-1")).thenReturn(true);
        when(ctxStore.acquireResumeLock(any(), any(), anyString(), anyLong())).thenReturn(true);
        when(ctxStore.load(11L, "user-1")).thenReturn(null);
        CanvasExecutionMapper mapper = mock(CanvasExecutionMapper.class);
        when(mapper.update(any(), any())).thenReturn(1);
        CanvasExecutionService service = preparedService(mapper, ctxStore);

        Map<String, ?> prep = ReflectionTestUtils.invokeMethod(service, "prepareExecution",
                11L, "user-1", TriggerType.WAIT_RESUME, NodeType.WAIT, "wait-1", Map.of(),
                "exec-1:wait:7:COMPLETED", false, false, 0, false, 0, null, null);

        assertThat(prep.get("skipped")).isEqualTo("missing-context");
        verify(ctxStore).releaseResumeLock(eq(11L), eq("user-1"), anyString());
        verify(mapper).update(any(CanvasExecutionDO.class), any());
    }

    @Test
    void triggerRejectsBeforeLoadingCanvasWhenLifecycleGateIsClosed() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasExecutionService service = service(canvasMapper, mock(CanvasExecutionMapper.class),
                mock(ContextPersistenceService.class));
        ExecutionLifecycleGate gate = new ExecutionLifecycleGate(0);
        gate.shutdown();
        service.setLifecycleGate(gate);

        assertThatThrownBy(() -> service.trigger(
                        11L,
                        "user-1",
                        TriggerType.DIRECT_CALL,
                        NodeType.DIRECT_CALL,
                        null,
                        Map.of(),
                        "msg-1",
                        false).block())
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessageContaining("shutting down");
        verifyNoInteractions(canvasMapper);
    }

    private CanvasExecutionService service(CanvasExecutionMapper mapper, ContextPersistenceService ctxStore) {
        return service(mock(org.chovy.canvas.dal.mapper.CanvasMapper.class), mapper, ctxStore);
    }

    private CanvasExecutionService service(CanvasMapper canvasMapper,
                                           CanvasExecutionMapper mapper,
                                           ContextPersistenceService ctxStore) {
        CanvasExecutionConfigLoader configLoader = new CanvasExecutionConfigLoader(
                canvasMapper,
                mock(org.chovy.canvas.dal.mapper.CanvasVersionMapper.class),
                mock(CanvasConfigCache.class),
                mock(CanvasEntityCache.class),
                mock(DagParser.class),
                mock(MqTriggerHandler.class));
        InFlightExecutionRegistry executionRegistry = mock(InFlightExecutionRegistry.class);
        TriggerPreCheckService preCheckService = mock(TriggerPreCheckService.class);
        return new CanvasExecutionService(
                mapper,
                configLoader,
                ctxStore,
                mock(DagEngine.class),
                preCheckService,
                executionRegistry,
                mock(org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper.class),
                admissionService(ctxStore, mapper, preCheckService, executionRegistry),
                new ExecutionLaneProperties(),
                new ExecutionLaneDispatcher(executionRegistry, ctxStore),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(CdpUserService.class),
                mock(CanvasDisruptorService.class),
                mock(org.springframework.data.redis.core.StringRedisTemplate.class),
                mock(org.chovy.canvas.infrastructure.redis.RedisKeyUtil.class));
    }

    private CanvasExecutionService preparedService(CanvasExecutionMapper mapper, ContextPersistenceService ctxStore) {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasEntityCache canvasEntityCache = mock(CanvasEntityCache.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(11L);
        canvas.setStatus(org.chovy.canvas.common.enums.CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(100L);
        when(canvasEntityCache.get(11L)).thenReturn(canvas);
        CanvasExecutionConfigLoader configLoader = new CanvasExecutionConfigLoader(
                canvasMapper,
                mock(org.chovy.canvas.dal.mapper.CanvasVersionMapper.class),
                mock(CanvasConfigCache.class),
                canvasEntityCache,
                mock(DagParser.class),
                mock(MqTriggerHandler.class));
        InFlightExecutionRegistry executionRegistry = mock(InFlightExecutionRegistry.class);
        TriggerPreCheckService preCheckService = mock(TriggerPreCheckService.class);
        CanvasExecutionService service = new CanvasExecutionService(
                mapper,
                configLoader,
                ctxStore,
                mock(DagEngine.class),
                preCheckService,
                executionRegistry,
                mock(org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper.class),
                admissionService(ctxStore, mapper, preCheckService, executionRegistry),
                new ExecutionLaneProperties(),
                new ExecutionLaneDispatcher(executionRegistry, ctxStore),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(CdpUserService.class),
                mock(CanvasDisruptorService.class),
                mock(org.springframework.data.redis.core.StringRedisTemplate.class),
                mock(org.chovy.canvas.infrastructure.redis.RedisKeyUtil.class));
        ReflectionTestUtils.setField(service, "globalMaxConcurrency", 100);
        ReflectionTestUtils.setField(service, "globalTimeoutSec", 600L);
        return service;
    }

    private TriggerAdmissionService admissionService(ContextPersistenceService ctxStore,
                                                     CanvasExecutionMapper mapper,
                                                     TriggerPreCheckService preCheckService,
                                                     InFlightExecutionRegistry executionRegistry) {
        return new TriggerAdmissionService(
                ctxStore,
                preCheckService,
                executionRegistry,
                new TriggerPriorityConfig(),
                new ExecutionLaneResolver(),
                mock(RocketMQTemplate.class),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mapper,
                mock(org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper.class),
                new Snowflake(1, 1));
    }
}
