package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CanvasExecutionRequestServiceIdempotencyTest {

    @Mock
    private CanvasExecutionRequestMapper mapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void duplicateMqSourceMessageBuildsSameRequestIdForSameCanvas() {
        CanvasExecutionRequestService service = new CanvasExecutionRequestService(mapper, objectMapper);

        String first = service.enqueue(10L, "user-1", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "ORDER_PAID", Map.of("orderId", "O-1"), "MSG-1");
        String second = service.enqueue(10L, "user-1", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "ORDER_PAID", Map.of("orderId", "O-1"), "MSG-1");

        assertThat(second).isEqualTo(first);

        ArgumentCaptor<CanvasExecutionRequestDO> captor =
                ArgumentCaptor.forClass(CanvasExecutionRequestDO.class);
        verify(mapper, times(2)).insertIgnore(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(CanvasExecutionRequestDO::getId)
                .containsExactly(first, first);
    }

    @Test
    void sameMqSourceMessageBuildsDifferentRequestIdsForDifferentCanvases() {
        CanvasExecutionRequestService service = new CanvasExecutionRequestService(mapper, objectMapper);

        String first = service.enqueue(10L, "user-1", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "ORDER_PAID", Map.of("orderId", "O-1"), "MSG-1");
        String second = service.enqueue(20L, "user-1", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "ORDER_PAID", Map.of("orderId", "O-1"), "MSG-1");

        assertThat(second).isNotEqualTo(first);
    }
}
