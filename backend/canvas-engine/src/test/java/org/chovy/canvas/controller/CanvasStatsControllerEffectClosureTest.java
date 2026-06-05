package org.chovy.canvas.web;

import org.chovy.canvas.dal.mapper.CanvasConversionAttributionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanvasStatsControllerEffectClosureTest {

    @Test
    void receiptsReturnStatusCountsForCanvas() {
        MessageSendRecordMapper messageSendRecordMapper = mock(MessageSendRecordMapper.class);
        when(messageSendRecordMapper.selectMaps(any())).thenReturn(List.of(
                Map.of("status", "SENT", "count", 3L),
                Map.of("status", "FAILED", "count", 1L)));
        CanvasStatsController controller = new CanvasStatsController(
                mock(CanvasExecutionMapper.class),
                mock(CanvasExecutionTraceMapper.class),
                null,
                null,
                messageSendRecordMapper,
                mock(CanvasConversionAttributionMapper.class));

        Map<String, Object> data = controller.receipts(10L).block().getData();

        assertThat(data).containsEntry("sent", 3L).containsEntry("failed", 1L);
    }

    @Test
    void attributionSummaryReturnsLastTouchTotals() {
        CanvasConversionAttributionMapper attributionMapper = mock(CanvasConversionAttributionMapper.class);
        when(attributionMapper.selectMaps(any())).thenReturn(List.of(
                Map.of("conversions", 4L, "conversionAmount", new BigDecimal("99.50"), "attributedSends", 3L)));
        CanvasStatsController controller = new CanvasStatsController(
                mock(CanvasExecutionMapper.class),
                mock(CanvasExecutionTraceMapper.class),
                null,
                null,
                mock(MessageSendRecordMapper.class),
                attributionMapper);

        Map<String, Object> data = controller.attributionSummary(10L).block().getData();

        assertThat(data)
                .containsEntry("conversions", 4L)
                .containsEntry("conversionAmount", new BigDecimal("99.50"))
                .containsEntry("attributedSends", 3L)
                .containsEntry("model", "LAST_TOUCH");
    }
}
