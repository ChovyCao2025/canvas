package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.rule.CanvasRuleGraphValidator;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;

@ExtendWith(MockitoExtension.class)
class CanvasServicePublishTest {

    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock DagParser dagParser;
    @Mock TriggerRouteService triggerRouteService;
    @Mock CanvasSchedulerService schedulerService;
    @Mock CanvasConfigCache configCache;
    @Mock CanvasExecutionService canvasExecutionService;
    @Mock TriggerPreCheckService preCheckService;
    @Mock GroovyHandler groovyHandler;
    @Mock MqTriggerHandler mqTriggerHandler;
    @Mock CanvasRuleGraphValidator canvasRuleGraphValidator;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock CanvasTransactionService canvasTransactionService;

    CanvasService canvasService;

    @BeforeEach
    void setUp() {
        canvasService = new CanvasService(
                canvasMapper,
                canvasVersionMapper,
                dagParser,
                triggerRouteService,
                schedulerService,
                configCache,
                canvasExecutionService,
                preCheckService,
                groovyHandler,
                mqTriggerHandler,
                canvasRuleGraphValidator,
                redis,
                canvasTransactionService,
                new CanvasExamplesProperties()
        );
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void publishClearsPreviouslyPublishedRoutesBeforeRegisteringNewRoutes() {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(100L);
        when(canvasMapper.selectById(10L)).thenReturn(canvas);

        CanvasVersionDO oldVersion = version(100L, 1, "old");
        CanvasVersionDO draft = version(200L, 2, "draft");
        when(canvasVersionMapper.selectOne(any())).thenReturn(draft);
        when(canvasVersionMapper.selectById(100L)).thenReturn(oldVersion);
        DagGraph oldGraph = graph("old-mq");
        DagGraph newGraph = graph("new-mq");
        when(dagParser.parse("old")).thenReturn(oldGraph);
        when(dagParser.parse("draft")).thenReturn(newGraph);
        when(canvasTransactionService.publishDb(10L, "draft", "operator"))
                .thenReturn(new CanvasTransactionService.PublishResult(version(300L, 3, "draft"), 100L));
        when(mqTriggerHandler.resolveTopic(Map.of("messageCodeKey", "old"))).thenReturn("old.topic");
        when(mqTriggerHandler.resolveTopic(Map.of("messageCodeKey", "new"))).thenReturn("new.topic");

        canvasService.publish(10L, "operator");

        verify(triggerRouteService).removeMq(10L, "old.topic");
        verify(triggerRouteService).registerMq(10L, "new.topic");
        verify(canvasRuleGraphValidator).validateOrThrow(newGraph);
        var order = inOrder(triggerRouteService);
        order.verify(triggerRouteService).removeMq(10L, "old.topic");
        order.verify(triggerRouteService).registerMq(10L, "new.topic");
    }

    private static CanvasVersionDO version(Long id, int version, String graphJson) {
        CanvasVersionDO canvasVersion = new CanvasVersionDO();
        canvasVersion.setId(id);
        canvasVersion.setCanvasId(10L);
        canvasVersion.setVersion(version);
        canvasVersion.setGraphJson(graphJson);
        canvasVersion.setStatus(VersionStatus.DRAFT.getCode());
        return canvasVersion;
    }

    private static DagGraph graph(String nodeId) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(nodeId);
        node.setType(NodeType.MQ_TRIGGER);
        node.setConfig(Map.of("messageCodeKey", nodeId.startsWith("old") ? "old" : "new"));
        return new DagGraph(
                Map.of(nodeId, node),
                Map.of(nodeId, java.util.List.of()),
                Map.of(nodeId, java.util.List.of()),
                Map.of(nodeId, 0)
        );
    }
}
