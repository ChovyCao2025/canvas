package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.execution.CanvasMqTriggerRejected;
import org.chovy.canvas.domain.execution.CanvasMqTriggerRejectedMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionRequestService;
import org.chovy.canvas.infra.redis.TriggerRouteService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasMqTriggerRejectedControllerTest {

    @Test
    void listReturnsRejectedMessagesPage() {
        CanvasMqTriggerRejectedMapper mapper = mock(CanvasMqTriggerRejectedMapper.class);
        CanvasMqTriggerRejectedController controller = controller(mapper);
        Page<CanvasMqTriggerRejected> page = new Page<>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(rejected()));
        when(mapper.selectPage(any(), any())).thenReturn(page);

        R<?> response = controller.list("ORDER_PAID", "INVALID_BODY", 1, 20).block();

        assertThat(response.getData()).isNotNull();
    }

    @Test
    void replayValidRejectedMessageEnqueuesExecutionRequestsForMatchedRoutes() {
        CanvasMqTriggerRejectedMapper mapper = mock(CanvasMqTriggerRejectedMapper.class);
        TriggerRouteService routeService = mock(TriggerRouteService.class);
        CanvasExecutionRequestService requestService = mock(CanvasExecutionRequestService.class);
        CanvasDisruptorService disruptor = mock(CanvasDisruptorService.class);
        CanvasMqTriggerRejectedController controller =
                new CanvasMqTriggerRejectedController(mapper, routeService, requestService, disruptor, new com.fasterxml.jackson.databind.ObjectMapper());
        CanvasMqTriggerRejected rejected = rejected();
        when(mapper.selectById(1L)).thenReturn(rejected);
        when(routeService.getCanvasByMqTopic("ORDER_PAID")).thenReturn(Set.of("101", "202"));
        when(requestService.enqueue(eq(101L), eq("user-7"), any(), any(), eq("ORDER_PAID"), any(), eq("MSG-1")))
                .thenReturn("req-101");
        when(requestService.enqueue(eq(202L), eq("user-7"), any(), any(), eq("ORDER_PAID"), any(), eq("MSG-1")))
                .thenReturn("req-202");

        R<Map<String, Object>> response = controller.replay(1L).block();

        verify(disruptor).publishRequest("req-101");
        verify(disruptor).publishRequest("req-202");
        assertThat(response.getData()).containsEntry("count", 2);
        assertThat(response.getData().get("requestIds")).isEqualTo(List.of("req-101", "req-202"));
    }

    @Test
    void replayRejectsInvalidStoredJson() {
        CanvasMqTriggerRejectedMapper mapper = mock(CanvasMqTriggerRejectedMapper.class);
        CanvasMqTriggerRejectedController controller = controller(mapper);
        CanvasMqTriggerRejected rejected = rejected();
        rejected.setBody("{bad-json");
        when(mapper.selectById(1L)).thenReturn(rejected);

        assertThatThrownBy(() -> controller.replay(1L).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无法重放 rejected 消息");
    }

    private CanvasMqTriggerRejectedController controller(CanvasMqTriggerRejectedMapper mapper) {
        return new CanvasMqTriggerRejectedController(
                mapper,
                mock(TriggerRouteService.class),
                mock(CanvasExecutionRequestService.class),
                mock(CanvasDisruptorService.class),
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    private CanvasMqTriggerRejected rejected() {
        CanvasMqTriggerRejected rejected = new CanvasMqTriggerRejected();
        rejected.setId(1L);
        rejected.setMsgId("MSG-1");
        rejected.setTag("ORDER_PAID");
        rejected.setReason("INVALID_BODY");
        rejected.setBody("{\"userId\":\"user-7\",\"messageCode\":\"PAYMENT\",\"payload\":{\"orderId\":\"O-1\"}}");
        return rejected;
    }
}
