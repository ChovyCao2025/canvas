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
    @SuppressWarnings("unchecked")
    void rebuildTriggerRoutesReplacesAllTriggerRoutesInOneBatchAfterParsingPublishedCanvases() {
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

        DagParser.CanvasNode mqNode = new DagParser.CanvasNode();
        mqNode.setId("mq-1");
        mqNode.setType(NodeType.MQ_TRIGGER);
        mqNode.setConfig(Map.of("messageCodeKey", "order_paid"));
        DagParser.CanvasNode eventNode = new DagParser.CanvasNode();
        eventNode.setId("event-1");
        eventNode.setType(NodeType.EVENT_TRIGGER);
        eventNode.setConfig(Map.of("eventCode", "ORDER_PAID"));
        DagParser.CanvasNode taggerNode = new DagParser.CanvasNode();
        taggerNode.setId("tagger-1");
        taggerNode.setType(NodeType.TAGGER);
        taggerNode.setConfig(Map.of("mode", "realtime", "tagCodeKey", "vip_level"));
        DagGraph graph = new DagGraph(
                Map.of("mq-1", mqNode, "event-1", eventNode, "tagger-1", taggerNode),
                Map.of("mq-1", List.of(), "event-1", List.of(), "tagger-1", List.of()),
                Map.of("mq-1", List.of(), "event-1", List.of(), "tagger-1", List.of()),
                Map.of("mq-1", 0, "event-1", 0, "tagger-1", 0));
        when(dagParser.parse("{\"nodes\":[]}")).thenReturn(graph);
        when(mqTriggerHandler.resolveTopic(Map.of("messageCodeKey", "order_paid"))).thenReturn("order.paid");

        MqRouteRefreshService service = new MqRouteRefreshService(
                canvasMapper, versionMapper, dagParser, routeService, mqTriggerHandler);

        service.rebuildTriggerRoutes();

        ArgumentCaptor<TriggerRouteService.TriggerRouteSnapshot> snapshot =
                ArgumentCaptor.forClass(TriggerRouteService.TriggerRouteSnapshot.class);
        org.mockito.Mockito.verify(routeService).replaceAllTriggerRoutes(snapshot.capture());
        assertThat(snapshot.getValue().mqRoutes()).containsEntry("order.paid", Set.of("10"));
        assertThat(snapshot.getValue().behaviorRoutes()).containsEntry("ORDER_PAID", Set.of("10"));
        assertThat(snapshot.getValue().taggerRoutes()).containsEntry("vip_level", Set.of("10"));
    }
}
