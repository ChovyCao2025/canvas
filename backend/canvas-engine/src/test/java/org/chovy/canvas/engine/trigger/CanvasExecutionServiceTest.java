package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionStatsMapper;
import org.chovy.canvas.engine.dag.DagGraph;
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
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void triggerPropagatesExecutionInsertFailureBeforeRunningDagAndReleasesDedup() {
        Canvas canvas = new Canvas();
        canvas.setId(10L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(100L);
        when(canvasEntityCache.get(10L)).thenReturn(canvas);
        when(ctxStore.exists(10L, "user-7")).thenReturn(false);
        when(ctxStore.acquireDedup(eq(10L), eq("user-7"), eq("MSG-1"), any()))
                .thenReturn(true);
        when(ctxStore.buildDedupKey(10L, "user-7", "MSG-1")).thenReturn("dedup-key");

        DagParser.CanvasNode start = new DagParser.CanvasNode();
        start.setId("start");
        start.setType(NodeType.DIRECT_CALL);
        DagGraph graph = new DagGraph(
                Map.of("start", start),
                Map.of("start", List.of()),
                Map.of("start", List.of()),
                Map.of("start", 0));
        when(configCache.get(10L, 100L)).thenReturn(graph);
        when(executionRegistry.tryAcquire(eq(10L), any(), eq(1000), eq(1000)))
                .thenReturn(Optional.of(Disposables.swap()));
        when(dagEngine.execute(eq(graph), eq("start"), any()))
                .thenReturn(Mono.just(Map.of("ok", true)));
        when(executionMapper.insert(any(CanvasExecution.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> sut.trigger(
                10L,
                "user-7",
                TriggerType.DIRECT_CALL,
                NodeType.DIRECT_CALL,
                null,
                Map.of(),
                "MSG-1",
                false).block())
                .hasMessageContaining("db down");

        verify(ctxStore).releaseDedup("dedup-key");
    }
}
