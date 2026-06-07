package org.chovy.canvas.domain.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingProviderChangeDO;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingProviderChangeMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class SearchMarketingReconciliationService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final SearchMarketingMutationMapper mutationMapper;
    private final SearchMarketingProviderChangeMapper providerChangeMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public SearchMarketingReconciliationService(SearchMarketingMutationMapper mutationMapper,
                                                SearchMarketingProviderChangeMapper providerChangeMapper,
                                                ObjectMapper objectMapper) {
        this(mutationMapper, providerChangeMapper, objectMapper, Clock.systemDefaultZone());
    }

    SearchMarketingReconciliationService(SearchMarketingMutationMapper mutationMapper,
                                         SearchMarketingProviderChangeMapper providerChangeMapper,
                                         ObjectMapper objectMapper,
                                         Clock clock) {
        this.mutationMapper = mutationMapper;
        this.providerChangeMapper = providerChangeMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public SearchMarketingReconciliationView reconcile(Long tenantId, Long mutationId, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        SearchMarketingMutationDO mutation = mutationMapper.selectById(requiredId(mutationId, "mutationId"));
        validateTenant(scopedTenantId, mutation == null ? null : mutation.getTenantId(), "mutation");
        LocalDateTime changedAt = now();
        Map<String, Object> providerResponse = map(mutation.getProviderResponseJson());
        String providerOperationId = providerOperationId(providerResponse);
        boolean confirmed = "APPLIED".equals(mutation.getStatus()) && providerOperationId != null;
        SearchMarketingProviderChangeDO change = new SearchMarketingProviderChangeDO();
        change.setTenantId(scopedTenantId);
        change.setSourceId(mutation.getSourceId());
        change.setMutationId(mutation.getId());
        change.setProvider(mutation.getProvider());
        change.setExternalResourceId(defaultString(mutation.getExternalEntityId(), providerOperationId));
        change.setChangeType(mutation.getMutationType());
        change.setChangedFieldsJson(json(map(mutation.getPayloadJson())));
        change.setProviderActor(defaultString(mutation.getExecutedBy(), defaultString(actor, "system")));
        change.setProviderChangedAt(mutation.getExecutedAt() == null ? changedAt : mutation.getExecutedAt());
        change.setReconciliationStatus(confirmed ? "CONFIRMED" : "FAILED");
        change.setEvidenceJson(json(evidence(mutation, providerResponse, providerOperationId, confirmed)));
        change.setCreatedAt(changedAt);
        change.setUpdatedAt(changedAt);
        providerChangeMapper.insert(change);

        mutation.setStatus(confirmed ? "RECONCILED" : "RECONCILE_FAILED");
        if (!confirmed) {
            mutation.setErrorCode("SEARCH_RECONCILIATION_NOT_CONFIRMED");
            mutation.setErrorMessage("search provider state did not confirm the applied mutation");
        }
        mutation.setUpdatedAt(changedAt);
        mutationMapper.updateById(mutation);

        return new SearchMarketingReconciliationView(
                scopedTenantId,
                mutation.getId(),
                change.getId(),
                mutation.getStatus(),
                providerOperationId,
                map(change.getEvidenceJson()),
                changedAt);
    }

    public List<SearchMarketingProviderChangeView> list(Long tenantId, SearchMarketingProviderChangeQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long sourceId = query == null ? null : query.sourceId();
        Long mutationId = query == null ? null : query.mutationId();
        String provider = query == null ? null : normalizeOptional(query.provider());
        String reconciliationStatus = query == null ? null : normalizeOptional(query.reconciliationStatus());
        int limit = Math.max(1, Math.min(query == null || query.limit() == null ? 50 : query.limit(), 100));
        return safeList(providerChangeMapper.selectList(new LambdaQueryWrapper<SearchMarketingProviderChangeDO>()
                .eq(SearchMarketingProviderChangeDO::getTenantId, scopedTenantId)
                .eq(sourceId != null, SearchMarketingProviderChangeDO::getSourceId, sourceId)
                .eq(mutationId != null, SearchMarketingProviderChangeDO::getMutationId, mutationId)
                .eq(provider != null, SearchMarketingProviderChangeDO::getProvider, provider)
                .eq(reconciliationStatus != null, SearchMarketingProviderChangeDO::getReconciliationStatus,
                        reconciliationStatus)
                .orderByDesc(SearchMarketingProviderChangeDO::getProviderChangedAt)
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> mutationId == null || Objects.equals(mutationId, row.getMutationId()))
                .filter(row -> provider == null || provider.equals(normalize(row.getProvider())))
                .filter(row -> reconciliationStatus == null
                        || reconciliationStatus.equals(normalize(row.getReconciliationStatus())))
                .limit(limit)
                .map(this::toProviderChangeView)
                .toList();
    }

    private SearchMarketingProviderChangeView toProviderChangeView(SearchMarketingProviderChangeDO row) {
        return new SearchMarketingProviderChangeView(
                row.getId(),
                row.getTenantId(),
                row.getSourceId(),
                row.getMutationId(),
                row.getProvider(),
                row.getExternalResourceId(),
                row.getChangeType(),
                map(row.getChangedFieldsJson()),
                row.getProviderActor(),
                row.getProviderChangedAt(),
                row.getReconciliationStatus(),
                map(row.getEvidenceJson()),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private Map<String, Object> evidence(SearchMarketingMutationDO mutation,
                                         Map<String, Object> providerResponse,
                                         String providerOperationId,
                                         boolean confirmed) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("mutationId", mutation.getId());
        evidence.put("mutationStatus", mutation.getStatus());
        if (providerOperationId != null) {
            evidence.put("providerOperationId", providerOperationId);
        }
        evidence.put("confirmed", confirmed);
        evidence.put("providerResponse", providerResponse);
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    private String providerOperationId(Map<String, Object> providerResponse) {
        Object direct = firstNonNull(providerResponse, "providerOperationId", "operationId", "batchJobId");
        return direct == null ? null : trimToNull(String.valueOf(direct));
    }

    private Object firstNonNull(Map<String, Object> values, String... keys) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private void validateTenant(Long expectedTenantId, Long actualTenantId, String resource) {
        if (!Objects.equals(expectedTenantId, actualTenantId)) {
            throw new IllegalArgumentException(resource + " does not belong to current tenant");
        }
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

    private String normalizeOptional(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("search marketing reconciliation evidence is not JSON serializable", ex);
        }
    }

    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(json, OBJECT_MAP);
            return values == null ? Map.of() : ProviderWriteEvidenceSanitizer.sanitizeMap(values);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }
}
