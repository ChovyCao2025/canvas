package org.chovy.canvas.domain.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSyncRunDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingUrlInspectionDO;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSyncRunMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingUrlInspectionMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class SearchMarketingSyncRunService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final Set<String> TERMINAL_STATUSES = Set.of("SUCCEEDED", "PARTIAL", "FAILED");

    private final SearchMarketingSourceMapper sourceMapper;
    private final SearchMarketingSyncRunMapper syncRunMapper;
    private final SearchMarketingUrlInspectionMapper urlInspectionMapper;
    private final SearchMarketingService searchMarketingService;
    private final SearchMarketingCredentialResolver credentialResolver;
    private final SearchMarketingProviderReadGateway readGateway;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public SearchMarketingSyncRunService(SearchMarketingSourceMapper sourceMapper,
                                         SearchMarketingSyncRunMapper syncRunMapper,
                                         SearchMarketingUrlInspectionMapper urlInspectionMapper,
                                         SearchMarketingService searchMarketingService,
                                         SearchMarketingCredentialResolver credentialResolver,
                                         SearchMarketingProviderReadGateway readGateway,
                                         ObjectMapper objectMapper) {
        this(sourceMapper, syncRunMapper, urlInspectionMapper, searchMarketingService, credentialResolver,
                readGateway, objectMapper, Clock.systemDefaultZone());
    }

    SearchMarketingSyncRunService(SearchMarketingSourceMapper sourceMapper,
                                  SearchMarketingSyncRunMapper syncRunMapper,
                                  SearchMarketingUrlInspectionMapper urlInspectionMapper,
                                  SearchMarketingService searchMarketingService,
                                  SearchMarketingCredentialResolver credentialResolver,
                                  SearchMarketingProviderReadGateway readGateway,
                                  ObjectMapper objectMapper,
                                  Clock clock) {
        this.sourceMapper = sourceMapper;
        this.syncRunMapper = syncRunMapper;
        this.urlInspectionMapper = urlInspectionMapper;
        this.searchMarketingService = searchMarketingService;
        this.credentialResolver = credentialResolver;
        this.readGateway = readGateway == null ? SearchMarketingProviderReadGateway.unsupported() : readGateway;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public SearchMarketingSyncRunView runManual(Long tenantId,
                                                Long sourceId,
                                                String runType,
                                                LocalDate windowStart,
                                                LocalDate windowEnd,
                                                String cursorValue,
                                                String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        SearchMarketingSourceDO source = source(scopedTenantId, sourceId);
        String normalizedRunType = normalizeUpper(runType, "runType");
        String idempotencyKey = idempotencyKey(scopedTenantId, source.getId(), normalizedRunType,
                windowStart, windowEnd, cursorValue);
        SearchMarketingSyncRunDO existing = syncRunMapper.selectOne(new LambdaQueryWrapper<SearchMarketingSyncRunDO>()
                .eq(SearchMarketingSyncRunDO::getTenantId, scopedTenantId)
                .eq(SearchMarketingSyncRunDO::getSourceId, source.getId())
                .eq(SearchMarketingSyncRunDO::getRunType, normalizedRunType)
                .eq(SearchMarketingSyncRunDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
        if (existing != null && scopedTenantId.equals(existing.getTenantId())
                && TERMINAL_STATUSES.contains(normalize(existing.getStatus()))) {
            return toView(existing);
        }

        LocalDateTime changedAt = now();
        SearchMarketingSyncRunDO run = new SearchMarketingSyncRunDO();
        run.setTenantId(scopedTenantId);
        run.setSourceId(source.getId());
        run.setRunType(normalizedRunType);
        run.setProvider(normalizeUpper(source.getProvider(), "provider"));
        run.setChannel(normalizeUpper(source.getChannel(), "channel"));
        run.setIdempotencyKey(idempotencyKey);
        run.setWindowStart(windowStart);
        run.setWindowEnd(windowEnd);
        run.setCursorValue(trimToNull(cursorValue));
        run.setStatus("RUNNING");
        run.setRetryable(0);
        run.setRequestedCount(0L);
        run.setSuccessCount(0L);
        run.setFailedCount(0L);
        run.setEvidenceJson(json(Map.of()));
        run.setCreatedBy(defaultString(actor, "system"));
        run.setStartedAt(changedAt);
        run.setUpdatedAt(changedAt);
        syncRunMapper.insert(run);

        SearchMarketingProviderSyncResult result;
        try {
            SearchMarketingCredentialRef credential = credentialResolver.resolve(scopedTenantId, source.getProvider(),
                    credentialKey(source.getMetadataJson()));
            SearchMarketingSyncCommand command = new SearchMarketingSyncCommand(
                    scopedTenantId,
                    source.getId(),
                    source.getProvider(),
                    source.getExternalAccountId(),
                    normalizedRunType,
                    windowStart,
                    windowEnd,
                    trimToNull(cursorValue),
                    providerMetadata(source));
            result = readGateway.sync(command, credential);
            long successCount = persistResultRows(scopedTenantId, source, result, run.getCreatedBy());
            finishRun(run, result, successCount);
        } catch (RuntimeException ex) {
            finishRun(run, SearchMarketingProviderSyncResult.failure(
                    "SEARCH_SYNC_RUN_FAILED",
                    ex.getMessage(),
                    false,
                    Map.of("exception", ex.getClass().getSimpleName())), 0L);
        }
        return toView(run);
    }

    public List<SearchMarketingSyncRunView> runDue(Long tenantId, int limit, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int normalizedLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 100));
        List<SearchMarketingSyncRunView> runs = new ArrayList<>();
        List<SearchMarketingSourceDO> candidates = safeList(sourceMapper.selectList(
                new LambdaQueryWrapper<SearchMarketingSourceDO>()
                        .eq(SearchMarketingSourceDO::getTenantId, scopedTenantId)
                        .eq(SearchMarketingSourceDO::getEnabled, 1)
                        .orderByAsc(SearchMarketingSourceDO::getId)
                        .last("LIMIT " + normalizedLimit)));
        for (SearchMarketingSourceDO source : candidates) {
            if (runs.size() >= normalizedLimit) {
                break;
            }
            if (source == null || !scopedTenantId.equals(source.getTenantId())
                    || !Integer.valueOf(1).equals(source.getEnabled())) {
                continue;
            }
            Map<String, Object> metadata = map(source.getMetadataJson());
            for (String runType : configuredRunTypes(metadata)) {
                if (runs.size() >= normalizedLimit) {
                    break;
                }
                if (!isDue(scopedTenantId, source, runType, metadata)) {
                    continue;
                }
                LocalDate windowEnd = dueWindowEnd(metadata);
                LocalDate windowStart = dueWindowStart(metadata, windowEnd);
                runs.add(runManual(scopedTenantId, source.getId(), runType, windowStart, windowEnd,
                        dueCursor(metadata), actor));
            }
        }
        return List.copyOf(runs);
    }

    public List<SearchMarketingSyncRunView> list(Long tenantId, SearchMarketingSyncRunQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long sourceId = query == null ? null : query.sourceId();
        String runType = query == null ? null : normalizeOptionalUpper(query.runType());
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        int limit = normalizeLimit(query == null ? null : query.limit());
        return safeList(syncRunMapper.selectList(new LambdaQueryWrapper<SearchMarketingSyncRunDO>()
                .eq(SearchMarketingSyncRunDO::getTenantId, scopedTenantId)
                .eq(sourceId != null, SearchMarketingSyncRunDO::getSourceId, sourceId)
                .eq(runType != null, SearchMarketingSyncRunDO::getRunType, runType)
                .eq(status != null, SearchMarketingSyncRunDO::getStatus, status)
                .orderByDesc(SearchMarketingSyncRunDO::getUpdatedAt)
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> runType == null || runType.equals(row.getRunType()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    public List<SearchMarketingUrlInspectionView> listUrlInspections(Long tenantId,
                                                                     SearchMarketingUrlInspectionQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long sourceId = query == null ? null : query.sourceId();
        String indexedState = query == null ? null : normalizeOptionalUpper(query.indexedState());
        LocalDate startDate = query == null ? null : query.startDate();
        LocalDate endDate = query == null ? null : query.endDate();
        int limit = normalizeLimit(query == null ? null : query.limit());
        return safeList(urlInspectionMapper.selectList(new LambdaQueryWrapper<SearchMarketingUrlInspectionDO>()
                .eq(SearchMarketingUrlInspectionDO::getTenantId, scopedTenantId)
                .eq(sourceId != null, SearchMarketingUrlInspectionDO::getSourceId, sourceId)
                .eq(indexedState != null, SearchMarketingUrlInspectionDO::getIndexedState, indexedState)
                .ge(startDate != null, SearchMarketingUrlInspectionDO::getInspectionDate, startDate)
                .le(endDate != null, SearchMarketingUrlInspectionDO::getInspectionDate, endDate)
                .orderByDesc(SearchMarketingUrlInspectionDO::getInspectionDate)
                .orderByDesc(SearchMarketingUrlInspectionDO::getUpdatedAt)
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> indexedState == null || indexedState.equals(row.getIndexedState()))
                .filter(row -> startDate == null
                        || (row.getInspectionDate() != null && !row.getInspectionDate().isBefore(startDate)))
                .filter(row -> endDate == null
                        || (row.getInspectionDate() != null && !row.getInspectionDate().isAfter(endDate)))
                .limit(limit)
                .map(this::toUrlInspectionView)
                .toList();
    }

    public SearchMarketingReadinessView readiness(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        long enabledSourceCount = safeList(sourceMapper.selectList(new LambdaQueryWrapper<SearchMarketingSourceDO>()
                .eq(SearchMarketingSourceDO::getTenantId, scopedTenantId)
                .eq(SearchMarketingSourceDO::getEnabled, 1))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> Integer.valueOf(1).equals(row.getEnabled()))
                .count();
        long failedSyncRunCount = safeList(syncRunMapper.selectList(new LambdaQueryWrapper<SearchMarketingSyncRunDO>()
                .eq(SearchMarketingSyncRunDO::getTenantId, scopedTenantId)
                .eq(SearchMarketingSyncRunDO::getStatus, "FAILED")
                .orderByDesc(SearchMarketingSyncRunDO::getUpdatedAt)
                .last("LIMIT 100"))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> "FAILED".equals(row.getStatus()))
                .count();
        List<String> blockers = new ArrayList<>();
        if (enabledSourceCount == 0) {
            blockers.add("no enabled search marketing source");
        }
        if (failedSyncRunCount > 0) {
            blockers.add("failed sync runs require operator attention");
        }
        String status = enabledSourceCount == 0 ? "NOT_READY" : failedSyncRunCount > 0 ? "DEGRADED" : "LIVE";
        return new SearchMarketingReadinessView(
                scopedTenantId,
                status,
                blockers,
                Map.of(
                        "enabledSourceCount", enabledSourceCount,
                        "failedSyncRunCount", failedSyncRunCount,
                        "hasEnabledSource", enabledSourceCount > 0,
                        "hasFailedSyncRuns", failedSyncRunCount > 0),
                now());
    }

    private long persistResultRows(Long tenantId,
                                   SearchMarketingSourceDO source,
                                   SearchMarketingProviderSyncResult result,
                                   String actor) {
        if (result == null || !result.success()) {
            return 0L;
        }
        long successCount = 0L;
        for (SearchMarketingPerformanceRow row : safeList(result.performanceRows())) {
            SearchMarketingKeywordView keyword = searchMarketingService.upsertKeyword(tenantId,
                    new SearchMarketingKeywordCommand(
                            source.getChannel(),
                            row.keywordText(),
                            row.matchType(),
                            row.landingPageUrl(),
                            null,
                            List.of(),
                            "ACTIVE",
                            row.metadata()),
                    actor);
            searchMarketingService.recordSnapshot(tenantId,
                    new SearchMarketingSnapshotCommand(
                            source.getId(),
                            keyword.id(),
                            row.snapshotDate(),
                            row.device(),
                            row.country(),
                            row.queryGroupKey(),
                            row.impressionCount(),
                            row.clickCount(),
                            row.costAmount(),
                            row.conversionCount(),
                            row.revenueAmount(),
                            row.averagePosition(),
                            row.metadata()),
                    actor);
            successCount++;
        }
        for (SearchMarketingUrlInspectionRow row : safeList(result.urlInspectionRows())) {
            upsertUrlInspection(tenantId, source, row, actor);
            successCount++;
        }
        return successCount;
    }

    private void upsertUrlInspection(Long tenantId,
                                     SearchMarketingSourceDO source,
                                     SearchMarketingUrlInspectionRow evidence,
                                     String actor) {
        String pageUrl = required(evidence.pageUrl(), "pageUrl");
        LocalDate inspectionDate = evidence.inspectionDate() == null ? LocalDate.now(clock) : evidence.inspectionDate();
        String pageUrlHash = sha256(pageUrl);
        LocalDateTime changedAt = now();
        SearchMarketingUrlInspectionDO row = urlInspectionMapper.selectOne(
                new LambdaQueryWrapper<SearchMarketingUrlInspectionDO>()
                        .eq(SearchMarketingUrlInspectionDO::getTenantId, tenantId)
                        .eq(SearchMarketingUrlInspectionDO::getSourceId, source.getId())
                        .eq(SearchMarketingUrlInspectionDO::getPageUrlHash, pageUrlHash)
                        .eq(SearchMarketingUrlInspectionDO::getInspectionDate, inspectionDate)
                        .eq(SearchMarketingUrlInspectionDO::getProvider, source.getProvider())
                        .last("LIMIT 1"));
        if (row == null) {
            row = new SearchMarketingUrlInspectionDO();
            row.setTenantId(tenantId);
            row.setSourceId(source.getId());
            row.setProvider(normalizeUpper(source.getProvider(), "provider"));
            row.setPageUrl(pageUrl);
            row.setPageUrlHash(pageUrlHash);
            row.setInspectionDate(inspectionDate);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setIndexedState(normalizeDefault(evidence.indexedState(), "UNKNOWN"));
        row.setCrawlState(normalizeDefault(evidence.crawlState(), "UNKNOWN"));
        row.setCanonicalUrl(trimToNull(evidence.canonicalUrl()));
        row.setSitemapState(normalizeDefault(evidence.sitemapState(), "UNKNOWN"));
        row.setMobileUsabilityState(normalizeDefault(evidence.mobileUsabilityState(), "UNKNOWN"));
        row.setLastCrawlAt(evidence.lastCrawlAt());
        row.setEvidenceJson(json(ProviderWriteEvidenceSanitizer.sanitizeMap(evidence.evidence())));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            urlInspectionMapper.insert(row);
        } else {
            urlInspectionMapper.updateById(row);
        }
    }

    private void finishRun(SearchMarketingSyncRunDO run,
                           SearchMarketingProviderSyncResult result,
                           long successCount) {
        SearchMarketingProviderSyncResult effectiveResult = result == null
                ? SearchMarketingProviderSyncResult.failure(
                "SEARCH_SYNC_EMPTY_RESULT", "search provider returned no sync result", false, Map.of())
                : result;
        long requestedCount = effectiveResult.requestedCount() > 0
                ? effectiveResult.requestedCount()
                : successCount;
        long failedCount = effectiveResult.success()
                ? Math.max(0L, requestedCount - successCount)
                : Math.max(1L, requestedCount - successCount);
        run.setStatus(status(effectiveResult.success(), failedCount));
        run.setRetryable(effectiveResult.retryable() ? 1 : 0);
        run.setRequestedCount(requestedCount);
        run.setSuccessCount(successCount);
        run.setFailedCount(failedCount);
        run.setProviderRequestId(trimToNull(effectiveResult.providerRequestId()));
        run.setErrorCode(trimToNull(effectiveResult.errorCode()));
        run.setErrorMessage(trimToNull(effectiveResult.errorMessage()));
        run.setEvidenceJson(json(ProviderWriteEvidenceSanitizer.sanitizeMap(effectiveResult.evidence())));
        run.setFinishedAt(now());
        run.setUpdatedAt(now());
        syncRunMapper.updateById(run);
    }

    private String status(boolean providerSuccess, long failedCount) {
        if (!providerSuccess) {
            return "FAILED";
        }
        return failedCount > 0 ? "PARTIAL" : "SUCCEEDED";
    }

    private SearchMarketingSourceDO source(Long tenantId, Long sourceId) {
        SearchMarketingSourceDO source = sourceMapper.selectById(requiredId(sourceId, "sourceId"));
        if (source == null || !tenantId.equals(source.getTenantId())) {
            throw new IllegalArgumentException("source does not belong to current tenant");
        }
        if (!Integer.valueOf(1).equals(source.getEnabled())) {
            throw new IllegalArgumentException("search marketing source is disabled");
        }
        return source;
    }

    private Map<String, Object> providerMetadata(SearchMarketingSourceDO source) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "sourceKey", source.getSourceKey());
        putIfPresent(metadata, "siteUrl", source.getSiteUrl());
        putIfPresent(metadata, "timezone", source.getTimezone());
        Map<String, Object> sourceMetadata = map(source.getMetadataJson());
        if (!sourceMetadata.isEmpty()) {
            metadata.put("sourceMetadata", sourceMetadata);
        }
        return ProviderWriteEvidenceSanitizer.sanitizeMap(metadata);
    }

    private void putIfPresent(Map<String, Object> metadata, String key, String value) {
        String trimmed = trimToNull(value);
        if (trimmed != null) {
            metadata.put(key, trimmed);
        }
    }

    private List<String> configuredRunTypes(Map<String, Object> metadata) {
        Object raw = firstPresent(metadata, "syncRunTypes", "runTypes", "run_types");
        List<String> runTypes = new ArrayList<>();
        if (raw instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String runType = trimToNull(item == null ? null : String.valueOf(item));
                if (runType != null) {
                    runTypes.add(normalizeUpper(runType, "runType"));
                }
            }
        } else {
            String value = trimToNull(raw == null ? null : String.valueOf(raw));
            if (value != null) {
                for (String item : value.split(",")) {
                    String runType = trimToNull(item);
                    if (runType != null) {
                        runTypes.add(normalizeUpper(runType, "runType"));
                    }
                }
            }
        }
        return runTypes.isEmpty() ? List.of("PERFORMANCE") : List.copyOf(runTypes);
    }

    private LocalDate dueWindowEnd(Map<String, Object> metadata) {
        Object raw = firstPresent(metadata, "syncWindowEnd", "windowEnd");
        String value = trimToNull(raw == null ? null : String.valueOf(raw));
        return value == null ? LocalDate.now(clock).minusDays(1) : LocalDate.parse(value);
    }

    private LocalDate dueWindowStart(Map<String, Object> metadata, LocalDate windowEnd) {
        Object rawStart = firstPresent(metadata, "syncWindowStart", "windowStart");
        String start = trimToNull(rawStart == null ? null : String.valueOf(rawStart));
        if (start != null) {
            return LocalDate.parse(start);
        }
        int days = positiveInt(firstPresent(metadata, "syncWindowDays", "windowDays"), 1);
        return windowEnd.minusDays(days - 1L);
    }

    private String dueCursor(Map<String, Object> metadata) {
        Object raw = firstPresent(metadata, "syncCursor", "cursorValue", "cursor");
        return raw == null ? null : trimToNull(String.valueOf(raw));
    }

    private boolean isDue(Long tenantId,
                          SearchMarketingSourceDO source,
                          String runType,
                          Map<String, Object> metadata) {
        SearchMarketingSyncRunDO latest = syncRunMapper.selectOne(new LambdaQueryWrapper<SearchMarketingSyncRunDO>()
                .eq(SearchMarketingSyncRunDO::getTenantId, tenantId)
                .eq(SearchMarketingSyncRunDO::getSourceId, source.getId())
                .eq(SearchMarketingSyncRunDO::getRunType, runType)
                .eq(SearchMarketingSyncRunDO::getStatus, "SUCCEEDED")
                .orderByDesc(SearchMarketingSyncRunDO::getFinishedAt)
                .last("LIMIT 1"));
        if (latest == null || !tenantId.equals(latest.getTenantId()) || latest.getFinishedAt() == null) {
            return true;
        }
        return latest.getFinishedAt().isBefore(now().minusHours(freshnessHours(metadata)));
    }

    private long freshnessHours(Map<String, Object> metadata) {
        Object raw = firstPresent(metadata, "freshnessHours", "syncFreshnessHours");
        if (raw instanceof Number number) {
            return Math.max(1L, Math.min(number.longValue(), 24L * 30L));
        }
        String value = trimToNull(raw == null ? null : String.valueOf(raw));
        if (value == null) {
            return 24L;
        }
        try {
            return Math.max(1L, Math.min(Long.parseLong(value), 24L * 30L));
        } catch (NumberFormatException ex) {
            return 24L;
        }
    }

    private Object firstPresent(Map<String, Object> metadata, String... keys) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            if (metadata.containsKey(key)) {
                return metadata.get(key);
            }
        }
        return null;
    }

    private int positiveInt(Object raw, int fallback) {
        if (raw instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        String value = trimToNull(raw == null ? null : String.valueOf(raw));
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String credentialKey(String metadataJson) {
        Map<String, Object> metadata = map(metadataJson);
        Object direct = metadata.get("credentialKey");
        if (direct == null && metadata.get("credentials") instanceof Map<?, ?> nested) {
            direct = nested.get("credentialKey");
        }
        return direct == null ? null : trimToNull(String.valueOf(direct));
    }

    private SearchMarketingSyncRunView toView(SearchMarketingSyncRunDO row) {
        return new SearchMarketingSyncRunView(
                row.getId(),
                row.getTenantId(),
                row.getSourceId(),
                row.getRunType(),
                row.getProvider(),
                row.getChannel(),
                row.getIdempotencyKey(),
                row.getWindowStart(),
                row.getWindowEnd(),
                row.getCursorValue(),
                row.getStatus(),
                Integer.valueOf(1).equals(row.getRetryable()),
                nonNegative(row.getRequestedCount()),
                nonNegative(row.getSuccessCount()),
                nonNegative(row.getFailedCount()),
                row.getProviderRequestId(),
                row.getErrorCode(),
                row.getErrorMessage(),
                map(row.getEvidenceJson()),
                row.getCreatedBy(),
                row.getStartedAt(),
                row.getFinishedAt(),
                row.getUpdatedAt());
    }

    private SearchMarketingUrlInspectionView toUrlInspectionView(SearchMarketingUrlInspectionDO row) {
        return new SearchMarketingUrlInspectionView(
                row.getId(),
                row.getTenantId(),
                row.getSourceId(),
                row.getProvider(),
                row.getPageUrl(),
                row.getPageUrlHash(),
                row.getInspectionDate(),
                row.getIndexedState(),
                row.getCrawlState(),
                row.getCanonicalUrl(),
                row.getSitemapState(),
                row.getMobileUsabilityState(),
                row.getLastCrawlAt(),
                map(row.getEvidenceJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
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

    private String required(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed;
    }

    private String normalizeUpper(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    private String normalizeDefault(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private int normalizeLimit(Integer value) {
        return Math.min(Math.max(value == null ? 50 : value, 1), 100);
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

    private long nonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("search marketing sync evidence is not JSON serializable", ex);
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

    private String idempotencyKey(Long tenantId,
                                  Long sourceId,
                                  String runType,
                                  LocalDate windowStart,
                                  LocalDate windowEnd,
                                  String cursorValue) {
        return sha256(tenantId + "|" + sourceId + "|" + runType + "|"
                + Objects.toString(windowStart, "") + "|" + Objects.toString(windowEnd, "") + "|"
                + defaultString(cursorValue, ""));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash search marketing sync identity", ex);
        }
    }
}
