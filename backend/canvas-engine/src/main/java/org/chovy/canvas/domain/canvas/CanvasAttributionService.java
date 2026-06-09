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

/**
 * CanvasAttributionService 编排 domain.canvas 场景的领域业务规则。
 */
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

    /**
     * 创建 CanvasAttributionService 实例并注入 domain.canvas 场景依赖。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param sendRecordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param attributionMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CanvasAttributionService(CanvasMapper canvasMapper,
                                    MessageSendRecordMapper sendRecordMapper,
                                    CanvasConversionAttributionMapper attributionMapper) {
        this.canvasMapper = canvasMapper;
        this.sendRecordMapper = sendRecordMapper;
        this.attributionMapper = attributionMapper;
    }

    /**
     * 根据转化事件为匹配的 Canvas 生成触点归因记录。
     * 方法会按事件编码查找配置了转化目标的画布，读取用户在归因窗口内的已发送消息，并按画布归因模型写入归因行；事件不完整或无匹配画布时直接跳过。
     */
    public void attribute(EventLogDO eventLog) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (eventLog == null || eventLog.getEventCode() == null || eventLog.getEventCode().isBlank()
                || eventLog.getUserId() == null || eventLog.getUserId().isBlank()) {
            return;
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<CanvasDO> canvases = canvasMapper.selectList(new LambdaQueryWrapper<CanvasDO>()
                .eq(CanvasDO::getConversionEventCode, eventLog.getEventCode()));
        if (canvases == null || canvases.isEmpty()) {
            return;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CanvasDO canvas : canvases) {
            List<MessageSendRecordDO> touches = eligibleTouches(canvas, eventLog);
            for (CanvasConversionAttributionDO row : attributionRows(canvas, eventLog, touches)) {
                insertAttribution(row);
            }
        }
    }

    /**
     * 写入单条转化归因记录。
     * 依赖唯一键保证重放事件幂等，重复插入会被吞掉而不影响上游事件处理。
     */
    public void insertAttribution(CanvasConversionAttributionDO row) {
        try {
            attributionMapper.insert(row);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DuplicateKeyException ignored) {
            // Idempotent by (canvas_id, event_log_id, attribution_model, send_record_id).
        }
    }

    /**
     * 执行 eligibleTouches 流程，围绕 eligible touches 完成校验、计算或结果组装。
     *
     * @param canvas canvas 参数，用于 eligibleTouches 流程中的校验、计算或对象转换。
     * @param eventLog event log 参数，用于 eligibleTouches 流程中的校验、计算或对象转换。
     * @return 返回 eligible touches 汇总后的集合、分页或映射视图。
     */
    private List<MessageSendRecordDO> eligibleTouches(CanvasDO canvas, EventLogDO eventLog) {
        LocalDateTime eventTime = eventLog.getCreatedAt() == null ? LocalDateTime.now() : eventLog.getCreatedAt();
        int windowDays = canvas.getAttributionWindowDays() == null || canvas.getAttributionWindowDays() <= 0
                ? 7
                : canvas.getAttributionWindowDays();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<MessageSendRecordDO> rows = sendRecordMapper.selectList(new LambdaQueryWrapper<MessageSendRecordDO>()
                .eq(MessageSendRecordDO::getCanvasId, canvas.getId())
                .eq(MessageSendRecordDO::getUserId, eventLog.getUserId())
                .eq(MessageSendRecordDO::getStatus, MessageSendRecordDO.STATUS_SENT)
                .le(MessageSendRecordDO::getCreatedAt, eventTime)
                .ge(MessageSendRecordDO::getCreatedAt, eventTime.minusDays(windowDays))
                .orderByAsc(MessageSendRecordDO::getCreatedAt));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return rows.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(row -> row.getCreatedAt() == null
                        ? LocalDateTime.MIN
                        : row.getCreatedAt()))
                .toList();
    }

    /**
     * 执行 attributionRows 流程，围绕 attribution rows 完成校验、计算或结果组装。
     *
     * @param canvas canvas 参数，用于 attributionRows 流程中的校验、计算或对象转换。
     * @param eventLog event log 参数，用于 attributionRows 流程中的校验、计算或对象转换。
     * @param touches touches 参数，用于 attributionRows 流程中的校验、计算或对象转换。
     * @return 返回 attribution rows 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param canvas canvas 参数，用于 rowsWithWeights 流程中的校验、计算或对象转换。
     * @param eventLog event log 参数，用于 rowsWithWeights 流程中的校验、计算或对象转换。
     * @param touches touches 参数，用于 rowsWithWeights 流程中的校验、计算或对象转换。
     * @param model model 参数，用于 rowsWithWeights 流程中的校验、计算或对象转换。
     * @param scores scores 参数，用于 rowsWithWeights 流程中的校验、计算或对象转换。
     * @return 返回 rows with weights 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 执行 equalScores 流程，围绕 equal scores 完成校验、计算或结果组装。
     *
     * @param count 分页、数量或序号参数，用于控制处理规模。
     * @return 返回 equal scores 汇总后的集合、分页或映射视图。
     */
    private List<BigDecimal> equalScores(int count) {
        List<BigDecimal> scores = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            scores.add(BigDecimal.ONE);
        }
        return scores;
    }

    /**
     * 执行 timeDecayScores 流程，围绕 time decay scores 完成校验、计算或结果组装。
     *
     * @param eventLog event log 参数，用于 timeDecayScores 流程中的校验、计算或对象转换。
     * @param touches touches 参数，用于 timeDecayScores 流程中的校验、计算或对象转换。
     * @return 返回 time decay scores 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 规范化输入值。
     *
     * @param scores scores 参数，用于 normalizedWeights 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<BigDecimal> normalizedWeights(List<BigDecimal> scores) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (scores == null || scores.isEmpty()) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return weights;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param canvas canvas 参数，用于 from 流程中的校验、计算或对象转换。
     * @param eventLog event log 参数，用于 from 流程中的校验、计算或对象转换。
     * @param touch touch 参数，用于 from 流程中的校验、计算或对象转换。
     * @param model model 参数，用于 from 流程中的校验、计算或对象转换。
     * @param weight weight 参数，用于 from 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 执行 attributionModel 流程，围绕 attribution model 完成校验、计算或结果组装。
     *
     * @param canvas canvas 参数，用于 attributionModel 流程中的校验、计算或对象转换。
     * @return 返回 attribution model 生成的文本或业务键。
     */
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

    /**
     * 执行 conversionAmount 流程，围绕 conversion amount 完成校验、计算或结果组装。
     *
     * @param attributes attributes 参数，用于 conversionAmount 流程中的校验、计算或对象转换。
     * @return 返回 conversion amount 计算得到的数量、金额或指标值。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
