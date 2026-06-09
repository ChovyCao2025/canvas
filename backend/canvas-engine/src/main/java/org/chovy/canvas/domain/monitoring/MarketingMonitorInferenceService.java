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

/**
 * MarketingMonitorInferenceService 编排 domain.monitoring 场景的领域业务规则。
 */
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

    /**
     * 创建 MarketingMonitorInferenceService 实例并注入 domain.monitoring 场景依赖。
     * @param itemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param inferenceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param generatorProvider generator provider 参数，用于 MarketingMonitorInferenceService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 执行 MarketingMonitorInferenceService 流程，围绕 marketing monitor inference service 完成校验、计算或结果组装。
     *
     * @param itemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param inferenceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param generator generator 参数，用于 MarketingMonitorInferenceService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 执行业务操作 analyze，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
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
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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

    /**
     * 查询业务列表，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 list 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingMonitorInferenceView> list(Long tenantId, MarketingMonitorInferenceQuery query) {
        // 准备本次流程的上下文、默认值和中间结果。
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorInferenceQuery effectiveQuery = query == null
                ? new MarketingMonitorInferenceQuery(null, null, null, null, null, 50)
                : query;
        int limit = boundedLimit(effectiveQuery.limit());
        String sentimentLabel = normalizeOptionalUpper(effectiveQuery.sentimentLabel());
        String modelKey = trimToNull(effectiveQuery.modelKey());
        String providerStatus = normalizeOptionalUpper(effectiveQuery.providerStatus());
        // 访问持久化数据，读取现有配置或写入本次变更。
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
        // 遍历候选记录并转换为前端或服务层需要的视图。
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

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param extraEvidence extra evidence 参数，用于 fallback 流程中的校验、计算或对象转换。
     * @return 返回 fallback 流程生成的业务结果。
     */
    private MarketingMonitorInferenceGenerationResult fallback(MarketingMonitorInferenceGenerationContext context,
                                                               Map<String, Object> extraEvidence) {
        MarketingMonitorItemDO item = context.item();
        List<String> negative = matchedTerms(item.getTextContent(), NEGATIVE_TERMS);
        List<String> positive = matchedTerms(item.getTextContent(), POSITIVE_TERMS);
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

    /**
     * 执行 inputContext 流程，围绕 input context 完成校验、计算或结果组装。
     *
     * @param item item 参数，用于 inputContext 流程中的校验、计算或对象转换。
     * @return 返回 inputContext 流程生成的业务结果。
     */
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

    /**
     * 执行 promptContext 流程，围绕 prompt context 完成校验、计算或结果组装。
     *
     * @param item item 参数，用于 promptContext 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 promptContext 流程生成的业务结果。
     */
    private Map<String, Object> promptContext(MarketingMonitorItemDO item,
                                              MarketingMonitorInferenceCommand command) {
        Map<String, Object> result = new LinkedHashMap<>(inputContext(item));
        result.put("task", "Classify monitoring sentiment and extract entities, topics, risk flags, and evidence.");
        result.put("allowedSentimentLabels", List.of("POSITIVE", "NEGATIVE", "NEUTRAL", "MIXED"));
        result.put("metadata", safeMap(command.metadata()));
        return result;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param item item 参数，用于 fallbackEntities 流程中的校验、计算或对象转换。
     * @return 返回 fallbackEntities 流程生成的业务结果。
     */
    private List<Map<String, Object>> fallbackEntities(MarketingMonitorItemDO item) {
        if (!hasText(item.getBrandKey())) {
            return List.of();
        }
        return List.of(Map.of(
                "name", item.getBrandKey(),
                "type", "BRAND",
                "sentiment", "UNKNOWN"));
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param item item 参数，用于 fallbackTopics 流程中的校验、计算或对象转换。
     * @return 返回 fallback topics 汇总后的集合、分页或映射视图。
     */
    private List<String> fallbackTopics(MarketingMonitorItemDO item) {
        // 准备本次处理所需的上下文和中间变量。
        String text = lower(item.getTextContent());
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (containsAny(text, "refund", "chargeback", "退款")) {
            topics.add("refund");
        }
        if (containsAny(text, "privacy", "personal data", "隐私")) {
            topics.add("privacy");
        }
        if (containsAny(text, "support", "service", "客服")) {
            topics.add("support");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ArrayList<>(topics);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param text text 参数，用于 fallbackRiskFlags 流程中的校验、计算或对象转换。
     * @return 返回 fallback risk flags 汇总后的集合、分页或映射视图。
     */
    private List<String> fallbackRiskFlags(String text) {
        // 准备本次处理所需的上下文和中间变量。
        String scopedText = lower(text);
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ArrayList<>(flags);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorInferenceView toView(MarketingMonitorInferenceDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
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
            throw new IllegalArgumentException("marketing monitor inference JSON serialization failed", ex);
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
     * 执行 entities 流程，围绕 entities 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 entities 流程生成的业务结果。
     */
    private List<Map<String, Object>> entities(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, ENTITY_LIST);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    /**
     * 执行 strings 流程，围绕 strings 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 strings 汇总后的集合、分页或映射视图。
     */
    private List<String> strings(String json) {
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
     * 执行 sha256 流程，围绕 sha256 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sha256 生成的文本或业务键。
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(defaultString(value, "").getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
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
        LinkedHashSet<String> matches = new LinkedHashSet<>();
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
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param text text 参数，用于 fallbackConfidence 流程中的校验、计算或对象转换。
     * @param hits hits 参数，用于 fallbackConfidence 流程中的校验、计算或对象转换。
     * @return 返回 fallback confidence 计算得到的数量、金额或指标值。
     */
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

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param String string 参数，用于 safeEntities 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 safeEntities 流程中的校验、计算或对象转换。
     * @return 返回 safeEntities 流程生成的业务结果。
     */
    private List<Map<String, Object>> safeEntities(List<Map<String, Object>> values) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map<String, Object> value : values) {
            if (value != null && !value.isEmpty()) {
                result.add(new LinkedHashMap<>(value));
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param values values 参数，用于 safeStrings 流程中的校验、计算或对象转换。
     * @return 返回 safe strings 汇总后的集合、分页或映射视图。
     */
    private List<String> safeStrings(List<String> values) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return values.stream()
                .filter(this::hasText)
                .map(String::trim)
                .toList();
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param String string 参数，用于 safeMap 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 safeMap 流程中的校验、计算或对象转换。
     * @return 返回 safeMap 流程生成的业务结果。
     */
    private Map<String, Object> safeMap(Map<String, Object> values) {
        return values == null || values.isEmpty() ? Map.of() : new LinkedHashMap<>(values);
    }

    /**
     * 执行 riskFlags 流程，围绕 risk flags 完成校验、计算或结果组装。
     *
     * @param values values 参数，用于 riskFlags 流程中的校验、计算或对象转换。
     * @return 返回 risk flags 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeRiskFlag(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param score score 参数，用于 normalizeSentiment 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
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

    /**
     * 规范化输入值。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        return defaultString(normalizeOptionalUpper(status), "UNKNOWN");
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param min min 参数，用于 clamped 流程中的校验、计算或对象转换。
     * @param max max 参数，用于 clamped 流程中的校验、计算或对象转换。
     * @param scale scale 参数，用于 clamped 流程中的校验、计算或对象转换。
     * @return 返回 clamped 计算得到的数量、金额或指标值。
     */
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
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
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
     * 执行数据写入或状态变更。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 enabled 的布尔判断结果。
     */
    private boolean enabled(Boolean value) {
        return Boolean.TRUE.equals(value);
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
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
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
     * 执行 lower 流程，围绕 lower 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 lower 生成的文本或业务键。
     */
    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * 执行 dateTime 流程，围绕 date time 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 date time 生成的文本或业务键。
     */
    private String dateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param text text 参数，用于 containsAny 流程中的校验、计算或对象转换。
     * @param terms terms 参数，用于 containsAny 流程中的校验、计算或对象转换。
     * @return 返回 contains any 的布尔判断结果。
     */
    private boolean containsAny(String text, String... terms) {
        String value = text == null ? "" : text;
        for (String term : terms) {
            if (term != null && !term.isBlank() && value.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行 message 流程，围绕 message 完成校验、计算或结果组装。
     *
     * @param throwable throwable 参数，用于 message 流程中的校验、计算或对象转换。
     * @return 返回 message 生成的文本或业务键。
     */
    private String message(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
