package org.chovy.canvas.domain.programmatic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspCampaignDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspLineItemDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspMutationDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSeatDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSupplyPathDO;
import org.chovy.canvas.dal.mapper.ProgrammaticDspCampaignMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspLineItemMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspMutationMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSeatMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSupplyPathMapper;
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
public class ProgrammaticDspMutationService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final Set<String> MUTATION_TYPES = Set.of(
            "CREATE_INSERTION_ORDER",
            "UPDATE_CAMPAIGN_BUDGET",
            "CREATE_LINE_ITEM",
            "UPDATE_LINE_ITEM_BID",
            "UPDATE_LINE_ITEM_BUDGET",
            "UPDATE_LINE_ITEM_STATUS",
            "ASSIGN_TARGETING",
            "ATTACH_DEAL",
            "SYNC_PROVIDER_STATUS");
    private static final Set<String> SECRET_KEYS = Set.of(
            "token",
            "access_token",
            "refresh_token",
            "client_secret",
            "api_key",
            "apikey",
            "password",
            "secret");

    private final ProgrammaticDspSeatMapper seatMapper;
    private final ProgrammaticDspCampaignMapper campaignMapper;
    private final ProgrammaticDspLineItemMapper lineItemMapper;
    private final ProgrammaticDspSupplyPathMapper supplyPathMapper;
    private final ProgrammaticDspMutationMapper mutationMapper;
    private final ObjectMapper objectMapper;
    private final ProgrammaticDspProviderWriteGateway gateway;
    private final Clock clock;

    @Autowired
    public ProgrammaticDspMutationService(ProgrammaticDspSeatMapper seatMapper,
                                          ProgrammaticDspCampaignMapper campaignMapper,
                                          ProgrammaticDspLineItemMapper lineItemMapper,
                                          ProgrammaticDspSupplyPathMapper supplyPathMapper,
                                          ProgrammaticDspMutationMapper mutationMapper,
                                          ObjectMapper objectMapper,
                                          ProgrammaticDspProviderWriteGateway gateway) {
        this(seatMapper, campaignMapper, lineItemMapper, supplyPathMapper, mutationMapper, objectMapper,
                gateway, Clock.systemDefaultZone());
    }

    ProgrammaticDspMutationService(ProgrammaticDspSeatMapper seatMapper,
                                   ProgrammaticDspCampaignMapper campaignMapper,
                                   ProgrammaticDspLineItemMapper lineItemMapper,
                                   ProgrammaticDspSupplyPathMapper supplyPathMapper,
                                   ProgrammaticDspMutationMapper mutationMapper,
                                   ObjectMapper objectMapper,
                                   ProgrammaticDspProviderWriteGateway gateway,
                                   Clock clock) {
        this.seatMapper = seatMapper;
        this.campaignMapper = campaignMapper;
        this.lineItemMapper = lineItemMapper;
        this.supplyPathMapper = supplyPathMapper;
        this.mutationMapper = mutationMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.gateway = gateway == null ? ProgrammaticDspProviderWriteGateway.unsupported() : gateway;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public ProgrammaticDspMutationView propose(Long tenantId,
                                               ProgrammaticDspMutationCommand command,
                                               String actor) {
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP mutation command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        ProgrammaticDspSeatDO seat = seat(scopedTenantId, command.seatId());
        ProgrammaticDspLineItemDO lineItem = lineItem(scopedTenantId, command.lineItemId());
        ProgrammaticDspCampaignDO campaign = campaign(scopedTenantId, campaignId(command.campaignId(), lineItem));
        ProgrammaticDspSupplyPathDO supplyPath = supplyPath(scopedTenantId, command.supplyPathId());
        validateRelationship(seat, campaign, lineItem, supplyPath);
        String mutationType = normalizeMutationType(command.mutationType());
        String entityType = normalizeUpper(command.entityType(), "entityType");
        Map<String, Object> payload = payload(command.payload());
        validatePayload(mutationType, entityType, campaign, lineItem, supplyPath, payload, command.externalEntityId());
        String payloadJson = json(payload);
        String requestHash = sha256(payloadJson);
        String mutationKey = required(command.mutationKey(), "mutationKey");
        String idempotencyKey = defaultString(command.idempotencyKey(), mutationKey);
        ProgrammaticDspMutationDO existing = mutationMapper.selectOne(new LambdaQueryWrapper<ProgrammaticDspMutationDO>()
                .eq(ProgrammaticDspMutationDO::getTenantId, scopedTenantId)
                .eq(ProgrammaticDspMutationDO::getMutationKey, mutationKey)
                .last("LIMIT 1"));
        if (existing != null) {
            validateTenant(scopedTenantId, existing.getTenantId(), "mutation");
            if (!Objects.equals(requestHash, existing.getRequestHash())) {
                throw new IllegalArgumentException("programmatic DSP mutation request hash conflicts with existing mutation key");
            }
            return toView(existing);
        }
        LocalDateTime changedAt = now();
        ProgrammaticDspMutationDO row = new ProgrammaticDspMutationDO();
        row.setTenantId(scopedTenantId);
        row.setSeatId(seat.getId());
        row.setCampaignId(campaign == null ? null : campaign.getId());
        row.setLineItemId(lineItem == null ? null : lineItem.getId());
        row.setSupplyPathId(supplyPath == null ? null : supplyPath.getId());
        row.setProvider(seat.getProvider());
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

    public ProgrammaticDspMutationView approve(Long tenantId,
                                               Long mutationId,
                                               ProgrammaticDspMutationApprovalCommand command,
                                               String actor) {
        ProgrammaticDspMutationDO row = mutation(normalizeTenant(tenantId), mutationId);
        String decision = normalizeUpper(command == null ? null : command.decision(), "decision");
        LocalDateTime changedAt = now();
        if ("APPROVED".equals(decision)) {
            rejectSelfApproval(row.getCreatedBy(), actor);
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
                "reason", command == null ? "" : defaultString(command.reason(), ""),
                "decidedAt", changedAt.toString())));
        row.setUpdatedAt(changedAt);
        mutationMapper.updateById(row);
        return toView(row);
    }

    public ProgrammaticDspMutationView execute(Long tenantId,
                                               Long mutationId,
                                               ProgrammaticDspMutationExecuteCommand command,
                                               String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        ProgrammaticDspMutationDO row = mutation(scopedTenantId, mutationId);
        if (!"APPROVED".equals(row.getApprovalStatus())) {
            throw new IllegalStateException("programmatic DSP mutation must be approved before execution");
        }
        boolean dryRun = command == null || !Boolean.FALSE.equals(command.dryRun());
        if (!dryRun && Integer.valueOf(1).equals(row.getDryRunRequired())
                && !"DRY_RUN_OK".equals(row.getStatus())) {
            throw new IllegalStateException("dry run must pass before applying programmatic DSP mutation");
        }
        ProgrammaticDspSeatDO seat = seat(scopedTenantId, row.getSeatId());
        ProgrammaticDspLineItemDO lineItem = lineItem(scopedTenantId, row.getLineItemId());
        ProgrammaticDspCampaignDO campaign = campaign(scopedTenantId, campaignId(row.getCampaignId(), lineItem));
        ProgrammaticDspSupplyPathDO supplyPath = supplyPath(scopedTenantId, row.getSupplyPathId());
        validateRelationship(seat, campaign, lineItem, supplyPath);
        ProgrammaticDspMutationRequest request = new ProgrammaticDspMutationRequest(
                scopedTenantId,
                row.getSeatId(),
                row.getCampaignId(),
                row.getLineItemId(),
                row.getSupplyPathId(),
                row.getProvider(),
                seat.getSeatKey(),
                seat.getAdvertiserAccountId(),
                row.getMutationType(),
                row.getEntityType(),
                row.getExternalEntityId(),
                row.getIdempotencyKey(),
                dryRun,
                command == null || !Boolean.FALSE.equals(command.partialFailure()),
                map(row.getPayloadJson()),
                command == null || command.metadata() == null ? Map.of() : command.metadata());
        ProgrammaticDspMutationResult result = gateway.execute(request);
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

    public List<ProgrammaticDspMutationView> list(Long tenantId, ProgrammaticDspMutationQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long seatId = query == null ? null : query.seatId();
        Long campaignId = query == null ? null : query.campaignId();
        Long lineItemId = query == null ? null : query.lineItemId();
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        String approvalStatus = query == null ? null : normalizeOptionalUpper(query.approvalStatus());
        int limit = Math.min(Math.max(query == null || query.limit() == null ? 50 : query.limit(), 1), 100);
        return safeList(mutationMapper.selectList(new LambdaQueryWrapper<ProgrammaticDspMutationDO>()
                .eq(ProgrammaticDspMutationDO::getTenantId, scopedTenantId)
                .eq(seatId != null, ProgrammaticDspMutationDO::getSeatId, seatId)
                .eq(campaignId != null, ProgrammaticDspMutationDO::getCampaignId, campaignId)
                .eq(lineItemId != null, ProgrammaticDspMutationDO::getLineItemId, lineItemId)
                .eq(status != null, ProgrammaticDspMutationDO::getStatus, status)
                .eq(approvalStatus != null, ProgrammaticDspMutationDO::getApprovalStatus, approvalStatus)
                .orderByDesc(ProgrammaticDspMutationDO::getUpdatedAt)
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> seatId == null || Objects.equals(seatId, row.getSeatId()))
                .filter(row -> campaignId == null || Objects.equals(campaignId, row.getCampaignId()))
                .filter(row -> lineItemId == null || Objects.equals(lineItemId, row.getLineItemId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .filter(row -> approvalStatus == null || approvalStatus.equals(row.getApprovalStatus()))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    private ProgrammaticDspSeatDO seat(Long tenantId, Long seatId) {
        ProgrammaticDspSeatDO seat = seatMapper.selectById(requiredId(seatId, "seatId"));
        validateTenant(tenantId, seat == null ? null : seat.getTenantId(), "seat");
        if (!Integer.valueOf(1).equals(seat.getEnabled())) {
            throw new IllegalArgumentException("programmatic DSP seat is disabled");
        }
        return seat;
    }

    private ProgrammaticDspCampaignDO campaign(Long tenantId, Long campaignId) {
        if (campaignId == null) {
            return null;
        }
        ProgrammaticDspCampaignDO campaign = campaignMapper.selectById(requiredId(campaignId, "campaignId"));
        validateTenant(tenantId, campaign == null ? null : campaign.getTenantId(), "campaign");
        return campaign;
    }

    private ProgrammaticDspLineItemDO lineItem(Long tenantId, Long lineItemId) {
        if (lineItemId == null) {
            return null;
        }
        ProgrammaticDspLineItemDO lineItem = lineItemMapper.selectById(requiredId(lineItemId, "lineItemId"));
        validateTenant(tenantId, lineItem == null ? null : lineItem.getTenantId(), "line item");
        return lineItem;
    }

    private ProgrammaticDspSupplyPathDO supplyPath(Long tenantId, Long supplyPathId) {
        if (supplyPathId == null) {
            return null;
        }
        ProgrammaticDspSupplyPathDO supplyPath = supplyPathMapper.selectById(requiredId(supplyPathId,
                "supplyPathId"));
        validateTenant(tenantId, supplyPath == null ? null : supplyPath.getTenantId(), "supply path");
        return supplyPath;
    }

    private ProgrammaticDspMutationDO mutation(Long tenantId, Long mutationId) {
        ProgrammaticDspMutationDO row = mutationMapper.selectById(requiredId(mutationId, "mutationId"));
        validateTenant(tenantId, row == null ? null : row.getTenantId(), "mutation");
        return row;
    }

    private Long campaignId(Long commandCampaignId, ProgrammaticDspLineItemDO lineItem) {
        return commandCampaignId == null && lineItem != null ? lineItem.getCampaignId() : commandCampaignId;
    }

    private void validateRelationship(ProgrammaticDspSeatDO seat,
                                      ProgrammaticDspCampaignDO campaign,
                                      ProgrammaticDspLineItemDO lineItem,
                                      ProgrammaticDspSupplyPathDO supplyPath) {
        if (lineItem != null && !Objects.equals(seat.getId(), lineItem.getSeatId())) {
            throw new IllegalArgumentException("line item does not belong to seat");
        }
        if (lineItem != null && campaign != null && !Objects.equals(campaign.getId(), lineItem.getCampaignId())) {
            throw new IllegalArgumentException("line item does not belong to campaign");
        }
        if (campaign != null && !Objects.equals(seat.getCurrency(), campaign.getCurrency())) {
            throw new IllegalArgumentException("campaign currency does not match seat");
        }
        if (supplyPath != null) {
            if (lineItem == null) {
                throw new IllegalArgumentException("supply path requires line item");
            }
            if (!Objects.equals(lineItem.getId(), supplyPath.getLineItemId())) {
                throw new IllegalArgumentException("supply path does not belong to line item");
            }
        }
    }

    private void validatePayload(String mutationType,
                                 String entityType,
                                 ProgrammaticDspCampaignDO campaign,
                                 ProgrammaticDspLineItemDO lineItem,
                                 ProgrammaticDspSupplyPathDO supplyPath,
                                 Map<String, Object> payload,
                                 String externalEntityId) {
        rejectProviderSecrets(payload);
        switch (mutationType) {
            case "CREATE_INSERTION_ORDER" -> {
                if (!"CAMPAIGN".equals(entityType) && !"INSERTION_ORDER".equals(entityType)) {
                    throw new IllegalArgumentException("insertion order mutation entity must be CAMPAIGN or INSERTION_ORDER");
                }
                requireAnyPayloadValue(payload, "insertionOrderName", "campaignName");
            }
            case "UPDATE_CAMPAIGN_BUDGET" -> {
                requireCampaign(campaign, mutationType);
                if (!"CAMPAIGN".equals(entityType) && !"INSERTION_ORDER".equals(entityType)) {
                    throw new IllegalArgumentException("campaign budget entity must be CAMPAIGN or INSERTION_ORDER");
                }
                requireAnyPayloadValue(payload, "budgetMicros", "budgetAmount");
            }
            case "CREATE_LINE_ITEM" -> {
                requireCampaign(campaign, mutationType);
                requireAnyPayloadValue(payload, "lineItemName", "lineItemKey");
            }
            case "UPDATE_LINE_ITEM_BID" -> {
                requireLineItem(lineItem, mutationType);
                if (!"LINE_ITEM".equals(entityType)) {
                    throw new IllegalArgumentException("line item bid entity must be LINE_ITEM");
                }
                requireAnyPayloadValue(payload, "bidCpmMicros", "maxBidCpm", "bidStrategy");
            }
            case "UPDATE_LINE_ITEM_BUDGET" -> {
                requireLineItem(lineItem, mutationType);
                if (!"LINE_ITEM".equals(entityType)) {
                    throw new IllegalArgumentException("line item budget entity must be LINE_ITEM");
                }
                requireAnyPayloadValue(payload, "dailyBudgetMicros", "totalBudgetMicros",
                        "dailyBudgetAmount", "totalBudgetAmount");
            }
            case "UPDATE_LINE_ITEM_STATUS" -> {
                requireLineItem(lineItem, mutationType);
                if (!"LINE_ITEM".equals(entityType)) {
                    throw new IllegalArgumentException("line item status entity must be LINE_ITEM");
                }
                requireAnyPayloadValue(payload, "status");
            }
            case "ASSIGN_TARGETING" -> {
                requireLineItem(lineItem, mutationType);
                if (!"LINE_ITEM".equals(entityType) && !"TARGETING".equals(entityType)) {
                    throw new IllegalArgumentException("targeting entity must be LINE_ITEM or TARGETING");
                }
                requireAnyPayloadValue(payload, "targeting", "targetingType", "assignedTargetingOptions");
            }
            case "ATTACH_DEAL" -> {
                requireLineItem(lineItem, mutationType);
                if (supplyPath == null) {
                    requireAnyPayloadValue(payload, "dealId");
                }
            }
            case "SYNC_PROVIDER_STATUS" -> {
                if (lineItem == null && campaign == null && trimToNull(externalEntityId) == null) {
                    throw new IllegalArgumentException("provider status sync requires campaign, line item, or external entity id");
                }
            }
            default -> throw new IllegalArgumentException("unsupported programmatic DSP mutation type");
        }
    }

    private void requireCampaign(ProgrammaticDspCampaignDO campaign, String mutationType) {
        if (campaign == null) {
            throw new IllegalArgumentException(mutationType + " requires campaign");
        }
    }

    private void requireLineItem(ProgrammaticDspLineItemDO lineItem, String mutationType) {
        if (lineItem == null) {
            throw new IllegalArgumentException(mutationType + " requires line item");
        }
    }

    private void requireAnyPayloadValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String text) {
                if (trimToNull(text) != null) {
                    return;
                }
            } else if (value != null) {
                return;
            }
        }
        throw new IllegalArgumentException(String.join(" or ", keys) + " is required");
    }

    private ProgrammaticDspMutationView toView(ProgrammaticDspMutationDO row) {
        return new ProgrammaticDspMutationView(
                row.getId(),
                row.getTenantId(),
                row.getSeatId(),
                row.getCampaignId(),
                row.getLineItemId(),
                row.getSupplyPathId(),
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

    private Map<String, Object> providerRequestEvidence(ProgrammaticDspMutationRequest request) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("provider", request.provider());
        evidence.put("seatKey", request.seatKey());
        evidence.put("advertiserAccountId", request.advertiserAccountId());
        evidence.put("mutationType", request.mutationType());
        evidence.put("entityType", request.entityType());
        evidence.put("externalEntityId", defaultString(request.externalEntityId(), ""));
        evidence.put("idempotencyKey", request.idempotencyKey());
        evidence.put("dryRun", request.dryRun());
        evidence.put("partialFailure", request.partialFailure());
        evidence.put("seatId", request.seatId());
        evidence.put("campaignId", request.campaignId());
        evidence.put("lineItemId", request.lineItemId());
        evidence.put("supplyPathId", request.supplyPathId());
        evidence.put("payload", request.payload());
        evidence.put("metadata", request.metadata());
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    private Map<String, Object> payload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("programmatic DSP mutation payload is required");
        }
        return Map.copyOf(payload);
    }

    private void rejectProviderSecrets(Object value) {
        if (value instanceof Map<?, ?> values) {
            values.forEach((key, nestedValue) -> {
                if (key != null && SECRET_KEYS.contains(key.toString().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("programmatic DSP mutation payload must not contain provider secrets");
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
            throw new IllegalArgumentException("unsupported programmatic DSP mutation type");
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

    private void rejectSelfApproval(String createdBy, String actor) {
        if (Objects.equals(defaultString(createdBy, "system"), defaultString(actor, "system"))) {
            throw new IllegalStateException("creator cannot approve live programmatic DSP provider mutation");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("programmatic DSP mutation metadata is not JSON serializable", ex);
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
            throw new IllegalStateException("Could not hash programmatic DSP mutation request", ex);
        }
    }
}
