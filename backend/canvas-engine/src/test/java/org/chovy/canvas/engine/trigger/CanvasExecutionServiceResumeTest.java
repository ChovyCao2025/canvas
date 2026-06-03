package org.chovy.canvas.engine.trigger;

import cn.hutool.core.lang.Snowflake;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.config.ExecutionLaneProperties;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.engine.concurrent.BackgroundTaskExecutor;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.lane.ExecutionLaneResolver;
import org.chovy.canvas.engine.lifecycle.ExecutionLifecycleException;
import org.chovy.canvas.engine.lifecycle.ExecutionLifecycleGate;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    void triggerRejectsNewDirectCallBeforeLoadingCanvasAfterShutdownBegins() {
        ExecutionLifecycleGate gate = new ExecutionLifecycleGate();
        CanvasEntityCache canvasEntityCache = mock(CanvasEntityCache.class);
        CanvasExecutionService service = service(
                mock(CanvasExecutionMapper.class),
                mock(ContextPersistenceService.class),
                gate,
                canvasEntityCache);
        gate.beginShutdown();

        assertThatThrownBy(() -> service.trigger(
                        11L,
                        "user-1",
                        NodeType.DIRECT_CALL,
                        NodeType.DIRECT_CALL,
                        null,
                        java.util.Map.of(),
                        "dedup-1",
                        false)
                .block())
                .isInstanceOf(ExecutionLifecycleException.class)
                .hasMessageContaining("canvas-trigger:DIRECT_CALL");

        verify(canvasEntityCache, never()).get(any());
    }

    @Test
    void triggerFromDisruptorTreatsQueuedEventAsAlreadyAcceptedDuringShutdownDrain() {
        ExecutionLifecycleGate gate = new ExecutionLifecycleGate();
        CanvasEntityCache canvasEntityCache = mock(CanvasEntityCache.class);
        CanvasExecutionService service = service(
                mock(CanvasExecutionMapper.class),
                mock(ContextPersistenceService.class),
                gate,
                canvasEntityCache);
        gate.beginShutdown();

        assertThatThrownBy(() -> service.triggerFromDisruptor(
                        11L,
                        "user-1",
                        NodeType.MQ_TRIGGER,
                        NodeType.MQ_TRIGGER,
                        "order.paid",
                        java.util.Map.of(),
                        "msg-1",
                        null)
                .block())
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(ExecutionLifecycleException.class)
                .hasMessageContaining("画布不存在");

        verify(canvasEntityCache).get(11L);
    }

    private CanvasExecutionService service(CanvasExecutionMapper mapper, ContextPersistenceService ctxStore) {
        return service(mapper, ctxStore, new ExecutionLifecycleGate(), mock(CanvasEntityCache.class));
    }

    private CanvasExecutionService service(CanvasExecutionMapper mapper,
                                           ContextPersistenceService ctxStore,
                                           ExecutionLifecycleGate lifecycleGate,
                                           CanvasEntityCache canvasEntityCache) {
        return new CanvasExecutionService(
                mock(org.chovy.canvas.dal.mapper.CanvasMapper.class),
                mock(org.chovy.canvas.dal.mapper.CanvasVersionMapper.class),
                mapper,
                mock(CanvasConfigCache.class),
                mock(DagParser.class),
                ctxStore,
                mock(DagEngine.class),
                mock(TriggerPreCheckService.class),
                mock(InFlightExecutionRegistry.class),
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
                new Snowflake(1, 1),
                mock(BackgroundTaskExecutor.class),
                lifecycleGate);
    }
}
