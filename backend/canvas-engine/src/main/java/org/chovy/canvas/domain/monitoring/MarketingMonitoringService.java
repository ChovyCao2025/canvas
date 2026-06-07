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

    MarketingMonitoringService(MarketingMonitorSourceMapper sourceMapper,
                               MarketingMonitorItemMapper itemMapper,
                               MarketingSentimentAnalysisMapper sentimentMapper,
                               MarketingCompetitorMentionMapper competitorMapper,
                               MarketingMonitorAlertMapper alertMapper,
                               ObjectMapper objectMapper,
                               Clock clock) {
        this(sourceMapper, itemMapper, sentimentMapper, competitorMapper, alertMapper, objectMapper, clock, null);
    }

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

    public MarketingMonitorSourceView upsertSource(Long tenantId,
                                                   MarketingMonitorSourceCommand command,
                                                   String actor) {
        if (command == null) {
            throw new IllegalArgumentException("marketing monitor source command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String sourceKey = normalizeKey(command.sourceKey(), "sourceKey");
        String sourceType = normalizeUpper(command.sourceType(), "sourceType");
        LocalDateTime changedAt = now();
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
        return toSourceView(row);
    }

    public MarketingMonitorIngestResult ingestItem(Long tenantId,
                                                   MarketingMonitorItemIngestCommand command,
                                                   String actor) {
        if (command == null) {
            throw new IllegalArgumentException("marketing monitor item command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
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
                mentions.stream().map(this::toCompetitorView).toList(),
                alerts.stream().map(this::toAlertView).toList());
    }

    public List<MarketingMonitorItemView> items(Long tenantId, MarketingMonitorItemQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("marketing monitor item query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int limit = boundedLimit(query.limit());
        String sentimentLabel = normalizeOptionalUpper(query.sentimentLabel());
        String competitorKey = normalizeOptionalKey(query.competitorKey());
        List<MarketingMonitorItemDO> rows = safeList(itemMapper.selectList(
                new LambdaQueryWrapper<MarketingMonitorItemDO>()
                        .eq(MarketingMonitorItemDO::getTenantId, scopedTenantId)
                        .orderByDesc(MarketingMonitorItemDO::getIngestedAt)
                        .last("LIMIT " + limit)));
        Map<Long, MarketingSentimentAnalysisDO> sentiments = sentimentByItem(scopedTenantId);
        Map<Long, List<MarketingCompetitorMentionDO>> competitors = competitorsByItem(scopedTenantId);
        return rows.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> matchesSentiment(row, sentiments, sentimentLabel))
                .filter(row -> matchesCompetitor(row, competitors, competitorKey))
                .limit(limit)
                .map(row -> toItemView(row, sentiments.get(row.getId()),
                        competitors.getOrDefault(row.getId(), List.of())))
                .toList();
    }

    public List<MarketingMonitorAlertView> alerts(Long tenantId, MarketingMonitorAlertQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("marketing monitor alert query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int limit = boundedLimit(query.limit());
        String status = normalizeOptionalUpper(query.status());
        return safeList(alertMapper.selectList(
                new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                        .eq(MarketingMonitorAlertDO::getTenantId, scopedTenantId)
                        .eq(status != null, MarketingMonitorAlertDO::getStatus, status)
                        .orderByDesc(MarketingMonitorAlertDO::getCreatedAt)
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toAlertView)
                .toList();
    }

    public MarketingMonitorAlertView resolveAlert(Long tenantId, Long alertId, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorAlertDO row = alertMapper.selectById(requiredId(alertId, "alertId"));
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("alert is not found");
        }
        LocalDateTime resolvedAt = now();
        row.setStatus("RESOLVED");
        row.setResolvedBy(defaultString(actor, "system"));
        row.setResolvedAt(resolvedAt);
        row.setUpdatedAt(resolvedAt);
        alertMapper.updateById(row);
        return toAlertView(row);
    }

    private MarketingMonitorIngestResult existingItemResult(Long tenantId, MarketingMonitorItemDO item) {
        MarketingSentimentAnalysisDO sentiment = sentimentMapper.selectOne(
                new LambdaQueryWrapper<MarketingSentimentAnalysisDO>()
                        .eq(MarketingSentimentAnalysisDO::getTenantId, tenantId)
                        .eq(MarketingSentimentAnalysisDO::getItemId, item.getId())
                        .last("LIMIT 1"));
        List<MarketingCompetitorMentionDO> mentions = safeList(competitorMapper.selectList(
                new LambdaQueryWrapper<MarketingCompetitorMentionDO>()
                        .eq(MarketingCompetitorMentionDO::getTenantId, tenantId)
                        .eq(MarketingCompetitorMentionDO::getItemId, item.getId())));
        return new MarketingMonitorIngestResult(
                toItemView(item, sentiment, mentions),
                sentiment == null ? null : toSentimentView(sentiment),
                mentions.stream()
                        .filter(row -> tenantId.equals(row.getTenantId()))
                        .map(this::toCompetitorView)
                        .toList(),
                List.of());
    }

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

    private List<MarketingCompetitorMentionDO> insertCompetitorMentions(Long tenantId,
                                                                        MarketingMonitorItemDO item,
                                                                        Map<String, List<String>> competitors,
                                                                        Sentiment sentiment,
                                                                        LocalDateTime createdAt) {
        List<MarketingCompetitorMentionDO> rows = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : safeCompetitors(competitors).entrySet()) {
            String competitorKey = normalizeOptionalKey(entry.getKey());
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
            competitorMapper.insert(row);
            rows.add(row);
        }
        return rows;
    }

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
                            "matchedTerms", list(mention.getMatchedTermsJson()),
                            "sentimentScore", sentiment.score()),
                    actor, createdAt));
        }
        return rows;
    }

    private void fanoutAlerts(Long tenantId, List<MarketingMonitorAlertDO> alerts, String actor) {
        if (alertFanoutService == null || alerts == null || alerts.isEmpty()) {
            return;
        }
        for (MarketingMonitorAlertDO alert : alerts) {
            try {
                alertFanoutService.dispatchAlert(tenantId, alert, actor);
            } catch (RuntimeException ex) {
                log.warn("[MONITORING] alert fanout skipped alert={} error={}", alert.getId(), ex.getMessage());
            }
        }
    }

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
        MarketingMonitorAlertDO row = new MarketingMonitorAlertDO();
        row.setTenantId(tenantId);
        row.setAlertType(alertType);
        row.setSeverity(severity);
        row.setStatus("OPEN");
        row.setScopeKey(trimToNull(scopeKey));
        row.setTitle(title);
        row.setReason(reason);
        row.setItemCount(1);
        row.setWindowStart(item.getPublishedAt() == null ? item.getIngestedAt() : item.getPublishedAt());
        row.setWindowEnd(item.getPublishedAt() == null ? item.getIngestedAt() : item.getPublishedAt());
        row.setMetadataJson(json(metadata));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setCreatedAt(createdAt);
        row.setUpdatedAt(createdAt);
        alertMapper.insert(row);
        return row;
    }

    private Sentiment analyze(String text) {
        List<String> negative = matchedTerms(text, NEGATIVE_TERMS);
        List<String> positive = matchedTerms(text, POSITIVE_TERMS);
        int hits = negative.size() + positive.size();
        BigDecimal score = hits == 0
                ? scaled(0)
                : scaled((positive.size() - negative.size()) / (double) hits);
        String label = score.compareTo(BigDecimal.ZERO) < 0
                ? "NEGATIVE"
                : score.compareTo(BigDecimal.ZERO) > 0 ? "POSITIVE" : "NEUTRAL";
        Map<String, Object> keywordHits = new LinkedHashMap<>();
        keywordHits.put("negative", negative);
        keywordHits.put("positive", positive);
        return new Sentiment(label, score, confidence(text, hits), keywordHits);
    }

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

    private List<String> matchedTerms(String text, List<String> terms) {
        if (!hasText(text) || terms == null || terms.isEmpty()) {
            return List.of();
        }
        Set<String> matches = new LinkedHashSet<>();
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
        return List.copyOf(matches);
    }

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

    private boolean matchesSentiment(MarketingMonitorItemDO item,
                                     Map<Long, MarketingSentimentAnalysisDO> sentiments,
                                     String label) {
        if (label == null) {
            return true;
        }
        MarketingSentimentAnalysisDO sentiment = sentiments.get(item.getId());
        return sentiment != null && label.equals(sentiment.getSentimentLabel());
    }

    private boolean matchesCompetitor(MarketingMonitorItemDO item,
                                      Map<Long, List<MarketingCompetitorMentionDO>> competitors,
                                      String competitorKey) {
        if (competitorKey == null) {
            return true;
        }
        return competitors.getOrDefault(item.getId(), List.of()).stream()
                .anyMatch(row -> competitorKey.equals(row.getCompetitorKey()));
    }

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

    private MarketingMonitorItemView toItemView(MarketingMonitorItemDO row,
                                                MarketingSentimentAnalysisDO sentiment,
                                                List<MarketingCompetitorMentionDO> competitors) {
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
                row.getPublishedAt(),
                row.getIngestedAt(),
                map(row.getRawPayloadJson()),
                sentiment == null ? null : sentiment.getSentimentLabel(),
                sentiment == null ? null : sentiment.getSentimentScore(),
                sentiment == null ? null : sentiment.getConfidence(),
                competitorKeys(competitors));
    }

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

    private MarketingMonitorAlertView toAlertView(MarketingMonitorAlertDO row) {
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
                row.getUpdatedAt());
    }

    private List<String> competitorKeys(List<MarketingCompetitorMentionDO> competitors) {
        Set<String> keys = new LinkedHashSet<>();
        for (MarketingCompetitorMentionDO row : competitors == null ? List.<MarketingCompetitorMentionDO>of() : competitors) {
            if (hasText(row.getCompetitorKey())) {
                keys.add(row.getCompetitorKey());
            }
        }
        return List.copyOf(keys);
    }

    private void validateSource(Long tenantId, MarketingMonitorSourceDO source) {
        if (source == null || !tenantId.equals(source.getTenantId())) {
            throw new IllegalArgumentException("monitor source is not found");
        }
        if (!enabled(source.getEnabled())) {
            throw new IllegalStateException("monitor source is disabled");
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("marketing monitoring JSON serialization failed", ex);
        }
    }

    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private List<String> list(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private Map<String, List<String>> safeCompetitors(Map<String, List<String>> competitors) {
        return competitors == null ? Map.of() : competitors;
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private Long requiredId(Long value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String normalizeUpper(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    private String normalizeKey(String value, String field) {
        return required(value, field).toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalKey(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean enabled(Integer value) {
        return value == null || value == 1;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private record Sentiment(String label,
                             BigDecimal score,
                             BigDecimal confidence,
                             Map<String, Object> keywordHits) {
    }
}
