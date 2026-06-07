package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthTaskDefinitionDO;
import org.chovy.canvas.dal.dataobject.GrowthTaskProgressDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthTaskDefinitionMapper;
import org.chovy.canvas.dal.mapper.GrowthTaskProgressMapper;
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
public class GrowthTaskService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthTaskDefinitionMapper definitionMapper;
    private final GrowthTaskProgressMapper progressMapper;
    private final GrowthRewardGrantService rewardGrantService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public GrowthTaskService(GrowthActivityMapper activityMapper,
                             GrowthTaskDefinitionMapper definitionMapper,
                             GrowthTaskProgressMapper progressMapper,
                             GrowthRewardGrantService rewardGrantService,
                             ObjectMapper objectMapper) {
        this(activityMapper, definitionMapper, progressMapper, rewardGrantService, objectMapper, Clock.systemDefaultZone());
    }

    GrowthTaskService(GrowthActivityMapper activityMapper,
                      GrowthTaskDefinitionMapper definitionMapper,
                      GrowthTaskProgressMapper progressMapper,
                      GrowthRewardGrantService rewardGrantService,
                      ObjectMapper objectMapper,
                      Clock clock) {
        this.activityMapper = activityMapper;
        this.definitionMapper = definitionMapper;
        this.progressMapper = progressMapper;
        this.rewardGrantService = rewardGrantService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public List<GrowthTaskDefinitionView> listTaskDefinitions(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        return definitionMapper.selectList(new LambdaQueryWrapper<GrowthTaskDefinitionDO>()
                        .eq(GrowthTaskDefinitionDO::getTenantId, scopedTenantId)
                        .eq(GrowthTaskDefinitionDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthTaskDefinitionDO::getUpdatedAt))
                .stream()
                .map(this::toDefinitionView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthTaskDefinitionView upsertTaskDefinition(Long tenantId,
                                                         Long activityId,
                                                         GrowthTaskDefinitionCommand command,
                                                         String actor) {
        if (command == null) {
            throw new IllegalArgumentException("growth task definition command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateTaskActivity(scopedTenantId, scopedActivityId);
        String taskKey = normalizeKey(command.taskKey(), "taskKey");
        GrowthTaskDefinitionDO row = definitionMapper.selectOne(new LambdaQueryWrapper<GrowthTaskDefinitionDO>()
                .eq(GrowthTaskDefinitionDO::getTenantId, scopedTenantId)
                .eq(GrowthTaskDefinitionDO::getActivityId, scopedActivityId)
                .eq(GrowthTaskDefinitionDO::getTaskKey, taskKey)
                .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new GrowthTaskDefinitionDO();
            row.setTenantId(scopedTenantId);
            row.setActivityId(scopedActivityId);
            row.setTaskKey(taskKey);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(now());
        }
        row.setTaskType(normalizeUpper(command.taskType(), "EVENT_COUNT"));
        row.setCompletionPolicy(normalizeCompletionPolicy(command.completionPolicy()));
        row.setResetPolicy(normalizeResetPolicy(command.resetPolicy()));
        row.setRewardPoolId(command.rewardPoolId());
        row.setTargetValue(positive(command.targetValue(), BigDecimal.ONE));
        row.setStatus(normalizeStatus(command.status()));
        row.setRuleJson(toJson(command.rule()));
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(now());
        if (insert) {
            definitionMapper.insert(row);
        } else {
            definitionMapper.updateById(row);
        }
        return toDefinitionView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthTaskProgressView recordProgress(Long tenantId,
                                                 Long activityId,
                                                 GrowthTaskProgressCommand command,
                                                 String actor) {
        if (command == null) {
            throw new IllegalArgumentException("growth task progress command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        GrowthTaskDefinitionDO task = task(scopedTenantId, scopedActivityId, command.taskId());
        if (!"ACTIVE".equals(task.getStatus())) {
            throw new IllegalArgumentException("growth task definition is not active");
        }
        Long participantId = requiredId(command.participantId(), "participantId");
        GrowthTaskProgressDO row = progressMapper.selectOne(new LambdaQueryWrapper<GrowthTaskProgressDO>()
                .eq(GrowthTaskProgressDO::getTenantId, scopedTenantId)
                .eq(GrowthTaskProgressDO::getActivityId, scopedActivityId)
                .eq(GrowthTaskProgressDO::getParticipantId, participantId)
                .eq(GrowthTaskProgressDO::getTaskId, task.getId())
                .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new GrowthTaskProgressDO();
            row.setTenantId(scopedTenantId);
            row.setActivityId(scopedActivityId);
            row.setParticipantId(participantId);
            row.setTaskId(task.getId());
            row.setProgressValue(BigDecimal.ZERO);
            row.setTargetValue(task.getTargetValue());
            row.setStatus("IN_PROGRESS");
        } else if ("COMPLETED".equals(row.getStatus()) && "ONCE".equals(task.getResetPolicy())) {
            return toProgressView(row);
        }
        row.setProgressValue(defaultAmount(row.getProgressValue()).add(positive(command.deltaValue(), BigDecimal.ONE)));
        row.setTargetValue(task.getTargetValue());
        row.setLastEventKey(trimToLimit(command.eventKey(), 191));
        row.setEvidenceJson(toJson(command.evidence()));
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(now());
        if (row.getProgressValue().compareTo(task.getTargetValue()) >= 0 && !"COMPLETED".equals(row.getStatus())) {
            row.setStatus("COMPLETED");
            row.setCompletedAt(now());
            if (task.getRewardPoolId() != null && row.getRewardGrantId() == null) {
                if (insert) {
                    progressMapper.insert(row);
                    insert = false;
                }
                GrowthRewardGrantView grant = rewardGrantService.createGrant(scopedTenantId, scopedActivityId,
                        new GrowthRewardGrantCommand(
                                task.getRewardPoolId(),
                                participantId,
                                null,
                                row.getId(),
                                "TASK_COMPLETION",
                                "task:" + row.getId() + ":completion",
                                Map.of("taskKey", task.getTaskKey(), "progressValue", row.getProgressValue()),
                                BigDecimal.ZERO),
                        actor);
                row.setRewardGrantId(grant.id());
            }
        }
        if (insert) {
            progressMapper.insert(row);
        } else {
            progressMapper.updateById(row);
        }
        return toProgressView(row);
    }

    public List<GrowthTaskProgressView> listTaskProgress(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        return progressMapper.selectList(new LambdaQueryWrapper<GrowthTaskProgressDO>()
                        .eq(GrowthTaskProgressDO::getTenantId, scopedTenantId)
                        .eq(GrowthTaskProgressDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthTaskProgressDO::getUpdatedAt))
                .stream()
                .map(this::toProgressView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthTaskProgressView resetProgress(Long tenantId, Long progressId, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        GrowthTaskProgressDO row = progressMapper.selectById(requiredId(progressId, "progressId"));
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth task progress does not belong to tenant");
        }
        GrowthTaskDefinitionDO task = task(scopedTenantId, row.getActivityId(), row.getTaskId());
        if ("ONCE".equals(task.getResetPolicy())) {
            throw new IllegalArgumentException("growth task progress cannot reset once-only task");
        }
        row.setProgressValue(BigDecimal.ZERO);
        row.setStatus("IN_PROGRESS");
        row.setRewardGrantId(null);
        row.setCompletedAt(null);
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(now());
        progressMapper.updateById(row);
        return toProgressView(row);
    }

    private GrowthTaskDefinitionDO task(Long tenantId, Long activityId, Long taskId) {
        GrowthTaskDefinitionDO row = definitionMapper.selectById(requiredId(taskId, "taskId"));
        if (row == null || !tenantId.equals(row.getTenantId()) || !activityId.equals(row.getActivityId())) {
            throw new IllegalArgumentException("growth task definition does not belong to activity");
        }
        return row;
    }

    private void validateTaskActivity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(activityId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
        if (!"TASK_INCENTIVE".equals(row.getActivityType())) {
            throw new IllegalArgumentException("growth activity is not a task incentive activity");
        }
    }

    private GrowthTaskDefinitionView toDefinitionView(GrowthTaskDefinitionDO row) {
        return new GrowthTaskDefinitionView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getTaskKey(),
                row.getTaskType(),
                row.getCompletionPolicy(),
                row.getResetPolicy(),
                row.getRewardPoolId(),
                row.getTargetValue(),
                row.getStatus(),
                fromJson(row.getRuleJson()),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private GrowthTaskProgressView toProgressView(GrowthTaskProgressDO row) {
        return new GrowthTaskProgressView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getParticipantId(),
                row.getTaskId(),
                row.getProgressValue(),
                row.getTargetValue(),
                row.getStatus(),
                row.getLastEventKey(),
                fromJson(row.getEvidenceJson()),
                row.getRewardGrantId(),
                row.getUpdatedBy(),
                row.getCompletedAt(),
                row.getUpdatedAt());
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("task payload must be JSON serializable", e);
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

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeCompletionPolicy(String value) {
        String policy = normalizeUpper(value, "EVENT");
        return switch (policy) {
            case "EVENT", "MANUAL" -> policy;
            default -> throw new IllegalArgumentException("unsupported task completion policy: " + policy);
        };
    }

    private static String normalizeResetPolicy(String value) {
        String policy = normalizeUpper(value, "ONCE");
        return switch (policy) {
            case "ONCE", "DAILY", "WEEKLY", "MANUAL_RESET" -> policy;
            default -> throw new IllegalArgumentException("unsupported task reset policy: " + policy);
        };
    }

    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "ACTIVE");
        return switch (status) {
            case "ACTIVE", "PAUSED", "CLOSED" -> status;
            default -> throw new IllegalArgumentException("unsupported task status: " + status);
        };
    }

    private static BigDecimal positive(BigDecimal value, BigDecimal fallback) {
        BigDecimal actual = value == null ? fallback : value;
        return actual.signum() <= 0 ? fallback : actual;
    }

    private static BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
