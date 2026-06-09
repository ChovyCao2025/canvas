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

/**
 * SearchMarketingReadinessService 编排 domain.search 场景的领域业务规则。
 */
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

    /**
     * 创建 SearchMarketingReadinessService 实例并注入 domain.search 场景依赖。
     * @param syncRunService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 创建 SearchMarketingReadinessService 实例并注入 domain.search 场景依赖。
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param credentialMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param syncRunMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param impactWindowMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param writeGateway write gateway 参数，用于 SearchMarketingReadinessService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 查询或读取业务数据。
     *
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param credentialMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param syncRunMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param impactWindowMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param writeGateway write gateway 参数，用于 SearchMarketingReadinessService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 执行业务操作 readiness，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingReadinessView readiness(Long tenantId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new SearchMarketingReadinessView(
                scopedTenantId,
                status,
                blockers,
                ProviderWriteEvidenceSanitizer.sanitizeMap(evidence),
                evaluatedAt);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 enabled sources 汇总后的集合、分页或映射视图。
     */
    private List<SearchMarketingSourceDO> enabledSources(Long tenantId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(sourceMapper.selectList(new LambdaQueryWrapper<SearchMarketingSourceDO>()
                .eq(SearchMarketingSourceDO::getTenantId, tenantId)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .eq(SearchMarketingSourceDO::getEnabled, 1))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> Integer.valueOf(1).equals(row.getEnabled()))
                .toList();
    }

    /**
     * 执行 activeCredentials 流程，围绕 active credentials 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 active credentials 汇总后的集合、分页或映射视图。
     */
    private List<MarketingMonitorProviderCredentialDO> activeCredentials(Long tenantId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(credentialMapper.selectList(new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                .eq(MarketingMonitorProviderCredentialDO::getTenantId, tenantId)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .eq(MarketingMonitorProviderCredentialDO::getStatus, "ACTIVE"))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> "ACTIVE".equals(normalize(row.getStatus())))
                .toList();
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回流程执行后的业务结果。
     */
    private List<SearchMarketingSyncRunDO> syncRuns(Long tenantId) {
        return safeList(syncRunMapper.selectList(new LambdaQueryWrapper<SearchMarketingSyncRunDO>()
                .eq(SearchMarketingSyncRunDO::getTenantId, tenantId)
                .orderByDesc(SearchMarketingSyncRunDO::getFinishedAt)
                .last("LIMIT 1000"))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .toList();
    }

    /**
     * 执行 mutations 流程，围绕 mutations 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 mutations 汇总后的集合、分页或映射视图。
     */
    private List<SearchMarketingMutationDO> mutations(Long tenantId) {
        return safeList(mutationMapper.selectList(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                .eq(SearchMarketingMutationDO::getTenantId, tenantId)
                .in(SearchMarketingMutationDO::getStatus, "APPLIED", "FAILED", "RECONCILE_FAILED")
                .last("LIMIT 1000"))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .toList();
    }

    /**
     * 执行 impactWindows 流程，围绕 impact windows 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 impact windows 汇总后的集合、分页或映射视图。
     */
    private List<SearchMarketingImpactWindowDO> impactWindows(Long tenantId) {
        return safeList(impactWindowMapper.selectList(new LambdaQueryWrapper<SearchMarketingImpactWindowDO>()
                .eq(SearchMarketingImpactWindowDO::getTenantId, tenantId)
                .in(SearchMarketingImpactWindowDO::getStatus, "SCHEDULED", "DUE")
                .last("LIMIT 1000"))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .toList();
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param source source 参数，用于 verifyCredential 流程中的校验、计算或对象转换。
     * @param credentialsByProvider credentials by provider 参数，用于 verifyCredential 流程中的校验、计算或对象转换。
     * @param blockers blockers 参数，用于 verifyCredential 流程中的校验、计算或对象转换。
     */
    private void verifyCredential(SearchMarketingSourceDO source,
                                  Map<String, List<MarketingMonitorProviderCredentialDO>> credentialsByProvider,
                                  List<String> blockers) {
        String provider = normalize(source.getProvider());
        String credentialKey = credentialKey(source);
        List<MarketingMonitorProviderCredentialDO> candidates = credentialsByProvider.getOrDefault(provider, List.of());
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        MarketingMonitorProviderCredentialDO credential = candidates.stream()
                .filter(row -> credentialKey == null || credentialKey.equals(row.getCredentialKey()))
                .findFirst()
                .orElse(null);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (credential == null) {
            blockers.add("active credential is unavailable for " + provider + " source " + source.getId());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (credential.getExpiresAt() != null && !credential.getExpiresAt().isAfter(now())) {
            blockers.add("credential " + credential.getCredentialKey() + " is expired");
        }
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param source source 参数，用于 verifyFreshPerformanceSync 流程中的校验、计算或对象转换。
     * @param syncRuns sync runs 参数，用于 verifyFreshPerformanceSync 流程中的校验、计算或对象转换。
     * @param blockers blockers 参数，用于 verifyFreshPerformanceSync 流程中的校验、计算或对象转换。
     */
    private void verifyFreshPerformanceSync(SearchMarketingSourceDO source,
                                            List<SearchMarketingSyncRunDO> syncRuns,
                                            List<String> blockers) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        SearchMarketingSyncRunDO latestSuccess = syncRuns.stream()
                .filter(row -> Objects.equals(source.getId(), row.getSourceId()))
                .filter(row -> "PERFORMANCE".equals(normalize(row.getRunType())))
                .filter(row -> "SUCCEEDED".equals(normalize(row.getStatus())))
                .filter(row -> row.getFinishedAt() != null)
                .max(Comparator.comparing(SearchMarketingSyncRunDO::getFinishedAt))
                .orElse(null);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (latestSuccess == null) {
            blockers.add("latest successful PERFORMANCE sync is missing for source " + source.getId());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (latestSuccess.getFinishedAt().isBefore(now().minusHours(freshnessHours(source)))) {
            blockers.add("latest successful PERFORMANCE sync is stale for source " + source.getId());
        }
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param source source 参数，用于 verifyLatestBlockingFailure 流程中的校验、计算或对象转换。
     * @param syncRuns sync runs 参数，用于 verifyLatestBlockingFailure 流程中的校验、计算或对象转换。
     * @param blockers blockers 参数，用于 verifyLatestBlockingFailure 流程中的校验、计算或对象转换。
     */
    private void verifyLatestBlockingFailure(SearchMarketingSourceDO source,
                                             List<SearchMarketingSyncRunDO> syncRuns,
                                             List<String> blockers) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        syncRuns.stream()
                .filter(row -> Objects.equals(source.getId(), row.getSourceId()))
                .filter(row -> "FAILED".equals(normalize(row.getStatus())))
                .filter(row -> blockingError(row.getErrorCode()))
                .max(Comparator.comparing(row -> row.getFinishedAt() == null ? LocalDateTime.MIN : row.getFinishedAt()))
                .ifPresent(row -> blockers.add("blocking sync failure " + row.getErrorCode()
                        + " for source " + source.getId()));
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param errorCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 blocking error 的布尔判断结果。
     */
    private boolean blockingError(String errorCode) {
        String normalized = normalize(errorCode);
        return BLOCKING_SYNC_ERROR_FRAGMENTS.stream().anyMatch(normalized::contains);
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param sourcesById 业务对象 ID，用于定位具体记录。
     * @param mutations mutations 参数，用于 verifyUnreconciledWrites 流程中的校验、计算或对象转换。
     * @param blockers blockers 参数，用于 verifyUnreconciledWrites 流程中的校验、计算或对象转换。
     */
    private void verifyUnreconciledWrites(Map<Long, SearchMarketingSourceDO> sourcesById,
                                          List<SearchMarketingMutationDO> mutations,
                                          List<String> blockers) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (SearchMarketingMutationDO mutation : mutations) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param impactWindows impact windows 参数，用于 verifyImpactWindows 流程中的校验、计算或对象转换。
     * @param blockers blockers 参数，用于 verifyImpactWindows 流程中的校验、计算或对象转换。
     */
    private void verifyImpactWindows(List<SearchMarketingImpactWindowDO> impactWindows, List<String> blockers) {
        for (SearchMarketingImpactWindowDO window : impactWindows) {
            if (window.getDueAt() != null && !window.getDueAt().isAfter(now())) {
                blockers.add("impact window " + window.getId() + " is due but not evaluated");
            }
        }
    }

    /**
     * 执行 freshnessHours 流程，围绕 freshness hours 完成校验、计算或结果组装。
     *
     * @param source source 参数，用于 freshnessHours 流程中的校验、计算或对象转换。
     * @return 返回 freshness hours 计算得到的数量、金额或指标值。
     */
    private long freshnessHours(SearchMarketingSourceDO source) {
        return boundedHours(source, "freshnessHours", "syncFreshnessHours", 24L);
    }

    /**
     * 执行 reconciliationSlaHours 流程，围绕 reconciliation sla hours 完成校验、计算或结果组装。
     *
     * @param source source 参数，用于 reconciliationSlaHours 流程中的校验、计算或对象转换。
     * @return 返回 reconciliation sla hours 计算得到的数量、金额或指标值。
     */
    private long reconciliationSlaHours(SearchMarketingSourceDO source) {
        return boundedHours(source, "reconciliationSlaHours", "reconcileSlaHours", 24L);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param source source 参数，用于 boundedHours 流程中的校验、计算或对象转换。
     * @param primaryKey 业务键，用于在同一租户下定位资源。
     * @param secondaryKey 业务键，用于在同一租户下定位资源。
     * @param fallback fallback 参数，用于 boundedHours 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * 执行 credentialKey 流程，围绕 credential key 完成校验、计算或结果组装。
     *
     * @param source source 参数，用于 credentialKey 流程中的校验、计算或对象转换。
     * @return 返回 credential key 生成的文本或业务键。
     */
    private String credentialKey(SearchMarketingSourceDO source) {
        Map<String, Object> metadata = map(source.getMetadataJson());
        Object value = metadata.get("credentialKey");
        if (value == null && metadata.get("credentials") instanceof Map<?, ?> nested) {
            value = nested.get("credentialKey");
        }
        return trimToNull(value == null ? null : String.valueOf(value));
    }

    /**
     * 执行 firstPresent 流程，围绕 first present 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 firstPresent 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 firstPresent 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstPresent 流程中的校验、计算或对象转换。
     * @return 返回 firstPresent 流程生成的业务结果。
     */
    private Object firstPresent(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        return null;
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
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed.toUpperCase(Locale.ROOT);
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
            return values == null ? Map.of() : ProviderWriteEvidenceSanitizer.sanitizeMap(values);
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
}
