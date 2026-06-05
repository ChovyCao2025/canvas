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

@Service
public class PredictionProfileWriter {

    public static final String CHURN_PROBABILITY = "churn_probability";
    public static final String CHURN_RISK_BAND = "churn_risk_band";
    public static final String BEST_SEND_HOUR = "best_send_hour";
    public static final String PREDICTION_UPDATED_AT = "prediction_updated_at";

    private final CdpUserProfileMapper profileMapper;
    private final ObjectMapper objectMapper;

    public PredictionProfileWriter(CdpUserProfileMapper profileMapper, ObjectMapper objectMapper) {
        this.profileMapper = profileMapper;
        this.objectMapper = objectMapper;
    }

    public void write(String userId,
                      BigDecimal churnProbability,
                      String churnRiskBand,
                      int bestSendHour,
                      LocalDateTime updatedAt) {
        CdpUserProfileDO profile = profileMapper.selectOne(new LambdaQueryWrapper<CdpUserProfileDO>()
                .eq(CdpUserProfileDO::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
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

    private Map<String, Object> readProperties(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String writeProperties(Map<String, Object> properties) {
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (Exception e) {
            throw new IllegalArgumentException("prediction profile properties cannot be serialized", e);
        }
    }
}
