package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardGrantMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardPoolMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GrowthRewardGrantService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthRewardPoolMapper poolMapper;
    private final GrowthRewardGrantMapper grantMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public GrowthRewardGrantService(GrowthActivityMapper activityMapper,
                                    GrowthRewardPoolMapper poolMapper,
                                    GrowthRewardGrantMapper grantMapper,
                                    ObjectMapper objectMapper) {
        this(activityMapper, poolMapper, grantMapper, objectMapper, Clock.systemDefaultZone());
    }

    GrowthRewardGrantService(GrowthActivityMapper activityMapper,
                             GrowthRewardPoolMapper poolMapper,
                             GrowthRewardGrantMapper grantMapper,
                             ObjectMapper objectMapper,
                             Clock clock) {
        this.activityMapper = activityMapper;
        this.poolMapper = poolMapper;
        this.grantMapper = grantMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public List<GrowthRewardGrantView> listGrants(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        return grantMapper.selectList(new LambdaQueryWrapper<GrowthRewardGrantDO>()
                        .eq(GrowthRewardGrantDO::getTenantId, scopedTenantId)
                        .eq(GrowthRewardGrantDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthRewardGrantDO::getUpdatedAt))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView createGrant(Long tenantId, Long activityId, GrowthRewardGrantCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("growth reward grant command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);
        GrowthRewardPoolDO pool = validatePool(scopedTenantId, scopedActivityId, command.poolId());
        if (!"ACTIVE".equals(pool.getStatus())) {
            throw new IllegalArgumentException("reward pool is not active");
        }
        String idempotencyKey = required(command.idempotencyKey(), "idempotencyKey");
        GrowthRewardGrantDO existing = grantMapper.selectOne(new LambdaQueryWrapper<GrowthRewardGrantDO>()
                .eq(GrowthRewardGrantDO::getTenantId, scopedTenantId)
                .eq(GrowthRewardGrantDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
        if (existing != null) {
            return toView(existing);
        }
        BigDecimal costAmount = defaultAmount(command.costAmount());
        ensureInventoryAvailable(pool);
        reservePool(pool, costAmount, actor);
        GrowthRewardGrantDO row = new GrowthRewardGrantDO();
        row.setTenantId(scopedTenantId);
        row.setActivityId(scopedActivityId);
        row.setPoolId(pool.getId());
        row.setParticipantId(command.participantId());
        row.setReferralRelationId(command.referralRelationId());
        row.setTaskProgressId(command.taskProgressId());
        row.setGrantReason(normalizeUpper(command.grantReason(), "MANUAL"));
        row.setStatus("RESERVED");
        row.setIdempotencyKey(idempotencyKey);
        row.setProviderRequestJson(toJson(command.providerRequest()));
        row.setProviderResponseJson("{}");
        row.setCostAmount(costAmount);
        row.setCreatedBy(defaultString(actor, "system"));
        row.setUpdatedBy(defaultString(actor, "system"));
        grantMapper.insert(row);
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView markSuccess(Long tenantId, Long grantId, Map<String, Object> providerResponse, String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if (!"RESERVED".equals(row.getStatus()) && !"FAILED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot mark reward grant success from status " + row.getStatus());
        }
        return transition(row, "SUCCESS", providerResponse, actor, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView markFailure(Long tenantId, Long grantId, Map<String, Object> providerResponse, String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if (!"RESERVED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot mark reward grant failure from status " + row.getStatus());
        }
        return transition(row, "FAILED", providerResponse, actor, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView retryGrant(Long tenantId, Long grantId, String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if (!"FAILED".equals(row.getStatus()) && !"CANCELED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot retry reward grant from status " + row.getStatus());
        }
        GrowthRewardPoolDO pool = poolMapper.selectById(row.getPoolId());
        if (pool != null && row.getTenantId().equals(pool.getTenantId())) {
            ensureInventoryAvailable(pool);
            reservePool(pool, defaultAmount(row.getCostAmount()), actor);
        }
        row.setStatus("RESERVED");
        row.setProviderResponseJson(toJson(Map.of("retry", true)));
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        grantMapper.updateById(row);
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView cancelGrant(Long tenantId, Long grantId, String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if ("SUCCESS".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot cancel successful reward grant");
        }
        return transition(row, "CANCELED", fromJson(row.getProviderResponseJson()), actor, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView markRedeemed(Long tenantId,
                                              Long grantId,
                                              Map<String, Object> providerResponse,
                                              String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if (!"SUCCESS".equals(row.getStatus()) && !"REDEEMED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot redeem reward grant from status " + row.getStatus());
        }
        return transition(row, "REDEEMED", providerResponse, actor, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView markExpired(Long tenantId,
                                             Long grantId,
                                             Map<String, Object> providerResponse,
                                             String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if (!"SUCCESS".equals(row.getStatus()) && !"EXPIRED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot expire reward grant from status " + row.getStatus());
        }
        return transition(row, "EXPIRED", providerResponse, actor, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView reconcileGrant(Long tenantId,
                                                Long grantId,
                                                String providerStatus,
                                                Map<String, Object> providerResponse,
                                                String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        String status = normalizeProviderStatus(providerStatus);
        if ("REDEEMED".equals(row.getStatus()) || "EXPIRED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot reconcile terminal reward grant from status " + row.getStatus());
        }
        return transition(row, status, providerResponse, actor, true);
    }

    private GrowthRewardGrantView transition(GrowthRewardGrantDO row,
                                             String status,
                                             Map<String, Object> providerResponse,
                                             String actor,
                                             boolean updateCounters) {
        String previousStatus = row.getStatus();
        if (updateCounters) {
            updatePoolCounters(row, previousStatus, status, actor);
        }
        row.setStatus(status);
        row.setProviderResponseJson(toJson(providerResponse));
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        grantMapper.updateById(row);
        return toView(row);
    }

    private GrowthRewardGrantDO grant(Long tenantId, Long grantId) {
        GrowthRewardGrantDO row = grantMapper.selectById(requiredId(grantId, "grantId"));
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("reward grant does not belong to tenant");
        }
        return row;
    }

    private void reservePool(GrowthRewardPoolDO pool, BigDecimal costAmount, String actor) {
        pool.setReservedInventory(defaultLong(pool.getReservedInventory()) + 1L);
        pool.setReservedAmount(defaultAmount(pool.getReservedAmount()).add(costAmount));
        pool.setUpdatedBy(defaultString(actor, "system"));
        pool.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        poolMapper.updateById(pool);
    }

    private void updatePoolCounters(GrowthRewardGrantDO grant,
                                    String previousStatus,
                                    String nextStatus,
                                    String actor) {
        GrowthRewardPoolDO pool = poolMapper.selectById(grant.getPoolId());
        if (pool == null || !grant.getTenantId().equals(pool.getTenantId())) {
            return;
        }
        BigDecimal costAmount = defaultAmount(grant.getCostAmount());
        if ("RESERVED".equals(previousStatus) && "SUCCESS".equals(nextStatus)) {
            pool.setReservedInventory(decrement(defaultLong(pool.getReservedInventory())));
            pool.setGrantedInventory(defaultLong(pool.getGrantedInventory()) + 1L);
            pool.setReservedAmount(decrement(defaultAmount(pool.getReservedAmount()), costAmount));
            pool.setGrantedAmount(defaultAmount(pool.getGrantedAmount()).add(costAmount));
        } else if ("RESERVED".equals(previousStatus)
                && ("FAILED".equals(nextStatus) || "CANCELED".equals(nextStatus))) {
            pool.setReservedInventory(decrement(defaultLong(pool.getReservedInventory())));
            pool.setReservedAmount(decrement(defaultAmount(pool.getReservedAmount()), costAmount));
        } else if ("FAILED".equals(previousStatus) && "SUCCESS".equals(nextStatus)) {
            pool.setGrantedInventory(defaultLong(pool.getGrantedInventory()) + 1L);
            pool.setGrantedAmount(defaultAmount(pool.getGrantedAmount()).add(costAmount));
        } else {
            return;
        }
        pool.setUpdatedBy(defaultString(actor, "system"));
        pool.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        poolMapper.updateById(pool);
    }

    private void ensureInventoryAvailable(GrowthRewardPoolDO pool) {
        if (!"LIMITED".equals(pool.getInventoryMode())) {
            return;
        }
        long total = defaultLong(pool.getTotalInventory());
        long used = defaultLong(pool.getReservedInventory()) + defaultLong(pool.getGrantedInventory());
        if (total <= 0 || used >= total) {
            throw new IllegalArgumentException("reward pool inventory exhausted");
        }
    }

    private void validateActivity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(activityId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
    }

    private GrowthRewardPoolDO validatePool(Long tenantId, Long activityId, Long poolId) {
        GrowthRewardPoolDO row = poolMapper.selectById(requiredId(poolId, "poolId"));
        if (row == null || !tenantId.equals(row.getTenantId()) || !activityId.equals(row.getActivityId())) {
            throw new IllegalArgumentException("reward pool does not belong to activity");
        }
        return row;
    }

    private GrowthRewardGrantView toView(GrowthRewardGrantDO row) {
        return new GrowthRewardGrantView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getPoolId(),
                row.getParticipantId(),
                row.getReferralRelationId(),
                row.getTaskProgressId(),
                row.getGrantReason(),
                row.getStatus(),
                row.getIdempotencyKey(),
                fromJson(row.getProviderRequestJson()),
                fromJson(row.getProviderResponseJson()),
                defaultAmount(row.getCostAmount()),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("provider payload must be JSON serializable", e);
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

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeProviderStatus(String value) {
        String status = normalizeUpper(value, "FAILED");
        return switch (status) {
            case "RESERVED", "PENDING" -> "RESERVED";
            case "SUCCESS", "SUCCEEDED", "GRANTED" -> "SUCCESS";
            case "FAILED", "FAILURE" -> "FAILED";
            case "CANCELED", "CANCELLED" -> "CANCELED";
            case "REDEEMED" -> "REDEEMED";
            case "EXPIRED" -> "EXPIRED";
            default -> throw new IllegalArgumentException("unsupported provider reward grant status: " + status);
        };
    }

    private static BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private static Long decrement(Long value) {
        return Math.max(0L, defaultLong(value) - 1L);
    }

    private static BigDecimal decrement(BigDecimal value, BigDecimal amount) {
        BigDecimal next = defaultAmount(value).subtract(defaultAmount(amount));
        return next.signum() < 0 ? BigDecimal.ZERO : next;
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }
}
