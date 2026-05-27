package org.chovy.canvas.engine.trigger;

import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.config.ExecutionLaneProperties;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper;
import org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO;
import org.chovy.canvas.dal.mapper.MqMessageDefinitionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.lane.ExecutionLaneResolver;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 画布执行 Service Trigger Node 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasExecutionServiceTriggerNodeTest {

    @Test
    void findTriggerNodeMatchesMqTriggerByResolvedMessageCodeTopic() {
        MqMessageDefinitionMapper mqMapper = mock(MqMessageDefinitionMapper.class);
        MqMessageDefinitionDO definition = new MqMessageDefinitionDO();
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
                mqTriggerHandler,
                mock(CanvasExecutionDlqMapper.class),
                new TriggerPriorityConfig(),
                new ExecutionLaneResolver(),
                new ExecutionLaneProperties(),
                mock(RocketMQTemplate.class),
                new ObjectMapper(),
                mock(CdpUserService.class),
                mock(CanvasDisruptorService.class),
                mock(StringRedisTemplate.class),
                mock(RedisKeyUtil.class),
                mock(Snowflake.class)
        );
    }
}
