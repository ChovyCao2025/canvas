package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GrowthActivityService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper mapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public GrowthActivityService(GrowthActivityMapper mapper, ObjectMapper objectMapper) {
        this(mapper, objectMapper, Clock.systemDefaultZone());
    }

    GrowthActivityService(GrowthActivityMapper mapper, ObjectMapper objectMapper, Clock clock) {
        this.mapper = mapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthActivityView upsertActivity(Long tenantId, GrowthActivityCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("growth activity command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        String activityKey = normalizeKey(command.activityKey(), "activityKey");
        GrowthActivityDO row = mapper.selectOne(new LambdaQueryWrapper<GrowthActivityDO>()
                .eq(GrowthActivityDO::getTenantId, scopedTenantId)
                .eq(GrowthActivityDO::getActivityKey, activityKey)
                .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new GrowthActivityDO();
            row.setTenantId(scopedTenantId);
            row.setActivityKey(activityKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setActivityName(defaultString(command.activityName(), activityKey));
        row.setActivityType(normalizeActivityType(command.activityType()));
        row.setStatus(normalizeStatus(command.status()));
        row.setCampaignId(command.campaignId());
        row.setObjective(normalizeUpper(command.objective(), "UNSPECIFIED"));
        row.setOwnerTeam(trimToLimit(command.ownerTeam(), 128));
        row.setStartAt(command.startAt());
        row.setEndAt(command.endAt());
        if (row.getStartAt() != null && row.getEndAt() != null && row.getEndAt().isBefore(row.getStartAt())) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        row.setChannelScope(normalizeOptionalUpper(command.channelScope()));
        row.setAudienceRefsJson(toJson(command.audienceRefs()));
        row.setRiskPolicyRef(trimToLimit(command.riskPolicyRef(), 128));
        row.setExperimentRef(trimToLimit(command.experimentRef(), 128));
        row.setDashboardRef(trimToLimit(command.dashboardRef(), 128));
        row.setMetadataJson(toJson(command.metadata()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return toView(row);
    }

    public List<GrowthActivityView> listActivities(Long tenantId, String activityType, String status, Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedType = normalizeOptionalActivityType(activityType);
        String normalizedStatus = normalizeOptionalStatus(status);
        return mapper.selectList(new LambdaQueryWrapper<GrowthActivityDO>()
                        .eq(GrowthActivityDO::getTenantId, scopedTenantId)
                        .eq(normalizedType != null, GrowthActivityDO::getActivityType, normalizedType)
                        .eq(normalizedStatus != null, GrowthActivityDO::getStatus, normalizedStatus)
                        .orderByDesc(GrowthActivityDO::getUpdatedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .filter(row -> normalizedType == null || normalizedType.equals(row.getActivityType()))
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .map(this::toView)
                .toList();
    }

    public GrowthActivityView getActivity(Long tenantId, Long activityId) {
        return toView(activity(safeTenantId(tenantId), activityId));
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthActivityView publishActivity(Long tenantId, Long activityId, String actor) {
        GrowthActivityDO row = activity(safeTenantId(tenantId), activityId);
        if (!"DRAFT".equals(row.getStatus()) && !"PAUSED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot publish activity from status " + row.getStatus());
        }
        return transition(row, "ACTIVE", actor);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthActivityView pauseActivity(Long tenantId, Long activityId, String actor) {
        GrowthActivityDO row = activity(safeTenantId(tenantId), activityId);
        if (!"ACTIVE".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot pause activity from status " + row.getStatus());
        }
        return transition(row, "PAUSED", actor);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthActivityView closeActivity(Long tenantId, Long activityId, String actor) {
        GrowthActivityDO row = activity(safeTenantId(tenantId), activityId);
        if ("CLOSED".equals(row.getStatus())) {
            return toView(row);
        }
        if ("ARCHIVED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot close activity from status " + row.getStatus());
        }
        return transition(row, "CLOSED", actor);
    }

    private GrowthActivityView transition(GrowthActivityDO row, String status, String actor) {
        row.setStatus(status);
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        mapper.updateById(row);
        return toView(row);
    }

    private GrowthActivityDO activity(Long tenantId, Long activityId) {
        GrowthActivityDO row = mapper.selectById(requiredId(activityId, "activityId"));
        validateTenant(tenantId, row == null ? null : row.getTenantId(), "growth activity");
        return row;
    }

    private GrowthActivityView toView(GrowthActivityDO row) {
        return new GrowthActivityView(
                row.getId(),
                row.getTenantId(),
                row.getActivityKey(),
                row.getActivityName(),
                row.getActivityType(),
                row.getStatus(),
                row.getCampaignId(),
                row.getObjective(),
                row.getOwnerTeam(),
                row.getStartAt(),
                row.getEndAt(),
                row.getChannelScope(),
                fromJson(row.getAudienceRefsJson()),
                row.getRiskPolicyRef(),
                row.getExperimentRef(),
                row.getDashboardRef(),
                fromJson(row.getMetadataJson()),
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

    private static String normalizeActivityType(String value) {
        String type = normalizeUpper(value, "BENEFIT_PROMOTION");
        return switch (type) {
            case "BENEFIT_PROMOTION", "REFERRAL_INVITE", "TASK_INCENTIVE",
                    "LOYALTY_MEMBER_ACTIVITY", "RETENTION_WINBACK",
                    "CONTENT_PRIVATE_DOMAIN_ACTIVITY" -> type;
            default -> throw new IllegalArgumentException("unsupported activity type: " + type);
        };
    }

    private static String normalizeOptionalActivityType(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeActivityType(trimmed);
    }

    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "DRAFT");
        return switch (status) {
            case "DRAFT", "ACTIVE", "PAUSED", "CLOSED", "ARCHIVED" -> status;
            default -> throw new IllegalArgumentException("unsupported activity status: " + status);
        };
    }

    private static String normalizeOptionalStatus(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeStatus(trimmed);
    }

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
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

    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    private static void validateTenant(Long expected, Long actual, String entity) {
        if (actual == null || !actual.equals(expected)) {
            throw new IllegalArgumentException(entity + " does not belong to tenant");
        }
    }
}
