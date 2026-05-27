package org.chovy.canvas.infrastructure.redis;

import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mq Route Refresh 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class MqRouteRefreshServiceTest {

    @Test
    void rebuildMqRoutesReplacesMqRoutesInOneBatchAfterParsingPublishedCanvases() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        DagParser dagParser = mock(DagParser.class);
        TriggerRouteService routeService = mock(TriggerRouteService.class);
        MqTriggerHandler mqTriggerHandler = mock(MqTriggerHandler.class);

        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(100L);
        when(canvasMapper.selectList(any())).thenReturn(List.of(canvas));

        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(100L);
        version.setGraphJson("{\"nodes\":[]}");
        when(versionMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(version));

        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId("mq-1");
        node.setType(NodeType.MQ_TRIGGER);
        node.setConfig(Map.of("messageCodeKey", "order_paid"));
        DagGraph graph = new DagGraph(
                Map.of("mq-1", node),
                Map.of("mq-1", List.of()),
                Map.of("mq-1", List.of()),
                Map.of("mq-1", 0));
        when(dagParser.parse("{\"nodes\":[]}")).thenReturn(graph);
        when(mqTriggerHandler.resolveTopic(Map.of("messageCodeKey", "order_paid"))).thenReturn("order.paid");

        MqRouteRefreshService service = new MqRouteRefreshService(
                canvasMapper, versionMapper, dagParser, routeService, mqTriggerHandler);

        service.rebuildMqRoutes();

        ArgumentCaptor<Map<String, Set<String>>> routes = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(routeService).replaceMqRoutes(routes.capture());
        assertThat(routes.getValue()).containsEntry("order.paid", Set.of("10"));
    }
}
