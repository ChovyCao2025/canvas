package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthActivityEventDO;
import org.chovy.canvas.dal.mapper.GrowthActivityEventMapper;
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
public class GrowthActivityEventService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthActivityEventMapper eventMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public GrowthActivityEventService(GrowthActivityMapper activityMapper,
                                      GrowthActivityEventMapper eventMapper,
                                      ObjectMapper objectMapper) {
        this(activityMapper, eventMapper, objectMapper, Clock.systemDefaultZone());
    }

    GrowthActivityEventService(GrowthActivityMapper activityMapper,
                               GrowthActivityEventMapper eventMapper,
                               ObjectMapper objectMapper,
                               Clock clock) {
        this.activityMapper = activityMapper;
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthActivityEventView recordEvent(Long tenantId,
                                               Long activityId,
                                               GrowthActivityEventCommand command,
                                               String actor) {
        if (command == null) {
            throw new IllegalArgumentException("growth activity event command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);
        String eventKey = required(command.eventKey(), "eventKey");
        GrowthActivityEventDO existing = eventMapper.selectOne(new LambdaQueryWrapper<GrowthActivityEventDO>()
                .eq(GrowthActivityEventDO::getTenantId, scopedTenantId)
                .eq(GrowthActivityEventDO::getEventKey, eventKey)
                .last("LIMIT 1"));
        if (existing != null) {
            return toView(existing);
        }
        GrowthActivityEventDO row = new GrowthActivityEventDO();
        row.setTenantId(scopedTenantId);
        row.setActivityId(scopedActivityId);
        row.setParticipantId(command.participantId());
        row.setEventType(normalizeUpper(command.eventType(), "CUSTOM"));
        row.setEventKey(eventKey);
        row.setSourceType(normalizeUpper(command.sourceType(), "GROWTH_ACTIVITY"));
        row.setSourceId(command.sourceId());
        row.setPayloadJson(toJson(command.payload()));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setOccurredAt(now());
        eventMapper.insert(row);
        return toView(row);
    }

    public List<GrowthActivityEventView> listEvents(Long tenantId, Long activityId, String eventType, Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);
        String normalizedType = optionalUpper(eventType);
        return eventMapper.selectList(new LambdaQueryWrapper<GrowthActivityEventDO>()
                        .eq(GrowthActivityEventDO::getTenantId, scopedTenantId)
                        .eq(GrowthActivityEventDO::getActivityId, scopedActivityId)
                        .eq(normalizedType != null, GrowthActivityEventDO::getEventType, normalizedType)
                        .orderByDesc(GrowthActivityEventDO::getOccurredAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> scopedActivityId.equals(row.getActivityId()))
                .filter(row -> normalizedType == null || normalizedType.equals(row.getEventType()))
                .map(this::toView)
                .toList();
    }

    public GrowthActivityEventView logLifecycle(Long tenantId, Long activityId, String status, String actor) {
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                null,
                "ACTIVITY_LIFECYCLE",
                "activity:" + activityId + ":lifecycle:" + normalizeUpper(status, "UNKNOWN"),
                "GROWTH_ACTIVITY",
                activityId,
                Map.of("status", normalizeUpper(status, "UNKNOWN"))), actor);
    }

    public GrowthActivityEventView logParticipantEntry(Long tenantId,
                                                       Long activityId,
                                                       Long participantId,
                                                       String status,
                                                       Map<String, Object> payload) {
        String normalizedStatus = normalizeUpper(status, "JOINED");
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                participantId,
                "PARTICIPANT_ENTRY",
                "activity:" + activityId + ":participant:" + participantId + ":" + normalizedStatus,
                "PARTICIPANT",
                participantId,
                merge(payload, Map.of("status", normalizedStatus))), "system");
    }

    public GrowthActivityEventView logReferralQualification(Long tenantId,
                                                            Long activityId,
                                                            Long relationId,
                                                            Long participantId,
                                                            Map<String, Object> payload) {
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                participantId,
                "REFERRAL_QUALIFICATION",
                "referral:" + relationId + ":qualified",
                "REFERRAL_RELATION",
                relationId,
                payload), "system");
    }

    public GrowthActivityEventView logTaskProgress(Long tenantId,
                                                   Long activityId,
                                                   Long progressId,
                                                   Long participantId,
                                                   Map<String, Object> payload) {
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                participantId,
                "TASK_PROGRESS",
                "task-progress:" + progressId,
                "TASK_PROGRESS",
                progressId,
                payload), "system");
    }

    public GrowthActivityEventView logGrantTransition(Long tenantId,
                                                      Long activityId,
                                                      Long grantId,
                                                      Long participantId,
                                                      String status,
                                                      Map<String, Object> payload) {
        String normalizedStatus = normalizeUpper(status, "UNKNOWN");
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                participantId,
                "GRANT_TRANSITION",
                "grant:" + grantId + ":" + normalizedStatus,
                "REWARD_GRANT",
                grantId,
                merge(payload, Map.of("status", normalizedStatus))), "system");
    }

    public GrowthActivityEventView logConversionEvidence(Long tenantId,
                                                         Long activityId,
                                                         String conversionKey,
                                                         Long participantId,
                                                         Map<String, Object> payload) {
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                participantId,
                "CONVERSION_EVIDENCE",
                "conversion:" + required(conversionKey, "conversionKey"),
                "CONVERSION",
                null,
                payload), "system");
    }

    private void validateActivity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(activityId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
    }

    private GrowthActivityEventView toView(GrowthActivityEventDO row) {
        return new GrowthActivityEventView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getParticipantId(),
                row.getEventType(),
                row.getEventKey(),
                row.getSourceType(),
                row.getSourceId(),
                fromJson(row.getPayloadJson()),
                row.getCreatedBy(),
                row.getOccurredAt());
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("event payload must be JSON serializable", e);
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

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    private static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> additions) {
        if ((base == null || base.isEmpty()) && (additions == null || additions.isEmpty())) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        if (additions != null) {
            merged.putAll(additions);
        }
        return Map.copyOf(merged);
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

    private static String optionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }
}
