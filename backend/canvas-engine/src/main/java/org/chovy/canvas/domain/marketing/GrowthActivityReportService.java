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

/**
 * GrowthActivityReportService 编排 domain.marketing 场景的领域业务规则。
 */
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

    /**
     * 创建 GrowthActivityReportService 实例并注入 domain.marketing 场景依赖。
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param participantMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param grantMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param referralRelationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param taskProgressMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 汇总活动报表指标，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @return 返回本次处理的状态、计数、命中明细或治理结论，供控制器和调度任务判断后续动作
     */
    public GrowthActivityReportView summarize(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<GrowthActivityParticipantDO> participants = participantMapper.selectList(
                        new LambdaQueryWrapper<GrowthActivityParticipantDO>()
                                .eq(GrowthActivityParticipantDO::getTenantId, scopedTenantId)
                                .eq(GrowthActivityParticipantDO::getActivityId, scopedActivityId))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
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

        // 汇总前面计算出的状态和明细，返回给调用方。
        return new GrowthActivityReportView(scopedTenantId, scopedActivityId, participation, referral, grantMetrics, conversion, task);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     */
    private void validateActivity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(activityId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
    }

    /**
     * 执行 conversionAmount 流程，围绕 conversion amount 完成校验、计算或结果组装。
     *
     * @param payloadJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 conversion amount 计算得到的数量、金额或指标值。
     */
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

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
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
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param statuses 待处理业务值，用于规则计算、转换或外部调用。
     * @param expected 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回统计数量。
     */
    private static long countStatus(List<String> statuses, String expected) {
        return statuses.stream()
                .filter(status -> expected.equals(normalize(status)))
                .count();
    }

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param statuses 待处理业务值，用于规则计算、转换或外部调用。
     * @param expected 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回统计数量。
     */
    private static long countAnyStatus(List<String> statuses, String... expected) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return statuses.stream()
                .map(GrowthActivityReportService::normalize)
                .filter(status -> List.of(expected).contains(status))
                .count();
    }

    /**
     * 执行 belongsTo 流程，围绕 belongs to 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param expectedTenantId 业务对象 ID，用于定位具体记录。
     * @param expectedActivityId 业务对象 ID，用于定位具体记录。
     * @return 返回布尔判断结果。
     */
    private static boolean belongsTo(Long tenantId, Long activityId, Long expectedTenantId, Long expectedActivityId) {
        return expectedTenantId.equals(tenantId) && expectedActivityId.equals(activityId);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 safe tenant id 计算得到的数量、金额或指标值。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
