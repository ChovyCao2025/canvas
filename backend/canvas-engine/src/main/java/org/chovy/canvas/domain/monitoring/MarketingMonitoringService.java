package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.MarketingCompetitorMentionDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorItemDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.dataobject.MarketingSentimentAnalysisDO;
import org.chovy.canvas.dal.mapper.MarketingCompetitorMentionMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorItemMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.dal.mapper.MarketingSentimentAnalysisMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * MarketingMonitoringService 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
@Slf4j
public class MarketingMonitoringService {

    public static final String SENTIMENT_MODEL_KEY = "MARKETING_MONITOR_LEXICON";
    private static final String SENTIMENT_MODEL_VERSION = "lexicon_v1";
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final List<String> NEGATIVE_TERMS = List.of(
            "churn", "bad", "slow", "broken", "complaint", "angry", "fail", "worse", "refund", "poor");
    private static final List<String> POSITIVE_TERMS = List.of(
            "great", "good", "fast", "love", "better", "excellent", "happy", "smooth", "win", "recommend");

    private final MarketingMonitorSourceMapper sourceMapper;
    private final MarketingMonitorItemMapper itemMapper;
    private final MarketingSentimentAnalysisMapper sentimentMapper;
    private final MarketingCompetitorMentionMapper competitorMapper;
    private final MarketingMonitorAlertMapper alertMapper;
    private final MarketingMonitorAlertFanoutService alertFanoutService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 MarketingMonitoringService 实例并注入 domain.monitoring 场景依赖。
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param itemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param sentimentMapper 时间参数，用于计算窗口、过期或审计时间。
     * @param competitorMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertFanoutProvider alert fanout provider 参数，用于 MarketingMonitoringService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public MarketingMonitoringService(MarketingMonitorSourceMapper sourceMapper,
                                      MarketingMonitorItemMapper itemMapper,
                                      MarketingSentimentAnalysisMapper sentimentMapper,
                                      MarketingCompetitorMentionMapper competitorMapper,
                                      MarketingMonitorAlertMapper alertMapper,
                                      ObjectMapper objectMapper,
                                      ObjectProvider<MarketingMonitorAlertFanoutService> alertFanoutProvider) {
        this(sourceMapper, itemMapper, sentimentMapper, competitorMapper, alertMapper,
                objectMapper, Clock.systemDefaultZone(),
                alertFanoutProvider == null ? null : alertFanoutProvider.getIfAvailable());
    }

