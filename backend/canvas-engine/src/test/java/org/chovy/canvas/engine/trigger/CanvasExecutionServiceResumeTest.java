package org.chovy.canvas.engine.trigger;

import cn.hutool.core.lang.Snowflake;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.config.ExecutionLaneProperties;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
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

import static org.assertj.core.api.Assertions.assertThat;
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

    private CanvasExecutionService service(CanvasExecutionMapper mapper, ContextPersistenceService ctxStore) {
        return service(mapper, ctxStore, mock(InFlightExecutionRegistry.class));
    }

    private CanvasExecutionService service(CanvasExecutionMapper mapper,
                                           ContextPersistenceService ctxStore,
                                           InFlightExecutionRegistry registry) {
        return new CanvasExecutionService(
                mock(org.chovy.canvas.dal.mapper.CanvasMapper.class),
                mock(org.chovy.canvas.dal.mapper.CanvasVersionMapper.class),
                mapper,
                mock(CanvasConfigCache.class),
                mock(DagParser.class),
                ctxStore,
                mock(DagEngine.class),
                mock(TriggerPreCheckService.class),
                registry,
                mock(org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper.class),
                mock(CanvasEntityCache.class),
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
    }
}
