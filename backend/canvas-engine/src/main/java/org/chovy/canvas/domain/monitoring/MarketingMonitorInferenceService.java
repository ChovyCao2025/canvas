package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorInferenceDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorItemDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorInferenceMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MarketingMonitorInferenceService {

    private static final String DEFAULT_MODEL_KEY = "monitoring-local-sentiment-v1";
    private static final String DEFAULT_MODEL_VERSION = "fallback_v1";
    private static final Set<String> SENTIMENT_LABELS = Set.of("POSITIVE", "NEGATIVE", "NEUTRAL", "MIXED");
    private static final TypeReference<List<Map<String, Object>>> ENTITY_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final List<String> NEGATIVE_TERMS = List.of(
            "churn", "bad", "slow", "broken", "complaint", "angry", "fail", "worse", "refund", "poor",
            "chargeback", "lawsuit", "privacy", "cancel", "unsubscribe", "投诉", "退款", "差", "坏", "隐私");
    private static final List<String> POSITIVE_TERMS = List.of(
            "great", "good", "fast", "love", "better", "excellent", "happy", "smooth", "win", "recommend",
            "好", "喜欢", "推荐", "优秀");

    private final MarketingMonitorItemMapper itemMapper;
    private final MarketingMonitorInferenceMapper inferenceMapper;
    private final MarketingMonitorInferenceGenerator generator;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public MarketingMonitorInferenceService(MarketingMonitorItemMapper itemMapper,
                                            MarketingMonitorInferenceMapper inferenceMapper,
                                            ObjectProvider<MarketingMonitorInferenceGenerator> generatorProvider,
                                            ObjectMapper objectMapper) {
        this(itemMapper, inferenceMapper,
                generatorProvider == null ? null : generatorProvider.getIfAvailable(),
                objectMapper,
                Clock.systemDefaultZone());
    }

    MarketingMonitorInferenceService(MarketingMonitorItemMapper itemMapper,
                                     MarketingMonitorInferenceMapper inferenceMapper,
                                     MarketingMonitorInferenceGenerator generator,
                                     ObjectMapper objectMapper,
                                     Clock clock) {
        this.itemMapper = itemMapper;
        this.inferenceMapper = inferenceMapper;
        this.generator = generator;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorInferenceView analyze(Long tenantId,
                                                 MarketingMonitorInferenceCommand command,
                                                 String actor) {
        if (command == null) {
            throw new IllegalArgumentException("marketing monitor inference command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorItemDO item = itemMapper.selectById(requiredId(command.itemId(), "itemId"));
        if (item == null || !scopedTenantId.equals(item.getTenantId())) {
            throw new IllegalArgumentException("monitor item is not found");
        }
        Map<String, Object> promptContext = promptContext(item, command);
        String inputHash = sha256(json(inputContext(item)));
        String promptHash = sha256("marketing-monitor-inference:" + json(promptContext));
        MarketingMonitorInferenceGenerationContext context = new MarketingMonitorInferenceGenerationContext(
                scopedTenantId,
                item,
                command,
                promptContext,
                inputHash,
                promptHash);

        MarketingMonitorInferenceGenerationResult result = null;
        if (!Boolean.TRUE.equals(command.forceFallback()) && generator != null) {
            try {
                result = generator.generate(context);
            } catch (RuntimeException ex) {
                result = fallback(context, Map.of("generatorError", message(ex)));
            }
        }
        if (result == null) {
            result = fallback(context, Map.of());
        }

        LocalDateTime now = now();
        MarketingMonitorInferenceDO row = new MarketingMonitorInferenceDO();
        row.setTenantId(scopedTenantId);
        row.setItemId(item.getId());
        row.setSourceId(item.getSourceId());
        row.setProviderId(result.providerId() == null ? command.providerId() : result.providerId());
        row.setTemplateId(result.templateId() == null ? command.templateId() : result.templateId());
        row.setModelKey(defaultString(result.modelKey(), defaultString(command.modelKey(), DEFAULT_MODEL_KEY)));
        row.setModelVersion(defaultString(result.modelVersion(), defaultString(command.modelVersion(), DEFAULT_MODEL_VERSION)));
        row.setProviderStatus(normalizeStatus(result.providerStatus()));
        row.setFallbackUsed(result.fallbackUsed());
        row.setInputHash(inputHash);
        row.setPromptHash(promptHash);
        row.setSentimentLabel(normalizeSentiment(result.sentimentLabel(), result.sentimentScore()));
        row.setSentimentScore(clamped(result.sentimentScore(), -1, 1, 5));
        row.setConfidence(clamped(result.confidence(), 0, 1, 5));
        row.setEntitiesJson(json(safeEntities(result.entities())));
        row.setTopicsJson(json(safeStrings(result.topics())));
        row.setRiskFlagsJson(json(riskFlags(result.riskFlags())));
        row.setEvidenceJson(json(safeMap(result.evidence())));
        row.setLatencyMs(Math.max(0L, result.latencyMs()));
        row.setRequestedBy(defaultString(actor, "system"));
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        inferenceMapper.insert(row);
        return toView(row);
    }

    public List<MarketingMonitorInferenceView> list(Long tenantId, MarketingMonitorInferenceQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorInferenceQuery effectiveQuery = query == null
                ? new MarketingMonitorInferenceQuery(null, null, null, null, null, 50)
                : query;
        int limit = boundedLimit(effectiveQuery.limit());
        String sentimentLabel = normalizeOptionalUpper(effectiveQuery.sentimentLabel());
        String modelKey = trimToNull(effectiveQuery.modelKey());
        String providerStatus = normalizeOptionalUpper(effectiveQuery.providerStatus());
        List<MarketingMonitorInferenceDO> rows = safeList(inferenceMapper.selectList(
                new LambdaQueryWrapper<MarketingMonitorInferenceDO>()
                        .eq(MarketingMonitorInferenceDO::getTenantId, scopedTenantId)
                        .eq(effectiveQuery.itemId() != null, MarketingMonitorInferenceDO::getItemId, effectiveQuery.itemId())
                        .eq(sentimentLabel != null, MarketingMonitorInferenceDO::getSentimentLabel, sentimentLabel)
                        .eq(modelKey != null, MarketingMonitorInferenceDO::getModelKey, modelKey)
                        .eq(providerStatus != null, MarketingMonitorInferenceDO::getProviderStatus, providerStatus)
                        .eq(effectiveQuery.fallbackUsed() != null,
                                MarketingMonitorInferenceDO::getFallbackUsed, effectiveQuery.fallbackUsed())
                        .orderByDesc(MarketingMonitorInferenceDO::getCreatedAt)
                        .last("LIMIT " + limit)));
        return rows.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> effectiveQuery.itemId() == null || effectiveQuery.itemId().equals(row.getItemId()))
                .filter(row -> sentimentLabel == null || sentimentLabel.equals(row.getSentimentLabel()))
                .filter(row -> modelKey == null || modelKey.equals(row.getModelKey()))
                .filter(row -> providerStatus == null || providerStatus.equals(row.getProviderStatus()))
                .filter(row -> effectiveQuery.fallbackUsed() == null
                        || effectiveQuery.fallbackUsed().equals(enabled(row.getFallbackUsed())))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    private MarketingMonitorInferenceGenerationResult fallback(MarketingMonitorInferenceGenerationContext context,
                                                               Map<String, Object> extraEvidence) {
        MarketingMonitorItemDO item = context.item();
        List<String> negative = matchedTerms(item.getTextContent(), NEGATIVE_TERMS);
        List<String> positive = matchedTerms(item.getTextContent(), POSITIVE_TERMS);
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
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("keywordHits", keywordHits);
        evidence.put("fallbackReason", Boolean.TRUE.equals(context.command().forceFallback())
                ? "FORCED_FALLBACK"
                : "GENERATOR_UNAVAILABLE");
        evidence.putAll(safeMap(extraEvidence));
        return new MarketingMonitorInferenceGenerationResult(
                context.command().providerId(),
                context.command().templateId(),
                defaultString(context.command().modelKey(), DEFAULT_MODEL_KEY),
                defaultString(context.command().modelVersion(), DEFAULT_MODEL_VERSION),
                "LOCAL_FALLBACK",
                true,
                label,
                score,
                fallbackConfidence(item.getTextContent(), hits),
                fallbackEntities(item),
                fallbackTopics(item),
                fallbackRiskFlags(item.getTextContent()),
                evidence,
                1L);
    }

    private Map<String, Object> inputContext(MarketingMonitorItemDO item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemId", item.getId());
        result.put("sourceId", item.getSourceId());
        result.put("externalItemId", item.getExternalItemId());
        result.put("sourceType", item.getSourceType());
        result.put("sourceUrl", item.getSourceUrl());
        result.put("authorKey", item.getAuthorKey());
        result.put("brandKey", item.getBrandKey());
        result.put("text", item.getTextContent());
        result.put("language", item.getLanguage());
        result.put("publishedAt", dateTime(item.getPublishedAt()));
        result.put("rawPayload", map(item.getRawPayloadJson()));
        return result;
    }

    private Map<String, Object> promptContext(MarketingMonitorItemDO item,
                                              MarketingMonitorInferenceCommand command) {
        Map<String, Object> result = new LinkedHashMap<>(inputContext(item));
        result.put("task", "Classify monitoring sentiment and extract entities, topics, risk flags, and evidence.");
        result.put("allowedSentimentLabels", List.of("POSITIVE", "NEGATIVE", "NEUTRAL", "MIXED"));
        result.put("metadata", safeMap(command.metadata()));
        return result;
    }

    private List<Map<String, Object>> fallbackEntities(MarketingMonitorItemDO item) {
        if (!hasText(item.getBrandKey())) {
            return List.of();
        }
        return List.of(Map.of(
                "name", item.getBrandKey(),
                "type", "BRAND",
                "sentiment", "UNKNOWN"));
    }

    private List<String> fallbackTopics(MarketingMonitorItemDO item) {
        String text = lower(item.getTextContent());
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        if (containsAny(text, "refund", "chargeback", "退款")) {
            topics.add("refund");
        }
        if (containsAny(text, "privacy", "personal data", "隐私")) {
            topics.add("privacy");
        }
        if (containsAny(text, "support", "service", "客服")) {
            topics.add("support");
        }
        return new ArrayList<>(topics);
    }

    private List<String> fallbackRiskFlags(String text) {
        String scopedText = lower(text);
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        if (containsAny(scopedText, "refund", "chargeback", "退款", "退费")) {
            flags.add("SENSITIVE_REFUND");
        }
        if (containsAny(scopedText, "legal", "lawyer", "lawsuit", "法律", "律师", "起诉")) {
            flags.add("SENSITIVE_LEGAL");
        }
        if (containsAny(scopedText, "privacy", "personal data", "隐私", "个人信息")) {
            flags.add("SENSITIVE_PRIVACY");
        }
        if (containsAny(scopedText, "payment", "invoice", "card", "支付", "发票", "扣款")) {
            flags.add("SENSITIVE_PAYMENT");
        }
        if (containsAny(scopedText, "complaint", "angry", "投诉", "不满")) {
            flags.add("SENSITIVE_COMPLAINT");
        }
        return new ArrayList<>(flags);
    }

    private MarketingMonitorInferenceView toView(MarketingMonitorInferenceDO row) {
        return new MarketingMonitorInferenceView(
                row.getId(),
                row.getTenantId(),
                row.getItemId(),
                row.getSourceId(),
                row.getProviderId(),
                row.getTemplateId(),
                row.getModelKey(),
                row.getModelVersion(),
                row.getProviderStatus(),
                enabled(row.getFallbackUsed()),
                row.getInputHash(),
                row.getPromptHash(),
                row.getSentimentLabel(),
                row.getSentimentScore(),
                row.getConfidence(),
                entities(row.getEntitiesJson()),
                strings(row.getTopicsJson()),
                strings(row.getRiskFlagsJson()),
                map(row.getEvidenceJson()),
                row.getLatencyMs() == null ? 0L : row.getLatencyMs(),
                row.getRequestedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("marketing monitor inference JSON serialization failed", ex);
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

    private List<Map<String, Object>> entities(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, ENTITY_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private List<String> strings(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(defaultString(value, "").getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private List<String> matchedTerms(String text, List<String> terms) {
        if (!hasText(text) || terms == null || terms.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> matches = new LinkedHashSet<>();
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

    private BigDecimal fallbackConfidence(String text, int hits) {
        if (!hasText(text)) {
            return scaled(0.2);
        }
        double value = 0.40 + Math.min(0.45, hits * 0.10);
        if (text.trim().length() < 20) {
            value -= 0.10;
        }
        return clamped(BigDecimal.valueOf(value), 0, 1, 5);
    }

    private List<Map<String, Object>> safeEntities(List<Map<String, Object>> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> value : values) {
            if (value != null && !value.isEmpty()) {
                result.add(new LinkedHashMap<>(value));
            }
        }
        return result;
    }

    private List<String> safeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(this::hasText)
                .map(String::trim)
                .toList();
    }

    private Map<String, Object> safeMap(Map<String, Object> values) {
        return values == null || values.isEmpty() ? Map.of() : new LinkedHashMap<>(values);
    }

    private List<String> riskFlags(List<String> values) {
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        for (String value : values == null ? List.<String>of() : values) {
            String flag = normalizeRiskFlag(value);
            if (!flag.isBlank()) {
                flags.add(flag);
            }
        }
        return new ArrayList<>(flags);
    }

    private String normalizeRiskFlag(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String normalizeSentiment(String value, BigDecimal score) {
        String label = normalizeOptionalUpper(value);
        if (label != null && SENTIMENT_LABELS.contains(label)) {
            return label;
        }
        BigDecimal boundedScore = clamped(score, -1, 1, 5);
        return boundedScore.compareTo(BigDecimal.ZERO) < 0
                ? "NEGATIVE"
                : boundedScore.compareTo(BigDecimal.ZERO) > 0 ? "POSITIVE" : "NEUTRAL";
    }

    private String normalizeStatus(String status) {
        return defaultString(normalizeOptionalUpper(status), "UNKNOWN");
    }

    private BigDecimal clamped(BigDecimal value, int min, int max, int scale) {
        BigDecimal effective = value == null ? BigDecimal.ZERO : value;
        if (effective.compareTo(BigDecimal.valueOf(min)) < 0) {
            effective = BigDecimal.valueOf(min);
        }
        if (effective.compareTo(BigDecimal.valueOf(max)) > 0) {
            effective = BigDecimal.valueOf(max);
        }
        return effective.setScale(scale, RoundingMode.HALF_UP);
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

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean enabled(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String dateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private boolean containsAny(String text, String... terms) {
        String value = text == null ? "" : text;
        for (String term : terms) {
            if (term != null && !term.isBlank() && value.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String message(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
