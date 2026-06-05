package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.rule.CanvasRuleGraphValidator;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasTransactionSideEffectTest {

    @Test
    void publishDoesNotRunExternalSideEffectsWhenDbPhaseFails() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        DagParser dagParser = mock(DagParser.class);
        TriggerRouteService routeService = mock(TriggerRouteService.class);
        CanvasSchedulerService schedulerService = mock(CanvasSchedulerService.class);
        CanvasConfigCache configCache = mock(CanvasConfigCache.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        GroovyHandler groovyHandler = mock(GroovyHandler.class);
        CanvasTransactionService transactionService = mock(CanvasTransactionService.class);
        StringRedisTemplate redis = redisWithAcquiredLock();

        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        when(canvasMapper.selectById(10L)).thenReturn(canvas);
        CanvasVersionDO draft = new CanvasVersionDO();
        draft.setId(20L);
        draft.setCanvasId(10L);
        draft.setGraphJson("{\"nodes\":[]}");
        draft.setStatus(VersionStatus.DRAFT.getCode());
        when(versionMapper.selectOne(any())).thenReturn(draft);
        DagGraph graph = graphWithEntryNode("start", NodeType.DIRECT_CALL);
        when(dagParser.parse(draft.getGraphJson())).thenReturn(graph);
        when(transactionService.publishDb(10L, draft.getGraphJson(), "operator"))
                .thenThrow(new IllegalStateException("db rollback"));

        CanvasService service = new CanvasService(
                canvasMapper,
                versionMapper,
                dagParser,
                routeService,
                schedulerService,
                configCache,
                executionService,
                mock(TriggerPreCheckService.class),
                groovyHandler,
                mock(org.chovy.canvas.engine.handlers.MqTriggerHandler.class),
                mock(CanvasRuleGraphValidator.class),
                redis,
                transactionService,
                new CanvasExamplesProperties());

        assertThatThrownBy(() -> service.publish(10L, "operator"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db rollback");

        verify(routeService, never()).registerBehavior(any(), anyString());
        verify(routeService, never()).registerMq(any(), anyString());
        verify(routeService, never()).registerTagger(any(), anyString());
        verify(schedulerService, never()).registerScheduledTriggers(eq(10L), any());
        verify(configCache, never()).invalidate(any(), any());
        verify(executionService, never()).invalidateCanvas(10L);
        verify(groovyHandler, never()).precompileScript(any(), anyString(), anyString());
    }

    @Test
    void offlineDoesNotRunExternalSideEffectsWhenDbPhaseFails() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        TriggerRouteService routeService = mock(TriggerRouteService.class);
        CanvasSchedulerService schedulerService = mock(CanvasSchedulerService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        TriggerPreCheckService preCheckService = mock(TriggerPreCheckService.class);
        CanvasTransactionService transactionService = mock(CanvasTransactionService.class);
        when(transactionService.offlineDb(10L)).thenThrow(new IllegalStateException("db rollback"));

        CanvasService service = new CanvasService(
                canvasMapper,
                versionMapper,
                mock(DagParser.class),
                routeService,
                schedulerService,
                mock(CanvasConfigCache.class),
                executionService,
                preCheckService,
                mock(GroovyHandler.class),
                mock(org.chovy.canvas.engine.handlers.MqTriggerHandler.class),
                mock(CanvasRuleGraphValidator.class),
                mock(StringRedisTemplate.class),
                transactionService,
                new CanvasExamplesProperties());

        assertThatThrownBy(() -> service.offline(10L, "operator"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db rollback");

        verify(versionMapper, never()).selectById(any());
        verify(routeService, never()).removeBehavior(any(), anyString());
        verify(routeService, never()).removeMq(any(), anyString());
        verify(routeService, never()).removeTagger(any(), anyString());
        verify(schedulerService, never()).cancelScheduledTriggers(eq(10L), any());
        verify(executionService, never()).invalidateCanvas(10L);
        verify(preCheckService, never()).cleanupCanvasQuotas(10L);
    }

    @Test
    void killDoesNotPublishOrCleanupWhenDbPhaseFails() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasTransactionService transactionService = mock(CanvasTransactionService.class);
        CanvasService canvasService = mock(CanvasService.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        when(canvasMapper.selectById(10L)).thenReturn(canvas);
        when(transactionService.killDb(10L)).thenThrow(new IllegalStateException("db rollback"));

        CanvasOpsService service = new CanvasOpsService(
                canvasMapper,
                mock(CanvasVersionMapper.class),
                mock(CanvasExecutionMapper.class),
                mock(CanvasExecutionRequestMapper.class),
                mock(TriggerRouteService.class),
                mock(TriggerPreCheckService.class),
                transactionService,
                canvasService,
                redis);

        assertThatThrownBy(() -> service.kill(10L, "FORCE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db rollback");

        verify(redis, never()).convertAndSend(anyString(), anyString());
        verify(canvasService, never()).applyKillExternalCleanup(any(), any());
    }

    private StringRedisTemplate redisWithAcquiredLock() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        return redis;
    }

    private DagGraph graphWithEntryNode(String nodeId, String nodeType) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(nodeId);
        node.setType(nodeType);
        node.setConfig(Map.of());
        node.setBizConfig(Map.of());
        return new DagGraph(
                Map.of(nodeId, node),
                Map.of(nodeId, List.of()),
                Map.of(nodeId, List.of()),
                Map.of(nodeId, 0));
    }
}
