package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
import org.chovy.canvas.infra.redis.TriggerRouteService;
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

@ExtendWith(MockitoExtension.class)
class CanvasServicePublishTest {

    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock DagParser dagParser;
    @Mock TriggerRouteService triggerRouteService;
    @Mock CanvasSchedulerService schedulerService;
    @Mock CanvasConfigCache configCache;
    @Mock CanvasExecutionService canvasExecutionService;
    @Mock GroovyHandler groovyHandler;
    @Mock MqTriggerHandler mqTriggerHandler;
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
                groovyHandler,
                mqTriggerHandler,
                redis,
                canvasTransactionService
        );
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void publishClearsPreviouslyPublishedRoutesBeforeRegisteringNewRoutes() {
        Canvas canvas = new Canvas();
        canvas.setId(10L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(100L);
        when(canvasMapper.selectById(10L)).thenReturn(canvas);

        CanvasVersion oldVersion = version(100L, 1, "old");
        CanvasVersion draft = version(200L, 2, "draft");
        when(canvasVersionMapper.selectOne(any())).thenReturn(draft);
        when(canvasVersionMapper.selectById(100L)).thenReturn(oldVersion);
        doAnswer(invocation -> {
            CanvasVersion inserted = invocation.getArgument(0);
            inserted.setId(300L);
            return 1;
        }).when(canvasVersionMapper).insert(any(CanvasVersion.class));

        DagGraph oldGraph = graph("old-mq");
        DagGraph newGraph = graph("new-mq");
        when(dagParser.parse("old")).thenReturn(oldGraph);
        when(dagParser.parse("draft")).thenReturn(newGraph);
        when(mqTriggerHandler.resolveTopic(Map.of("messageCodeKey", "old"))).thenReturn("old.topic");
        when(mqTriggerHandler.resolveTopic(Map.of("messageCodeKey", "new"))).thenReturn("new.topic");

        canvasService.publish(10L, "operator");

        verify(triggerRouteService).removeMq(10L, "old.topic");
        verify(triggerRouteService).registerMq(10L, "new.topic");
        var order = inOrder(triggerRouteService);
        order.verify(triggerRouteService).removeMq(10L, "old.topic");
        order.verify(triggerRouteService).registerMq(10L, "new.topic");
    }

    private static CanvasVersion version(Long id, int version, String graphJson) {
        CanvasVersion canvasVersion = new CanvasVersion();
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
