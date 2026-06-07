package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthActivityEventDO;
import org.chovy.canvas.dal.dataobject.GrowthActivityParticipantDO;
import org.chovy.canvas.dal.dataobject.GrowthReferralRelationDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthTaskProgressDO;
import org.chovy.canvas.dal.mapper.GrowthActivityEventMapper;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthActivityParticipantMapper;
import org.chovy.canvas.dal.mapper.GrowthReferralRelationMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardGrantMapper;
import org.chovy.canvas.dal.mapper.GrowthTaskProgressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class GrowthActivityReportService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthActivityParticipantMapper participantMapper;
    private final GrowthRewardGrantMapper grantMapper;
    private final GrowthReferralRelationMapper referralRelationMapper;
    private final GrowthTaskProgressMapper taskProgressMapper;
    private final GrowthActivityEventMapper eventMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public GrowthActivityReportService(GrowthActivityMapper activityMapper,
                                       GrowthActivityParticipantMapper participantMapper,
                                       GrowthRewardGrantMapper grantMapper,
                                       GrowthReferralRelationMapper referralRelationMapper,
                                       GrowthTaskProgressMapper taskProgressMapper,
                                       GrowthActivityEventMapper eventMapper,
                                       ObjectMapper objectMapper) {
        this.activityMapper = activityMapper;
        this.participantMapper = participantMapper;
        this.grantMapper = grantMapper;
        this.referralRelationMapper = referralRelationMapper;
        this.taskProgressMapper = taskProgressMapper;
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public GrowthActivityReportView summarize(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);

        List<GrowthActivityParticipantDO> participants = participantMapper.selectList(
                        new LambdaQueryWrapper<GrowthActivityParticipantDO>()
                                .eq(GrowthActivityParticipantDO::getTenantId, scopedTenantId)
                                .eq(GrowthActivityParticipantDO::getActivityId, scopedActivityId))
                .stream()
                .filter(row -> belongsTo(row.getTenantId(), row.getActivityId(), scopedTenantId, scopedActivityId))
                .toList();

        List<GrowthReferralRelationDO> referrals = referralRelationMapper.selectList(
                        new LambdaQueryWrapper<GrowthReferralRelationDO>()
                                .eq(GrowthReferralRelationDO::getTenantId, scopedTenantId)
                                .eq(GrowthReferralRelationDO::getActivityId, scopedActivityId))
                .stream()
                .filter(row -> belongsTo(row.getTenantId(), row.getActivityId(), scopedTenantId, scopedActivityId))
                .toList();

        List<GrowthRewardGrantDO> grants = grantMapper.selectList(
                        new LambdaQueryWrapper<GrowthRewardGrantDO>()
                                .eq(GrowthRewardGrantDO::getTenantId, scopedTenantId)
                                .eq(GrowthRewardGrantDO::getActivityId, scopedActivityId))
                .stream()
                .filter(row -> belongsTo(row.getTenantId(), row.getActivityId(), scopedTenantId, scopedActivityId))
                .toList();

        List<GrowthTaskProgressDO> taskProgress = taskProgressMapper.selectList(
                        new LambdaQueryWrapper<GrowthTaskProgressDO>()
                                .eq(GrowthTaskProgressDO::getTenantId, scopedTenantId)
                                .eq(GrowthTaskProgressDO::getActivityId, scopedActivityId))
                .stream()
                .filter(row -> belongsTo(row.getTenantId(), row.getActivityId(), scopedTenantId, scopedActivityId))
                .toList();

        List<GrowthActivityEventDO> conversions = eventMapper.selectList(
                        new LambdaQueryWrapper<GrowthActivityEventDO>()
                                .eq(GrowthActivityEventDO::getTenantId, scopedTenantId)
                                .eq(GrowthActivityEventDO::getActivityId, scopedActivityId)
                                .eq(GrowthActivityEventDO::getEventType, "CONVERSION_EVIDENCE"))
                .stream()
                .filter(row -> belongsTo(row.getTenantId(), row.getActivityId(), scopedTenantId, scopedActivityId))
                .filter(row -> "CONVERSION_EVIDENCE".equals(normalize(row.getEventType())))
                .toList();

        GrowthActivityReportView.ParticipationMetrics participation = new GrowthActivityReportView.ParticipationMetrics(
                participants.size(),
                countStatus(participants.stream().map(GrowthActivityParticipantDO::getStatus).toList(), "ACTIVE"));

        GrowthActivityReportView.ReferralMetrics referral = new GrowthActivityReportView.ReferralMetrics(
                referrals.size(),
                countStatus(referrals.stream().map(GrowthReferralRelationDO::getStatus).toList(), "QUALIFIED"),
                countStatus(referrals.stream().map(GrowthReferralRelationDO::getStatus).toList(), "PENDING"),
                countAnyStatus(referrals.stream().map(GrowthReferralRelationDO::getStatus).toList(), "REJECTED", "FAILED"));

        BigDecimal totalCost = grants.stream()
                .map(GrowthRewardGrantDO::getCostAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        GrowthActivityReportView.GrantMetrics grantMetrics = new GrowthActivityReportView.GrantMetrics(
                grants.size(),
                countStatus(grants.stream().map(GrowthRewardGrantDO::getStatus).toList(), "RESERVED"),
                countStatus(grants.stream().map(GrowthRewardGrantDO::getStatus).toList(), "SUCCESS"),
                countStatus(grants.stream().map(GrowthRewardGrantDO::getStatus).toList(), "FAILED"),
                countAnyStatus(grants.stream().map(GrowthRewardGrantDO::getStatus).toList(), "CANCELED", "CANCELLED"),
                countStatus(grants.stream().map(GrowthRewardGrantDO::getStatus).toList(), "REDEEMED"),
                countStatus(grants.stream().map(GrowthRewardGrantDO::getStatus).toList(), "EXPIRED"),
                totalCost);

        BigDecimal conversionAmount = conversions.stream()
                .map(row -> conversionAmount(row.getPayloadJson()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal roi = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : conversionAmount.divide(totalCost, 4, RoundingMode.HALF_UP);
        GrowthActivityReportView.ConversionMetrics conversion = new GrowthActivityReportView.ConversionMetrics(
                conversions.size(),
                conversionAmount,
                roi);

        long completedProgress = countStatus(taskProgress.stream().map(GrowthTaskProgressDO::getStatus).toList(), "COMPLETED");
        GrowthActivityReportView.TaskMetrics task = new GrowthActivityReportView.TaskMetrics(
                taskProgress.size(),
                completedProgress,
                GrowthActivityReportView.rate(completedProgress, taskProgress.size()));

        return new GrowthActivityReportView(scopedTenantId, scopedActivityId, participation, referral, grantMetrics, conversion, task);
    }

    private void validateActivity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(activityId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
    }

    private BigDecimal conversionAmount(String payloadJson) {
        Map<String, Object> payload = fromJson(payloadJson);
        for (String key : List.of("amount", "conversionAmount", "orderAmount", "revenueAmount")) {
            BigDecimal amount = decimal(payload.get(key));
            if (amount != null) {
                return amount;
            }
        }
        return BigDecimal.ZERO;
    }

    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static long countStatus(List<String> statuses, String expected) {
        return statuses.stream()
                .filter(status -> expected.equals(normalize(status)))
                .count();
    }

    private static long countAnyStatus(List<String> statuses, String... expected) {
        return statuses.stream()
                .map(GrowthActivityReportService::normalize)
                .filter(status -> List.of(expected).contains(status))
                .count();
    }

    private static boolean belongsTo(Long tenantId, Long activityId, Long expectedTenantId, Long expectedActivityId) {
        return expectedTenantId.equals(tenantId) && expectedActivityId.equals(activityId);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
