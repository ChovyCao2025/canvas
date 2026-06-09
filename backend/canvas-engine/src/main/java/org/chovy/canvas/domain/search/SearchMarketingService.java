package org.chovy.canvas.domain.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingKeywordDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingOpportunityDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSnapshotDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.mapper.SearchMarketingKeywordMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingOpportunityMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSnapshotMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * SearchMarketingService 编排 domain.search 场景的领域业务规则。
 */
@Service
public class SearchMarketingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_LOW_CTR = new BigDecimal("0.010000");
    private static final BigDecimal DEFAULT_SEO_PAGE_TWO = new BigDecimal("10.0000");
    private static final BigDecimal DEFAULT_WASTED_SPEND = new BigDecimal("100.0000");
    private static final long DEFAULT_MIN_IMPRESSIONS = 100L;
    private static final Set<String> OPPORTUNITY_STATUSES = Set.of(
            "OPEN",
            "ACCEPTED",
            "MUTED",
            "CLOSED",
            "IMPACT_POSITIVE",
            "IMPACT_NEUTRAL",
            "IMPACT_NEGATIVE",
            "ROLLBACK_REQUIRED");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final SearchMarketingSourceMapper sourceMapper;
    private final SearchMarketingKeywordMapper keywordMapper;
    private final SearchMarketingSnapshotMapper snapshotMapper;
    private final SearchMarketingOpportunityMapper opportunityMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 SearchMarketingService 实例并注入 domain.search 场景依赖。
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param keywordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param opportunityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public SearchMarketingService(SearchMarketingSourceMapper sourceMapper,
                                  SearchMarketingKeywordMapper keywordMapper,
                                  SearchMarketingSnapshotMapper snapshotMapper,
                                  SearchMarketingOpportunityMapper opportunityMapper,
                                  ObjectMapper objectMapper) {
        this(sourceMapper, keywordMapper, snapshotMapper, opportunityMapper, objectMapper,
                Clock.systemDefaultZone());
    }

    /**
     * 查询或读取业务数据。
     *
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param keywordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param opportunityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    SearchMarketingService(SearchMarketingSourceMapper sourceMapper,
                           SearchMarketingKeywordMapper keywordMapper,
                           SearchMarketingSnapshotMapper snapshotMapper,
                           SearchMarketingOpportunityMapper opportunityMapper,
                           ObjectMapper objectMapper,
                           Clock clock) {
        this.sourceMapper = sourceMapper;
        this.keywordMapper = keywordMapper;
        this.snapshotMapper = snapshotMapper;
        this.opportunityMapper = opportunityMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建或更新业务记录，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingSourceView upsertSource(Long tenantId, SearchMarketingSourceCommand command, String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("search marketing source command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeUpper(command.provider(), "provider");
        String sourceKey = required(command.sourceKey(), "sourceKey");
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SearchMarketingSourceDO row = sourceMapper.selectOne(new LambdaQueryWrapper<SearchMarketingSourceDO>()
                .eq(SearchMarketingSourceDO::getTenantId, scopedTenantId)
                .eq(SearchMarketingSourceDO::getProvider, provider)
                .eq(SearchMarketingSourceDO::getSourceKey, sourceKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new SearchMarketingSourceDO();
            row.setTenantId(scopedTenantId);
            row.setProvider(provider);
            row.setSourceKey(sourceKey);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setDisplayName(defaultString(command.displayName(), sourceKey));
        row.setChannel(normalizeChannel(command.channel()));
        row.setExternalAccountId(trimToNull(command.externalAccountId()));
        row.setSiteUrl(trimToNull(command.siteUrl()));
        row.setCurrency(normalizeUpper(defaultString(command.currency(), "CNY"), "currency"));
        row.setTimezone(defaultString(command.timezone(), "UTC"));
        row.setEnabled(Boolean.FALSE.equals(command.enabled()) ? 0 : 1);
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            sourceMapper.insert(row);
        } else {
            sourceMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toSourceView(row);
    }

    /**
     * 创建或更新业务记录，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingKeywordView upsertKeyword(Long tenantId,
                                                    SearchMarketingKeywordCommand command,
                                                    String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("search marketing keyword command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String channel = normalizeChannel(command.channel());
        String keywordText = displayKeyword(command.keywordText());
        String keywordKey = keywordKey(keywordText);
        String matchType = normalizeUpper(defaultString(command.matchType(), "EXACT"), "matchType");
        String landingPageUrl = trimToNull(command.landingPageUrl());
        String landingPageUrlHash = sha256(defaultString(landingPageUrl, ""));
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SearchMarketingKeywordDO row = keywordMapper.selectOne(new LambdaQueryWrapper<SearchMarketingKeywordDO>()
                .eq(SearchMarketingKeywordDO::getTenantId, scopedTenantId)
                .eq(SearchMarketingKeywordDO::getChannel, channel)
                .eq(SearchMarketingKeywordDO::getKeywordKey, keywordKey)
                .eq(SearchMarketingKeywordDO::getMatchType, matchType)
                .eq(SearchMarketingKeywordDO::getLandingPageUrlHash, landingPageUrlHash)
                .last("LIMIT 1"));
        if (row == null) {
            row = new SearchMarketingKeywordDO();
            row.setTenantId(scopedTenantId);
            row.setChannel(channel);
            row.setKeywordKey(keywordKey);
            row.setMatchType(matchType);
            row.setLandingPageUrlHash(landingPageUrlHash);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setKeywordText(keywordText);
        row.setLandingPageUrl(landingPageUrl);
        row.setSearchIntent(normalizeOptionalUpper(command.searchIntent()));
        row.setLabelsJson(json(command.labels()));
        row.setStatus(normalizeUpper(defaultString(command.status(), "ACTIVE"), "status"));
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            keywordMapper.insert(row);
        } else {
            keywordMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toKeywordView(row);
    }

    /**
     * 执行业务操作 recordSnapshot，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingSnapshotView recordSnapshot(Long tenantId,
                                                      SearchMarketingSnapshotCommand command,
                                                      String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("search marketing snapshot command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SearchMarketingSourceDO source = sourceMapper.selectById(requiredId(command.sourceId(), "sourceId"));
        validateTenant(scopedTenantId, source == null ? null : source.getTenantId(), "source");
        SearchMarketingKeywordDO keyword = keywordMapper.selectById(requiredId(command.keywordId(), "keywordId"));
        validateTenant(scopedTenantId, keyword == null ? null : keyword.getTenantId(), "keyword");
        String channel = normalizeChannel(defaultString(keyword.getChannel(), source.getChannel()));
        LocalDate snapshotDate = command.snapshotDate() == null ? LocalDate.now(clock) : command.snapshotDate();
        String device = normalizeUpper(defaultString(command.device(), "ALL"), "device");
        String country = normalizeUpper(defaultString(command.country(), "ALL"), "country");
        String queryGroupKey = required(defaultString(command.queryGroupKey(), "DEFAULT"), "queryGroupKey");
        LocalDateTime changedAt = now();
        SearchMarketingSnapshotDO row = snapshotMapper.selectOne(new LambdaQueryWrapper<SearchMarketingSnapshotDO>()
                .eq(SearchMarketingSnapshotDO::getTenantId, scopedTenantId)
                .eq(SearchMarketingSnapshotDO::getSourceId, command.sourceId())
                .eq(SearchMarketingSnapshotDO::getKeywordId, command.keywordId())
                .eq(SearchMarketingSnapshotDO::getSnapshotDate, snapshotDate)
                .eq(SearchMarketingSnapshotDO::getDevice, device)
                .eq(SearchMarketingSnapshotDO::getCountry, country)
                .eq(SearchMarketingSnapshotDO::getQueryGroupKey, queryGroupKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new SearchMarketingSnapshotDO();
            row.setTenantId(scopedTenantId);
            row.setSourceId(command.sourceId());
            row.setKeywordId(command.keywordId());
            row.setSnapshotDate(snapshotDate);
            row.setDevice(device);
            row.setCountry(country);
            row.setQueryGroupKey(queryGroupKey);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setChannel(channel);
        row.setImpressionCount(nonNegative(command.impressionCount()));
        row.setClickCount(nonNegative(command.clickCount()));
        row.setCostAmount(money(command.costAmount()));
        row.setConversionCount(nonNegative(command.conversionCount()));
        row.setRevenueAmount(money(command.revenueAmount()));
        row.setAveragePosition(command.averagePosition());
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            snapshotMapper.insert(row);
        } else {
            snapshotMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toSnapshotView(row);
    }

    /**
     * 查询业务列表，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 listSources 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingSourceView> listSources(Long tenantId, SearchMarketingSourceQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = query == null ? null : normalizeOptionalUpper(query.provider());
        String channel = query == null ? null : normalizeOptionalUpper(query.channel());
        Boolean enabled = query == null ? null : query.enabled();
        int limit = normalizeLimit(query == null ? null : query.limit());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(sourceMapper.selectList(new LambdaQueryWrapper<SearchMarketingSourceDO>()
                .eq(SearchMarketingSourceDO::getTenantId, scopedTenantId)
                .eq(provider != null, SearchMarketingSourceDO::getProvider, provider)
                .eq(channel != null, SearchMarketingSourceDO::getChannel, channel)
                .eq(enabled != null, SearchMarketingSourceDO::getEnabled, Boolean.TRUE.equals(enabled) ? 1 : 0)
                .orderByDesc(SearchMarketingSourceDO::getUpdatedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> provider == null || provider.equals(row.getProvider()))
                .filter(row -> channel == null || channel.equals(row.getChannel()))
                .filter(row -> enabled == null
                        || (Boolean.TRUE.equals(enabled) && Integer.valueOf(1).equals(row.getEnabled()))
                        || (Boolean.FALSE.equals(enabled) && !Integer.valueOf(1).equals(row.getEnabled())))
                .limit(limit)
                .map(this::toSourceView)
                .toList();
    }

    /**
     * 查询业务列表，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 listKeywords 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingKeywordView> listKeywords(Long tenantId, SearchMarketingKeywordQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String channel = query == null ? null : normalizeOptionalUpper(query.channel());
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        int limit = normalizeLimit(query == null ? null : query.limit());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(keywordMapper.selectList(new LambdaQueryWrapper<SearchMarketingKeywordDO>()
                .eq(SearchMarketingKeywordDO::getTenantId, scopedTenantId)
                .eq(channel != null, SearchMarketingKeywordDO::getChannel, channel)
                .eq(status != null, SearchMarketingKeywordDO::getStatus, status)
                .orderByDesc(SearchMarketingKeywordDO::getUpdatedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> channel == null || channel.equals(row.getChannel()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toKeywordView)
                .toList();
    }

    /**
     * 查询业务列表，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 listSnapshots 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingSnapshotView> listSnapshots(Long tenantId, SearchMarketingSnapshotQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String channel = query == null ? null : normalizeOptionalUpper(query.channel());
        Long sourceId = query == null ? null : query.sourceId();
        Long keywordId = query == null ? null : query.keywordId();
        LocalDate startDate = query == null ? null : query.startDate();
        LocalDate endDate = query == null ? null : query.endDate();
        int limit = normalizeLimit(query == null ? null : query.limit());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(snapshotMapper.selectList(new LambdaQueryWrapper<SearchMarketingSnapshotDO>()
                .eq(SearchMarketingSnapshotDO::getTenantId, scopedTenantId)
                .eq(channel != null, SearchMarketingSnapshotDO::getChannel, channel)
                .eq(sourceId != null, SearchMarketingSnapshotDO::getSourceId, sourceId)
                .eq(keywordId != null, SearchMarketingSnapshotDO::getKeywordId, keywordId)
                .ge(startDate != null, SearchMarketingSnapshotDO::getSnapshotDate, startDate)
                .le(endDate != null, SearchMarketingSnapshotDO::getSnapshotDate, endDate)
                .orderByDesc(SearchMarketingSnapshotDO::getSnapshotDate)
                .orderByDesc(SearchMarketingSnapshotDO::getUpdatedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> channel == null || channel.equals(row.getChannel()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> keywordId == null || Objects.equals(keywordId, row.getKeywordId()))
                .filter(row -> startDate == null
                        || (row.getSnapshotDate() != null && !row.getSnapshotDate().isBefore(startDate)))
                .filter(row -> endDate == null
                        || (row.getSnapshotDate() != null && !row.getSnapshotDate().isAfter(endDate)))
                .limit(limit)
                .map(this::toSnapshotView)
                .toList();
    }

    /**
     * 查询业务列表，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 listOpportunities 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingOpportunityView> listOpportunities(Long tenantId,
                                                                  SearchMarketingOpportunityQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String channel = query == null ? null : normalizeOptionalUpper(query.channel());
        Long sourceId = query == null ? null : query.sourceId();
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        String severity = query == null ? null : normalizeOptionalUpper(query.severity());
        int limit = normalizeLimit(query == null ? null : query.limit());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(opportunityMapper.selectList(new LambdaQueryWrapper<SearchMarketingOpportunityDO>()
                .eq(SearchMarketingOpportunityDO::getTenantId, scopedTenantId)
                .eq(channel != null, SearchMarketingOpportunityDO::getChannel, channel)
                .eq(sourceId != null, SearchMarketingOpportunityDO::getSourceId, sourceId)
                .eq(status != null, SearchMarketingOpportunityDO::getStatus, status)
                .eq(severity != null, SearchMarketingOpportunityDO::getSeverity, severity)
                .orderByDesc(SearchMarketingOpportunityDO::getUpdatedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> channel == null || channel.equals(row.getChannel()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .filter(row -> severity == null || severity.equals(row.getSeverity()))
                .limit(limit)
                .map(this::toOpportunityView)
                .toList();
    }

    /**
     * 执行业务操作 summary，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 summary 流程中的校验、计算或对象转换。
     * @return 返回本次处理的状态、计数、命中明细或治理结论，供控制器和调度任务判断后续动作
     */
    public SearchMarketingSummaryView summary(Long tenantId, SearchMarketingSummaryQuery query) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (query == null) {
            throw new IllegalArgumentException("search marketing summary query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String channel = normalizeOptionalUpper(query.channel());
        List<SearchMarketingSnapshotDO> snapshots = scopedSnapshots(scopedTenantId, channel, query.sourceId(),
                query.keywordId(), query.startDate(), query.endDate());
        long impressions = 0L;
        long clicks = 0L;
        BigDecimal cost = ZERO;
        long conversions = 0L;
        BigDecimal revenue = ZERO;
        BigDecimal weightedPosition = ZERO;
        long positionImpressions = 0L;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (SearchMarketingSnapshotDO snapshot : snapshots) {
            long rowImpressions = nonNegative(snapshot.getImpressionCount());
            impressions += rowImpressions;
            clicks += nonNegative(snapshot.getClickCount());
            cost = cost.add(money(snapshot.getCostAmount()));
            conversions += nonNegative(snapshot.getConversionCount());
            revenue = revenue.add(money(snapshot.getRevenueAmount()));
            if (snapshot.getAveragePosition() != null && rowImpressions > 0) {
                weightedPosition = weightedPosition.add(snapshot.getAveragePosition().multiply(BigDecimal.valueOf(rowImpressions)));
                positionImpressions += rowImpressions;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new SearchMarketingSummaryView(
                scopedTenantId,
                channel,
                query.sourceId(),
                query.keywordId(),
                query.startDate(),
                query.endDate(),
                snapshots.size(),
                impressions,
                clicks,
                cost,
                conversions,
                revenue,
                divide(BigDecimal.valueOf(clicks), BigDecimal.valueOf(impressions)),
                divide(cost, BigDecimal.valueOf(clicks)),
                divide(BigDecimal.valueOf(conversions), BigDecimal.valueOf(clicks)),
                divide(revenue, cost),
                divide(weightedPosition, BigDecimal.valueOf(positionImpressions)));
    }

    /**
     * 执行业务操作 evaluateOpportunities，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingOpportunityView> evaluateOpportunities(Long tenantId,
                                                                      SearchMarketingOpportunityEvaluationCommand command,
                                                                      String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("search marketing opportunity evaluation command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String channel = normalizeOptionalUpper(command.channel());
        long minImpressions = command.minImpressions() == null || command.minImpressions() < 0
                ? DEFAULT_MIN_IMPRESSIONS
                : command.minImpressions();
        BigDecimal lowCtr = command.lowCtrThreshold() == null ? DEFAULT_LOW_CTR : command.lowCtrThreshold();
        BigDecimal pageTwo = command.seoPageTwoPosition() == null ? DEFAULT_SEO_PAGE_TWO : command.seoPageTwoPosition();
        BigDecimal wastedSpend = command.wastedSpendThreshold() == null
                ? DEFAULT_WASTED_SPEND
                : command.wastedSpendThreshold();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return scopedSnapshots(scopedTenantId, channel, command.sourceId(), command.keywordId(),
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                command.startDate(), command.endDate()).stream()
                .flatMap(snapshot -> opportunitiesForSnapshot(scopedTenantId, snapshot, minImpressions, lowCtr,
                        pageTwo, wastedSpend, actor).stream())
                .toList();
    }

    /**
     * 更新业务记录，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param opportunityId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingOpportunityView updateOpportunityStatus(Long tenantId,
                                                                  Long opportunityId,
                                                                  SearchMarketingOpportunityStatusCommand command,
                                                                  String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("search marketing opportunity status command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        SearchMarketingOpportunityDO row = opportunity(scopedTenantId, opportunityId);
        String status = normalizeUpper(command.status(), "status");
        if (!OPPORTUNITY_STATUSES.contains(status)) {
            throw new IllegalArgumentException("unsupported search marketing opportunity status");
        }
        LocalDateTime changedAt = now();
        Map<String, Object> evidence = new LinkedHashMap<>(map(row.getEvidenceJson()));
        String reason = trimToNull(command.reason());
        if (reason != null) {
            evidence.put("statusReason", reason);
        }
        evidence.put("statusUpdatedBy", defaultString(actor, "system"));
        evidence.put("statusUpdatedAt", changedAt.toString());
        row.setStatus(status);
        row.setEvidenceJson(json(evidence));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(changedAt);
        opportunityMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toOpportunityView(row);
    }

    /**
     * 执行 opportunitiesForSnapshot 流程，围绕 opportunities for snapshot 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param snapshot snapshot 参数，用于 opportunitiesForSnapshot 流程中的校验、计算或对象转换。
     * @param minImpressions min impressions 参数，用于 opportunitiesForSnapshot 流程中的校验、计算或对象转换。
     * @param lowCtr low ctr 参数，用于 opportunitiesForSnapshot 流程中的校验、计算或对象转换。
     * @param pageTwo page two 参数，用于 opportunitiesForSnapshot 流程中的校验、计算或对象转换。
     * @param wastedSpend wasted spend 参数，用于 opportunitiesForSnapshot 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 opportunities for snapshot 汇总后的集合、分页或映射视图。
     */
    private List<SearchMarketingOpportunityView> opportunitiesForSnapshot(Long tenantId,
                                                                          SearchMarketingSnapshotDO snapshot,
                                                                          long minImpressions,
                                                                          BigDecimal lowCtr,
                                                                          BigDecimal pageTwo,
                                                                          BigDecimal wastedSpend,
                                                                          String actor) {
        long impressions = nonNegative(snapshot.getImpressionCount());
        BigDecimal ctr = divide(BigDecimal.valueOf(nonNegative(snapshot.getClickCount())), BigDecimal.valueOf(impressions));
        java.util.ArrayList<SearchMarketingOpportunityView> opportunities = new java.util.ArrayList<>();
        if (impressions >= minImpressions && ctr.compareTo(lowCtr) < 0) {
            opportunities.add(upsertOpportunity(tenantId, snapshot, "LOW_CTR", "HIGH",
                    "Improve ad copy, title, snippet, or query match to lift search CTR.",
                    BigDecimal.valueOf(impressions).multiply(lowCtr.subtract(ctr).max(ZERO)),
                    evidence(snapshot, Map.of("ctr", ctr, "threshold", lowCtr)), actor));
        }
        if ("SEO".equals(snapshot.getChannel())
                && impressions >= minImpressions
                && snapshot.getAveragePosition() != null
                && snapshot.getAveragePosition().compareTo(pageTwo) >= 0) {
            opportunities.add(upsertOpportunity(tenantId, snapshot, "SEO_PAGE_TWO", "MEDIUM",
                    "Prioritize content refresh, internal links, and SERP snippet improvements for page-two SEO terms.",
                    BigDecimal.valueOf(impressions),
                    evidence(snapshot, Map.of("averagePosition", snapshot.getAveragePosition(), "threshold", pageTwo)), actor));
        }
        if ("SEM".equals(snapshot.getChannel())
                /**
                 * 执行 money 流程，围绕 money 完成校验、计算或结果组装。
                 *
                 * @return 返回 money 流程生成的业务结果。
                 */
                && money(snapshot.getCostAmount()).compareTo(wastedSpend) >= 0
                /**
                 * 执行 nonNegative 流程，围绕 non negative 完成校验、计算或结果组装。
                 *
                 * @return 返回 nonNegative 流程生成的业务结果。
                 */
                && nonNegative(snapshot.getConversionCount()) == 0) {
            opportunities.add(upsertOpportunity(tenantId, snapshot, "WASTED_SPEND", "HIGH",
                    "Review paid-search query relevance, bids, negatives, and landing page conversion fit.",
                    money(snapshot.getCostAmount()),
                    evidence(snapshot, Map.of("costAmount", money(snapshot.getCostAmount()), "threshold", wastedSpend)), actor));
        }
        return opportunities;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param snapshot snapshot 参数，用于 upsertOpportunity 流程中的校验、计算或对象转换。
     * @param type 类型标识，用于选择对应处理分支。
     * @param severity severity 参数，用于 upsertOpportunity 流程中的校验、计算或对象转换。
     * @param recommendation recommendation 参数，用于 upsertOpportunity 流程中的校验、计算或对象转换。
     * @param impactScore impact score 参数，用于 upsertOpportunity 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 upsertOpportunity 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    private SearchMarketingOpportunityView upsertOpportunity(Long tenantId,
                                                             SearchMarketingSnapshotDO snapshot,
                                                             String type,
                                                             String severity,
                                                             String recommendation,
                                                             BigDecimal impactScore,
                                                             Map<String, Object> evidence,
                                                             String actor) {
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SearchMarketingOpportunityDO row = opportunityMapper.selectOne(new LambdaQueryWrapper<SearchMarketingOpportunityDO>()
                .eq(SearchMarketingOpportunityDO::getTenantId, tenantId)
                .eq(SearchMarketingOpportunityDO::getSourceId, snapshot.getSourceId())
                .eq(SearchMarketingOpportunityDO::getKeywordId, snapshot.getKeywordId())
                .eq(SearchMarketingOpportunityDO::getOpportunityType, type)
                .eq(SearchMarketingOpportunityDO::getSnapshotDate, snapshot.getSnapshotDate())
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new SearchMarketingOpportunityDO();
            row.setTenantId(tenantId);
            row.setSourceId(snapshot.getSourceId());
            row.setKeywordId(snapshot.getKeywordId());
            row.setOpportunityType(type);
            row.setSnapshotDate(snapshot.getSnapshotDate());
            row.setStatus("OPEN");
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setChannel(snapshot.getChannel());
        row.setSeverity(severity);
        row.setRecommendation(recommendation);
        row.setImpactScore(impactScore == null ? ZERO : impactScore);
        row.setEvidenceJson(json(evidence));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            opportunityMapper.insert(row);
        } else {
            opportunityMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toOpportunityView(row);
    }

    /**
     * 执行 opportunity 流程，围绕 opportunity 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param opportunityId 业务对象 ID，用于定位具体记录。
     * @return 返回 opportunity 流程生成的业务结果。
     */
    private SearchMarketingOpportunityDO opportunity(Long tenantId, Long opportunityId) {
        SearchMarketingOpportunityDO row = opportunityMapper.selectById(requiredId(opportunityId, "opportunityId"));
        validateTenant(tenantId, row == null ? null : row.getTenantId(), "opportunity");
        return row;
    }

    /**
     * 执行 scopedSnapshots 流程，围绕 scoped snapshots 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param channel channel 参数，用于 scopedSnapshots 流程中的校验、计算或对象转换。
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param keywordId 业务对象 ID，用于定位具体记录。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 scoped snapshots 汇总后的集合、分页或映射视图。
     */
    private List<SearchMarketingSnapshotDO> scopedSnapshots(Long tenantId,
                                                            String channel,
                                                            Long sourceId,
                                                            Long keywordId,
                                                            LocalDate startDate,
                                                            LocalDate endDate) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(snapshotMapper.selectList(new LambdaQueryWrapper<SearchMarketingSnapshotDO>()
                .eq(SearchMarketingSnapshotDO::getTenantId, tenantId)
                .eq(channel != null, SearchMarketingSnapshotDO::getChannel, channel)
                .eq(sourceId != null, SearchMarketingSnapshotDO::getSourceId, sourceId)
                .eq(keywordId != null, SearchMarketingSnapshotDO::getKeywordId, keywordId)
                .ge(startDate != null, SearchMarketingSnapshotDO::getSnapshotDate, startDate)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .le(endDate != null, SearchMarketingSnapshotDO::getSnapshotDate, endDate))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> channel == null || channel.equals(row.getChannel()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> keywordId == null || Objects.equals(keywordId, row.getKeywordId()))
                .filter(row -> startDate == null || !row.getSnapshotDate().isBefore(startDate))
                .filter(row -> endDate == null || !row.getSnapshotDate().isAfter(endDate))
                .toList();
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SearchMarketingSourceView toSourceView(SearchMarketingSourceDO row) {
        return new SearchMarketingSourceView(
                row.getId(),
                row.getTenantId(),
                row.getProvider(),
                row.getSourceKey(),
                row.getDisplayName(),
                row.getChannel(),
                row.getExternalAccountId(),
                row.getSiteUrl(),
                row.getCurrency(),
                row.getTimezone(),
                Integer.valueOf(1).equals(row.getEnabled()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SearchMarketingKeywordView toKeywordView(SearchMarketingKeywordDO row) {
        return new SearchMarketingKeywordView(
                row.getId(),
                row.getTenantId(),
                row.getChannel(),
                row.getKeywordText(),
                row.getKeywordKey(),
                row.getMatchType(),
                row.getLandingPageUrl(),
                row.getLandingPageUrlHash(),
                row.getSearchIntent(),
                list(row.getLabelsJson()),
                row.getStatus(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SearchMarketingSnapshotView toSnapshotView(SearchMarketingSnapshotDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new SearchMarketingSnapshotView(
                row.getId(),
                row.getTenantId(),
                row.getSourceId(),
                row.getKeywordId(),
                row.getChannel(),
                row.getSnapshotDate(),
                row.getDevice(),
                row.getCountry(),
                row.getQueryGroupKey(),
                nonNegative(row.getImpressionCount()),
                nonNegative(row.getClickCount()),
                money(row.getCostAmount()),
                nonNegative(row.getConversionCount()),
                money(row.getRevenueAmount()),
                row.getAveragePosition(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SearchMarketingOpportunityView toOpportunityView(SearchMarketingOpportunityDO row) {
        return new SearchMarketingOpportunityView(
                row.getId(),
                row.getTenantId(),
                row.getSourceId(),
                row.getKeywordId(),
                row.getChannel(),
                row.getOpportunityType(),
                row.getSnapshotDate(),
                row.getSeverity(),
                row.getStatus(),
                row.getRecommendation(),
                row.getImpactScore(),
                map(row.getEvidenceJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 执行 evidence 流程，围绕 evidence 完成校验、计算或结果组装。
     *
     * @param snapshot snapshot 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @param extra extra 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @return 返回 evidence 流程生成的业务结果。
     */
    private Map<String, Object> evidence(SearchMarketingSnapshotDO snapshot, Map<String, Object> extra) {
        java.util.LinkedHashMap<String, Object> evidence = new java.util.LinkedHashMap<>();
        evidence.put("snapshotDate", snapshot.getSnapshotDate() == null ? null : snapshot.getSnapshotDate().toString());
        evidence.put("channel", snapshot.getChannel());
        evidence.put("sourceId", snapshot.getSourceId());
        evidence.put("keywordId", snapshot.getKeywordId());
        evidence.put("impressionCount", nonNegative(snapshot.getImpressionCount()));
        evidence.put("clickCount", nonNegative(snapshot.getClickCount()));
        evidence.put("ctr", divide(BigDecimal.valueOf(nonNegative(snapshot.getClickCount())),
                BigDecimal.valueOf(nonNegative(snapshot.getImpressionCount()))));
        evidence.put("costAmount", money(snapshot.getCostAmount()));
        evidence.put("conversionCount", nonNegative(snapshot.getConversionCount()));
        evidence.putAll(extra);
        return evidence;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param expectedTenantId 业务对象 ID，用于定位具体记录。
     * @param actualTenantId 业务对象 ID，用于定位具体记录。
     * @param resource resource 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     */
    private void validateTenant(Long expectedTenantId, Long actualTenantId, String resource) {
        if (!expectedTenantId.equals(actualTenantId)) {
            throw new IllegalArgumentException(resource + " does not belong to current tenant");
        }
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            return 0L;
        }
        return tenantId;
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
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeChannel(String value) {
        String channel = normalizeUpper(value, "channel");
        if (!"SEO".equals(channel) && !"SEM".equals(channel)) {
            throw new IllegalArgumentException("channel must be SEO or SEM");
        }
        return channel;
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
     * 执行 displayKeyword 流程，围绕 display keyword 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 display keyword 生成的文本或业务键。
     */
    private String displayKeyword(String value) {
        return required(value, "keywordText").replaceAll("\\s+", " ");
    }

    /**
     * 执行 keywordKey 流程，围绕 keyword key 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 keyword key 生成的文本或业务键。
     */
    private String keywordKey(String value) {
        return displayKeyword(value).toLowerCase(Locale.ROOT);
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
    private Long nonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    /**
     * 执行 money 流程，围绕 money 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 money 计算得到的数量、金额或指标值。
     */
    private BigDecimal money(BigDecimal value) {
        return value == null || value.compareTo(ZERO) < 0 ? ZERO : value;
    }

    /**
     * 执行 divide 流程，围绕 divide 完成校验、计算或结果组装。
     *
     * @param numerator numerator 参数，用于 divide 流程中的校验、计算或对象转换。
     * @param denominator denominator 参数，用于 divide 流程中的校验、计算或对象转换。
     * @return 返回 divide 计算得到的数量、金额或指标值。
     */
    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(ZERO) == 0) {
            return ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP);
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
            throw new IllegalArgumentException("search marketing metadata is not JSON serializable", ex);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<String> list(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST);
            return values == null ? List.of() : values;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return List.of();
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
            throw new IllegalStateException("Could not hash search marketing identity", ex);
        }
    }
}
