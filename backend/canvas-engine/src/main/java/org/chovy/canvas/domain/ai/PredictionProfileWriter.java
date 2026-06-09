package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PredictionProfileWriter 编排 domain.ai 场景的领域业务规则。
 */
@Service
public class PredictionProfileWriter {

    public static final String CHURN_PROBABILITY = "churn_probability";
    public static final String CHURN_RISK_BAND = "churn_risk_band";
    public static final String BEST_SEND_HOUR = "best_send_hour";
    public static final String PREDICTION_UPDATED_AT = "prediction_updated_at";

    private final CdpUserProfileMapper profileMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建 PredictionProfileWriter 实例并注入 domain.ai 场景依赖。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public PredictionProfileWriter(CdpUserProfileMapper profileMapper, ObjectMapper objectMapper) {
        this.profileMapper = profileMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * write 处理 domain.ai 场景的业务逻辑。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param churnProbability churn probability 参数，用于 write 流程中的校验、计算或对象转换。
     * @param churnRiskBand churn risk band 参数，用于 write 流程中的校验、计算或对象转换。
     * @param bestSendHour best send hour 参数，用于 write 流程中的校验、计算或对象转换。
     * @param updatedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    public void write(String userId,
                      BigDecimal churnProbability,
                      String churnRiskBand,
                      int bestSendHour,
                      LocalDateTime updatedAt) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CdpUserProfileDO profile = profileMapper.selectOne(new LambdaQueryWrapper<CdpUserProfileDO>()
                .eq(CdpUserProfileDO::getUserId, userId)
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (profile == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        Map<String, Object> properties = new LinkedHashMap<>(readProperties(profile.getPropertiesJson()));
        properties.put(CHURN_PROBABILITY, churnProbability.doubleValue());
        properties.put(CHURN_RISK_BAND, churnRiskBand);
        properties.put(BEST_SEND_HOUR, bestSendHour);
        properties.put(PREDICTION_UPDATED_AT, updatedAt.toString());
        profile.setPropertiesJson(writeProperties(properties));
        profileMapper.updateById(profile);
    }

    /**
     * 查询或读取业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 readProperties 流程生成的业务结果。
     */
    private Map<String, Object> readProperties(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param String string 参数，用于 writeProperties 流程中的校验、计算或对象转换。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @return 返回 write properties 生成的文本或业务键。
     */
    private String writeProperties(Map<String, Object> properties) {
        try {
            return objectMapper.writeValueAsString(properties);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("prediction profile properties cannot be serialized", e);
        }
    }
}
