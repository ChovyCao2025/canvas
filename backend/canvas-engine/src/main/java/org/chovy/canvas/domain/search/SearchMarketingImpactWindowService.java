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

/**
 * SearchMarketingImpactWindowService 编排 domain.search 场景的领域业务规则。
 */
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

    /**
     * 创建 SearchMarketingImpactWindowService 实例并注入 domain.search 场景依赖。
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param opportunityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param impactWindowMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param urlInspectionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 查询或读取业务数据。
     *
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param opportunityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param impactWindowMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param urlInspectionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 执行业务操作 scheduleForReconciledMutation，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param mutationId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
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
                /**
                 * 执行核心业务处理流程。
                 *
                 * @return 返回流程执行后的业务结果。
                 */
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

    /**
     * 执行业务操作 evaluateDue，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingImpactWindowView> evaluateDue(Long tenantId, int limit, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int normalizedLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 100));
        LocalDateTime evaluatedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(impactWindowMapper.selectList(new LambdaQueryWrapper<SearchMarketingImpactWindowDO>()
                .eq(SearchMarketingImpactWindowDO::getTenantId, scopedTenantId)
                .in(SearchMarketingImpactWindowDO::getStatus, "SCHEDULED", "DUE")
                .le(SearchMarketingImpactWindowDO::getDueAt, evaluatedAt)
                .orderByAsc(SearchMarketingImpactWindowDO::getDueAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .last("LIMIT " + normalizedLimit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> SetLike.contains(row.getStatus(), "SCHEDULED", "DUE"))
                .filter(row -> row.getDueAt() != null && !row.getDueAt().isAfter(evaluatedAt))
                .limit(normalizedLimit)
                .map(row -> evaluate(scopedTenantId, row, actor))
                .toList();
    }

    /**
     * 查询业务列表，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 list 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingImpactWindowView> list(Long tenantId, SearchMarketingImpactWindowQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long opportunityId = query == null ? null : query.opportunityId();
        Long mutationId = query == null ? null : query.mutationId();
        Long sourceId = query == null ? null : query.sourceId();
        String status = query == null ? null : normalizeOptional(query.status());
        String decision = query == null ? null : normalizeOptional(query.decision());
        int limit = Math.max(1, Math.min(query == null || query.limit() == null ? 50 : query.limit(), 100));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(impactWindowMapper.selectList(new LambdaQueryWrapper<SearchMarketingImpactWindowDO>()
                .eq(SearchMarketingImpactWindowDO::getTenantId, scopedTenantId)
                .eq(opportunityId != null, SearchMarketingImpactWindowDO::getOpportunityId, opportunityId)
                .eq(mutationId != null, SearchMarketingImpactWindowDO::getMutationId, mutationId)
                .eq(sourceId != null, SearchMarketingImpactWindowDO::getSourceId, sourceId)
                .eq(status != null, SearchMarketingImpactWindowDO::getStatus, status)
                .eq(decision != null, SearchMarketingImpactWindowDO::getDecision, decision)
                .orderByDesc(SearchMarketingImpactWindowDO::getUpdatedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    private SearchMarketingImpactWindowView evaluate(Long tenantId,
                                                     SearchMarketingImpactWindowDO row,
                                                     String actor) {
        MetricAggregate baseline = aggregateSnapshots(tenantId, row, row.getBaselineStartDate(),
                row.getBaselineEndDate());
        MetricAggregate post = aggregateSnapshots(tenantId, row, row.getPostStartDate(), row.getPostEndDate());
        Map<String, Object> deltas = deltas(baseline, post);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(changedAt);
        impactWindowMapper.updateById(row);
        updateOpportunity(tenantId, row, decision, actor, changedAt);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param window window 参数，用于 aggregateSnapshots 流程中的校验、计算或对象转换。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 aggregateSnapshots 流程生成的业务结果。
     */
    private MetricAggregate aggregateSnapshots(Long tenantId,
                                               SearchMarketingImpactWindowDO window,
                                               LocalDate startDate,
                                               LocalDate endDate) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (startDate == null || endDate == null) {
            return MetricAggregate.empty();
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 执行 deltas 流程，围绕 deltas 完成校验、计算或结果组装。
     *
     * @param baseline baseline 参数，用于 deltas 流程中的校验、计算或对象转换。
     * @param post post 参数，用于 deltas 流程中的校验、计算或对象转换。
     * @return 返回 deltas 流程生成的业务结果。
     */
    private Map<String, Object> deltas(MetricAggregate baseline, MetricAggregate post) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return deltas;
    }

    /**
     * 执行 indexedStateDeltas 流程，围绕 indexed state deltas 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param window window 参数，用于 indexedStateDeltas 流程中的校验、计算或对象转换。
     * @param deltas deltas 参数，用于 indexedStateDeltas 流程中的校验、计算或对象转换。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param window window 参数，用于 latestIndexedState 流程中的校验、计算或对象转换。
     * @param inspections inspections 参数，用于 latestIndexedState 流程中的校验、计算或对象转换。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 latest indexed state 生成的文本或业务键。
     */
    private java.util.Optional<String> latestIndexedState(Long tenantId,
                                                          SearchMarketingImpactWindowDO window,
                                                          List<SearchMarketingUrlInspectionDO> inspections,
                                                          LocalDate startDate,
                                                          LocalDate endDate) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 执行 decision 流程，围绕 decision 完成校验、计算或结果组装。
     *
     * @param baseline baseline 参数，用于 decision 流程中的校验、计算或对象转换。
     * @param post post 参数，用于 decision 流程中的校验、计算或对象转换。
     * @param deltas deltas 参数，用于 decision 流程中的校验、计算或对象转换。
     * @return 返回 decision 生成的文本或业务键。
     */
    private String decision(MetricAggregate baseline,
                            MetricAggregate post,
                            Map<String, Object> deltas) {
        if (indexedStateRegressed(deltas)
                || (increasedByAtLeast(post.cost(), baseline.cost(), TWENTY_PERCENT)
                && post.conversions() == 0)) {
            return "ROLLBACK_REQUIRED";
        }
        if (worsenedByAtLeast(post.ctr(), baseline.ctr(), TEN_PERCENT)
                /**
                 * 执行 worsenedByAtLeast 流程，围绕 worsened by at least 完成校验、计算或结果组装。
                 *
                 * @return 返回 worsenedByAtLeast 流程生成的业务结果。
                 */
                || worsenedByAtLeast(post.conversionRate(), baseline.conversionRate(), TEN_PERCENT)
                /**
                 * 执行 worsenedByAtLeast 流程，围绕 worsened by at least 完成校验、计算或结果组装。
                 *
                 * @return 返回 worsenedByAtLeast 流程生成的业务结果。
                 */
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

    /**
     * 执行 indexedStateRegressed 流程，围绕 indexed state regressed 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 indexedStateRegressed 流程中的校验、计算或对象转换。
     * @param deltas deltas 参数，用于 indexedStateRegressed 流程中的校验、计算或对象转换。
     * @return 返回 indexed state regressed 的布尔判断结果。
     */
    private boolean indexedStateRegressed(Map<String, Object> deltas) {
        String baseline = normalize(stringValue(deltas.get("baselineIndexedState")));
        String post = normalize(stringValue(deltas.get("postIndexedState")));
        return indexed(baseline) && !indexed(post);
    }

    /**
     * 执行 indexed 流程，围绕 indexed 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 indexed 的布尔判断结果。
     */
    private boolean indexed(String value) {
        return "INDEXED".equals(value) || "PASS".equals(value) || "VERDICT_PASS".equals(value);
    }

    /**
     * 执行 increasedByAtLeast 流程，围绕 increased by at least 完成校验、计算或结果组装。
     *
     * @param post post 参数，用于 increasedByAtLeast 流程中的校验、计算或对象转换。
     * @param baseline baseline 参数，用于 increasedByAtLeast 流程中的校验、计算或对象转换。
     * @param threshold threshold 参数，用于 increasedByAtLeast 流程中的校验、计算或对象转换。
     * @return 返回 increased by at least 的布尔判断结果。
     */
    private boolean increasedByAtLeast(BigDecimal post, BigDecimal baseline, BigDecimal threshold) {
        if (baseline.compareTo(ZERO) <= 0) {
            return post.compareTo(ZERO) > 0;
        }
        return post.subtract(baseline).compareTo(baseline.multiply(threshold)) >= 0;
    }

    /**
     * 执行 worsenedByAtLeast 流程，围绕 worsened by at least 完成校验、计算或结果组装。
     *
     * @param post post 参数，用于 worsenedByAtLeast 流程中的校验、计算或对象转换。
     * @param baseline baseline 参数，用于 worsenedByAtLeast 流程中的校验、计算或对象转换。
     * @param threshold threshold 参数，用于 worsenedByAtLeast 流程中的校验、计算或对象转换。
     * @return 返回 worsened by at least 的布尔判断结果。
     */
    private boolean worsenedByAtLeast(BigDecimal post, BigDecimal baseline, BigDecimal threshold) {
        if (baseline.compareTo(ZERO) <= 0) {
            return false;
        }
        return baseline.subtract(post).compareTo(baseline.multiply(threshold)) >= 0;
    }

    /**
     * 执行 confidence 流程，围绕 confidence 完成校验、计算或结果组装。
     *
     * @param decision decision 参数，用于 confidence 流程中的校验、计算或对象转换。
     * @param baseline baseline 参数，用于 confidence 流程中的校验、计算或对象转换。
     * @param post post 参数，用于 confidence 流程中的校验、计算或对象转换。
     * @return 返回 confidence 计算得到的数量、金额或指标值。
     */
    private BigDecimal confidence(String decision, MetricAggregate baseline, MetricAggregate post) {
        if (baseline.snapshotCount() == 0 || post.snapshotCount() == 0) {
            return new BigDecimal("0.3000");
        }
        return "IMPACT_NEUTRAL".equals(decision) ? new BigDecimal("0.6000") : new BigDecimal("0.8000");
    }

    /**
     * 执行 evaluationEvidence 流程，围绕 evaluation evidence 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param decision decision 参数，用于 evaluationEvidence 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 evaluationEvidence 流程生成的业务结果。
     */
    private Map<String, Object> evaluationEvidence(SearchMarketingImpactWindowDO row,
                                                   String decision,
                                                   String actor) {
        Map<String, Object> evidence = new LinkedHashMap<>(map(row.getEvidenceJson()));
        evidence.put("decision", decision);
        evidence.put("evaluatedBy", defaultString(actor, "system"));
        evidence.put("evaluatedAt", now().toString());
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param window window 参数，用于 updateOpportunity 流程中的校验、计算或对象转换。
     * @param decision decision 参数，用于 updateOpportunity 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param changedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void updateOpportunity(Long tenantId,
                                   SearchMarketingImpactWindowDO window,
                                   String decision,
                                   String actor,
                                   LocalDateTime changedAt) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (window.getOpportunityId() == null) {
            return;
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SearchMarketingOpportunityDO opportunity = opportunityMapper.selectById(window.getOpportunityId());
        if (opportunity == null || !tenantId.equals(opportunity.getTenantId())) {
            // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 执行 mutation 流程，围绕 mutation 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mutationId 业务对象 ID，用于定位具体记录。
     * @return 返回 mutation 流程生成的业务结果。
     */
    private SearchMarketingMutationDO mutation(Long tenantId, Long mutationId) {
        SearchMarketingMutationDO row = mutationMapper.selectById(requiredId(mutationId, "mutationId"));
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("mutation does not belong to current tenant");
        }
        return row;
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
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("opportunity does not belong to current tenant");
        }
        return row;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SearchMarketingImpactWindowView toView(SearchMarketingImpactWindowDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param mutation mutation 参数，用于 executedDate 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private LocalDate executedDate(SearchMarketingMutationDO mutation) {
        return mutation.getExecutedAt() == null ? LocalDate.now(clock) : mutation.getExecutedAt().toLocalDate();
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
     * 执行 stringValue 流程，围绕 string value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string value 生成的文本或业务键。
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptional(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
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
     * 执行 nonNegative 流程，围绕 non negative 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 non negative 计算得到的数量、金额或指标值。
     */
    private long nonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    /**
     * 执行 money 流程，围绕 money 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 money 计算得到的数量、金额或指标值。
     */
    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    /**
     * 执行 divide 流程，围绕 divide 完成校验、计算或结果组装。
     *
     * @param numerator numerator 参数，用于 divide 流程中的校验、计算或对象转换。
     * @param denominator denominator 参数，用于 divide 流程中的校验、计算或对象转换。
     * @return 返回 divide 计算得到的数量、金额或指标值。
     */
    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(ZERO) == 0) {
            return ZERO;
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP);
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
            throw new IllegalArgumentException("search marketing impact evidence is not JSON serializable", ex);
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

    /**
     * MetricAggregate 数据记录。
     */
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

        /**
         * 执行 empty 流程，围绕 empty 完成校验、计算或结果组装。
         *
         * @return 返回 empty 流程生成的业务结果。
         */
        static MetricAggregate empty() {
            return new MetricAggregate(0, 0, 0, ZERO, 0, ZERO, ZERO, ZERO, ZERO, ZERO);
        }
    }

    /**
     * SetLike 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class SetLike {

        /**
         * 执行 SetLike 流程，围绕 set like 完成校验、计算或结果组装。
         *
         * @return 返回 SetLike 流程生成的业务结果。
         */
        private SetLike() {
        }

        /**
         * 判断业务条件是否成立。
         *
         * @param value 待处理值，用于规则计算或转换。
         * @param candidates 时间参数，用于计算窗口、过期或审计时间。
         * @return 返回 contains 的布尔判断结果。
         */
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
