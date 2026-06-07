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

@Service
public class GrowthRewardPoolService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final long LOW_INVENTORY_THRESHOLD = 10L;

    private final GrowthActivityMapper activityMapper;
    private final GrowthRewardPoolMapper poolMapper;
    private final ObjectMapper objectMapper;
    @SuppressWarnings("unused")
    private final Clock clock;

    @Autowired
    public GrowthRewardPoolService(GrowthActivityMapper activityMapper,
                                   GrowthRewardPoolMapper poolMapper,
                                   ObjectMapper objectMapper) {
        this(activityMapper, poolMapper, objectMapper, Clock.systemDefaultZone());
    }

    GrowthRewardPoolService(GrowthActivityMapper activityMapper,
                            GrowthRewardPoolMapper poolMapper,
                            ObjectMapper objectMapper,
                            Clock clock) {
        this.activityMapper = activityMapper;
        this.poolMapper = poolMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardPoolView upsertPool(Long tenantId, Long activityId, GrowthRewardPoolCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("growth reward pool command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);
        String poolKey = normalizeKey(command.poolKey(), "poolKey");
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
        return toView(row);
    }

    public List<GrowthRewardPoolView> listPools(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);
        return poolMapper.selectList(new LambdaQueryWrapper<GrowthRewardPoolDO>()
                        .eq(GrowthRewardPoolDO::getTenantId, scopedTenantId)
                        .eq(GrowthRewardPoolDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthRewardPoolDO::getUpdatedAt))
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> scopedActivityId.equals(row.getActivityId()))
                .map(this::toView)
                .toList();
    }

    private void validateActivity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(activityId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
    }

    private GrowthRewardPoolView toView(GrowthRewardPoolDO row) {
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
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private boolean inventoryLow(GrowthRewardPoolDO row) {
        if (!"LIMITED".equals(row.getInventoryMode())) {
            return false;
        }
        long total = defaultLong(row.getTotalInventory());
        long used = defaultLong(row.getReservedInventory()) + defaultLong(row.getGrantedInventory());
        long remaining = Math.max(0L, total - used);
        return total > 0 && remaining <= LOW_INVENTORY_THRESHOLD;
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("metadata must be JSON serializable", e);
        }
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

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

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

    private static String normalizeRewardType(String value) {
        String type = normalizeUpper(value, "COUPON");
        return switch (type) {
            case "COUPON", "POINTS", "LOYALTY" -> type;
            default -> throw new IllegalArgumentException("unsupported reward type: " + type);
        };
    }

    private static String normalizeGrantChannel(String value) {
        String channel = normalizeUpper(value, "COMMIT_ACTION");
        return switch (channel) {
            case "COMMIT_ACTION", "LOYALTY", "MANUAL", "PROVIDER" -> channel;
            default -> throw new IllegalArgumentException("unsupported grant channel: " + channel);
        };
    }

    private static String normalizeInventoryMode(String value) {
        String mode = normalizeUpper(value, "LIMITED");
        return switch (mode) {
            case "LIMITED", "UNLIMITED" -> mode;
            default -> throw new IllegalArgumentException("unsupported inventory mode: " + mode);
        };
    }

    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "ACTIVE");
        return switch (status) {
            case "ACTIVE", "PAUSED", "CLOSED" -> status;
            default -> throw new IllegalArgumentException("unsupported reward pool status: " + status);
        };
    }

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static Long normalizeInventory(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    private static Integer normalizeLimit(Integer value) {
        return value == null ? null : Math.max(0, value);
    }

    private static BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

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
