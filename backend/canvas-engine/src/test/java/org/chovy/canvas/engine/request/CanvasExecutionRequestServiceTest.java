package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 画布执行请求 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasExecutionRequestServiceTest {

    @Test
    void enqueueMqRequestPersistsDeterministicPendingRequest() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionRequestService service = new CanvasExecutionRequestService(mapper, new ObjectMapper());
        when(mapper.insertIgnore(org.mockito.Mockito.any())).thenReturn(1);

        String requestId = service.enqueue(
                10L,
                "user-7",
                TriggerType.MQ,
                NodeType.MQ_TRIGGER,
                "order.paid",
                Map.of("orderId", "O-1", "perfRunId", "perf_20260523_001"),
                "MSG-1"
        );

        assertThat(requestId).startsWith("mq-10-");
        ArgumentCaptor<CanvasExecutionRequestDO> captor = ArgumentCaptor.forClass(CanvasExecutionRequestDO.class);
        verify(mapper).insertIgnore(captor.capture());
        CanvasExecutionRequestDO request = captor.getValue();
        assertThat(request.getId()).isEqualTo(requestId);
        assertThat(request.getCanvasId()).isEqualTo(10L);
        assertThat(request.getUserId()).isEqualTo("user-7");
        assertThat(request.getTriggerType()).isEqualTo(TriggerType.MQ);
        assertThat(request.getTriggerNodeType()).isEqualTo(NodeType.MQ_TRIGGER);
        assertThat(request.getMatchKey()).isEqualTo("order.paid");
        assertThat(request.getPayloadJson()).contains("\"orderId\":\"O-1\"");
        assertThat(request.getPerfRunId()).isEqualTo("perf_20260523_001");
        assertThat(request.getSourceMsgId()).isEqualTo("MSG-1");
        assertThat(request.getStatus()).isEqualTo(CanvasExecutionRequestStatus.PENDING);
        assertThat(request.getAttemptCount()).isZero();
    }

    @Test
    void enqueueReturnsSameIdWhenRequestAlreadyExists() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionRequestService service = new CanvasExecutionRequestService(mapper, new ObjectMapper());
        when(mapper.insertIgnore(org.mockito.Mockito.any())).thenReturn(0);

        String first = service.enqueue(10L, "user-7", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "order.paid", Map.of(), "MSG-1");
        String second = service.enqueue(10L, "user-7", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "order.paid", Map.of(), "MSG-1");

        assertThat(second).isEqualTo(first);
        verify(mapper, org.mockito.Mockito.times(2)).insertIgnore(org.mockito.Mockito.any());
    }

    @Test
    void enqueueGeneratesDistinctRequestIdsWhenSourceMsgIdIsMissing() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionRequestService service = new CanvasExecutionRequestService(mapper, new ObjectMapper());
        when(mapper.insertIgnore(org.mockito.Mockito.any())).thenReturn(1);

        String first = service.enqueue(10L, "user-7", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "order.paid", Map.of("orderId", "O-1"), null);
        String second = service.enqueue(10L, "user-7", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "order.paid", Map.of("orderId", "O-2"), null);

        assertThat(first).startsWith("mq-10-");
        assertThat(second).startsWith("mq-10-");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void enqueuePersistsTenantFromTargetCanvas() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setTenantId(99L);
        when(canvasMapper.selectById(10L)).thenReturn(canvas);
        CanvasExecutionRequestService service =
                new CanvasExecutionRequestService(mapper, new ObjectMapper(), canvasMapper);

        service.enqueue(10L, "user-7", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "order.paid", Map.of(), "MSG-1");

        ArgumentCaptor<CanvasExecutionRequestDO> captor = ArgumentCaptor.forClass(CanvasExecutionRequestDO.class);
        verify(mapper).insertIgnore(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(99L);
    }
}
