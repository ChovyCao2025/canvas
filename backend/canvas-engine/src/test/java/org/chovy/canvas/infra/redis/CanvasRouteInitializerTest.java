package org.chovy.canvas.infrastructure.redis;

import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasRouteInitializerTest {

    @Test
    void initTriggerRoutesUsesManagedWaiterWhenAnotherInstanceHoldsRebuildLock() {
        TriggerRouteService routeService = mock(TriggerRouteService.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        AtomicInteger waitCalls = new AtomicInteger();
        when(routeService.isRouteTableEmpty()).thenReturn(true);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("canvas:route-init:lock"), any(), any(Duration.class))).thenReturn(false);
        CanvasRouteInitializer initializer = new CanvasRouteInitializer(
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class),
                mock(DagParser.class),
                routeService,
                mock(MqTriggerHandler.class),
                redis,
                delay -> waitCalls.incrementAndGet());

        initializer.initTriggerRoutes();

        assertThat(waitCalls).hasValue(1);
        verify(routeService, never()).markRouteRebuilding();
        verify(redis, never()).delete("canvas:route-init:lock");
    }
}