    /**
     * 执行 MarketingMonitoringService 流程，围绕 marketing monitoring service 完成校验、计算或结果组装。
     *
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param itemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param sentimentMapper 时间参数，用于计算窗口、过期或审计时间。
     * @param competitorMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingMonitoringService(MarketingMonitorSourceMapper sourceMapper,
                               MarketingMonitorItemMapper itemMapper,
                               MarketingSentimentAnalysisMapper sentimentMapper,
                               MarketingCompetitorMentionMapper competitorMapper,
                               MarketingMonitorAlertMapper alertMapper,
                               ObjectMapper objectMapper,
                               Clock clock) {
        this(sourceMapper, itemMapper, sentimentMapper, competitorMapper, alertMapper, objectMapper, clock, null);
    }

    /**
     * 执行 MarketingMonitoringService 流程，围绕 marketing monitoring service 完成校验、计算或结果组装。
     *
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param itemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param sentimentMapper 时间参数，用于计算窗口、过期或审计时间。
     * @param competitorMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param alertFanoutService 依赖组件，用于完成数据访问或外部能力调用。
     */
    MarketingMonitoringService(MarketingMonitorSourceMapper sourceMapper,
                               MarketingMonitorItemMapper itemMapper,
                               MarketingSentimentAnalysisMapper sentimentMapper,
                               MarketingCompetitorMentionMapper competitorMapper,
                               MarketingMonitorAlertMapper alertMapper,
                               ObjectMapper objectMapper,
                               Clock clock,
                               MarketingMonitorAlertFanoutService alertFanoutService) {
        this.sourceMapper = sourceMapper;
        this.itemMapper = itemMapper;
        this.sentimentMapper = sentimentMapper;
        this.competitorMapper = competitorMapper;
        this.alertMapper = alertMapper;
        this.alertFanoutService = alertFanoutService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建或更新业务记录，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingMonitorSourceView upsertSource(Long tenantId,
                                                   MarketingMonitorSourceCommand command,
                                                   String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("marketing monitor source command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String sourceKey = normalizeKey(command.sourceKey(), "sourceKey");
        String sourceType = normalizeUpper(command.sourceType(), "sourceType");
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorSourceDO row = sourceMapper.selectOne(
                new LambdaQueryWrapper<MarketingMonitorSourceDO>()
                        .eq(MarketingMonitorSourceDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorSourceDO::getSourceKey, sourceKey)
                        .last("LIMIT 1"));
        if (row == null) {
            row = new MarketingMonitorSourceDO();
            row.setTenantId(scopedTenantId);
            row.setSourceKey(sourceKey);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setSourceType(sourceType);
        row.setDisplayName(defaultString(command.displayName(), sourceKey));
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
     * 执行业务操作 ingestItem，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回本次处理的状态、计数、命中明细或治理结论，供控制器和调度任务判断后续动作
     */
    public MarketingMonitorIngestResult ingestItem(Long tenantId,
                                                   MarketingMonitorItemIngestCommand command,
                                                   String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("marketing monitor item command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorSourceDO source = sourceMapper.selectById(requiredId(command.sourceId(), "sourceId"));
        validateSource(scopedTenantId, source);
        String externalItemId = required(command.externalItemId(), "externalItemId");
        MarketingMonitorItemDO existing = itemMapper.selectOne(
                new LambdaQueryWrapper<MarketingMonitorItemDO>()
                        .eq(MarketingMonitorItemDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorItemDO::getSourceId, source.getId())
                        .eq(MarketingMonitorItemDO::getExternalItemId, externalItemId)
                        .last("LIMIT 1"));
        if (existing != null && scopedTenantId.equals(existing.getTenantId())) {
            return existingItemResult(scopedTenantId, existing);
        }
        LocalDateTime ingestedAt = now();
        MarketingMonitorItemDO item = new MarketingMonitorItemDO();
        item.setTenantId(scopedTenantId);
        item.setSourceId(source.getId());
        item.setExternalItemId(externalItemId);
        item.setSourceType(source.getSourceType());
        item.setSourceUrl(trimToNull(command.sourceUrl()));
        item.setAuthorKey(trimToNull(command.authorKey()));
        item.setBrandKey(trimToNull(command.brandKey()));
        item.setTextContent(required(command.text(), "text"));
        item.setLanguage(trimToNull(command.language()));
        item.setPublishedAt(command.publishedAt());
        item.setIngestedAt(ingestedAt);
        item.setRawPayloadJson(json(command.rawPayload()));
        item.setCreatedAt(ingestedAt);
        item.setUpdatedAt(ingestedAt);
        itemMapper.insert(item);

        Sentiment sentiment = analyze(item.getTextContent());
        MarketingSentimentAnalysisDO sentimentRow = insertSentiment(scopedTenantId, item, sentiment, ingestedAt);
        List<MarketingCompetitorMentionDO> mentions = insertCompetitorMentions(
                scopedTenantId, item, command.competitors(), sentiment, ingestedAt);
        List<MarketingMonitorAlertDO> alerts = insertAlerts(scopedTenantId, item, sentiment, mentions, actor,
                ingestedAt);
        fanoutAlerts(scopedTenantId, alerts, actor);
        return new MarketingMonitorIngestResult(
                toItemView(item, sentimentRow, mentions),
                toSentimentView(sentimentRow),
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                mentions.stream().map(this::toCompetitorView).toList(),
                alerts.stream().map(this::toAlertView).toList());
    }

    /**
     * 执行业务操作 items，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 items 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingMonitorItemView> items(Long tenantId, MarketingMonitorItemQuery query) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (query == null) {
            throw new IllegalArgumentException("marketing monitor item query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int limit = boundedLimit(query.limit());
        String sentimentLabel = normalizeOptionalUpper(query.sentimentLabel());
        String competitorKey = normalizeOptionalKey(query.competitorKey());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<MarketingMonitorItemDO> rows = safeList(itemMapper.selectList(
                new LambdaQueryWrapper<MarketingMonitorItemDO>()
                        .eq(MarketingMonitorItemDO::getTenantId, scopedTenantId)
                        .orderByDesc(MarketingMonitorItemDO::getIngestedAt)
                        .last("LIMIT " + limit)));
        Map<Long, MarketingSentimentAnalysisDO> sentiments = sentimentByItem(scopedTenantId);
        Map<Long, List<MarketingCompetitorMentionDO>> competitors = competitorsByItem(scopedTenantId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return rows.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> matchesSentiment(row, sentiments, sentimentLabel))
                .filter(row -> matchesCompetitor(row, competitors, competitorKey))
                .limit(limit)
                .map(row -> toItemView(row, sentiments.get(row.getId()),
                        competitors.getOrDefault(row.getId(), List.of())))
                .toList();
    }

    /**
     * 执行业务操作 alerts，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 alerts 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingMonitorAlertView> alerts(Long tenantId, MarketingMonitorAlertQuery query) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (query == null) {
            throw new IllegalArgumentException("marketing monitor alert query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int limit = boundedLimit(query.limit());
        String status = normalizeOptionalUpper(query.status());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(alertMapper.selectList(
                new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                        .eq(MarketingMonitorAlertDO::getTenantId, scopedTenantId)
                        .eq(status != null, MarketingMonitorAlertDO::getStatus, status)
                        .orderByDesc(MarketingMonitorAlertDO::getCreatedAt)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toAlertView)
                .toList();
    }

    /**
     * 关闭或解析业务异常，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param alertId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingMonitorAlertView resolveAlert(Long tenantId, Long alertId, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorAlertDO row = alertMapper.selectById(requiredId(alertId, "alertId"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("alert is not found");
        }
        LocalDateTime resolvedAt = now();
        row.setStatus("RESOLVED");
        row.setResolvedBy(defaultString(actor, "system"));
        row.setResolvedAt(resolvedAt);
        row.setUpdatedAt(resolvedAt);
        alertMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toAlertView(row);
    }

    /**
     * 执行 existingItemResult 流程，围绕 existing item result 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param item item 参数，用于 existingItemResult 流程中的校验、计算或对象转换。
     * @return 返回 existingItemResult 流程生成的业务结果。
     */
    private MarketingMonitorIngestResult existingItemResult(Long tenantId, MarketingMonitorItemDO item) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingSentimentAnalysisDO sentiment = sentimentMapper.selectOne(
                new LambdaQueryWrapper<MarketingSentimentAnalysisDO>()
                        .eq(MarketingSentimentAnalysisDO::getTenantId, tenantId)
                        .eq(MarketingSentimentAnalysisDO::getItemId, item.getId())
                        .last("LIMIT 1"));
        List<MarketingCompetitorMentionDO> mentions = safeList(competitorMapper.selectList(
                new LambdaQueryWrapper<MarketingCompetitorMentionDO>()
                        .eq(MarketingCompetitorMentionDO::getTenantId, tenantId)
                        .eq(MarketingCompetitorMentionDO::getItemId, item.getId())));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorIngestResult(
                toItemView(item, sentiment, mentions),
                sentiment == null ? null : toSentimentView(sentiment),
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                mentions.stream()
                        .filter(row -> tenantId.equals(row.getTenantId()))
                        .map(this::toCompetitorView)
                        .toList(),
                List.of());
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param item item 参数，用于 insertSentiment 流程中的校验、计算或对象转换。
     * @param sentiment 时间参数，用于计算窗口、过期或审计时间。
     * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 insertSentiment 流程生成的业务结果。
     */
    private MarketingSentimentAnalysisDO insertSentiment(Long tenantId,
                                                         MarketingMonitorItemDO item,
                                                         Sentiment sentiment,
                                                         LocalDateTime createdAt) {
        MarketingSentimentAnalysisDO row = new MarketingSentimentAnalysisDO();
        row.setTenantId(tenantId);
        row.setItemId(item.getId());
        row.setSentimentLabel(sentiment.label());
        row.setSentimentScore(sentiment.score());
        row.setConfidence(sentiment.confidence());
        row.setModelKey(SENTIMENT_MODEL_KEY);
        row.setModelVersion(SENTIMENT_MODEL_VERSION);
        row.setKeywordHitsJson(json(sentiment.keywordHits()));
        row.setCreatedAt(createdAt);
        sentimentMapper.insert(row);
        return row;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param item item 参数，用于 insertCompetitorMentions 流程中的校验、计算或对象转换。
     * @param competitors competitors 参数，用于 insertCompetitorMentions 流程中的校验、计算或对象转换。
     * @param sentiment 时间参数，用于计算窗口、过期或审计时间。
     * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 insert competitor mentions 汇总后的集合、分页或映射视图。
     */
    private List<MarketingCompetitorMentionDO> insertCompetitorMentions(Long tenantId,
                                                                        MarketingMonitorItemDO item,
                                                                        Map<String, List<String>> competitors,
                                                                        Sentiment sentiment,
                                                                        LocalDateTime createdAt) {
        List<MarketingCompetitorMentionDO> rows = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map.Entry<String, List<String>> entry : safeCompetitors(competitors).entrySet()) {
            String competitorKey = normalizeOptionalKey(entry.getKey());
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (competitorKey == null) {
                continue;
            }
            List<String> matchedTerms = matchedTerms(item.getTextContent(), entry.getValue());
            if (matchedTerms.isEmpty()) {
                continue;
            }
            MarketingCompetitorMentionDO row = new MarketingCompetitorMentionDO();
            row.setTenantId(tenantId);
            row.setItemId(item.getId());
            row.setCompetitorKey(competitorKey);
            row.setCompetitorName(matchedTerms.getFirst());
            row.setMatchedTermsJson(json(matchedTerms));
            row.setSentimentLabel(sentiment.label());
            row.setSentimentScore(sentiment.score());
            row.setCreatedAt(createdAt);
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            competitorMapper.insert(row);
            rows.add(row);
        }
        return rows;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param item item 参数，用于 insertAlerts 流程中的校验、计算或对象转换。
     * @param sentiment 时间参数，用于计算窗口、过期或审计时间。
     * @param mentions mentions 参数，用于 insertAlerts 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 insert alerts 汇总后的集合、分页或映射视图。
     */
    private List<MarketingMonitorAlertDO> insertAlerts(Long tenantId,
                                                       MarketingMonitorItemDO item,
                                                       Sentiment sentiment,
                                                       List<MarketingCompetitorMentionDO> mentions,
                                                       String actor,
                                                       LocalDateTime createdAt) {
        if (!"NEGATIVE".equals(sentiment.label())) {
            return List.of();
        }
        List<MarketingMonitorAlertDO> rows = new ArrayList<>();
        rows.add(insertAlert(tenantId, "NEGATIVE_SENTIMENT", "HIGH", item.getBrandKey(),
                "Negative sentiment detected",
                "Detected negative sentiment on monitored item " + item.getExternalItemId(),
                item, Map.of(
                        "itemId", item.getId(),
                        "sourceId", item.getSourceId(),
                        "sentimentScore", sentiment.score(),
                        "keywordHits", sentiment.keywordHits()),
                actor, createdAt));
        for (MarketingCompetitorMentionDO mention : mentions) {
            rows.add(insertAlert(tenantId, "COMPETITOR_NEGATIVE", "HIGH", mention.getCompetitorKey(),
                    "Negative competitor mention detected",
                    "Detected negative sentiment mentioning " + mention.getCompetitorName(),
                    item, Map.of(
                            "itemId", item.getId(),
                            "competitorKey", mention.getCompetitorKey(),
                            /**
                             * 查询或读取业务数据。
                             *
                             * @param actor 操作人标识，用于审计和权限判断。
                             * @return 返回符合条件的数据列表或视图。
                             */
                            "matchedTerms", list(mention.getMatchedTermsJson()),
                            "sentimentScore", sentiment.score()),
                    actor, createdAt));
        }
        return rows;
    }

    /**
     * 执行 fanoutAlerts 流程，围绕 fanout alerts 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param alerts alerts 参数，用于 fanoutAlerts 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void fanoutAlerts(Long tenantId, List<MarketingMonitorAlertDO> alerts, String actor) {
        if (alertFanoutService == null || alerts == null || alerts.isEmpty()) {
            return;
        }
        for (MarketingMonitorAlertDO alert : alerts) {
            try {
                alertFanoutService.dispatchAlert(tenantId, alert, actor);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException ex) {
                log.warn("[MONITORING] alert fanout skipped alert={} error={}", alert.getId(), ex.getMessage());
            }
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param alertType 类型标识，用于选择对应处理分支。
     * @param severity severity 参数，用于 insertAlert 流程中的校验、计算或对象转换。
     * @param scopeKey 业务键，用于在同一租户下定位资源。
     * @param title title 参数，用于 insertAlert 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param item item 参数，用于 insertAlert 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 insertAlert 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 insertAlert 流程生成的业务结果。
     */
    private MarketingMonitorAlertDO insertAlert(Long tenantId,
                                                String alertType,
                                                String severity,
                                                String scopeKey,
                                                String title,
                                                String reason,
                                                MarketingMonitorItemDO item,
                                                Map<String, Object> metadata,
                                                String actor,
                                                LocalDateTime createdAt) {
        // 准备本次处理所需的上下文和中间变量。
        MarketingMonitorAlertDO row = new MarketingMonitorAlertDO();
        row.setTenantId(tenantId);
        row.setAlertType(alertType);
        row.setSeverity(severity);
        row.setStatus("OPEN");
        row.setScopeKey(trimToNull(scopeKey));
        row.setTitle(title);
        row.setReason(reason);
        row.setItemCount(1);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setWindowStart(item.getPublishedAt() == null ? item.getIngestedAt() : item.getPublishedAt());
        row.setWindowEnd(item.getPublishedAt() == null ? item.getIngestedAt() : item.getPublishedAt());
        row.setMetadataJson(json(metadata));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setCreatedAt(createdAt);
        row.setUpdatedAt(createdAt);
        alertMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 执行 analyze 流程，围绕 analyze 完成校验、计算或结果组装。
     *
     * @param text text 参数，用于 analyze 流程中的校验、计算或对象转换。
     * @return 返回 analyze 流程生成的业务结果。
     */
    private Sentiment analyze(String text) {
        List<String> negative = matchedTerms(text, NEGATIVE_TERMS);
        List<String> positive = matchedTerms(text, POSITIVE_TERMS);
        int hits = negative.size() + positive.size();
        BigDecimal score = hits == 0
                /**
                 * 执行 scaled 流程，围绕 scaled 完成校验、计算或结果组装。
                 *
                 * @param hits hits 参数，用于 scaled 流程中的校验、计算或对象转换。
                 * @return 返回 scaled 流程生成的业务结果。
                 */
                ? scaled(0)
                /**
                 * 执行 scaled 流程，围绕 scaled 完成校验、计算或结果组装。
                 *
                 * @param hits hits 参数，用于 scaled 流程中的校验、计算或对象转换。
                 * @return 返回 scaled 流程生成的业务结果。
                 */
                : scaled((positive.size() - negative.size()) / (double) hits);
        String label = score.compareTo(BigDecimal.ZERO) < 0
                ? "NEGATIVE"
                : score.compareTo(BigDecimal.ZERO) > 0 ? "POSITIVE" : "NEUTRAL";
        Map<String, Object> keywordHits = new LinkedHashMap<>();
        keywordHits.put("negative", negative);
        keywordHits.put("positive", positive);
        return new Sentiment(label, score, confidence(text, hits), keywordHits);
    }

    /**
     * 执行 confidence 流程，围绕 confidence 完成校验、计算或结果组装。
     *
     * @param text text 参数，用于 confidence 流程中的校验、计算或对象转换。
     * @param hits hits 参数，用于 confidence 流程中的校验、计算或对象转换。
     * @return 返回 confidence 计算得到的数量、金额或指标值。
     */
    private BigDecimal confidence(String text, int hits) {
        if (!hasText(text)) {
            return scaled(0.2);
        }
        double value = 0.45 + Math.min(0.45, hits * 0.12);
        if (text.trim().length() < 20) {
            value -= 0.15;
        }
        return scaled(Math.max(0.2, Math.min(0.95, value)));
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param text text 参数，用于 matchedTerms 流程中的校验、计算或对象转换。
     * @param terms terms 参数，用于 matchedTerms 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private List<String> matchedTerms(String text, List<String> terms) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(text) || terms == null || terms.isEmpty()) {
            return List.of();
        }
        Set<String> matches = new LinkedHashSet<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String term : terms) {
            if (!hasText(term)) {
                continue;
            }
            String trimmed = term.trim();
            Pattern pattern = Pattern.compile("(?iu)(?<![\\p{Alnum}_])"
                    + Pattern.quote(trimmed)
                    + "(?![\\p{Alnum}_])");
            if (pattern.matcher(text).find()) {
                matches.add(trimmed);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(matches);
    }

    /**
     * 执行 sentimentByItem 流程，围绕 sentiment by item 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 sentimentByItem 流程生成的业务结果。
     */
    private Map<Long, MarketingSentimentAnalysisDO> sentimentByItem(Long tenantId) {
        Map<Long, MarketingSentimentAnalysisDO> rows = new LinkedHashMap<>();
        for (MarketingSentimentAnalysisDO row : safeList(sentimentMapper.selectList(
                new LambdaQueryWrapper<MarketingSentimentAnalysisDO>()
                        .eq(MarketingSentimentAnalysisDO::getTenantId, tenantId)))) {
            if (tenantId.equals(row.getTenantId()) && row.getItemId() != null) {
                rows.putIfAbsent(row.getItemId(), row);
            }
        }
        return rows;
    }

    /**
     * 执行 competitorsByItem 流程，围绕 competitors by item 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 competitors by item 汇总后的集合、分页或映射视图。
     */
    private Map<Long, List<MarketingCompetitorMentionDO>> competitorsByItem(Long tenantId) {
        Map<Long, List<MarketingCompetitorMentionDO>> rows = new LinkedHashMap<>();
        for (MarketingCompetitorMentionDO row : safeList(competitorMapper.selectList(
                new LambdaQueryWrapper<MarketingCompetitorMentionDO>()
                        .eq(MarketingCompetitorMentionDO::getTenantId, tenantId)))) {
            if (tenantId.equals(row.getTenantId()) && row.getItemId() != null) {
                rows.computeIfAbsent(row.getItemId(), ignored -> new ArrayList<>()).add(row);
            }
        }
        return rows;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param item item 参数，用于 matchesSentiment 流程中的校验、计算或对象转换。
     * @param sentiments 时间参数，用于计算窗口、过期或审计时间。
     * @param label label 参数，用于 matchesSentiment 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean matchesSentiment(MarketingMonitorItemDO item,
                                     Map<Long, MarketingSentimentAnalysisDO> sentiments,
                                     String label) {
        if (label == null) {
            return true;
        }
        MarketingSentimentAnalysisDO sentiment = sentiments.get(item.getId());
        return sentiment != null && label.equals(sentiment.getSentimentLabel());
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param item item 参数，用于 matchesCompetitor 流程中的校验、计算或对象转换。
     * @param competitors competitors 参数，用于 matchesCompetitor 流程中的校验、计算或对象转换。
     * @param competitorKey 业务键，用于在同一租户下定位资源。
     * @return 返回布尔判断结果。
     */
    private boolean matchesCompetitor(MarketingMonitorItemDO item,
                                      Map<Long, List<MarketingCompetitorMentionDO>> competitors,
                                      String competitorKey) {
        if (competitorKey == null) {
            return true;
        }
        return competitors.getOrDefault(item.getId(), List.of()).stream()
                .anyMatch(row -> competitorKey.equals(row.getCompetitorKey()));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorSourceView toSourceView(MarketingMonitorSourceDO row) {
        return new MarketingMonitorSourceView(
                row.getId(),
                row.getTenantId(),
                row.getSourceKey(),
                row.getSourceType(),
                row.getDisplayName(),
                enabled(row.getEnabled()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param sentiment 时间参数，用于计算窗口、过期或审计时间。
     * @param competitors competitors 参数，用于 toItemView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorItemView toItemView(MarketingMonitorItemDO row,
                                                MarketingSentimentAnalysisDO sentiment,
                                                List<MarketingCompetitorMentionDO> competitors) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorItemView(
                row.getId(),
                row.getTenantId(),
                row.getSourceId(),
                row.getExternalItemId(),
                row.getSourceType(),
                row.getSourceUrl(),
                row.getAuthorKey(),
                row.getBrandKey(),
                row.getTextContent(),
                row.getLanguage(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getPublishedAt(),
                row.getIngestedAt(),
                map(row.getRawPayloadJson()),
                sentiment == null ? null : sentiment.getSentimentLabel(),
                sentiment == null ? null : sentiment.getSentimentScore(),
                sentiment == null ? null : sentiment.getConfidence(),
                competitorKeys(competitors));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingSentimentAnalysisView toSentimentView(MarketingSentimentAnalysisDO row) {
        return new MarketingSentimentAnalysisView(
                row.getId(),
                row.getTenantId(),
                row.getItemId(),
                row.getSentimentLabel(),
                row.getSentimentScore(),
                row.getConfidence(),
                row.getModelKey(),
                row.getModelVersion(),
                map(row.getKeywordHitsJson()),
                row.getCreatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingCompetitorMentionView toCompetitorView(MarketingCompetitorMentionDO row) {
        return new MarketingCompetitorMentionView(
                row.getId(),
                row.getTenantId(),
                row.getItemId(),
                row.getCompetitorKey(),
                row.getCompetitorName(),
                list(row.getMatchedTermsJson()),
                row.getSentimentLabel(),
                row.getSentimentScore(),
                row.getCreatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorAlertView toAlertView(MarketingMonitorAlertDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorAlertView(
                row.getId(),
                row.getTenantId(),
                row.getAlertType(),
                row.getSeverity(),
                row.getStatus(),
                row.getScopeKey(),
                row.getTitle(),
                row.getReason(),
                defaultInt(row.getItemCount()),
                row.getWindowStart(),
                row.getWindowEnd(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getResolvedBy(),
                row.getResolvedAt(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 执行 competitorKeys 流程，围绕 competitor keys 完成校验、计算或结果组装。
     *
     * @param competitors competitors 参数，用于 competitorKeys 流程中的校验、计算或对象转换。
     * @return 返回 competitor keys 汇总后的集合、分页或映射视图。
     */
    private List<String> competitorKeys(List<MarketingCompetitorMentionDO> competitors) {
        Set<String> keys = new LinkedHashSet<>();
        for (MarketingCompetitorMentionDO row : competitors == null ? List.<MarketingCompetitorMentionDO>of() : competitors) {
            if (hasText(row.getCompetitorKey())) {
                keys.add(row.getCompetitorKey());
            }
        }
        return List.copyOf(keys);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param source source 参数，用于 validateSource 流程中的校验、计算或对象转换。
     */
    private void validateSource(Long tenantId, MarketingMonitorSourceDO source) {
        if (source == null || !tenantId.equals(source.getTenantId())) {
            throw new IllegalArgumentException("monitor source is not found");
        }
        if (!enabled(source.getEnabled())) {
            throw new IllegalStateException("monitor source is disabled");
        }
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
            throw new IllegalArgumentException("marketing monitoring JSON serialization failed", ex);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<String> list(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param String string 参数，用于 safeCompetitors 流程中的校验、计算或对象转换。
     * @param competitors competitors 参数，用于 safeCompetitors 流程中的校验、计算或对象转换。
     * @return 返回 safe competitors 汇总后的集合、分页或映射视图。
     */
    private Map<String, List<String>> safeCompetitors(Map<String, List<String>> competitors) {
        return competitors == null ? Map.of() : competitors;
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
     * 执行 scaled 流程，围绕 scaled 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 scaled 计算得到的数量、金额或指标值。
     */
    private BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private Long requiredId(Long value, String field) {
        if (value == null) {
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
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
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
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeKey(String value, String field) {
        return required(value, field).toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalKey(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 enabled 的布尔判断结果。
     */
    private boolean enabled(Integer value) {
        return value == null || value == 1;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default int 计算得到的数量、金额或指标值。
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
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
     * Sentiment 数据记录。
     */
    private record Sentiment(String label,
                             BigDecimal score,
                             BigDecimal confidence,
                             Map<String, Object> keywordHits) {
    }
}
