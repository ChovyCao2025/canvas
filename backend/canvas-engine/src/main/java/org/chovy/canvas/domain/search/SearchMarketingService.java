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

    @Autowired
    public SearchMarketingService(SearchMarketingSourceMapper sourceMapper,
                                  SearchMarketingKeywordMapper keywordMapper,
                                  SearchMarketingSnapshotMapper snapshotMapper,
                                  SearchMarketingOpportunityMapper opportunityMapper,
                                  ObjectMapper objectMapper) {
        this(sourceMapper, keywordMapper, snapshotMapper, opportunityMapper, objectMapper,
                Clock.systemDefaultZone());
    }

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

    public SearchMarketingSourceView upsertSource(Long tenantId, SearchMarketingSourceCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("search marketing source command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeUpper(command.provider(), "provider");
        String sourceKey = required(command.sourceKey(), "sourceKey");
        LocalDateTime changedAt = now();
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
        return toSourceView(row);
    }

    public SearchMarketingKeywordView upsertKeyword(Long tenantId,
                                                    SearchMarketingKeywordCommand command,
                                                    String actor) {
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
        return toKeywordView(row);
    }

    public SearchMarketingSnapshotView recordSnapshot(Long tenantId,
                                                      SearchMarketingSnapshotCommand command,
                                                      String actor) {
        if (command == null) {
            throw new IllegalArgumentException("search marketing snapshot command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
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
        return toSnapshotView(row);
    }

    public List<SearchMarketingSourceView> listSources(Long tenantId, SearchMarketingSourceQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = query == null ? null : normalizeOptionalUpper(query.provider());
        String channel = query == null ? null : normalizeOptionalUpper(query.channel());
        Boolean enabled = query == null ? null : query.enabled();
        int limit = normalizeLimit(query == null ? null : query.limit());
        return safeList(sourceMapper.selectList(new LambdaQueryWrapper<SearchMarketingSourceDO>()
                .eq(SearchMarketingSourceDO::getTenantId, scopedTenantId)
                .eq(provider != null, SearchMarketingSourceDO::getProvider, provider)
                .eq(channel != null, SearchMarketingSourceDO::getChannel, channel)
                .eq(enabled != null, SearchMarketingSourceDO::getEnabled, Boolean.TRUE.equals(enabled) ? 1 : 0)
                .orderByDesc(SearchMarketingSourceDO::getUpdatedAt)
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

    public List<SearchMarketingKeywordView> listKeywords(Long tenantId, SearchMarketingKeywordQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String channel = query == null ? null : normalizeOptionalUpper(query.channel());
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        int limit = normalizeLimit(query == null ? null : query.limit());
        return safeList(keywordMapper.selectList(new LambdaQueryWrapper<SearchMarketingKeywordDO>()
                .eq(SearchMarketingKeywordDO::getTenantId, scopedTenantId)
                .eq(channel != null, SearchMarketingKeywordDO::getChannel, channel)
                .eq(status != null, SearchMarketingKeywordDO::getStatus, status)
                .orderByDesc(SearchMarketingKeywordDO::getUpdatedAt)
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> channel == null || channel.equals(row.getChannel()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toKeywordView)
                .toList();
    }

    public List<SearchMarketingSnapshotView> listSnapshots(Long tenantId, SearchMarketingSnapshotQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String channel = query == null ? null : normalizeOptionalUpper(query.channel());
        Long sourceId = query == null ? null : query.sourceId();
        Long keywordId = query == null ? null : query.keywordId();
        LocalDate startDate = query == null ? null : query.startDate();
        LocalDate endDate = query == null ? null : query.endDate();
        int limit = normalizeLimit(query == null ? null : query.limit());
        return safeList(snapshotMapper.selectList(new LambdaQueryWrapper<SearchMarketingSnapshotDO>()
                .eq(SearchMarketingSnapshotDO::getTenantId, scopedTenantId)
                .eq(channel != null, SearchMarketingSnapshotDO::getChannel, channel)
                .eq(sourceId != null, SearchMarketingSnapshotDO::getSourceId, sourceId)
                .eq(keywordId != null, SearchMarketingSnapshotDO::getKeywordId, keywordId)
                .ge(startDate != null, SearchMarketingSnapshotDO::getSnapshotDate, startDate)
                .le(endDate != null, SearchMarketingSnapshotDO::getSnapshotDate, endDate)
                .orderByDesc(SearchMarketingSnapshotDO::getSnapshotDate)
                .orderByDesc(SearchMarketingSnapshotDO::getUpdatedAt)
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

    public List<SearchMarketingOpportunityView> listOpportunities(Long tenantId,
                                                                  SearchMarketingOpportunityQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String channel = query == null ? null : normalizeOptionalUpper(query.channel());
        Long sourceId = query == null ? null : query.sourceId();
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        String severity = query == null ? null : normalizeOptionalUpper(query.severity());
        int limit = normalizeLimit(query == null ? null : query.limit());
        return safeList(opportunityMapper.selectList(new LambdaQueryWrapper<SearchMarketingOpportunityDO>()
                .eq(SearchMarketingOpportunityDO::getTenantId, scopedTenantId)
                .eq(channel != null, SearchMarketingOpportunityDO::getChannel, channel)
                .eq(sourceId != null, SearchMarketingOpportunityDO::getSourceId, sourceId)
                .eq(status != null, SearchMarketingOpportunityDO::getStatus, status)
                .eq(severity != null, SearchMarketingOpportunityDO::getSeverity, severity)
                .orderByDesc(SearchMarketingOpportunityDO::getUpdatedAt)
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

    public SearchMarketingSummaryView summary(Long tenantId, SearchMarketingSummaryQuery query) {
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

    public List<SearchMarketingOpportunityView> evaluateOpportunities(Long tenantId,
                                                                      SearchMarketingOpportunityEvaluationCommand command,
                                                                      String actor) {
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
        return scopedSnapshots(scopedTenantId, channel, command.sourceId(), command.keywordId(),
                command.startDate(), command.endDate()).stream()
                .flatMap(snapshot -> opportunitiesForSnapshot(scopedTenantId, snapshot, minImpressions, lowCtr,
                        pageTwo, wastedSpend, actor).stream())
                .toList();
    }

    public SearchMarketingOpportunityView updateOpportunityStatus(Long tenantId,
                                                                  Long opportunityId,
                                                                  SearchMarketingOpportunityStatusCommand command,
                                                                  String actor) {
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
        row.setUpdatedAt(changedAt);
        opportunityMapper.updateById(row);
        return toOpportunityView(row);
    }

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
                && money(snapshot.getCostAmount()).compareTo(wastedSpend) >= 0
                && nonNegative(snapshot.getConversionCount()) == 0) {
            opportunities.add(upsertOpportunity(tenantId, snapshot, "WASTED_SPEND", "HIGH",
                    "Review paid-search query relevance, bids, negatives, and landing page conversion fit.",
                    money(snapshot.getCostAmount()),
                    evidence(snapshot, Map.of("costAmount", money(snapshot.getCostAmount()), "threshold", wastedSpend)), actor));
        }
        return opportunities;
    }

    private SearchMarketingOpportunityView upsertOpportunity(Long tenantId,
                                                             SearchMarketingSnapshotDO snapshot,
                                                             String type,
                                                             String severity,
                                                             String recommendation,
                                                             BigDecimal impactScore,
                                                             Map<String, Object> evidence,
                                                             String actor) {
        LocalDateTime changedAt = now();
        SearchMarketingOpportunityDO row = opportunityMapper.selectOne(new LambdaQueryWrapper<SearchMarketingOpportunityDO>()
                .eq(SearchMarketingOpportunityDO::getTenantId, tenantId)
                .eq(SearchMarketingOpportunityDO::getSourceId, snapshot.getSourceId())
                .eq(SearchMarketingOpportunityDO::getKeywordId, snapshot.getKeywordId())
                .eq(SearchMarketingOpportunityDO::getOpportunityType, type)
                .eq(SearchMarketingOpportunityDO::getSnapshotDate, snapshot.getSnapshotDate())
                .last("LIMIT 1"));
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
        return toOpportunityView(row);
    }

    private SearchMarketingOpportunityDO opportunity(Long tenantId, Long opportunityId) {
        SearchMarketingOpportunityDO row = opportunityMapper.selectById(requiredId(opportunityId, "opportunityId"));
        validateTenant(tenantId, row == null ? null : row.getTenantId(), "opportunity");
        return row;
    }

    private List<SearchMarketingSnapshotDO> scopedSnapshots(Long tenantId,
                                                            String channel,
                                                            Long sourceId,
                                                            Long keywordId,
                                                            LocalDate startDate,
                                                            LocalDate endDate) {
        return safeList(snapshotMapper.selectList(new LambdaQueryWrapper<SearchMarketingSnapshotDO>()
                .eq(SearchMarketingSnapshotDO::getTenantId, tenantId)
                .eq(channel != null, SearchMarketingSnapshotDO::getChannel, channel)
                .eq(sourceId != null, SearchMarketingSnapshotDO::getSourceId, sourceId)
                .eq(keywordId != null, SearchMarketingSnapshotDO::getKeywordId, keywordId)
                .ge(startDate != null, SearchMarketingSnapshotDO::getSnapshotDate, startDate)
                .le(endDate != null, SearchMarketingSnapshotDO::getSnapshotDate, endDate))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> channel == null || channel.equals(row.getChannel()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> keywordId == null || Objects.equals(keywordId, row.getKeywordId()))
                .filter(row -> startDate == null || !row.getSnapshotDate().isBefore(startDate))
                .filter(row -> endDate == null || !row.getSnapshotDate().isAfter(endDate))
                .toList();
    }

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

    private SearchMarketingSnapshotView toSnapshotView(SearchMarketingSnapshotDO row) {
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
                row.getUpdatedAt());
    }

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

    private String normalizeChannel(String value) {
        String channel = normalizeUpper(value, "channel");
        if (!"SEO".equals(channel) && !"SEM".equals(channel)) {
            throw new IllegalArgumentException("channel must be SEO or SEM");
        }
        return channel;
    }

    private String normalizeUpper(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
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

    private String displayKeyword(String value) {
        return required(value, "keywordText").replaceAll("\\s+", " ");
    }

    private String keywordKey(String value) {
        return displayKeyword(value).toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long nonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null || value.compareTo(ZERO) < 0 ? ZERO : value;
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(ZERO) == 0) {
            return ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("search marketing metadata is not JSON serializable", ex);
        }
    }

    private List<String> list(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST);
            return values == null ? List.of() : values;
        } catch (JsonProcessingException ex) {
            return List.of();
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
            throw new IllegalStateException("Could not hash search marketing identity", ex);
        }
    }
}
