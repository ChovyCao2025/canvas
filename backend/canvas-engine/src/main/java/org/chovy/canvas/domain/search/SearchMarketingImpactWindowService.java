package org.chovy.canvas.domain.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingImpactWindowDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingOpportunityDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSnapshotDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingUrlInspectionDO;
import org.chovy.canvas.dal.mapper.SearchMarketingImpactWindowMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingOpportunityMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSnapshotMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingUrlInspectionMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class SearchMarketingImpactWindowService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
    private static final BigDecimal TWENTY_PERCENT = new BigDecimal("0.20");

    private final SearchMarketingMutationMapper mutationMapper;
    private final SearchMarketingOpportunityMapper opportunityMapper;
    private final SearchMarketingImpactWindowMapper impactWindowMapper;
    private final SearchMarketingSnapshotMapper snapshotMapper;
    private final SearchMarketingUrlInspectionMapper urlInspectionMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public SearchMarketingImpactWindowService(SearchMarketingMutationMapper mutationMapper,
                                             SearchMarketingOpportunityMapper opportunityMapper,
                                             SearchMarketingImpactWindowMapper impactWindowMapper,
                                             SearchMarketingSnapshotMapper snapshotMapper,
                                             SearchMarketingUrlInspectionMapper urlInspectionMapper,
                                             ObjectMapper objectMapper) {
        this(mutationMapper, opportunityMapper, impactWindowMapper, snapshotMapper, urlInspectionMapper,
                objectMapper, Clock.systemDefaultZone());
    }

    SearchMarketingImpactWindowService(SearchMarketingMutationMapper mutationMapper,
                                       SearchMarketingOpportunityMapper opportunityMapper,
                                       SearchMarketingImpactWindowMapper impactWindowMapper,
                                       SearchMarketingSnapshotMapper snapshotMapper,
                                       SearchMarketingUrlInspectionMapper urlInspectionMapper,
                                       ObjectMapper objectMapper,
                                       Clock clock) {
        this.mutationMapper = mutationMapper;
        this.opportunityMapper = opportunityMapper;
        this.impactWindowMapper = impactWindowMapper;
        this.snapshotMapper = snapshotMapper;
        this.urlInspectionMapper = urlInspectionMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public SearchMarketingImpactWindowView scheduleForReconciledMutation(Long tenantId,
                                                                         Long mutationId,
                                                                         String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        SearchMarketingMutationDO mutation = mutation(scopedTenantId, mutationId);
        if (!"RECONCILED".equals(normalize(mutation.getStatus()))) {
            throw new IllegalStateException("search marketing mutation must be reconciled before impact scheduling");
        }
        SearchMarketingOpportunityDO opportunity = opportunity(scopedTenantId, mutation.getOpportunityId());
        LocalDate anchorDate = opportunity.getSnapshotDate() == null
                ? executedDate(mutation).minusDays(1)
                : opportunity.getSnapshotDate();
        LocalDate baselineEnd = anchorDate;
        LocalDate baselineStart = baselineEnd.minusDays(6);
        LocalDate postStart = executedDate(mutation).plusDays(1);
        LocalDate postEnd = postStart.plusDays(6);
        SearchMarketingImpactWindowDO existing = impactWindowMapper.selectOne(
                new LambdaQueryWrapper<SearchMarketingImpactWindowDO>()
                        .eq(SearchMarketingImpactWindowDO::getTenantId, scopedTenantId)
                        .eq(SearchMarketingImpactWindowDO::getOpportunityId, opportunity.getId())
                        .eq(SearchMarketingImpactWindowDO::getMutationId, mutation.getId())
                        .eq(SearchMarketingImpactWindowDO::getBaselineStartDate, baselineStart)
                        .eq(SearchMarketingImpactWindowDO::getPostStartDate, postStart)
                        .last("LIMIT 1"));
        if (existing != null && scopedTenantId.equals(existing.getTenantId())) {
            return toView(existing);
        }
        LocalDateTime changedAt = now();
        SearchMarketingImpactWindowDO row = new SearchMarketingImpactWindowDO();
        row.setTenantId(scopedTenantId);
        row.setOpportunityId(opportunity.getId());
        row.setMutationId(mutation.getId());
        row.setSourceId(mutation.getSourceId());
        row.setKeywordId(mutation.getKeywordId());
        row.setPageUrlHash(stringValue(map(opportunity.getEvidenceJson()).get("landingPageUrlHash")));
        row.setBaselineStartDate(baselineStart);
        row.setBaselineEndDate(baselineEnd);
        row.setPostStartDate(postStart);
        row.setPostEndDate(postEnd);
        row.setStatus("SCHEDULED");
        row.setConfidence(new BigDecimal("0.0000"));
        row.setMetricDeltaJson(json(Map.of()));
        row.setEvidenceJson(json(Map.of(
                "scheduledFromMutationStatus", mutation.getStatus(),
                "channel", opportunity.getChannel(),
                "baselineDays", 7,
                "postDays", 7)));
        row.setDueAt(postEnd.plusDays(1).atStartOfDay());
        row.setCreatedBy(defaultString(actor, "system"));
        row.setCreatedAt(changedAt);
        row.setUpdatedAt(changedAt);
        impactWindowMapper.insert(row);
        return toView(row);
    }

    public List<SearchMarketingImpactWindowView> evaluateDue(Long tenantId, int limit, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int normalizedLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 100));
        LocalDateTime evaluatedAt = now();
        return safeList(impactWindowMapper.selectList(new LambdaQueryWrapper<SearchMarketingImpactWindowDO>()
                .eq(SearchMarketingImpactWindowDO::getTenantId, scopedTenantId)
                .in(SearchMarketingImpactWindowDO::getStatus, "SCHEDULED", "DUE")
                .le(SearchMarketingImpactWindowDO::getDueAt, evaluatedAt)
                .orderByAsc(SearchMarketingImpactWindowDO::getDueAt)
                .last("LIMIT " + normalizedLimit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> SetLike.contains(row.getStatus(), "SCHEDULED", "DUE"))
                .filter(row -> row.getDueAt() != null && !row.getDueAt().isAfter(evaluatedAt))
                .limit(normalizedLimit)
                .map(row -> evaluate(scopedTenantId, row, actor))
                .toList();
    }

    public List<SearchMarketingImpactWindowView> list(Long tenantId, SearchMarketingImpactWindowQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long opportunityId = query == null ? null : query.opportunityId();
        Long mutationId = query == null ? null : query.mutationId();
        Long sourceId = query == null ? null : query.sourceId();
        String status = query == null ? null : normalizeOptional(query.status());
        String decision = query == null ? null : normalizeOptional(query.decision());
        int limit = Math.max(1, Math.min(query == null || query.limit() == null ? 50 : query.limit(), 100));
        return safeList(impactWindowMapper.selectList(new LambdaQueryWrapper<SearchMarketingImpactWindowDO>()
                .eq(SearchMarketingImpactWindowDO::getTenantId, scopedTenantId)
                .eq(opportunityId != null, SearchMarketingImpactWindowDO::getOpportunityId, opportunityId)
                .eq(mutationId != null, SearchMarketingImpactWindowDO::getMutationId, mutationId)
                .eq(sourceId != null, SearchMarketingImpactWindowDO::getSourceId, sourceId)
                .eq(status != null, SearchMarketingImpactWindowDO::getStatus, status)
                .eq(decision != null, SearchMarketingImpactWindowDO::getDecision, decision)
                .orderByDesc(SearchMarketingImpactWindowDO::getUpdatedAt)
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> opportunityId == null || Objects.equals(opportunityId, row.getOpportunityId()))
                .filter(row -> mutationId == null || Objects.equals(mutationId, row.getMutationId()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> status == null || status.equals(normalize(row.getStatus())))
                .filter(row -> decision == null || decision.equals(normalize(row.getDecision())))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    private SearchMarketingImpactWindowView evaluate(Long tenantId,
                                                     SearchMarketingImpactWindowDO row,
                                                     String actor) {
        MetricAggregate baseline = aggregateSnapshots(tenantId, row, row.getBaselineStartDate(),
                row.getBaselineEndDate());
        MetricAggregate post = aggregateSnapshots(tenantId, row, row.getPostStartDate(), row.getPostEndDate());
        Map<String, Object> deltas = deltas(baseline, post);
        if (row.getPageUrlHash() != null) {
            indexedStateDeltas(tenantId, row, deltas);
        }
        String decision = decision(baseline, post, deltas);
        LocalDateTime changedAt = now();
        row.setStatus("EVALUATED");
        row.setDecision(decision);
        row.setConfidence(confidence(decision, baseline, post));
        row.setMetricDeltaJson(json(deltas));
        row.setEvidenceJson(json(evaluationEvidence(row, decision, actor)));
        row.setEvaluatedAt(changedAt);
        row.setUpdatedAt(changedAt);
        impactWindowMapper.updateById(row);
        updateOpportunity(tenantId, row, decision, actor, changedAt);
        return toView(row);
    }

    private MetricAggregate aggregateSnapshots(Long tenantId,
                                               SearchMarketingImpactWindowDO window,
                                               LocalDate startDate,
                                               LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return MetricAggregate.empty();
        }
        List<SearchMarketingSnapshotDO> rows = safeList(snapshotMapper.selectList(
                new LambdaQueryWrapper<SearchMarketingSnapshotDO>()
                        .eq(SearchMarketingSnapshotDO::getTenantId, tenantId)
                        .eq(SearchMarketingSnapshotDO::getSourceId, window.getSourceId())
                        .eq(window.getKeywordId() != null, SearchMarketingSnapshotDO::getKeywordId,
                                window.getKeywordId())
                        .ge(SearchMarketingSnapshotDO::getSnapshotDate, startDate)
                        .le(SearchMarketingSnapshotDO::getSnapshotDate, endDate)));
        long impressions = 0L;
        long clicks = 0L;
        BigDecimal cost = ZERO;
        long conversions = 0L;
        BigDecimal revenue = ZERO;
        BigDecimal weightedPosition = ZERO;
        long positionImpressions = 0L;
        int count = 0;
        for (SearchMarketingSnapshotDO row : rows) {
            if (!tenantId.equals(row.getTenantId()) || !Objects.equals(window.getSourceId(), row.getSourceId())
                    || (window.getKeywordId() != null && !Objects.equals(window.getKeywordId(), row.getKeywordId()))
                    || row.getSnapshotDate() == null
                    || row.getSnapshotDate().isBefore(startDate)
                    || row.getSnapshotDate().isAfter(endDate)) {
                continue;
            }
            long rowImpressions = nonNegative(row.getImpressionCount());
            impressions += rowImpressions;
            clicks += nonNegative(row.getClickCount());
            cost = cost.add(money(row.getCostAmount()));
            conversions += nonNegative(row.getConversionCount());
            revenue = revenue.add(money(row.getRevenueAmount()));
            if (row.getAveragePosition() != null && rowImpressions > 0) {
                weightedPosition = weightedPosition.add(row.getAveragePosition().multiply(BigDecimal.valueOf(rowImpressions)));
                positionImpressions += rowImpressions;
            }
            count++;
        }
        return new MetricAggregate(
                count,
                impressions,
                clicks,
                cost,
                conversions,
                revenue,
                divide(BigDecimal.valueOf(clicks), BigDecimal.valueOf(impressions)),
                divide(BigDecimal.valueOf(conversions), BigDecimal.valueOf(clicks)),
                divide(revenue, cost),
                divide(weightedPosition, BigDecimal.valueOf(positionImpressions)));
    }

    private Map<String, Object> deltas(MetricAggregate baseline, MetricAggregate post) {
        Map<String, Object> deltas = new LinkedHashMap<>();
        deltas.put("baselineSnapshotCount", baseline.snapshotCount());
        deltas.put("postSnapshotCount", post.snapshotCount());
        deltas.put("baselineImpressions", baseline.impressions());
        deltas.put("postImpressions", post.impressions());
        deltas.put("impressionDelta", post.impressions() - baseline.impressions());
        deltas.put("baselineClicks", baseline.clicks());
        deltas.put("postClicks", post.clicks());
        deltas.put("clickDelta", post.clicks() - baseline.clicks());
        deltas.put("baselineCost", baseline.cost());
        deltas.put("postCost", post.cost());
        deltas.put("costDelta", post.cost().subtract(baseline.cost()));
        deltas.put("baselineConversions", baseline.conversions());
        deltas.put("postConversions", post.conversions());
        deltas.put("conversionDelta", post.conversions() - baseline.conversions());
        deltas.put("baselineRevenue", baseline.revenue());
        deltas.put("postRevenue", post.revenue());
        deltas.put("revenueDelta", post.revenue().subtract(baseline.revenue()));
        deltas.put("baselineCtr", baseline.ctr());
        deltas.put("postCtr", post.ctr());
        deltas.put("ctrDelta", post.ctr().subtract(baseline.ctr()));
        deltas.put("baselineConversionRate", baseline.conversionRate());
        deltas.put("postConversionRate", post.conversionRate());
        deltas.put("conversionRateDelta", post.conversionRate().subtract(baseline.conversionRate()));
        deltas.put("baselineRoas", baseline.roas());
        deltas.put("postRoas", post.roas());
        deltas.put("roasDelta", post.roas().subtract(baseline.roas()));
        deltas.put("baselineAveragePosition", baseline.averagePosition());
        deltas.put("postAveragePosition", post.averagePosition());
        deltas.put("averagePositionDelta", post.averagePosition().subtract(baseline.averagePosition()));
        return deltas;
    }

    private void indexedStateDeltas(Long tenantId,
                                    SearchMarketingImpactWindowDO window,
                                    Map<String, Object> deltas) {
        List<SearchMarketingUrlInspectionDO> inspections = safeList(urlInspectionMapper.selectList(
                new LambdaQueryWrapper<SearchMarketingUrlInspectionDO>()
                        .eq(SearchMarketingUrlInspectionDO::getTenantId, tenantId)
                        .eq(SearchMarketingUrlInspectionDO::getSourceId, window.getSourceId())
                        .eq(SearchMarketingUrlInspectionDO::getPageUrlHash, window.getPageUrlHash())
                        .ge(SearchMarketingUrlInspectionDO::getInspectionDate, window.getBaselineStartDate())
                        .le(SearchMarketingUrlInspectionDO::getInspectionDate, window.getPostEndDate())));
        latestIndexedState(tenantId, window, inspections, window.getBaselineStartDate(), window.getBaselineEndDate())
                .ifPresent(value -> deltas.put("baselineIndexedState", value));
        latestIndexedState(tenantId, window, inspections, window.getPostStartDate(), window.getPostEndDate())
                .ifPresent(value -> deltas.put("postIndexedState", value));
    }

    private java.util.Optional<String> latestIndexedState(Long tenantId,
                                                          SearchMarketingImpactWindowDO window,
                                                          List<SearchMarketingUrlInspectionDO> inspections,
                                                          LocalDate startDate,
                                                          LocalDate endDate) {
        return inspections.stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> Objects.equals(window.getSourceId(), row.getSourceId()))
                .filter(row -> Objects.equals(window.getPageUrlHash(), row.getPageUrlHash()))
                .filter(row -> row.getInspectionDate() != null)
                .filter(row -> !row.getInspectionDate().isBefore(startDate))
                .filter(row -> !row.getInspectionDate().isAfter(endDate))
                .max(Comparator.comparing(SearchMarketingUrlInspectionDO::getInspectionDate))
                .map(SearchMarketingUrlInspectionDO::getIndexedState)
                .map(this::normalize);
    }

    private String decision(MetricAggregate baseline,
                            MetricAggregate post,
                            Map<String, Object> deltas) {
        if (indexedStateRegressed(deltas)
                || (increasedByAtLeast(post.cost(), baseline.cost(), TWENTY_PERCENT)
                && post.conversions() == 0)) {
            return "ROLLBACK_REQUIRED";
        }
        if (worsenedByAtLeast(post.ctr(), baseline.ctr(), TEN_PERCENT)
                || worsenedByAtLeast(post.conversionRate(), baseline.conversionRate(), TEN_PERCENT)
                || worsenedByAtLeast(post.roas(), baseline.roas(), TEN_PERCENT)) {
            return "IMPACT_NEGATIVE";
        }
        boolean costAllowed = !increasedByAtLeast(post.cost(), baseline.cost(), TWENTY_PERCENT);
        if (costAllowed
                && (post.roas().compareTo(baseline.roas()) > 0
                || post.conversionRate().compareTo(baseline.conversionRate()) > 0)) {
            return "IMPACT_POSITIVE";
        }
        return "IMPACT_NEUTRAL";
    }

    private boolean indexedStateRegressed(Map<String, Object> deltas) {
        String baseline = normalize(stringValue(deltas.get("baselineIndexedState")));
        String post = normalize(stringValue(deltas.get("postIndexedState")));
        return indexed(baseline) && !indexed(post);
    }

    private boolean indexed(String value) {
        return "INDEXED".equals(value) || "PASS".equals(value) || "VERDICT_PASS".equals(value);
    }

    private boolean increasedByAtLeast(BigDecimal post, BigDecimal baseline, BigDecimal threshold) {
        if (baseline.compareTo(ZERO) <= 0) {
            return post.compareTo(ZERO) > 0;
        }
        return post.subtract(baseline).compareTo(baseline.multiply(threshold)) >= 0;
    }

    private boolean worsenedByAtLeast(BigDecimal post, BigDecimal baseline, BigDecimal threshold) {
        if (baseline.compareTo(ZERO) <= 0) {
            return false;
        }
        return baseline.subtract(post).compareTo(baseline.multiply(threshold)) >= 0;
    }

    private BigDecimal confidence(String decision, MetricAggregate baseline, MetricAggregate post) {
        if (baseline.snapshotCount() == 0 || post.snapshotCount() == 0) {
            return new BigDecimal("0.3000");
        }
        return "IMPACT_NEUTRAL".equals(decision) ? new BigDecimal("0.6000") : new BigDecimal("0.8000");
    }

    private Map<String, Object> evaluationEvidence(SearchMarketingImpactWindowDO row,
                                                   String decision,
                                                   String actor) {
        Map<String, Object> evidence = new LinkedHashMap<>(map(row.getEvidenceJson()));
        evidence.put("decision", decision);
        evidence.put("evaluatedBy", defaultString(actor, "system"));
        evidence.put("evaluatedAt", now().toString());
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    private void updateOpportunity(Long tenantId,
                                   SearchMarketingImpactWindowDO window,
                                   String decision,
                                   String actor,
                                   LocalDateTime changedAt) {
        if (window.getOpportunityId() == null) {
            return;
        }
        SearchMarketingOpportunityDO opportunity = opportunityMapper.selectById(window.getOpportunityId());
        if (opportunity == null || !tenantId.equals(opportunity.getTenantId())) {
            return;
        }
        Map<String, Object> evidence = new LinkedHashMap<>(map(opportunity.getEvidenceJson()));
        evidence.put("impactWindowId", window.getId());
        evidence.put("impactDecision", decision);
        evidence.put("impactEvaluatedBy", defaultString(actor, "system"));
        evidence.put("impactEvaluatedAt", changedAt.toString());
        opportunity.setStatus(decision);
        opportunity.setEvidenceJson(json(ProviderWriteEvidenceSanitizer.sanitizeMap(evidence)));
        opportunity.setUpdatedAt(changedAt);
        opportunityMapper.updateById(opportunity);
    }

    private SearchMarketingMutationDO mutation(Long tenantId, Long mutationId) {
        SearchMarketingMutationDO row = mutationMapper.selectById(requiredId(mutationId, "mutationId"));
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("mutation does not belong to current tenant");
        }
        return row;
    }

    private SearchMarketingOpportunityDO opportunity(Long tenantId, Long opportunityId) {
        SearchMarketingOpportunityDO row = opportunityMapper.selectById(requiredId(opportunityId, "opportunityId"));
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("opportunity does not belong to current tenant");
        }
        return row;
    }

    private SearchMarketingImpactWindowView toView(SearchMarketingImpactWindowDO row) {
        return new SearchMarketingImpactWindowView(
                row.getId(),
                row.getTenantId(),
                row.getOpportunityId(),
                row.getMutationId(),
                row.getSourceId(),
                row.getKeywordId(),
                row.getPageUrlHash(),
                row.getBaselineStartDate(),
                row.getBaselineEndDate(),
                row.getPostStartDate(),
                row.getPostEndDate(),
                row.getStatus(),
                row.getDecision(),
                row.getConfidence(),
                map(row.getMetricDeltaJson()),
                map(row.getEvidenceJson()),
                row.getDueAt(),
                row.getEvaluatedAt(),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private LocalDate executedDate(SearchMarketingMutationDO mutation) {
        return mutation.getExecutedAt() == null ? LocalDate.now(clock) : mutation.getExecutedAt().toLocalDate();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeOptional(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    private long nonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(ZERO) == 0) {
            return ZERO;
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("search marketing impact evidence is not JSON serializable", ex);
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

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private record MetricAggregate(
            int snapshotCount,
            long impressions,
            long clicks,
            BigDecimal cost,
            long conversions,
            BigDecimal revenue,
            BigDecimal ctr,
            BigDecimal conversionRate,
            BigDecimal roas,
            BigDecimal averagePosition) {

        static MetricAggregate empty() {
            return new MetricAggregate(0, 0, 0, ZERO, 0, ZERO, ZERO, ZERO, ZERO, ZERO);
        }
    }

    private static final class SetLike {

        private SetLike() {
        }

        private static boolean contains(String value, String... candidates) {
            String normalized = value == null ? "" : value.toUpperCase(Locale.ROOT);
            for (String candidate : candidates) {
                if (candidate.equals(normalized)) {
                    return true;
                }
            }
            return false;
        }
    }
}
