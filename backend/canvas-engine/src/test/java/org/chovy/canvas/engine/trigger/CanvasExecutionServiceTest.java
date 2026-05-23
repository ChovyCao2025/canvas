package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionStatsMapper;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
import org.chovy.canvas.infra.cache.CanvasEntityCache;
import org.chovy.canvas.infra.redis.ContextPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CanvasExecutionServiceTest {

    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock CanvasExecutionMapper executionMapper;
    @Mock CanvasConfigCache configCache;
    @Mock DagParser dagParser;
    @Mock ContextPersistenceService ctxStore;
    @Mock DagEngine dagEngine;
    @Mock TriggerPreCheckService preCheckService;
    @Mock InFlightExecutionRegistry executionRegistry;
    @Mock CanvasExecutionStatsMapper statsMapper;
    @Mock CanvasEntityCache canvasEntityCache;
    @Mock MqTriggerHandler mqTriggerHandler;

    CanvasExecutionService sut;

    @BeforeEach
    void setUp() {
        sut = new CanvasExecutionService(
                canvasMapper,
                canvasVersionMapper,
                executionMapper,
                configCache,
                dagParser,
                ctxStore,
                dagEngine,
                preCheckService,
                executionRegistry,
                statsMapper,
                canvasEntityCache,
                mqTriggerHandler
        );
        ReflectionTestUtils.setField(sut, "ctxTtlSec", 86400L);
        ReflectionTestUtils.setField(sut, "globalTimeoutSec", 600L);
        ReflectionTestUtils.setField(sut, "globalMaxConcurrency", 1000);
    }

    @Test
    void invalidateCanvas_delegatesToCanvasEntityCache() {
        sut.invalidateCanvas(20L);

        verify(canvasEntityCache).invalidate(20L);
    }
}
