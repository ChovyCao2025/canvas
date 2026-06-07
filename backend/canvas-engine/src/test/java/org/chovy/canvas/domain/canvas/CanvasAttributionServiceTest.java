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
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
        canvas.setAttributionModel("LAST_TOUCH");
        MessageSendRecordDO earlier = sentRecord(21L, 10L, "user-1",
                LocalDateTime.parse("2026-06-01T08:00:00"));
        MessageSendRecordDO send = sentRecord(22L, 10L, "user-1",
                LocalDateTime.parse("2026-06-01T10:00:00"));
        when(canvasMapper.selectList(any())).thenReturn(List.of(canvas));
        when(sendRecordMapper.selectList(any())).thenReturn(List.of(earlier, send));

        service.attribute(eventLog(99L, "ORDER_PAID", "user-1",
                LocalDateTime.parse("2026-06-01T12:00:00"),
                "{\"conversionAmount\":99.50}"));

        ArgumentCaptor<CanvasConversionAttributionDO> captor =
                ArgumentCaptor.forClass(CanvasConversionAttributionDO.class);
        verify(attributionMapper).insert(captor.capture());
        CanvasConversionAttributionDO row = captor.getValue();
        assertThat(row.getCanvasId()).isEqualTo(10L);
        assertThat(row.getSendRecordId()).isEqualTo(22L);
        assertThat(row.getEventLogId()).isEqualTo(99L);
        assertThat(row.getConversionAmount()).isEqualByComparingTo("99.50");
        assertThat(row.getAttributionModel()).isEqualTo("LAST_TOUCH");
        assertThat(row.getAttributionWeight()).isEqualByComparingTo("1.00000000");
        assertThat(row.getTouchCreatedAt()).isEqualTo(send.getCreatedAt());
    }

    @Test
    void firstTouchAttributesOnlyEarliestEligibleSentRecord() {
        CanvasDO canvas = canvasWithConversion("ORDER_PAID");
        canvas.setAttributionModel("FIRST_TOUCH");
        MessageSendRecordDO first = sentRecord(21L, 10L, "user-1",
                LocalDateTime.parse("2026-06-01T08:00:00"));
        MessageSendRecordDO latest = sentRecord(22L, 10L, "user-1",
                LocalDateTime.parse("2026-06-01T10:00:00"));
        when(canvasMapper.selectList(any())).thenReturn(List.of(canvas));
        when(sendRecordMapper.selectList(any())).thenReturn(List.of(first, latest));

        service.attribute(eventLog(99L, "ORDER_PAID", "user-1",
                LocalDateTime.parse("2026-06-01T12:00:00"),
                "{\"conversion_amount\":\"120.00\"}"));

        ArgumentCaptor<CanvasConversionAttributionDO> captor =
                ArgumentCaptor.forClass(CanvasConversionAttributionDO.class);
        verify(attributionMapper).insert(captor.capture());
        CanvasConversionAttributionDO row = captor.getValue();
        assertThat(row.getSendRecordId()).isEqualTo(21L);
        assertThat(row.getAttributionModel()).isEqualTo("FIRST_TOUCH");
        assertThat(row.getAttributionWeight()).isEqualByComparingTo("1.00000000");
        assertThat(row.getTouchCreatedAt()).isEqualTo(first.getCreatedAt());
        assertThat(row.getConversionAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void linearAttributionSplitsCreditAcrossEveryEligibleSentRecord() {
        CanvasDO canvas = canvasWithConversion("ORDER_PAID");
        canvas.setAttributionModel("LINEAR");
        List<MessageSendRecordDO> touches = List.of(
                sentRecord(21L, 10L, "user-1", LocalDateTime.parse("2026-06-01T08:00:00")),
                sentRecord(22L, 10L, "user-1", LocalDateTime.parse("2026-06-01T10:00:00")),
                sentRecord(23L, 10L, "user-1", LocalDateTime.parse("2026-06-01T11:00:00")));
        when(canvasMapper.selectList(any())).thenReturn(List.of(canvas));
        when(sendRecordMapper.selectList(any())).thenReturn(touches);

        service.attribute(eventLog(99L, "ORDER_PAID", "user-1",
                LocalDateTime.parse("2026-06-01T12:00:00"),
                "{\"conversionAmount\":90}"));

        ArgumentCaptor<CanvasConversionAttributionDO> captor =
                ArgumentCaptor.forClass(CanvasConversionAttributionDO.class);
        verify(attributionMapper, times(3)).insert(captor.capture());
        List<CanvasConversionAttributionDO> rows = captor.getAllValues();
        assertThat(rows).extracting(CanvasConversionAttributionDO::getSendRecordId)
                .containsExactly(21L, 22L, 23L);
        assertThat(rows).extracting(CanvasConversionAttributionDO::getAttributionModel)
                .containsOnly("LINEAR");
        assertThat(rows).extracting(CanvasConversionAttributionDO::getAttributionWeight)
                .containsExactly(new BigDecimal("0.33333333"),
                        new BigDecimal("0.33333333"),
                        new BigDecimal("0.33333334"));
        assertThat(sumWeights(rows)).isEqualByComparingTo("1.00000000");
    }

    @Test
    void timeDecayAttributesMoreCreditToNewerTouchesAndSumsToOne() {
        CanvasDO canvas = canvasWithConversion("ORDER_PAID");
        canvas.setAttributionModel("TIME_DECAY");
        List<MessageSendRecordDO> touches = List.of(
                sentRecord(21L, 10L, "user-1", LocalDateTime.parse("2026-05-28T12:00:00")),
                sentRecord(22L, 10L, "user-1", LocalDateTime.parse("2026-06-01T11:00:00")));
        when(canvasMapper.selectList(any())).thenReturn(List.of(canvas));
        when(sendRecordMapper.selectList(any())).thenReturn(touches);

        service.attribute(eventLog(99L, "ORDER_PAID", "user-1",
                LocalDateTime.parse("2026-06-01T12:00:00"),
                "{\"conversionAmount\":100}"));

        ArgumentCaptor<CanvasConversionAttributionDO> captor =
                ArgumentCaptor.forClass(CanvasConversionAttributionDO.class);
        verify(attributionMapper, times(2)).insert(captor.capture());
        List<CanvasConversionAttributionDO> rows = captor.getAllValues();
        assertThat(rows).extracting(CanvasConversionAttributionDO::getSendRecordId)
                .containsExactly(21L, 22L);
        assertThat(rows.get(1).getAttributionWeight())
                .isGreaterThan(rows.get(0).getAttributionWeight());
        assertThat(sumWeights(rows)).isEqualByComparingTo("1.00000000");
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

    private BigDecimal sumWeights(List<CanvasConversionAttributionDO> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        for (CanvasConversionAttributionDO row : new ArrayList<>(rows)) {
            sum = sum.add(row.getAttributionWeight());
        }
        return sum;
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
