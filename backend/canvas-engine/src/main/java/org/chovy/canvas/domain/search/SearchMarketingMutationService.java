package org.chovy.canvas.domain.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingKeywordDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingOpportunityDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.mapper.SearchMarketingKeywordMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingOpportunityMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
public class SearchMarketingMutationService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final Set<String> MUTATION_TYPES = Set.of(
            "ADD_KEYWORD",
            "UPDATE_KEYWORD_BID",
            "ADD_NEGATIVE_KEYWORD",
            "UPDATE_CAMPAIGN_BUDGET",
            "PAUSE_KEYWORD");
    private static final Set<String> SECRET_KEYS = Set.of(
            "token",
            "access_token",
            "refresh_token",
            "client_secret",
            "api_key",
            "apikey",
            "password");

    private final SearchMarketingSourceMapper sourceMapper;
    private final SearchMarketingKeywordMapper keywordMapper;
    private final SearchMarketingOpportunityMapper opportunityMapper;
    private final SearchMarketingMutationMapper mutationMapper;
    private final ObjectMapper objectMapper;
    private final SearchMarketingProviderWriteGateway gateway;
    private final Clock clock;

    @Autowired
    public SearchMarketingMutationService(SearchMarketingSourceMapper sourceMapper,
                                          SearchMarketingKeywordMapper keywordMapper,
                                          SearchMarketingOpportunityMapper opportunityMapper,
                                          SearchMarketingMutationMapper mutationMapper,
                                          ObjectMapper objectMapper,
                                          SearchMarketingProviderWriteGateway gateway) {
        this(sourceMapper, keywordMapper, opportunityMapper, mutationMapper, objectMapper, gateway,
                Clock.systemDefaultZone());
    }

    SearchMarketingMutationService(SearchMarketingSourceMapper sourceMapper,
                                   SearchMarketingKeywordMapper keywordMapper,
                                   SearchMarketingOpportunityMapper opportunityMapper,
                                   SearchMarketingMutationMapper mutationMapper,
                                   ObjectMapper objectMapper,
                                   SearchMarketingProviderWriteGateway gateway,
                                   Clock clock) {
        this.sourceMapper = sourceMapper;
        this.keywordMapper = keywordMapper;
        this.opportunityMapper = opportunityMapper;
        this.mutationMapper = mutationMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.gateway = gateway == null ? SearchMarketingProviderWriteGateway.unsupported() : gateway;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public SearchMarketingMutationView propose(Long tenantId, SearchMarketingMutationCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("search marketing mutation command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        SearchMarketingSourceDO source = source(scopedTenantId, command.sourceId());
        SearchMarketingKeywordDO keyword = keyword(scopedTenantId, command.keywordId());
        SearchMarketingOpportunityDO opportunity = opportunity(scopedTenantId, command.opportunityId());
        validateOpportunityScope(opportunity, command.sourceId(), command.keywordId());
        String mutationType = normalizeMutationType(command.mutationType());
        String entityType = normalizeUpper(command.entityType(), "entityType");
        Map<String, Object> payload = payload(command.payload());
        validatePayload(mutationType, entityType, command.externalEntityId(), command.keywordId(), payload);
        String payloadJson = json(payload);
        String requestHash = sha256(payloadJson);
        String mutationKey = required(command.mutationKey(), "mutationKey");
        String idempotencyKey = defaultString(command.idempotencyKey(), mutationKey);
        SearchMarketingMutationDO existing = mutationMapper.selectOne(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                .eq(SearchMarketingMutationDO::getTenantId, scopedTenantId)
                .eq(SearchMarketingMutationDO::getMutationKey, mutationKey)
                .last("LIMIT 1"));
        if (existing != null) {
            validateTenant(scopedTenantId, existing.getTenantId(), "mutation");
            if (!Objects.equals(requestHash, existing.getRequestHash())
                    && !Objects.equals(payloadJson, json(map(existing.getPayloadJson())))) {
                throw new IllegalArgumentException("mutation request hash conflicts with existing mutation key");
            }
            return toView(existing);
        }
        LocalDateTime changedAt = now();
        SearchMarketingMutationDO row = new SearchMarketingMutationDO();
        row.setTenantId(scopedTenantId);
        row.setSourceId(source.getId());
        row.setOpportunityId(opportunity == null ? null : opportunity.getId());
        row.setKeywordId(keyword == null ? null : keyword.getId());
        row.setProvider(source.getProvider());
        row.setChannel(source.getChannel());
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

    public SearchMarketingMutationView proposeFromOpportunity(Long tenantId,
                                                              Long opportunityId,
                                                              SearchMarketingOpportunityMutationCommand command,
                                                              String actor) {
        if (command == null) {
            throw new IllegalArgumentException("search marketing opportunity mutation command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        SearchMarketingOpportunityDO opportunity = opportunity(scopedTenantId, opportunityId);
        if (!"ACCEPTED".equals(opportunity.getStatus())) {
            throw new IllegalStateException("search marketing opportunity must be accepted before creating mutation");
        }
        return propose(scopedTenantId,
                new SearchMarketingMutationCommand(
                        opportunity.getSourceId(),
                        opportunity.getId(),
                        opportunity.getKeywordId(),
                        command.mutationKey(),
                        command.mutationType(),
                        command.entityType(),
                        command.externalEntityId(),
                        command.dryRunRequired(),
                        command.idempotencyKey(),
                        command.payload()),
                actor);
    }

    public SearchMarketingMutationView approve(Long tenantId,
                                               Long mutationId,
                                               SearchMarketingMutationApprovalCommand command,
                                               String actor) {
        SearchMarketingMutationDO row = mutation(normalizeTenant(tenantId), mutationId);
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

    public SearchMarketingMutationView execute(Long tenantId,
                                               Long mutationId,
                                               SearchMarketingMutationExecuteCommand command,
                                               String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        SearchMarketingMutationDO row = mutation(scopedTenantId, mutationId);
        if (!"APPROVED".equals(row.getApprovalStatus())) {
            throw new IllegalStateException("search marketing mutation must be approved before execution");
        }
        boolean dryRun = command == null || !Boolean.FALSE.equals(command.dryRun());
        if (!dryRun && Integer.valueOf(1).equals(row.getDryRunRequired())
                && !"DRY_RUN_OK".equals(row.getStatus())) {
            throw new IllegalStateException("dry run must pass before applying search marketing mutation");
        }
        SearchMarketingSourceDO source = source(scopedTenantId, row.getSourceId());
        Map<String, Object> metadata = command == null || command.metadata() == null ? Map.of() : command.metadata();
        boolean partialFailure = command == null || !Boolean.FALSE.equals(command.partialFailure());
        SearchMarketingProviderMutationRequest request = new SearchMarketingProviderMutationRequest(
                scopedTenantId,
                row.getSourceId(),
                row.getProvider(),
                source.getSourceKey(),
                source.getExternalAccountId(),
                row.getMutationType(),
                row.getEntityType(),
                row.getExternalEntityId(),
                row.getIdempotencyKey(),
                dryRun,
                partialFailure,
                map(row.getPayloadJson()),
                metadata);
        SearchMarketingProviderMutationResult result = gateway.execute(request);
        LocalDateTime changedAt = now();
        row.setProviderRequestJson(json(providerRequestEvidence(request)));
        row.setProviderResponseJson(json(providerResponseEvidence(result)));
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

    public List<SearchMarketingMutationView> list(Long tenantId, SearchMarketingMutationQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long sourceId = query == null ? null : query.sourceId();
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        String approvalStatus = query == null ? null : normalizeOptionalUpper(query.approvalStatus());
        int limit = Math.min(Math.max(query == null || query.limit() == null ? 50 : query.limit(), 1), 100);
        return safeList(mutationMapper.selectList(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                .eq(SearchMarketingMutationDO::getTenantId, scopedTenantId)
                .eq(sourceId != null, SearchMarketingMutationDO::getSourceId, sourceId)
                .eq(status != null, SearchMarketingMutationDO::getStatus, status)
                .eq(approvalStatus != null, SearchMarketingMutationDO::getApprovalStatus, approvalStatus)
                .orderByDesc(SearchMarketingMutationDO::getUpdatedAt)
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .filter(row -> approvalStatus == null || approvalStatus.equals(row.getApprovalStatus()))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    private SearchMarketingSourceDO source(Long tenantId, Long sourceId) {
        SearchMarketingSourceDO source = sourceMapper.selectById(requiredId(sourceId, "sourceId"));
        validateTenant(tenantId, source == null ? null : source.getTenantId(), "source");
        if (!"SEM".equals(source.getChannel())) {
            throw new IllegalArgumentException("search marketing provider mutations require SEM source");
        }
        if (!Integer.valueOf(1).equals(source.getEnabled())) {
            throw new IllegalArgumentException("search marketing source is disabled");
        }
        return source;
    }

    private SearchMarketingKeywordDO keyword(Long tenantId, Long keywordId) {
        if (keywordId == null) {
            return null;
        }
        SearchMarketingKeywordDO keyword = keywordMapper.selectById(requiredId(keywordId, "keywordId"));
        validateTenant(tenantId, keyword == null ? null : keyword.getTenantId(), "keyword");
        return keyword;
    }

    private SearchMarketingOpportunityDO opportunity(Long tenantId, Long opportunityId) {
        if (opportunityId == null) {
            return null;
        }
        SearchMarketingOpportunityDO opportunity = opportunityMapper.selectById(requiredId(opportunityId,
                "opportunityId"));
        validateTenant(tenantId, opportunity == null ? null : opportunity.getTenantId(), "opportunity");
        return opportunity;
    }

    private SearchMarketingMutationDO mutation(Long tenantId, Long mutationId) {
        SearchMarketingMutationDO row = mutationMapper.selectById(requiredId(mutationId, "mutationId"));
        validateTenant(tenantId, row == null ? null : row.getTenantId(), "mutation");
        return row;
    }

    private void validateOpportunityScope(SearchMarketingOpportunityDO opportunity, Long sourceId, Long keywordId) {
        if (opportunity == null) {
            return;
        }
        if (!Objects.equals(sourceId, opportunity.getSourceId())) {
            throw new IllegalArgumentException("opportunity source does not match mutation source");
        }
        if (keywordId != null && !Objects.equals(keywordId, opportunity.getKeywordId())) {
            throw new IllegalArgumentException("opportunity keyword does not match mutation keyword");
        }
    }

    private void validatePayload(String mutationType,
                                 String entityType,
                                 String externalEntityId,
                                 Long keywordId,
                                 Map<String, Object> payload) {
        rejectProviderSecrets(payload);
        switch (mutationType) {
            case "ADD_KEYWORD" -> {
                requirePayloadText(payload, "text");
                requirePayloadText(payload, "matchType");
            }
            case "UPDATE_KEYWORD_BID" -> {
                requireEntityReference(keywordId, externalEntityId, "keyword bid update");
                requirePositive(payload, "bidMicros");
            }
            case "ADD_NEGATIVE_KEYWORD" -> {
                if (!"CAMPAIGN".equals(entityType) && !"AD_GROUP".equals(entityType)) {
                    throw new IllegalArgumentException("negative keyword mutation entity must be CAMPAIGN or AD_GROUP");
                }
                requirePayloadText(payload, "text");
            }
            case "UPDATE_CAMPAIGN_BUDGET" -> {
                if (!"CAMPAIGN".equals(entityType)) {
                    throw new IllegalArgumentException("campaign budget mutation entity must be CAMPAIGN");
                }
                requirePositive(payload, "budgetMicros");
            }
            case "PAUSE_KEYWORD" -> requireEntityReference(keywordId, externalEntityId, "keyword pause");
            default -> throw new IllegalArgumentException("unsupported search marketing mutation type");
        }
    }

    private void requirePayloadText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof String text) || text.trim().isEmpty()) {
            throw new IllegalArgumentException(key + " is required");
        }
    }

    private void requirePositive(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        BigDecimal number = switch (value) {
            case Number numeric -> new BigDecimal(numeric.toString());
            case String text when !text.isBlank() -> new BigDecimal(text.trim());
            default -> BigDecimal.ZERO;
        };
        if (number.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
    }

    private void requireEntityReference(Long keywordId, String externalEntityId, String operation) {
        if ((keywordId == null || keywordId <= 0) && trimToNull(externalEntityId) == null) {
            throw new IllegalArgumentException(operation + " requires keywordId or externalEntityId");
        }
    }

    private SearchMarketingMutationView toView(SearchMarketingMutationDO row) {
        return new SearchMarketingMutationView(
                row.getId(),
                row.getTenantId(),
                row.getSourceId(),
                row.getOpportunityId(),
                row.getKeywordId(),
                row.getProvider(),
                row.getChannel(),
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

    private Map<String, Object> providerRequestEvidence(SearchMarketingProviderMutationRequest request) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("provider", request.provider());
        evidence.put("sourceKey", request.sourceKey());
        evidence.put("externalAccountId", defaultString(request.externalAccountId(), ""));
        evidence.put("mutationType", request.mutationType());
        evidence.put("entityType", request.entityType());
        evidence.put("externalEntityId", defaultString(request.externalEntityId(), ""));
        evidence.put("idempotencyKey", request.idempotencyKey());
        evidence.put("dryRun", request.dryRun());
        evidence.put("partialFailure", request.partialFailure());
        evidence.put("payload", request.payload());
        evidence.put("metadata", request.metadata());
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    private Map<String, Object> providerResponseEvidence(SearchMarketingProviderMutationResult result) {
        Map<String, Object> evidence = new LinkedHashMap<>(
                ProviderWriteEvidenceSanitizer.sanitizeMap(result.response()));
        if (result.providerOperationId() != null) {
            evidence.put("providerOperationId", result.providerOperationId());
        }
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    private Map<String, Object> payload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("mutation payload is required");
        }
        return Map.copyOf(payload);
    }

    private void rejectProviderSecrets(Object value) {
        if (value instanceof Map<?, ?> values) {
            values.forEach((key, nestedValue) -> {
                if (key != null && SECRET_KEYS.contains(key.toString().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("mutation payload must not contain provider secrets");
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
            throw new IllegalArgumentException("unsupported search marketing mutation type");
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

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("search marketing mutation metadata is not JSON serializable", ex);
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
            throw new IllegalStateException("Could not hash search marketing mutation request", ex);
        }
    }
}
