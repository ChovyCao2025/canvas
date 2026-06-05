package org.chovy.canvas.engine.trigger;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanvasExecutionConfigLoaderTest {

    @Test
    void validateAndLoadCanvasRejectsUnpublishedCanvasOutsideDryRun() {
        CanvasEntityCache canvasEntityCache = mock(CanvasEntityCache.class);
        CanvasDO canvas = canvas(10L);
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        when(canvasEntityCache.get(10L)).thenReturn(canvas);
        CanvasExecutionConfigLoader loader = loader(canvasEntityCache);

        assertThatThrownBy(() -> loader.validateAndLoadCanvas(10L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("画布未发布");

        assertThat(loader.validateAndLoadCanvas(10L, true)).isSameAs(canvas);
    }

    @Test
    void resolveVersionIdPrefersDraftVersionForDryRun() {
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasVersionDO draft = new CanvasVersionDO();
        draft.setId(200L);
        when(versionMapper.selectOne(any(Wrapper.class))).thenReturn(draft);
        CanvasExecutionConfigLoader loader = new CanvasExecutionConfigLoader(
                mock(CanvasMapper.class),
                versionMapper,
                mock(CanvasConfigCache.class),
                mock(CanvasEntityCache.class),
                mock(DagParser.class),
                mock(MqTriggerHandler.class));

        assertThat(loader.resolveVersionId(canvas(10L), "user-1", true)).isEqualTo(200L);
    }

    @Test
    void findTriggerNodeSupportsNodeIdAndMqTopicMatching() {
        MqTriggerHandler mqTriggerHandler = mock(MqTriggerHandler.class);
        when(mqTriggerHandler.resolveTopic(Map.of("topicKey", "topic-a"))).thenReturn("topic-a");
        CanvasExecutionConfigLoader loader = new CanvasExecutionConfigLoader(
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class),
                mock(CanvasConfigCache.class),
                mock(CanvasEntityCache.class),
                mock(DagParser.class),
                mqTriggerHandler);
        DagGraph graph = graph(List.of(
                node("wait-1", NodeType.USER_INPUT, Map.of(), Map.of()),
                node("mq-1", NodeType.MQ_TRIGGER, Map.of("topicKey", "topic-a"), Map.of())
        ));

        assertThat(loader.findTriggerNode(graph, NodeType.USER_INPUT, "wait-1")).isEqualTo("wait-1");
        assertThat(loader.findTriggerNode(graph, NodeType.MQ_TRIGGER, "topic-a")).isEqualTo("mq-1");
    }

    private CanvasExecutionConfigLoader loader(CanvasEntityCache canvasEntityCache) {
        return new CanvasExecutionConfigLoader(
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class),
                mock(CanvasConfigCache.class),
                canvasEntityCache,
                mock(DagParser.class),
                mock(MqTriggerHandler.class));
    }

    private CanvasDO canvas(Long id) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(id);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(100L);
        return canvas;
    }

    private DagGraph graph(List<DagParser.CanvasNode> nodes) {
        Map<String, DagParser.CanvasNode> nodeMap = new LinkedHashMap<>();
        Map<String, List<String>> forward = new LinkedHashMap<>();
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (DagParser.CanvasNode node : nodes) {
            nodeMap.put(node.getId(), node);
            forward.put(node.getId(), List.of());
            reverse.put(node.getId(), List.of());
            inDegree.put(node.getId(), 0);
        }
        return new DagGraph(nodeMap, forward, reverse, inDegree);
    }

    private DagParser.CanvasNode node(String id,
                                      String type,
                                      Map<String, Object> config,
                                      Map<String, Object> bizConfig) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(id);
        node.setType(type);
        node.setName(id);
        node.setConfig(config);
        node.setBizConfig(bizConfig);
        return node;
    }
}
