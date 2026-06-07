package org.chovy.canvas.domain.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingImpactWindowDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSyncRunDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingImpactWindowMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSyncRunMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SearchMarketingReadinessService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final Set<String> BLOCKING_SYNC_ERROR_FRAGMENTS = Set.of(
            "AUTH",
            "PERMISSION",
            "SCHEMA",
            "QUOTA_EXHAUSTED");

    private final SearchMarketingSyncRunService legacySyncRunService;
    private final SearchMarketingSourceMapper sourceMapper;
    private final MarketingMonitorProviderCredentialMapper credentialMapper;
    private final SearchMarketingSyncRunMapper syncRunMapper;
    private final SearchMarketingMutationMapper mutationMapper;
    private final SearchMarketingImpactWindowMapper impactWindowMapper;
    private final SearchMarketingProviderWriteGateway writeGateway;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SearchMarketingReadinessService(SearchMarketingSyncRunService syncRunService) {
        this.legacySyncRunService = syncRunService;
        this.sourceMapper = null;
        this.credentialMapper = null;
        this.syncRunMapper = null;
        this.mutationMapper = null;
        this.impactWindowMapper = null;
        this.writeGateway = null;
        this.objectMapper = new ObjectMapper();
        this.clock = Clock.systemDefaultZone();
    }

    @Autowired
    public SearchMarketingReadinessService(SearchMarketingSourceMapper sourceMapper,
                                           MarketingMonitorProviderCredentialMapper credentialMapper,
                                           SearchMarketingSyncRunMapper syncRunMapper,
                                           SearchMarketingMutationMapper mutationMapper,
                                           SearchMarketingImpactWindowMapper impactWindowMapper,
                                           SearchMarketingProviderWriteGateway writeGateway,
                                           ObjectMapper objectMapper) {
        this(sourceMapper, credentialMapper, syncRunMapper, mutationMapper, impactWindowMapper, writeGateway,
                objectMapper, Clock.systemDefaultZone());
    }

    SearchMarketingReadinessService(SearchMarketingSourceMapper sourceMapper,
                                    MarketingMonitorProviderCredentialMapper credentialMapper,
                                    SearchMarketingSyncRunMapper syncRunMapper,
                                    SearchMarketingMutationMapper mutationMapper,
                                    SearchMarketingImpactWindowMapper impactWindowMapper,
                                    SearchMarketingProviderWriteGateway writeGateway,
                                    ObjectMapper objectMapper,
                                    Clock clock) {
        this.legacySyncRunService = null;
        this.sourceMapper = sourceMapper;
        this.credentialMapper = credentialMapper;
        this.syncRunMapper = syncRunMapper;
        this.mutationMapper = mutationMapper;
        this.impactWindowMapper = impactWindowMapper;
        this.writeGateway = writeGateway == null ? SearchMarketingProviderWriteGateway.unsupported() : writeGateway;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public SearchMarketingReadinessView readiness(Long tenantId) {
        if (sourceMapper == null || credentialMapper == null || syncRunMapper == null
                || mutationMapper == null || impactWindowMapper == null) {
            if (legacySyncRunService == null) {
                return new SearchMarketingReadinessView(normalizeTenant(tenantId), "BLOCKED",
                        List.of("search marketing readiness dependencies are not configured"), Map.of(), now());
            }
            return legacySyncRunService.readiness(tenantId);
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime evaluatedAt = now();
        List<SearchMarketingSourceDO> sources = enabledSources(scopedTenantId);
        List<MarketingMonitorProviderCredentialDO> credentials = activeCredentials(scopedTenantId);
        List<SearchMarketingSyncRunDO> syncRuns = syncRuns(scopedTenantId);
        List<SearchMarketingMutationDO> mutations = mutations(scopedTenantId);
        List<SearchMarketingImpactWindowDO> impactWindows = impactWindows(scopedTenantId);

        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (sources.isEmpty()) {
            blockers.add("no enabled search marketing source");
        }
        Map<String, List<MarketingMonitorProviderCredentialDO>> credentialsByProvider = credentials.stream()
                .collect(Collectors.groupingBy(row -> normalize(row.getProviderType())));
        Map<Long, SearchMarketingSourceDO> sourcesById = sources.stream()
                .collect(Collectors.toMap(SearchMarketingSourceDO::getId, source -> source, (left, right) -> left));
        for (SearchMarketingSourceDO source : sources) {
            verifyCredential(source, credentialsByProvider, blockers);
            verifyFreshPerformanceSync(source, syncRuns, blockers);
            verifyLatestBlockingFailure(source, syncRuns, blockers);
            if ("SEM".equals(normalize(source.getChannel()))
                    && !writeGateway.supportsLiveApply(source.getProvider())) {
                blockers.add("live write adapter is unavailable for " + normalize(source.getProvider()));
            }
        }
        verifyUnreconciledWrites(sourcesById, mutations, blockers);
        verifyImpactWindows(impactWindows, blockers);

        String status = blockers.isEmpty() ? (warnings.isEmpty() ? "LIVE" : "DEGRADED") : "BLOCKED";
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("enabledSourceCount", (long) sources.size());
        evidence.put("activeCredentialCount", (long) credentials.size());
        evidence.put("syncRunCount", (long) syncRuns.size());
        evidence.put("mutationCount", (long) mutations.size());
        evidence.put("impactWindowCount", (long) impactWindows.size());
        evidence.put("blockerCount", (long) blockers.size());
        evidence.put("warningCount", (long) warnings.size());
        return new SearchMarketingReadinessView(
                scopedTenantId,
                status,
                blockers,
                ProviderWriteEvidenceSanitizer.sanitizeMap(evidence),
                evaluatedAt);
    }

    private List<SearchMarketingSourceDO> enabledSources(Long tenantId) {
        return safeList(sourceMapper.selectList(new LambdaQueryWrapper<SearchMarketingSourceDO>()
                .eq(SearchMarketingSourceDO::getTenantId, tenantId)
                .eq(SearchMarketingSourceDO::getEnabled, 1))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> Integer.valueOf(1).equals(row.getEnabled()))
                .toList();
    }

    private List<MarketingMonitorProviderCredentialDO> activeCredentials(Long tenantId) {
        return safeList(credentialMapper.selectList(new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                .eq(MarketingMonitorProviderCredentialDO::getTenantId, tenantId)
                .eq(MarketingMonitorProviderCredentialDO::getStatus, "ACTIVE"))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> "ACTIVE".equals(normalize(row.getStatus())))
                .toList();
    }

    private List<SearchMarketingSyncRunDO> syncRuns(Long tenantId) {
        return safeList(syncRunMapper.selectList(new LambdaQueryWrapper<SearchMarketingSyncRunDO>()
                .eq(SearchMarketingSyncRunDO::getTenantId, tenantId)
                .orderByDesc(SearchMarketingSyncRunDO::getFinishedAt)
                .last("LIMIT 1000"))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .toList();
    }

    private List<SearchMarketingMutationDO> mutations(Long tenantId) {
        return safeList(mutationMapper.selectList(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                .eq(SearchMarketingMutationDO::getTenantId, tenantId)
                .in(SearchMarketingMutationDO::getStatus, "APPLIED", "FAILED", "RECONCILE_FAILED")
                .last("LIMIT 1000"))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .toList();
    }

    private List<SearchMarketingImpactWindowDO> impactWindows(Long tenantId) {
        return safeList(impactWindowMapper.selectList(new LambdaQueryWrapper<SearchMarketingImpactWindowDO>()
                .eq(SearchMarketingImpactWindowDO::getTenantId, tenantId)
                .in(SearchMarketingImpactWindowDO::getStatus, "SCHEDULED", "DUE")
                .last("LIMIT 1000"))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .toList();
    }

    private void verifyCredential(SearchMarketingSourceDO source,
                                  Map<String, List<MarketingMonitorProviderCredentialDO>> credentialsByProvider,
                                  List<String> blockers) {
        String provider = normalize(source.getProvider());
        String credentialKey = credentialKey(source);
        List<MarketingMonitorProviderCredentialDO> candidates = credentialsByProvider.getOrDefault(provider, List.of());
        MarketingMonitorProviderCredentialDO credential = candidates.stream()
                .filter(row -> credentialKey == null || credentialKey.equals(row.getCredentialKey()))
                .findFirst()
                .orElse(null);
        if (credential == null) {
            blockers.add("active credential is unavailable for " + provider + " source " + source.getId());
            return;
        }
        if (credential.getExpiresAt() != null && !credential.getExpiresAt().isAfter(now())) {
            blockers.add("credential " + credential.getCredentialKey() + " is expired");
        }
    }

    private void verifyFreshPerformanceSync(SearchMarketingSourceDO source,
                                            List<SearchMarketingSyncRunDO> syncRuns,
                                            List<String> blockers) {
        SearchMarketingSyncRunDO latestSuccess = syncRuns.stream()
                .filter(row -> Objects.equals(source.getId(), row.getSourceId()))
                .filter(row -> "PERFORMANCE".equals(normalize(row.getRunType())))
                .filter(row -> "SUCCEEDED".equals(normalize(row.getStatus())))
                .filter(row -> row.getFinishedAt() != null)
                .max(Comparator.comparing(SearchMarketingSyncRunDO::getFinishedAt))
                .orElse(null);
        if (latestSuccess == null) {
            blockers.add("latest successful PERFORMANCE sync is missing for source " + source.getId());
            return;
        }
        if (latestSuccess.getFinishedAt().isBefore(now().minusHours(freshnessHours(source)))) {
            blockers.add("latest successful PERFORMANCE sync is stale for source " + source.getId());
        }
    }

    private void verifyLatestBlockingFailure(SearchMarketingSourceDO source,
                                             List<SearchMarketingSyncRunDO> syncRuns,
                                             List<String> blockers) {
        syncRuns.stream()
                .filter(row -> Objects.equals(source.getId(), row.getSourceId()))
                .filter(row -> "FAILED".equals(normalize(row.getStatus())))
                .filter(row -> blockingError(row.getErrorCode()))
                .max(Comparator.comparing(row -> row.getFinishedAt() == null ? LocalDateTime.MIN : row.getFinishedAt()))
                .ifPresent(row -> blockers.add("blocking sync failure " + row.getErrorCode()
                        + " for source " + source.getId()));
    }

    private boolean blockingError(String errorCode) {
        String normalized = normalize(errorCode);
        return BLOCKING_SYNC_ERROR_FRAGMENTS.stream().anyMatch(normalized::contains);
    }

    private void verifyUnreconciledWrites(Map<Long, SearchMarketingSourceDO> sourcesById,
                                          List<SearchMarketingMutationDO> mutations,
                                          List<String> blockers) {
        for (SearchMarketingMutationDO mutation : mutations) {
            if (!"APPLIED".equals(normalize(mutation.getStatus()))) {
                continue;
            }
            SearchMarketingSourceDO source = sourcesById.get(mutation.getSourceId());
            long slaHours = source == null ? 24L : reconciliationSlaHours(source);
            if (mutation.getExecutedAt() == null || mutation.getExecutedAt().isBefore(now().minusHours(slaHours))) {
                blockers.add("unreconciled live mutation " + mutation.getId()
                        + " exceeded reconciliation SLA");
            }
        }
    }

    private void verifyImpactWindows(List<SearchMarketingImpactWindowDO> impactWindows, List<String> blockers) {
        for (SearchMarketingImpactWindowDO window : impactWindows) {
            if (window.getDueAt() != null && !window.getDueAt().isAfter(now())) {
                blockers.add("impact window " + window.getId() + " is due but not evaluated");
            }
        }
    }

    private long freshnessHours(SearchMarketingSourceDO source) {
        return boundedHours(source, "freshnessHours", "syncFreshnessHours", 24L);
    }

    private long reconciliationSlaHours(SearchMarketingSourceDO source) {
        return boundedHours(source, "reconciliationSlaHours", "reconcileSlaHours", 24L);
    }

    private long boundedHours(SearchMarketingSourceDO source, String primaryKey, String secondaryKey, long fallback) {
        Object raw = firstPresent(map(source.getMetadataJson()), primaryKey, secondaryKey);
        if (raw instanceof Number number) {
            return Math.max(1L, Math.min(number.longValue(), 24L * 30L));
        }
        String value = trimToNull(raw == null ? null : String.valueOf(raw));
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(1L, Math.min(Long.parseLong(value), 24L * 30L));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String credentialKey(SearchMarketingSourceDO source) {
        Map<String, Object> metadata = map(source.getMetadataJson());
        Object value = metadata.get("credentialKey");
        if (value == null && metadata.get("credentials") instanceof Map<?, ?> nested) {
            value = nested.get("credentialKey");
        }
        return trimToNull(value == null ? null : String.valueOf(value));
    }

    private Object firstPresent(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        return null;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
