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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class CanvasAttributionService {

    private static final int WEIGHT_SCALE = 8;
    private static final Long NO_TOUCH_SEND_RECORD_ID = 0L;
    private static final BigDecimal FULL_CREDIT = new BigDecimal("1.00000000");
    private static final String MODEL_FIRST_TOUCH = "FIRST_TOUCH";
    private static final String MODEL_LAST_TOUCH = "LAST_TOUCH";
    private static final String MODEL_LINEAR = "LINEAR";
    private static final String MODEL_TIME_DECAY = "TIME_DECAY";

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
            List<MessageSendRecordDO> touches = eligibleTouches(canvas, eventLog);
            for (CanvasConversionAttributionDO row : attributionRows(canvas, eventLog, touches)) {
                insertAttribution(row);
            }
        }
    }

    public void insertAttribution(CanvasConversionAttributionDO row) {
        try {
            attributionMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            // Idempotent by (canvas_id, event_log_id, attribution_model, send_record_id).
        }
    }

    private List<MessageSendRecordDO> eligibleTouches(CanvasDO canvas, EventLogDO eventLog) {
        LocalDateTime eventTime = eventLog.getCreatedAt() == null ? LocalDateTime.now() : eventLog.getCreatedAt();
        int windowDays = canvas.getAttributionWindowDays() == null || canvas.getAttributionWindowDays() <= 0
                ? 7
                : canvas.getAttributionWindowDays();
        List<MessageSendRecordDO> rows = sendRecordMapper.selectList(new LambdaQueryWrapper<MessageSendRecordDO>()
                .eq(MessageSendRecordDO::getCanvasId, canvas.getId())
                .eq(MessageSendRecordDO::getUserId, eventLog.getUserId())
                .eq(MessageSendRecordDO::getStatus, MessageSendRecordDO.STATUS_SENT)
                .le(MessageSendRecordDO::getCreatedAt, eventTime)
                .ge(MessageSendRecordDO::getCreatedAt, eventTime.minusDays(windowDays))
                .orderByAsc(MessageSendRecordDO::getCreatedAt));
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(row -> row.getCreatedAt() == null
                        ? LocalDateTime.MIN
                        : row.getCreatedAt()))
                .toList();
    }

    private List<CanvasConversionAttributionDO> attributionRows(CanvasDO canvas,
                                                                EventLogDO eventLog,
                                                                List<MessageSendRecordDO> touches) {
        String model = attributionModel(canvas);
        if (touches == null || touches.isEmpty()) {
            return List.of(from(canvas, eventLog, null, model, FULL_CREDIT));
        }
        return switch (model) {
            case MODEL_FIRST_TOUCH -> List.of(from(canvas, eventLog, touches.getFirst(), model, FULL_CREDIT));
            case MODEL_LINEAR -> rowsWithWeights(canvas, eventLog, touches, model, equalScores(touches.size()));
            case MODEL_TIME_DECAY -> rowsWithWeights(canvas, eventLog, touches, model, timeDecayScores(eventLog, touches));
            case MODEL_LAST_TOUCH -> List.of(from(canvas, eventLog, touches.getLast(), model, FULL_CREDIT));
            default -> List.of(from(canvas, eventLog, touches.getLast(), MODEL_LAST_TOUCH, FULL_CREDIT));
        };
    }

    private List<CanvasConversionAttributionDO> rowsWithWeights(CanvasDO canvas,
                                                               EventLogDO eventLog,
                                                               List<MessageSendRecordDO> touches,
                                                               String model,
                                                               List<BigDecimal> scores) {
        List<BigDecimal> weights = normalizedWeights(scores);
        List<CanvasConversionAttributionDO> rows = new ArrayList<>(touches.size());
        for (int i = 0; i < touches.size(); i++) {
            rows.add(from(canvas, eventLog, touches.get(i), model, weights.get(i)));
        }
        return rows;
    }

    private List<BigDecimal> equalScores(int count) {
        List<BigDecimal> scores = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            scores.add(BigDecimal.ONE);
        }
        return scores;
    }

    private List<BigDecimal> timeDecayScores(EventLogDO eventLog, List<MessageSendRecordDO> touches) {
        LocalDateTime eventTime = eventLog.getCreatedAt() == null ? LocalDateTime.now() : eventLog.getCreatedAt();
        List<BigDecimal> scores = new ArrayList<>(touches.size());
        for (MessageSendRecordDO touch : touches) {
            LocalDateTime touchTime = touch.getCreatedAt() == null ? eventTime : touch.getCreatedAt();
            double ageDays = Math.max(0.0D,
                    Duration.between(touchTime, eventTime).toMillis()
                            / (double) Duration.ofDays(1).toMillis());
            scores.add(BigDecimal.valueOf(Math.pow(0.5D, ageDays)));
        }
        return scores;
    }

    private List<BigDecimal> normalizedWeights(List<BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) {
            return List.of();
        }
        BigDecimal totalScore = scores.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalScore.compareTo(BigDecimal.ZERO) <= 0) {
            totalScore = BigDecimal.valueOf(scores.size());
            scores = equalScores(scores.size());
        }
        List<BigDecimal> weights = new ArrayList<>(scores.size());
        BigDecimal assigned = BigDecimal.ZERO.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
        for (int i = 0; i < scores.size(); i++) {
            BigDecimal weight = i == scores.size() - 1
                    ? FULL_CREDIT.subtract(assigned).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP)
                    : scores.get(i).divide(totalScore, WEIGHT_SCALE, RoundingMode.HALF_UP);
            weights.add(weight);
            assigned = assigned.add(weight);
        }
        return weights;
    }

    private CanvasConversionAttributionDO from(CanvasDO canvas,
                                               EventLogDO eventLog,
                                               MessageSendRecordDO touch,
                                               String model,
                                               BigDecimal weight) {
        CanvasConversionAttributionDO row = new CanvasConversionAttributionDO();
        row.setCanvasId(canvas.getId());
        row.setUserId(eventLog.getUserId());
        row.setEventLogId(eventLog.getId());
        row.setSendRecordId(touch == null ? NO_TOUCH_SEND_RECORD_ID : touch.getId());
        row.setConversionEventCode(eventLog.getEventCode());
        row.setConversionAmount(conversionAmount(eventLog.getAttributes()));
        row.setAttributionModel(model);
        row.setAttributionWeight(weight == null ? FULL_CREDIT : weight.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP));
        row.setTouchCreatedAt(touch == null ? null : touch.getCreatedAt());
        row.setAttributedAt(LocalDateTime.now());
        return row;
    }

    private String attributionModel(CanvasDO canvas) {
        String model = canvas.getAttributionModel();
        if (model == null || model.isBlank()) {
            return MODEL_LAST_TOUCH;
        }
        String normalized = model.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case MODEL_FIRST_TOUCH, MODEL_LAST_TOUCH, MODEL_LINEAR, MODEL_TIME_DECAY -> normalized;
            default -> MODEL_LAST_TOUCH;
        };
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
