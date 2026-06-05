package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.rule.CanvasRuleGraphValidator;
import org.chovy.canvas.engine.rule.RuleParser;
import org.chovy.canvas.engine.rule.RuleValidationException;
import org.chovy.canvas.engine.rule.RuleValidator;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanvasValidationRuntimeGuardTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsNodeCountAboveConfiguredMaximum() {
        CanvasRuleGraphValidator validator = validator();
        ReflectionTestUtils.setField(validator, "maxNodeCount", 2);
        DagGraph graph = graph(Map.of(
                "trigger", node("trigger", NodeType.DIRECT_CALL, Map.of("nextNodeId", "a")),
                "a", node("a", NodeType.API_CALL, Map.of("nextNodeId", "b")),
                "b", node("b", NodeType.END, Map.of())
        ));

        assertThatThrownBy(() -> validator.validateOrThrow(graph))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("节点数量")
                .hasMessageContaining("2");
    }

    @Test
    void rejectsGotoMaxJumpsAboveConfiguredMaximum() {
        CanvasRuleGraphValidator validator = validator();
        ReflectionTestUtils.setField(validator, "maxGotoJumps", 3);
        DagGraph graph = graph(Map.of(
                "goto", node("goto", "GOTO", Map.of("targetNodeId", "end", "maxJumps", 4)),
                "end", node("end", NodeType.END, Map.of())
        ));

        assertThatThrownBy(() -> validator.validateOrThrow(graph))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("goto")
                .hasMessageContaining("maxJumps")
                .hasMessageContaining("3");
    }

    @Test
    void rejectsLoopMaxIterationsAboveConfiguredMaximum() {
        CanvasRuleGraphValidator validator = validator();
        ReflectionTestUtils.setField(validator, "maxLoopIterations", 5);
        DagGraph graph = graph(Map.of(
                "loop", node("loop", "LOOP", Map.of("loopStartNodeId", "work", "maxIterations", 6)),
                "work", node("work", NodeType.API_CALL, Map.of("nextNodeId", "end")),
                "end", node("end", NodeType.END, Map.of())
        ));

        assertThatThrownBy(() -> validator.validateOrThrow(graph))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("loop")
                .hasMessageContaining("maxIterations")
                .hasMessageContaining("5");
    }

    @Test
    void publishRejectsTransitiveSubFlowCycle() throws Exception {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasService service = canvasService(canvasMapper, versionMapper);

        when(canvasMapper.selectById(1L)).thenReturn(canvas(1L, "root", CanvasStatusEnum.DRAFT, null));
        when(canvasMapper.selectById(2L)).thenReturn(canvas(2L, "child-a", CanvasStatusEnum.PUBLISHED, 20L));
        when(canvasMapper.selectById(3L)).thenReturn(canvas(3L, "child-b", CanvasStatusEnum.PUBLISHED, 30L));
        when(versionMapper.selectOne(any())).thenReturn(version(10L, 1L, VersionStatus.DRAFT, subFlowGraph(2L)));
        when(versionMapper.selectById(20L)).thenReturn(version(20L, 2L, VersionStatus.PUBLISHED, subFlowGraph(3L)));
        when(versionMapper.selectById(30L)).thenReturn(version(30L, 3L, VersionStatus.PUBLISHED, subFlowGraph(1L)));

        assertThatThrownBy(() -> service.publish(1L, "tester"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("循环")
                .hasMessageContaining("1 -> 2 -> 3 -> 1");
    }

    private CanvasRuleGraphValidator validator() {
        return new CanvasRuleGraphValidator(new RuleParser(objectMapper), new RuleValidator());
    }

    private CanvasService canvasService(CanvasMapper canvasMapper, CanvasVersionMapper versionMapper) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class))).thenReturn(true);

        return new CanvasService(
                canvasMapper,
                versionMapper,
                new DagParser(objectMapper),
                mock(TriggerRouteService.class),
                mock(CanvasSchedulerService.class),
                mock(CanvasConfigCache.class),
                mock(CanvasExecutionService.class),
                mock(TriggerPreCheckService.class),
                mock(GroovyHandler.class),
                mock(org.chovy.canvas.engine.handlers.MqTriggerHandler.class),
                validator(),
                redis,
                mock(CanvasTransactionService.class),
                new CanvasExamplesProperties());
    }

    private String subFlowGraph(Long subFlowId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "nodes", List.of(Map.of(
                        "id", "sub-" + subFlowId,
                        "type", NodeType.SUB_FLOW_REF,
                        "name", "sub-" + subFlowId,
                        "config", Map.of("subFlowId", subFlowId)))));
    }

    private DagGraph graph(Map<String, DagParser.CanvasNode> nodes) {
        Map<String, List<String>> forward = new LinkedHashMap<>();
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        nodes.forEach((id, ignored) -> {
            forward.put(id, List.of());
            reverse.put(id, List.of());
            inDegree.put(id, 0);
        });
        return new DagGraph(nodes, forward, reverse, inDegree);
    }

    private static DagParser.CanvasNode node(String id, String type, Map<String, Object> config) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(id);
        node.setName(id);
        node.setType(type);
        node.setConfig(config);
        return node;
    }

    private static CanvasDO canvas(Long id, String name, CanvasStatusEnum status, Long publishedVersionId) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(id);
        canvas.setTenantId(1L);
        canvas.setName(name);
        canvas.setStatus(status.getCode());
        canvas.setPublishedVersionId(publishedVersionId);
        return canvas;
    }

    private static CanvasVersionDO version(Long id, Long canvasId, VersionStatus status, String graphJson) {
        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(id);
        version.setTenantId(1L);
        version.setCanvasId(canvasId);
        version.setVersion(1);
        version.setStatus(status.getCode());
        version.setGraphJson(graphJson);
        return version;
    }
}
