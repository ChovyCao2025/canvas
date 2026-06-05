package org.chovy.canvas.infrastructure.redis;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.MqTriggerHandler;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriggerRouteRecoveryServiceTest {

    @Test
    void rebuildRuntimeStateReplacesAllTriggerRoutesAndSchedulesPublishedGraphs() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        DagParser dagParser = mock(DagParser.class);
        TriggerRouteService routeService = mock(TriggerRouteService.class);
        MqTriggerHandler mqTriggerHandler = mock(MqTriggerHandler.class);
        CanvasSchedulerService schedulerService = mock(CanvasSchedulerService.class);

        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(100L);
        when(canvasMapper.selectList(any())).thenReturn(List.of(canvas));

        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(100L);
        version.setGraphJson("{\"nodes\":[]}");
        when(versionMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(version));

        DagGraph graph = graph();
        when(dagParser.parse("{\"nodes\":[]}")).thenReturn(graph);
        when(mqTriggerHandler.resolveTopic(any())).thenReturn("order.paid");
        when(schedulerService.replaceScheduledTriggers(any())).thenReturn(1);

        TriggerRouteRecoveryService service = new TriggerRouteRecoveryService(
                canvasMapper, versionMapper, dagParser, routeService, mqTriggerHandler, schedulerService);

        TriggerRouteRecoveryService.RecoveryReport report = service.rebuildRuntimeState();

        ArgumentCaptor<TriggerRouteService.TriggerRouteSnapshot> routes =
                ArgumentCaptor.forClass(TriggerRouteService.TriggerRouteSnapshot.class);
        verify(routeService).replaceAllTriggerRoutes(routes.capture());
        assertThat(routes.getValue().mqRoutes()).containsEntry("order.paid", Set.of("10"));
        assertThat(routes.getValue().behaviorRoutes()).containsEntry("USER_SIGNED_UP", Set.of("10"));
        assertThat(routes.getValue().taggerRoutes()).containsEntry("vip_member", Set.of("10"));

        ArgumentCaptor<Map<Long, DagGraph>> graphs = ArgumentCaptor.forClass(Map.class);
        verify(schedulerService).replaceScheduledTriggers(graphs.capture());
        assertThat(graphs.getValue()).containsEntry(10L, graph);
        assertThat(report).isEqualTo(new TriggerRouteRecoveryService.RecoveryReport(1, 1, 1, 1, 1));
    }

    private DagGraph graph() {
        Map<String, DagParser.CanvasNode> nodes = new LinkedHashMap<>();
        nodes.put("event-1", node("event-1", NodeType.EVENT_TRIGGER, Map.of("eventCode", "USER_SIGNED_UP")));
        nodes.put("mq-1", node("mq-1", NodeType.MQ_TRIGGER, Map.of("messageCodeKey", "ORDER_PAID")));
        nodes.put("tagger-1", node("tagger-1", NodeType.TAGGER,
                Map.of("mode", "realtime", "tagCodeKey", "vip_member")));
        nodes.put("schedule-1", node("schedule-1", NodeType.SCHEDULED_TRIGGER,
                Map.of("cronExpression", "0 0/5 * * * *")));
        return new DagGraph(
                nodes,
                Map.of("event-1", List.of(), "mq-1", List.of(), "tagger-1", List.of(), "schedule-1", List.of()),
                Map.of("event-1", List.of(), "mq-1", List.of(), "tagger-1", List.of(), "schedule-1", List.of()),
                Map.of("event-1", 0, "mq-1", 0, "tagger-1", 0, "schedule-1", 0));
    }

    private DagParser.CanvasNode node(String id, String type, Map<String, Object> config) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(id);
        node.setType(type);
        node.setConfig(config);
        node.setBizConfig(Map.of());
        return node;
    }
}
