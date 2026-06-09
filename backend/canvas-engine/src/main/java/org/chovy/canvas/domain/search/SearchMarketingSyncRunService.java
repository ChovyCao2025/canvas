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

/**
 * SearchMarketingSyncRunService 编排 domain.search 场景的领域业务规则。
 */
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

    /**
     * 创建 SearchMarketingSyncRunService 实例并注入 domain.search 场景依赖。
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param syncRunMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param urlInspectionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param searchMarketingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param credentialResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param readGateway read gateway 参数，用于 SearchMarketingSyncRunService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 查询或读取业务数据。
     *
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param syncRunMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param urlInspectionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param searchMarketingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param credentialResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param readGateway read gateway 参数，用于 SearchMarketingSyncRunService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 执行后台业务任务，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param sourceId 目标业务记录 ID，需与租户边界匹配
     * @param runType 类型标识，用于选择对应处理分支。
     * @param windowStart window start 参数，用于 runManual 流程中的校验、计算或对象转换。
     * @param windowEnd window end 参数，用于 runManual 流程中的校验、计算或对象转换。
     * @param cursorValue 待处理业务值，用于规则计算、转换或外部调用。
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            finishRun(run, SearchMarketingProviderSyncResult.failure(
                    "SEARCH_SYNC_RUN_FAILED",
                    ex.getMessage(),
                    false,
                    Map.of("exception", ex.getClass().getSimpleName())), 0L);
        }
        return toView(run);
    }

    /**
     * 执行后台业务任务，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingSyncRunView> runDue(Long tenantId, int limit, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int normalizedLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 100));
        List<SearchMarketingSyncRunView> runs = new ArrayList<>();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<SearchMarketingSourceDO> candidates = safeList(sourceMapper.selectList(
                new LambdaQueryWrapper<SearchMarketingSourceDO>()
                        .eq(SearchMarketingSourceDO::getTenantId, scopedTenantId)
                        .eq(SearchMarketingSourceDO::getEnabled, 1)
                        .orderByAsc(SearchMarketingSourceDO::getId)
                        .last("LIMIT " + normalizedLimit)));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (SearchMarketingSourceDO source : candidates) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

    /**
     * 查询业务列表，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 list 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingSyncRunView> list(Long tenantId, SearchMarketingSyncRunQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long sourceId = query == null ? null : query.sourceId();
        String runType = query == null ? null : normalizeOptionalUpper(query.runType());
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        int limit = normalizeLimit(query == null ? null : query.limit());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(syncRunMapper.selectList(new LambdaQueryWrapper<SearchMarketingSyncRunDO>()
                .eq(SearchMarketingSyncRunDO::getTenantId, scopedTenantId)
                .eq(sourceId != null, SearchMarketingSyncRunDO::getSourceId, sourceId)
                .eq(runType != null, SearchMarketingSyncRunDO::getRunType, runType)
                .eq(status != null, SearchMarketingSyncRunDO::getStatus, status)
                .orderByDesc(SearchMarketingSyncRunDO::getUpdatedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> runType == null || runType.equals(row.getRunType()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    /**
     * 查询业务列表，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 listUrlInspections 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingUrlInspectionView> listUrlInspections(Long tenantId,
                                                                     SearchMarketingUrlInspectionQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long sourceId = query == null ? null : query.sourceId();
        String indexedState = query == null ? null : normalizeOptionalUpper(query.indexedState());
        LocalDate startDate = query == null ? null : query.startDate();
        LocalDate endDate = query == null ? null : query.endDate();
        int limit = normalizeLimit(query == null ? null : query.limit());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(urlInspectionMapper.selectList(new LambdaQueryWrapper<SearchMarketingUrlInspectionDO>()
                .eq(SearchMarketingUrlInspectionDO::getTenantId, scopedTenantId)
                .eq(sourceId != null, SearchMarketingUrlInspectionDO::getSourceId, sourceId)
                .eq(indexedState != null, SearchMarketingUrlInspectionDO::getIndexedState, indexedState)
                .ge(startDate != null, SearchMarketingUrlInspectionDO::getInspectionDate, startDate)
                .le(endDate != null, SearchMarketingUrlInspectionDO::getInspectionDate, endDate)
                .orderByDesc(SearchMarketingUrlInspectionDO::getInspectionDate)
                .orderByDesc(SearchMarketingUrlInspectionDO::getUpdatedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 执行业务操作 readiness，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingReadinessView readiness(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        long enabledSourceCount = safeList(sourceMapper.selectList(new LambdaQueryWrapper<SearchMarketingSourceDO>()
                .eq(SearchMarketingSourceDO::getTenantId, scopedTenantId)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param source source 参数，用于 persistResultRows 流程中的校验、计算或对象转换。
     * @param result result 参数，用于 persistResultRows 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 persist result rows 计算得到的数量、金额或指标值。
     */
    private long persistResultRows(Long tenantId,
                                   SearchMarketingSourceDO source,
                                   SearchMarketingProviderSyncResult result,
                                   String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (result == null || !result.success()) {
            return 0L;
        }
        long successCount = 0L;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return successCount;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param source source 参数，用于 upsertUrlInspection 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 upsertUrlInspection 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void upsertUrlInspection(Long tenantId,
                                     SearchMarketingSourceDO source,
                                     SearchMarketingUrlInspectionRow evidence,
                                     String actor) {
        String pageUrl = required(evidence.pageUrl(), "pageUrl");
        LocalDate inspectionDate = evidence.inspectionDate() == null ? LocalDate.now(clock) : evidence.inspectionDate();
        String pageUrlHash = sha256(pageUrl);
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SearchMarketingUrlInspectionDO row = urlInspectionMapper.selectOne(
                new LambdaQueryWrapper<SearchMarketingUrlInspectionDO>()
                        .eq(SearchMarketingUrlInspectionDO::getTenantId, tenantId)
                        .eq(SearchMarketingUrlInspectionDO::getSourceId, source.getId())
                        .eq(SearchMarketingUrlInspectionDO::getPageUrlHash, pageUrlHash)
                        .eq(SearchMarketingUrlInspectionDO::getInspectionDate, inspectionDate)
                        .eq(SearchMarketingUrlInspectionDO::getProvider, source.getProvider())
                        .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

    /**
     * 执行 finishRun 流程，围绕 finish run 完成校验、计算或结果组装。
     *
     * @param run run 参数，用于 finishRun 流程中的校验、计算或对象转换。
     * @param result result 参数，用于 finishRun 流程中的校验、计算或对象转换。
     * @param successCount success count 参数，用于 finishRun 流程中的校验、计算或对象转换。
     */
    private void finishRun(SearchMarketingSyncRunDO run,
                           SearchMarketingProviderSyncResult result,
                           long successCount) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        run.setUpdatedAt(now());
        syncRunMapper.updateById(run);
    }

    /**
     * 执行 status 流程，围绕 status 完成校验、计算或结果组装。
     *
     * @param providerSuccess provider success 参数，用于 status 流程中的校验、计算或对象转换。
     * @param failedCount failed count 参数，用于 status 流程中的校验、计算或对象转换。
     * @return 返回 status 生成的文本或业务键。
     */
    private String status(boolean providerSuccess, long failedCount) {
        if (!providerSuccess) {
            return "FAILED";
        }
        return failedCount > 0 ? "PARTIAL" : "SUCCEEDED";
    }

    /**
     * 执行 source 流程，围绕 source 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @return 返回 source 流程生成的业务结果。
     */
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

    /**
     * 执行 providerMetadata 流程，围绕 provider metadata 完成校验、计算或结果组装。
     *
     * @param source source 参数，用于 providerMetadata 流程中的校验、计算或对象转换。
     * @return 返回 providerMetadata 流程生成的业务结果。
     */
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

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     */
    private void putIfPresent(Map<String, Object> metadata, String key, String value) {
        String trimmed = trimToNull(value);
        if (trimmed != null) {
            metadata.put(key, trimmed);
        }
    }

    /**
     * 执行 configuredRunTypes 流程，围绕 configured run types 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 configuredRunTypes 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 configuredRunTypes 流程中的校验、计算或对象转换。
     * @return 返回 configured run types 汇总后的集合、分页或映射视图。
     */
    private List<String> configuredRunTypes(Map<String, Object> metadata) {
        Object raw = firstPresent(metadata, "syncRunTypes", "runTypes", "run_types");
        List<String> runTypes = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (raw instanceof Iterable<?> iterable) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return runTypes.isEmpty() ? List.of("PERFORMANCE") : List.copyOf(runTypes);
    }

    /**
     * 执行 dueWindowEnd 流程，围绕 due window end 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 dueWindowEnd 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 dueWindowEnd 流程中的校验、计算或对象转换。
     * @return 返回 dueWindowEnd 流程生成的业务结果。
     */
    private LocalDate dueWindowEnd(Map<String, Object> metadata) {
        Object raw = firstPresent(metadata, "syncWindowEnd", "windowEnd");
        String value = trimToNull(raw == null ? null : String.valueOf(raw));
        return value == null ? LocalDate.now(clock).minusDays(1) : LocalDate.parse(value);
    }

    /**
     * 执行 dueWindowStart 流程，围绕 due window start 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 dueWindowStart 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 dueWindowStart 流程中的校验、计算或对象转换。
     * @param windowEnd window end 参数，用于 dueWindowStart 流程中的校验、计算或对象转换。
     * @return 返回 dueWindowStart 流程生成的业务结果。
     */
    private LocalDate dueWindowStart(Map<String, Object> metadata, LocalDate windowEnd) {
        Object rawStart = firstPresent(metadata, "syncWindowStart", "windowStart");
        String start = trimToNull(rawStart == null ? null : String.valueOf(rawStart));
        if (start != null) {
            return LocalDate.parse(start);
        }
        int days = positiveInt(firstPresent(metadata, "syncWindowDays", "windowDays"), 1);
        return windowEnd.minusDays(days - 1L);
    }

    /**
     * 执行 dueCursor 流程，围绕 due cursor 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 dueCursor 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 dueCursor 流程中的校验、计算或对象转换。
     * @return 返回 due cursor 生成的文本或业务键。
     */
    private String dueCursor(Map<String, Object> metadata) {
        Object raw = firstPresent(metadata, "syncCursor", "cursorValue", "cursor");
        return raw == null ? null : trimToNull(String.valueOf(raw));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param source source 参数，用于 isDue 流程中的校验、计算或对象转换。
     * @param runType 类型标识，用于选择对应处理分支。
     * @param metadata metadata 参数，用于 isDue 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 执行 freshnessHours 流程，围绕 freshness hours 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 freshnessHours 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 freshnessHours 流程中的校验、计算或对象转换。
     * @return 返回 freshness hours 计算得到的数量、金额或指标值。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ex) {
            return 24L;
        }
    }

    /**
     * 执行 firstPresent 流程，围绕 first present 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 firstPresent 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 firstPresent 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstPresent 流程中的校验、计算或对象转换。
     * @return 返回 firstPresent 流程生成的业务结果。
     */
    private Object firstPresent(Map<String, Object> metadata, String... keys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String key : keys) {
            if (metadata.containsKey(key)) {
                return metadata.get(key);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 执行 positiveInt 流程，围绕 positive int 完成校验、计算或结果组装。
     *
     * @param raw raw 参数，用于 positiveInt 流程中的校验、计算或对象转换。
     * @param fallback fallback 参数，用于 positiveInt 流程中的校验、计算或对象转换。
     * @return 返回 positive int 计算得到的数量、金额或指标值。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * 执行 credentialKey 流程，围绕 credential key 完成校验、计算或结果组装。
     *
     * @param metadataJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 credential key 生成的文本或业务键。
     */
    private String credentialKey(String metadataJson) {
        Map<String, Object> metadata = map(metadataJson);
        Object direct = metadata.get("credentialKey");
        if (direct == null && metadata.get("credentials") instanceof Map<?, ?> nested) {
            direct = nested.get("credentialKey");
        }
        return direct == null ? null : trimToNull(String.valueOf(direct));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SearchMarketingSyncRunView toView(SearchMarketingSyncRunDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SearchMarketingUrlInspectionView toUrlInspectionView(SearchMarketingUrlInspectionDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeUpper(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeDefault 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeDefault(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizeLimit(Integer value) {
        return Math.min(Math.max(value == null ? 50 : value, 1), 100);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 执行 nonNegative 流程，围绕 non negative 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 non negative 计算得到的数量、金额或指标值。
     */
    private long nonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("search marketing sync evidence is not JSON serializable", ex);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(json, OBJECT_MAP);
            return values == null ? Map.of() : values;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 执行 idempotencyKey 流程，围绕 idempotency key 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param runType 类型标识，用于选择对应处理分支。
     * @param windowStart window start 参数，用于 idempotencyKey 流程中的校验、计算或对象转换。
     * @param windowEnd window end 参数，用于 idempotencyKey 流程中的校验、计算或对象转换。
     * @param cursorValue 待处理值，用于规则计算或转换。
     * @return 返回 idempotency key 生成的文本或业务键。
     */
    private String idempotencyKey(Long tenantId,
                                  Long sourceId,
                                  String runType,
                                  LocalDate windowStart,
                                  LocalDate windowEnd,
                                  String cursorValue) {
        return sha256(tenantId + "|" + sourceId + "|" + runType + "|"
                + Objects.toString(windowStart, "") + "|" + Objects.toString(windowEnd, "") + "|"
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @param cursorValue 待处理值，用于规则计算或转换。
                 * @return 返回 defaultString 流程生成的业务结果。
                 */
                + defaultString(cursorValue, ""));
    }

    /**
     * 执行 sha256 流程，围绕 sha256 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sha256 生成的文本或业务键。
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash search marketing sync identity", ex);
        }
    }
}
