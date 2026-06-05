package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasConversionAttributionDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.CanvasConversionAttributionMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CanvasAttributionService {

    private static final String MODEL_LAST_TOUCH = "LAST_TOUCH";

    private final CanvasMapper canvasMapper;
    private final MessageSendRecordMapper sendRecordMapper;
    private final CanvasConversionAttributionMapper attributionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CanvasAttributionService(CanvasMapper canvasMapper,
                                    MessageSendRecordMapper sendRecordMapper,
                                    CanvasConversionAttributionMapper attributionMapper) {
        this.canvasMapper = canvasMapper;
        this.sendRecordMapper = sendRecordMapper;
        this.attributionMapper = attributionMapper;
    }

    public void attribute(EventLogDO eventLog) {
        if (eventLog == null || eventLog.getEventCode() == null || eventLog.getEventCode().isBlank()
                || eventLog.getUserId() == null || eventLog.getUserId().isBlank()) {
            return;
        }
        List<CanvasDO> canvases = canvasMapper.selectList(new LambdaQueryWrapper<CanvasDO>()
                .eq(CanvasDO::getConversionEventCode, eventLog.getEventCode()));
        if (canvases == null || canvases.isEmpty()) {
            return;
        }
        for (CanvasDO canvas : canvases) {
            insertAttribution(from(canvas, eventLog, latestTouch(canvas, eventLog)));
        }
    }

    public void insertAttribution(CanvasConversionAttributionDO row) {
        try {
            attributionMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            // Idempotent by (canvas_id, event_log_id); retries should not double-count conversions.
        }
    }

    private MessageSendRecordDO latestTouch(CanvasDO canvas, EventLogDO eventLog) {
        LocalDateTime eventTime = eventLog.getCreatedAt() == null ? LocalDateTime.now() : eventLog.getCreatedAt();
        int windowDays = canvas.getAttributionWindowDays() == null || canvas.getAttributionWindowDays() <= 0
                ? 7
                : canvas.getAttributionWindowDays();
        return sendRecordMapper.selectOne(new LambdaQueryWrapper<MessageSendRecordDO>()
                .eq(MessageSendRecordDO::getCanvasId, canvas.getId())
                .eq(MessageSendRecordDO::getUserId, eventLog.getUserId())
                .eq(MessageSendRecordDO::getStatus, MessageSendRecordDO.STATUS_SENT)
                .le(MessageSendRecordDO::getCreatedAt, eventTime)
                .ge(MessageSendRecordDO::getCreatedAt, eventTime.minusDays(windowDays))
                .orderByDesc(MessageSendRecordDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    private CanvasConversionAttributionDO from(CanvasDO canvas,
                                               EventLogDO eventLog,
                                               MessageSendRecordDO touch) {
        CanvasConversionAttributionDO row = new CanvasConversionAttributionDO();
        row.setCanvasId(canvas.getId());
        row.setUserId(eventLog.getUserId());
        row.setEventLogId(eventLog.getId());
        row.setSendRecordId(touch == null ? null : touch.getId());
        row.setConversionEventCode(eventLog.getEventCode());
        row.setConversionAmount(conversionAmount(eventLog.getAttributes()));
        row.setAttributionModel(MODEL_LAST_TOUCH);
        row.setAttributedAt(LocalDateTime.now());
        return row;
    }

    private BigDecimal conversionAmount(String attributes) {
        if (attributes == null || attributes.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> values = objectMapper.readValue(
                    attributes,
                    new TypeReference<Map<String, Object>>() {
                    });
            Object amount = values.getOrDefault("conversionAmount", values.get("conversion_amount"));
            if (amount instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            if (amount instanceof String text && !text.isBlank()) {
                return new BigDecimal(text.trim());
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
