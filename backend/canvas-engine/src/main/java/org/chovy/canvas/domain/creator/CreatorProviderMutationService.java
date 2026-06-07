package org.chovy.canvas.domain.creator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CreatorCampaignDO;
import org.chovy.canvas.dal.dataobject.CreatorCollaborationDO;
import org.chovy.canvas.dal.dataobject.CreatorDeliverableDO;
import org.chovy.canvas.dal.dataobject.CreatorProfileDO;
import org.chovy.canvas.dal.dataobject.CreatorProviderMutationDO;
import org.chovy.canvas.dal.mapper.CreatorCampaignMapper;
import org.chovy.canvas.dal.mapper.CreatorCollaborationMapper;
import org.chovy.canvas.dal.mapper.CreatorDeliverableMapper;
import org.chovy.canvas.dal.mapper.CreatorProfileMapper;
import org.chovy.canvas.dal.mapper.CreatorProviderMutationMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class CreatorProviderMutationService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final Set<String> MUTATION_TYPES = Set.of(
            "PUBLISH_BRIEF",
            "INVITE_CREATOR",
            "GENERATE_AFFILIATE_LINK",
            "CREATE_DISCOUNT_CODE",
            "REQUEST_CONTENT_AUTHORIZATION",
            "SYNC_DELIVERABLE_STATUS");
    private static final Set<String> SECRET_KEYS = Set.of(
            "token",
            "access_token",
            "refresh_token",
            "client_secret",
            "api_key",
            "apikey",
            "password");

    private final CreatorCampaignMapper campaignMapper;
    private final CreatorCollaborationMapper collaborationMapper;
    private final CreatorDeliverableMapper deliverableMapper;
    private final CreatorProfileMapper profileMapper;
    private final CreatorProviderMutationMapper mutationMapper;
    private final ObjectMapper objectMapper;
    private final CreatorProviderWriteGateway gateway;
    private final Clock clock;

    @Autowired
    public CreatorProviderMutationService(CreatorCampaignMapper campaignMapper,
                                          CreatorCollaborationMapper collaborationMapper,
                                          CreatorDeliverableMapper deliverableMapper,
                                          CreatorProfileMapper profileMapper,
                                          CreatorProviderMutationMapper mutationMapper,
                                          ObjectMapper objectMapper,
                                          CreatorProviderWriteGateway gateway) {
        this(campaignMapper, collaborationMapper, deliverableMapper, profileMapper, mutationMapper, objectMapper,
                gateway, Clock.systemDefaultZone());
    }

    CreatorProviderMutationService(CreatorCampaignMapper campaignMapper,
                                   CreatorCollaborationMapper collaborationMapper,
                                   CreatorDeliverableMapper deliverableMapper,
                                   CreatorProfileMapper profileMapper,
                                   CreatorProviderMutationMapper mutationMapper,
                                   ObjectMapper objectMapper,
                                   CreatorProviderWriteGateway gateway,
                                   Clock clock) {
        this.campaignMapper = campaignMapper;
        this.collaborationMapper = collaborationMapper;
        this.deliverableMapper = deliverableMapper;
        this.profileMapper = profileMapper;
        this.mutationMapper = mutationMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.gateway = gateway == null ? CreatorProviderWriteGateway.unsupported() : gateway;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public CreatorProviderMutationView propose(Long tenantId,
                                               CreatorProviderMutationCommand command,
                                               String actor) {
        if (command == null) {
            throw new IllegalArgumentException("creator provider mutation command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        CreatorCampaignDO campaign = campaign(scopedTenantId, command.campaignId());
        CreatorCollaborationDO collaboration = collaboration(scopedTenantId, command.collaborationId());
        CreatorDeliverableDO deliverable = deliverable(scopedTenantId, command.deliverableId());
        validateRelationship(campaign, collaboration, deliverable);
        CreatorProfileDO creator = creator(scopedTenantId, collaboration == null ? null : collaboration.getCreatorId());
        String mutationType = normalizeMutationType(command.mutationType());
        String entityType = normalizeUpper(command.entityType(), "entityType");
        Map<String, Object> payload = payload(command.payload());
        validatePayload(mutationType, entityType, collaboration, deliverable, payload);
        String payloadJson = json(payload);
        String requestHash = sha256(payloadJson);
        String mutationKey = required(command.mutationKey(), "mutationKey");
        String idempotencyKey = defaultString(command.idempotencyKey(), mutationKey);
        SearchExistingMutation existing = existingMutation(scopedTenantId, mutationKey);
        if (existing.row() != null) {
            if (!Objects.equals(requestHash, existing.row().getRequestHash())) {
                throw new IllegalArgumentException("creator provider mutation request hash conflicts with existing mutation key");
            }
            return toView(existing.row());
        }
        LocalDateTime changedAt = now();
        CreatorProviderMutationDO row = new CreatorProviderMutationDO();
        row.setTenantId(scopedTenantId);
        row.setCampaignId(campaign.getId());
        row.setCollaborationId(collaboration == null ? null : collaboration.getId());
        row.setDeliverableId(deliverable == null ? null : deliverable.getId());
        row.setCreatorId(creator == null ? null : creator.getId());
        row.setProvider(provider(creator, payload));
        row.setMutationKey(mutationKey);
        row.setMutationType(mutationType);
        row.setEntityType(entityType);
        row.setExternalEntityId(trimToNull(command.externalEntityId()));
        row.setRequestHash(requestHash);
        row.setIdempotencyKey(idempotencyKey);
        row.setStatus("DRAFT");
        row.setApprovalStatus("PENDING");
        row.setDryRunRequired(Boolean.FALSE.equals(command.dryRunRequired()) ? 0 : 1);
        row.setPayloadJson(payloadJson);
        row.setValidationJson(json(Map.of("validatedAt", changedAt.toString(), "mutationType", mutationType)));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setCreatedAt(changedAt);
        row.setUpdatedAt(changedAt);
        mutationMapper.insert(row);
        return toView(row);
    }

    public CreatorProviderMutationView approve(Long tenantId,
                                               Long mutationId,
                                               CreatorProviderMutationApprovalCommand command,
                                               String actor) {
        CreatorProviderMutationDO row = mutation(normalizeTenant(tenantId), mutationId);
        String decision = normalizeUpper(command == null ? null : command.decision(), "decision");
        LocalDateTime changedAt = now();
        if ("APPROVED".equals(decision)) {
            row.setApprovalStatus("APPROVED");
            row.setStatus("READY");
        } else if ("REJECTED".equals(decision)) {
            row.setApprovalStatus("REJECTED");
            row.setStatus("CANCELLED");
        } else {
            throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
        }
        row.setApprovedBy(defaultString(actor, "system"));
        row.setApprovedAt(changedAt);
        row.setValidationJson(json(Map.of(
                "decision", decision,
                "reason", defaultString(command.reason(), ""),
                "decidedAt", changedAt.toString())));
        row.setUpdatedAt(changedAt);
        mutationMapper.updateById(row);
        return toView(row);
    }

    public CreatorProviderMutationView execute(Long tenantId,
                                               Long mutationId,
                                               CreatorProviderMutationExecuteCommand command,
                                               String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CreatorProviderMutationDO row = mutation(scopedTenantId, mutationId);
        if (!"APPROVED".equals(row.getApprovalStatus())) {
            throw new IllegalStateException("creator provider mutation must be approved before execution");
        }
        boolean dryRun = command == null || !Boolean.FALSE.equals(command.dryRun());
        if (!dryRun && Integer.valueOf(1).equals(row.getDryRunRequired())
                && !"DRY_RUN_OK".equals(row.getStatus())) {
            throw new IllegalStateException("dry run must pass before applying creator provider mutation");
        }
        CreatorProviderMutationRequest request = new CreatorProviderMutationRequest(
                scopedTenantId,
                row.getCampaignId(),
                row.getCollaborationId(),
                row.getDeliverableId(),
                row.getCreatorId(),
                row.getProvider(),
                row.getMutationType(),
                row.getEntityType(),
                row.getExternalEntityId(),
                row.getIdempotencyKey(),
                dryRun,
                command == null || !Boolean.FALSE.equals(command.partialFailure()),
                map(row.getPayloadJson()),
                command == null || command.metadata() == null ? Map.of() : command.metadata());
        CreatorProviderMutationResult result = gateway.execute(request);
        LocalDateTime changedAt = now();
        row.setProviderRequestJson(json(providerRequestEvidence(request)));
        row.setProviderResponseJson(json(ProviderWriteEvidenceSanitizer.sanitizeMap(result.response())));
        row.setErrorCode(result.errorCode());
        row.setErrorMessage(result.errorMessage());
        row.setExecutedBy(defaultString(actor, "system"));
        row.setExecutedAt(changedAt);
        row.setUpdatedAt(changedAt);
        if (result.success()) {
            row.setStatus(dryRun ? "DRY_RUN_OK" : "APPLIED");
        } else {
            row.setStatus(dryRun ? "DRY_RUN_FAILED" : "FAILED");
        }
        mutationMapper.updateById(row);
        return toView(row);
    }

    public List<CreatorProviderMutationView> list(Long tenantId, CreatorProviderMutationQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long campaignId = query == null ? null : query.campaignId();
        Long collaborationId = query == null ? null : query.collaborationId();
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        String approvalStatus = query == null ? null : normalizeOptionalUpper(query.approvalStatus());
        int limit = Math.min(Math.max(query == null || query.limit() == null ? 50 : query.limit(), 1), 100);
        return safeList(mutationMapper.selectList(new LambdaQueryWrapper<CreatorProviderMutationDO>()
                .eq(CreatorProviderMutationDO::getTenantId, scopedTenantId)
                .eq(campaignId != null, CreatorProviderMutationDO::getCampaignId, campaignId)
                .eq(collaborationId != null, CreatorProviderMutationDO::getCollaborationId, collaborationId)
                .eq(status != null, CreatorProviderMutationDO::getStatus, status)
                .eq(approvalStatus != null, CreatorProviderMutationDO::getApprovalStatus, approvalStatus)
                .orderByDesc(CreatorProviderMutationDO::getUpdatedAt)
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> campaignId == null || Objects.equals(campaignId, row.getCampaignId()))
                .filter(row -> collaborationId == null || Objects.equals(collaborationId, row.getCollaborationId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .filter(row -> approvalStatus == null || approvalStatus.equals(row.getApprovalStatus()))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    private CreatorCampaignDO campaign(Long tenantId, Long campaignId) {
        CreatorCampaignDO campaign = campaignMapper.selectById(requiredId(campaignId, "campaignId"));
        validateTenant(tenantId, campaign == null ? null : campaign.getTenantId(), "campaign");
        return campaign;
    }

    private CreatorCollaborationDO collaboration(Long tenantId, Long collaborationId) {
        if (collaborationId == null) {
            return null;
        }
        CreatorCollaborationDO collaboration = collaborationMapper.selectById(requiredId(collaborationId,
                "collaborationId"));
        validateTenant(tenantId, collaboration == null ? null : collaboration.getTenantId(), "collaboration");
        return collaboration;
    }

    private CreatorDeliverableDO deliverable(Long tenantId, Long deliverableId) {
        if (deliverableId == null) {
            return null;
        }
        CreatorDeliverableDO deliverable = deliverableMapper.selectById(requiredId(deliverableId, "deliverableId"));
        validateTenant(tenantId, deliverable == null ? null : deliverable.getTenantId(), "deliverable");
        return deliverable;
    }

    private CreatorProfileDO creator(Long tenantId, Long creatorId) {
        if (creatorId == null) {
            return null;
        }
        CreatorProfileDO creator = profileMapper.selectById(requiredId(creatorId, "creatorId"));
        validateTenant(tenantId, creator == null ? null : creator.getTenantId(), "creator");
        return creator;
    }

    private CreatorProviderMutationDO mutation(Long tenantId, Long mutationId) {
        CreatorProviderMutationDO row = mutationMapper.selectById(requiredId(mutationId, "mutationId"));
        validateTenant(tenantId, row == null ? null : row.getTenantId(), "mutation");
        return row;
    }

    private void validateRelationship(CreatorCampaignDO campaign,
                                      CreatorCollaborationDO collaboration,
                                      CreatorDeliverableDO deliverable) {
        if (collaboration != null && !Objects.equals(campaign.getId(), collaboration.getCampaignId())) {
            throw new IllegalArgumentException("collaboration does not belong to campaign");
        }
        if (deliverable != null) {
            if (!Objects.equals(campaign.getId(), deliverable.getCampaignId())) {
                throw new IllegalArgumentException("deliverable does not belong to campaign");
            }
            if (collaboration != null && !Objects.equals(collaboration.getId(), deliverable.getCollaborationId())) {
                throw new IllegalArgumentException("deliverable does not belong to collaboration");
            }
            if (collaboration != null && !Objects.equals(collaboration.getCreatorId(), deliverable.getCreatorId())) {
                throw new IllegalArgumentException("deliverable creator does not match collaboration");
            }
        }
    }

    private void validatePayload(String mutationType,
                                 String entityType,
                                 CreatorCollaborationDO collaboration,
                                 CreatorDeliverableDO deliverable,
                                 Map<String, Object> payload) {
        rejectProviderSecrets(payload);
        switch (mutationType) {
            case "PUBLISH_BRIEF" -> requireAnyPayloadText(payload, "briefUrl", "briefText");
            case "INVITE_CREATOR", "GENERATE_AFFILIATE_LINK" -> requireCollaboration(collaboration, mutationType);
            case "CREATE_DISCOUNT_CODE" -> {
                requireCollaboration(collaboration, mutationType);
                if (trimToNull(string(payload.get("discountCode"))) == null
                        && trimToNull(collaboration.getDiscountCode()) == null) {
                    throw new IllegalArgumentException("discountCode is required");
                }
            }
            case "REQUEST_CONTENT_AUTHORIZATION" -> {
                requireDeliverable(deliverable, mutationType);
                if (!"DELIVERABLE".equals(entityType)) {
                    throw new IllegalArgumentException("content authorization entity must be DELIVERABLE");
                }
                requireAnyPayloadText(payload, "sparkAuthorizationCode", "authorizationAccountId", "permissionToken");
            }
            case "SYNC_DELIVERABLE_STATUS" -> requireDeliverable(deliverable, mutationType);
            default -> throw new IllegalArgumentException("unsupported creator provider mutation type");
        }
    }

    private void requireCollaboration(CreatorCollaborationDO collaboration, String mutationType) {
        if (collaboration == null) {
            throw new IllegalArgumentException(mutationType + " requires collaboration");
        }
    }

    private void requireDeliverable(CreatorDeliverableDO deliverable, String mutationType) {
        if (deliverable == null) {
            throw new IllegalArgumentException(mutationType + " requires deliverable");
        }
    }

    private void requireAnyPayloadText(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            if (trimToNull(string(payload.get(key))) != null) {
                return;
            }
        }
        throw new IllegalArgumentException(String.join(" or ", keys) + " is required");
    }

    private CreatorProviderMutationView toView(CreatorProviderMutationDO row) {
        return new CreatorProviderMutationView(
                row.getId(),
                row.getTenantId(),
                row.getCampaignId(),
                row.getCollaborationId(),
                row.getDeliverableId(),
                row.getCreatorId(),
                row.getProvider(),
                row.getMutationKey(),
                row.getMutationType(),
                row.getEntityType(),
                row.getExternalEntityId(),
                row.getRequestHash(),
                row.getIdempotencyKey(),
                row.getStatus(),
                row.getApprovalStatus(),
                Integer.valueOf(1).equals(row.getDryRunRequired()),
                map(row.getPayloadJson()),
                map(row.getValidationJson()),
                map(row.getProviderRequestJson()),
                map(row.getProviderResponseJson()),
                row.getErrorCode(),
                row.getErrorMessage(),
                row.getCreatedBy(),
                row.getApprovedBy(),
                row.getApprovedAt(),
                row.getExecutedBy(),
                row.getExecutedAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private Map<String, Object> providerRequestEvidence(CreatorProviderMutationRequest request) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("provider", request.provider());
        evidence.put("mutationType", request.mutationType());
        evidence.put("entityType", request.entityType());
        evidence.put("externalEntityId", defaultString(request.externalEntityId(), ""));
        evidence.put("idempotencyKey", request.idempotencyKey());
        evidence.put("dryRun", request.dryRun());
        evidence.put("partialFailure", request.partialFailure());
        evidence.put("campaignId", request.campaignId());
        evidence.put("collaborationId", request.collaborationId());
        evidence.put("deliverableId", request.deliverableId());
        evidence.put("creatorId", request.creatorId());
        evidence.put("payload", request.payload());
        evidence.put("metadata", request.metadata());
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    private SearchExistingMutation existingMutation(Long tenantId, String mutationKey) {
        CreatorProviderMutationDO row = mutationMapper.selectOne(new LambdaQueryWrapper<CreatorProviderMutationDO>()
                .eq(CreatorProviderMutationDO::getTenantId, tenantId)
                .eq(CreatorProviderMutationDO::getMutationKey, mutationKey)
                .last("LIMIT 1"));
        return new SearchExistingMutation(row);
    }

    private String provider(CreatorProfileDO creator, Map<String, Object> payload) {
        if (creator != null) {
            return normalizeUpper(creator.getProvider(), "provider");
        }
        return normalizeUpper(string(payload.get("provider")), "provider");
    }

    private Map<String, Object> payload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("creator provider mutation payload is required");
        }
        return Map.copyOf(payload);
    }

    private void rejectProviderSecrets(Object value) {
        if (value instanceof Map<?, ?> values) {
            values.forEach((key, nestedValue) -> {
                if (key != null && SECRET_KEYS.contains(key.toString().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("creator provider mutation payload must not contain provider secrets");
                }
                rejectProviderSecrets(nestedValue);
            });
        } else if (value instanceof Iterable<?> values) {
            values.forEach(this::rejectProviderSecrets);
        }
    }

    private void validateTenant(Long expectedTenantId, Long actualTenantId, String resource) {
        if (!expectedTenantId.equals(actualTenantId)) {
            throw new IllegalArgumentException(resource + " does not belong to current tenant");
        }
    }

    private Long normalizeTenant(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            return 0L;
        }
        return tenantId;
    }

    private Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String required(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed;
    }

    private String normalizeMutationType(String value) {
        String type = normalizeUpper(value, "mutationType");
        if (!MUTATION_TYPES.contains(type)) {
            throw new IllegalArgumentException("unsupported creator provider mutation type");
        }
        return type;
    }

    private String normalizeUpper(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("creator provider mutation metadata is not JSON serializable", ex);
        }
    }

    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(json, OBJECT_MAP);
            return values == null ? Map.of() : values;
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash creator provider mutation request", ex);
        }
    }

    private record SearchExistingMutation(CreatorProviderMutationDO row) {
    }
}
