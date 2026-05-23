package org.chovy.canvas.engine.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionStatsMapper;
import org.chovy.canvas.domain.meta.MqMessageDefinition;
import org.chovy.canvas.domain.meta.MqMessageDefinitionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
import org.chovy.canvas.infra.cache.CanvasEntityCache;
import org.chovy.canvas.infra.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanvasExecutionServiceTriggerNodeTest {

    @Test
    void findTriggerNodeMatchesMqTriggerByResolvedMessageCodeTopic() {
        MqMessageDefinitionMapper mqMapper = mock(MqMessageDefinitionMapper.class);
        MqMessageDefinition definition = new MqMessageDefinition();
        definition.setTopic("order.paid");
        when(mqMapper.selectOne(any())).thenReturn(definition);

        MqTriggerHandler mqTriggerHandler = new MqTriggerHandler();
        ReflectionTestUtils.setField(mqTriggerHandler, "mqMessageDefinitionMapper", mqMapper);
        CanvasExecutionService service = service(mqTriggerHandler);

        DagGraph graph = new DagParser(new ObjectMapper()).parse("""
                {
                  "nodes": [
                    {
                      "id": "mq-1",
                      "type": "MQ_TRIGGER",
                      "config": {
                        "messageCodeKey": "order_paid",
                        "nextNodeId": "next-1"
                      }
                    },
                    {
                      "id": "next-1",
                      "type": "SEND_MQ",
                      "config": {}
                    }
                  ]
                }
                """);

        String triggerNodeId = ReflectionTestUtils.invokeMethod(
                service, "findTriggerNode", graph, NodeType.MQ_TRIGGER, "order.paid");

        assertThat(triggerNodeId).isEqualTo("mq-1");
    }

    @Test
    void findTriggerNodeStillMatchesLegacyMqTriggerByTopicKey() {
        CanvasExecutionService service = service(new MqTriggerHandler());

        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId("mq-legacy");
        node.setType(NodeType.MQ_TRIGGER);
        node.setConfig(Map.of("topicKey", "legacy.topic"));
        DagGraph graph = new DagGraph(
                Map.of("mq-legacy", node),
                Map.of("mq-legacy", java.util.List.of()),
                Map.of("mq-legacy", java.util.List.of()),
                Map.of("mq-legacy", 0)
        );

        String triggerNodeId = ReflectionTestUtils.invokeMethod(
                service, "findTriggerNode", graph, NodeType.MQ_TRIGGER, "legacy.topic");

        assertThat(triggerNodeId).isEqualTo("mq-legacy");
    }

    private static CanvasExecutionService service(MqTriggerHandler mqTriggerHandler) {
        return new CanvasExecutionService(
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class),
                mock(CanvasExecutionMapper.class),
                mock(CanvasConfigCache.class),
                new DagParser(new ObjectMapper()),
                mock(ContextPersistenceService.class),
                mock(DagEngine.class),
                mock(TriggerPreCheckService.class),
                mock(InFlightExecutionRegistry.class),
                mock(CanvasExecutionStatsMapper.class),
                mock(CanvasEntityCache.class),
                mqTriggerHandler
        );
    }
}
