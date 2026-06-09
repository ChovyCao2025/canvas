package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardPoolMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GrowthRewardPoolService 编排 domain.marketing 场景的领域业务规则。
 */
@Service
public class GrowthRewardPoolService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final long LOW_INVENTORY_THRESHOLD = 10L;

    private final GrowthActivityMapper activityMapper;
    private final GrowthRewardPoolMapper poolMapper;
    private final ObjectMapper objectMapper;
    @SuppressWarnings("unused")
    private final Clock clock;

    /**
     * 创建 GrowthRewardPoolService 实例并注入 domain.marketing 场景依赖。
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param poolMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public GrowthRewardPoolService(GrowthActivityMapper activityMapper,
                                   GrowthRewardPoolMapper poolMapper,
                                   ObjectMapper objectMapper) {
        this(activityMapper, poolMapper, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 GrowthRewardPoolService 流程，围绕 growth reward pool service 完成校验、计算或结果组装。
     *
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param poolMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    GrowthRewardPoolService(GrowthActivityMapper activityMapper,
                            GrowthRewardPoolMapper poolMapper,
                            ObjectMapper objectMapper,
                            Clock clock) {
        this.activityMapper = activityMapper;
        this.poolMapper = poolMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建或更新业务记录，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardPoolView upsertPool(Long tenantId, Long activityId, GrowthRewardPoolCommand command, String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("growth reward pool command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);
        String poolKey = normalizeKey(command.poolKey(), "poolKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        GrowthRewardPoolDO row = poolMapper.selectOne(new LambdaQueryWrapper<GrowthRewardPoolDO>()
                .eq(GrowthRewardPoolDO::getTenantId, scopedTenantId)
                .eq(GrowthRewardPoolDO::getActivityId, scopedActivityId)
                .eq(GrowthRewardPoolDO::getPoolKey, poolKey)
                .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new GrowthRewardPoolDO();
            row.setTenantId(scopedTenantId);
            row.setActivityId(scopedActivityId);
            row.setPoolKey(poolKey);
            row.setReservedInventory(0L);
            row.setGrantedInventory(0L);
            row.setReservedAmount(BigDecimal.ZERO);
            row.setGrantedAmount(BigDecimal.ZERO);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setRewardType(normalizeRewardType(command.rewardType()));
        row.setGrantChannel(normalizeGrantChannel(command.grantChannel()));
        row.setCouponTypeKey(trimToLimit(command.couponTypeKey(), 128));
        row.setLoyaltyRewardKey(trimToLimit(command.loyaltyRewardKey(), 128));
        row.setPointsType(normalizeOptionalUpper(command.pointsType()));
        row.setExternalContractKey(trimToLimit(command.externalContractKey(), 128));
        row.setInventoryMode(normalizeInventoryMode(command.inventoryMode()));
        row.setTotalInventory(normalizeInventory(command.totalInventory()));
        row.setPerUserLimit(normalizeLimit(command.perUserLimit()));
        row.setPerReferralLimit(normalizeLimit(command.perReferralLimit()));
        row.setBudgetAmount(defaultAmount(command.budgetAmount()));
        row.setCostCurrency(normalizeUpper(command.costCurrency(), "CNY"));
        row.setStatus(normalizeStatus(command.status()));
        row.setMetadataJson(toJson(command.metadata()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            poolMapper.insert(row);
        } else {
            poolMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<GrowthRewardPoolView> listPools(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return poolMapper.selectList(new LambdaQueryWrapper<GrowthRewardPoolDO>()
                        .eq(GrowthRewardPoolDO::getTenantId, scopedTenantId)
                        .eq(GrowthRewardPoolDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthRewardPoolDO::getUpdatedAt))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> scopedActivityId.equals(row.getActivityId()))
                .map(this::toView)
                .toList();
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
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private GrowthRewardPoolView toView(GrowthRewardPoolDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new GrowthRewardPoolView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getPoolKey(),
                row.getRewardType(),
                row.getGrantChannel(),
                row.getCouponTypeKey(),
                row.getLoyaltyRewardKey(),
                row.getPointsType(),
                row.getExternalContractKey(),
                row.getInventoryMode(),
                row.getTotalInventory(),
                defaultLong(row.getReservedInventory()),
                defaultLong(row.getGrantedInventory()),
                row.getPerUserLimit(),
                row.getPerReferralLimit(),
                defaultAmount(row.getBudgetAmount()),
                defaultAmount(row.getReservedAmount()),
                defaultAmount(row.getGrantedAmount()),
                row.getCostCurrency(),
                row.getStatus(),
                inventoryLow(row),
                fromJson(row.getMetadataJson()),
                row.getCreatedBy(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 执行 inventoryLow 流程，围绕 inventory low 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 inventory low 的布尔判断结果。
     */
    private boolean inventoryLow(GrowthRewardPoolDO row) {
        if (!"LIMITED".equals(row.getInventoryMode())) {
            return false;
        }
        long total = defaultLong(row.getTotalInventory());
        long used = defaultLong(row.getReservedInventory()) + defaultLong(row.getGrantedInventory());
        long remaining = Math.max(0L, total - used);
        return total > 0 && remaining <= LOW_INVENTORY_THRESHOLD;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param String string 参数，用于 toJson 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("metadata must be JSON serializable", e);
        }
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

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeKey(String value, String field) {
        String normalized = required(value, field).trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeRewardType(String value) {
        String type = normalizeUpper(value, "COUPON");
        return switch (type) {
            case "COUPON", "POINTS", "LOYALTY" -> type;
            default -> throw new IllegalArgumentException("unsupported reward type: " + type);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeGrantChannel(String value) {
        String channel = normalizeUpper(value, "COMMIT_ACTION");
        return switch (channel) {
            case "COMMIT_ACTION", "LOYALTY", "MANUAL", "PROVIDER" -> channel;
            default -> throw new IllegalArgumentException("unsupported grant channel: " + channel);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeInventoryMode(String value) {
        String mode = normalizeUpper(value, "LIMITED");
        return switch (mode) {
            case "LIMITED", "UNLIMITED" -> mode;
            default -> throw new IllegalArgumentException("unsupported inventory mode: " + mode);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "ACTIVE");
        return switch (status) {
            case "ACTIVE", "PAUSED", "CLOSED" -> status;
            default -> throw new IllegalArgumentException("unsupported reward pool status: " + status);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeUpper 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static Long normalizeInventory(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static Integer normalizeLimit(Integer value) {
        return value == null ? null : Math.max(0, value);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default amount 计算得到的数量、金额或指标值。
     */
    private static BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default long 计算得到的数量、金额或指标值。
     */
    private static long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String trimToLimit(String value, int limit) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit);
    }
}
