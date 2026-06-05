package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.dal.dataobject.CanvasConversionAttributionDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.CanvasConversionAttributionMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasAttributionServiceTest {

    private final CanvasMapper canvasMapper = mock(CanvasMapper.class);
    private final MessageSendRecordMapper sendRecordMapper = mock(MessageSendRecordMapper.class);
    private final CanvasConversionAttributionMapper attributionMapper =
            mock(CanvasConversionAttributionMapper.class);
    private final CanvasAttributionService service = new CanvasAttributionService(
            canvasMapper,
            sendRecordMapper,
            attributionMapper);

    @Test
    void attributesConversionToLatestPriorSentRecord() {
        CanvasDO canvas = canvasWithConversion("ORDER_PAID");
        MessageSendRecordDO send = sentRecord(22L, 10L, "user-1",
                LocalDateTime.parse("2026-06-01T10:00:00"));
        when(canvasMapper.selectList(any())).thenReturn(List.of(canvas));
        when(sendRecordMapper.selectOne(any())).thenReturn(send);

        service.attribute(eventLog(99L, "ORDER_PAID", "user-1",
                LocalDateTime.parse("2026-06-01T12:00:00"),
                "{\"conversionAmount\":99.50}"));

        verify(attributionMapper).insert((CanvasConversionAttributionDO) argThat((CanvasConversionAttributionDO row) ->
                row.getCanvasId().equals(10L)
                        && row.getSendRecordId().equals(22L)
                        && row.getEventLogId().equals(99L)
                        && new BigDecimal("99.50").compareTo(row.getConversionAmount()) == 0
                        && "LAST_TOUCH".equals(row.getAttributionModel())));
    }

    @Test
    void duplicateAttributionIsIgnoredByEventAndCanvas() {
        doThrow(new DuplicateKeyException("duplicate")).when(attributionMapper)
                .insert((CanvasConversionAttributionDO) any(CanvasConversionAttributionDO.class));

        assertThatCode(() -> service.insertAttribution(row()))
                .doesNotThrowAnyException();
    }

    private CanvasDO canvasWithConversion(String eventCode) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setConversionEventCode(eventCode);
        canvas.setAttributionWindowDays(7);
        return canvas;
    }

    private MessageSendRecordDO sentRecord(Long id, Long canvasId, String userId, LocalDateTime createdAt) {
        MessageSendRecordDO send = new MessageSendRecordDO();
        send.setId(id);
        send.setCanvasId(canvasId);
        send.setUserId(userId);
        send.setStatus(MessageSendRecordDO.STATUS_SENT);
        send.setCreatedAt(createdAt);
        return send;
    }

    private EventLogDO eventLog(Long id,
                                String eventCode,
                                String userId,
                                LocalDateTime createdAt,
                                String attributes) {
        EventLogDO eventLog = new EventLogDO();
        eventLog.setId(id);
        eventLog.setEventCode(eventCode);
        eventLog.setUserId(userId);
        eventLog.setCreatedAt(createdAt);
        eventLog.setAttributes(attributes);
        return eventLog;
    }

    private CanvasConversionAttributionDO row() {
        CanvasConversionAttributionDO row = new CanvasConversionAttributionDO();
        row.setCanvasId(10L);
        row.setEventLogId(99L);
        row.setUserId("user-1");
        row.setConversionEventCode("ORDER_PAID");
        row.setAttributionModel("LAST_TOUCH");
        row.setAttributedAt(LocalDateTime.now());
        return row;
    }
}
