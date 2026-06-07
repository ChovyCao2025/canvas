package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthReferralCodeDO;
import org.chovy.canvas.dal.dataobject.GrowthReferralRelationDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthReferralCodeMapper;
import org.chovy.canvas.dal.mapper.GrowthReferralRelationMapper;
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
public class GrowthReferralService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthReferralCodeMapper codeMapper;
    private final GrowthReferralRelationMapper relationMapper;
    private final GrowthRewardGrantService rewardGrantService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public GrowthReferralService(GrowthActivityMapper activityMapper,
                                 GrowthReferralCodeMapper codeMapper,
                                 GrowthReferralRelationMapper relationMapper,
                                 GrowthRewardGrantService rewardGrantService,
                                 ObjectMapper objectMapper) {
        this(activityMapper, codeMapper, relationMapper, rewardGrantService, objectMapper, Clock.systemDefaultZone());
    }

    GrowthReferralService(GrowthActivityMapper activityMapper,
                          GrowthReferralCodeMapper codeMapper,
                          GrowthReferralRelationMapper relationMapper,
                          GrowthRewardGrantService rewardGrantService,
                          ObjectMapper objectMapper,
                          Clock clock) {
        this.activityMapper = activityMapper;
        this.codeMapper = codeMapper;
        this.relationMapper = relationMapper;
        this.rewardGrantService = rewardGrantService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthReferralCodeView generateCode(Long tenantId, Long activityId, Long participantId, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        Long scopedParticipantId = requiredId(participantId, "participantId");
        validateReferralActivity(scopedTenantId, scopedActivityId);
        GrowthReferralCodeDO existing = codeMapper.selectOne(new LambdaQueryWrapper<GrowthReferralCodeDO>()
                .eq(GrowthReferralCodeDO::getTenantId, scopedTenantId)
                .eq(GrowthReferralCodeDO::getActivityId, scopedActivityId)
                .eq(GrowthReferralCodeDO::getParticipantId, scopedParticipantId)
                .last("LIMIT 1"));
        if (existing != null) {
            return toCodeView(existing);
        }
        GrowthReferralCodeDO row = new GrowthReferralCodeDO();
        row.setTenantId(scopedTenantId);
        row.setActivityId(scopedActivityId);
        row.setParticipantId(scopedParticipantId);
        row.setCode("G" + scopedActivityId + "P" + scopedParticipantId);
        row.setStatus("ACTIVE");
        row.setCreatedBy(defaultString(actor, "system"));
        row.setCreatedAt(LocalDateTime.now(clock).withNano(0));
        codeMapper.insert(row);
        return toCodeView(row);
    }

    public List<GrowthReferralCodeView> listCodes(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateReferralActivity(scopedTenantId, scopedActivityId);
        return codeMapper.selectList(new LambdaQueryWrapper<GrowthReferralCodeDO>()
                        .eq(GrowthReferralCodeDO::getTenantId, scopedTenantId)
                        .eq(GrowthReferralCodeDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthReferralCodeDO::getCreatedAt))
                .stream()
                .map(this::toCodeView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthReferralRelationView upsertRelation(Long tenantId,
                                                     Long activityId,
                                                     GrowthReferralRelationCommand command,
                                                     String actor) {
        if (command == null) {
            throw new IllegalArgumentException("growth referral relation command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateReferralActivity(scopedTenantId, scopedActivityId);
        String referralCode = normalizeCode(command.referralCode());
        String inviteeUserId = required(command.inviteeUserId(), "inviteeUserId");
        GrowthReferralCodeDO code = codeMapper.selectOne(new LambdaQueryWrapper<GrowthReferralCodeDO>()
                .eq(GrowthReferralCodeDO::getTenantId, scopedTenantId)
                .eq(GrowthReferralCodeDO::getCode, referralCode)
                .last("LIMIT 1"));
        if (code == null || !scopedActivityId.equals(code.getActivityId())) {
            throw new IllegalArgumentException("referral code does not belong to activity");
        }
        if (!"ACTIVE".equals(code.getStatus())) {
            throw new IllegalArgumentException("referral code is not active");
        }
        GrowthReferralRelationDO row = relationMapper.selectOne(new LambdaQueryWrapper<GrowthReferralRelationDO>()
                .eq(GrowthReferralRelationDO::getTenantId, scopedTenantId)
                .eq(GrowthReferralRelationDO::getActivityId, scopedActivityId)
                .eq(GrowthReferralRelationDO::getInviteeUserId, inviteeUserId)
                .last("LIMIT 1"));
        if (row != null) {
            return toRelationView(row);
        }
        row = new GrowthReferralRelationDO();
        row.setTenantId(scopedTenantId);
        row.setActivityId(scopedActivityId);
        row.setReferralCodeId(code.getId());
        row.setReferrerParticipantId(code.getParticipantId());
        row.setInviteeUserId(inviteeUserId);
        row.setStatus("PENDING");
        row.setRiskEvidenceJson(toJson(command.riskEvidence()));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setCreatedAt(LocalDateTime.now(clock).withNano(0));
        row.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        relationMapper.insert(row);
        return toRelationView(row);
    }

    public List<GrowthReferralRelationView> listRelations(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateReferralActivity(scopedTenantId, scopedActivityId);
        return relationMapper.selectList(new LambdaQueryWrapper<GrowthReferralRelationDO>()
                        .eq(GrowthReferralRelationDO::getTenantId, scopedTenantId)
                        .eq(GrowthReferralRelationDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthReferralRelationDO::getUpdatedAt))
                .stream()
                .map(this::toRelationView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthReferralRelationView qualifyRelation(Long tenantId,
                                                      Long relationId,
                                                      GrowthReferralQualificationCommand command,
                                                      String actor) {
        if (command == null) {
            throw new IllegalArgumentException("growth referral qualification command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        GrowthReferralRelationDO row = relationMapper.selectById(requiredId(relationId, "relationId"));
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("referral relation does not belong to tenant");
        }
        if ("QUALIFIED".equals(row.getStatus())) {
            return toRelationView(row);
        }
        if (!"PENDING".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot qualify referral relation from status " + row.getStatus());
        }
        GrowthRewardGrantView inviterGrant = rewardGrantService.createGrant(scopedTenantId, row.getActivityId(),
                new GrowthRewardGrantCommand(
                        requiredId(command.inviterRewardPoolId(), "inviterRewardPoolId"),
                        row.getReferrerParticipantId(),
                        row.getId(),
                        null,
                        "REFERRAL_INVITER",
                        "referral:" + row.getId() + ":inviter",
                        Map.of("role", "INVITER", "inviteeUserId", row.getInviteeUserId()),
                        BigDecimal.ZERO),
                actor);
        GrowthRewardGrantView inviteeGrant = rewardGrantService.createGrant(scopedTenantId, row.getActivityId(),
                new GrowthRewardGrantCommand(
                        requiredId(command.inviteeRewardPoolId(), "inviteeRewardPoolId"),
                        null,
                        row.getId(),
                        null,
                        "REFERRAL_INVITEE",
                        "referral:" + row.getId() + ":invitee",
                        Map.of("role", "INVITEE", "inviteeUserId", row.getInviteeUserId()),
                        BigDecimal.ZERO),
                actor);
        row.setStatus("QUALIFIED");
        row.setRiskEvidenceJson(toJson(command.riskEvidence()));
        row.setInviterRewardGrantId(inviterGrant.id());
        row.setInviteeRewardGrantId(inviteeGrant.id());
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        relationMapper.updateById(row);
        return toRelationView(row);
    }

    private void validateReferralActivity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(activityId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
        if (!"REFERRAL_INVITE".equals(row.getActivityType())) {
            throw new IllegalArgumentException("growth activity is not a referral invite activity");
        }
    }

    private GrowthReferralCodeView toCodeView(GrowthReferralCodeDO row) {
        return new GrowthReferralCodeView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getParticipantId(),
                row.getCode(),
                row.getStatus(),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    private GrowthReferralRelationView toRelationView(GrowthReferralRelationDO row) {
        return new GrowthReferralRelationView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getReferralCodeId(),
                row.getReferrerParticipantId(),
                row.getInviteeUserId(),
                row.getStatus(),
                fromJson(row.getRiskEvidenceJson()),
                row.getInviterRewardGrantId(),
                row.getInviteeRewardGrantId(),
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
            throw new IllegalArgumentException("risk evidence must be JSON serializable", e);
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

    private static String normalizeCode(String value) {
        return required(value, "referralCode").toUpperCase(Locale.ROOT);
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }
}
