package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MarketingMonitorWebhookPayloadMapper 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class MarketingMonitorWebhookPayloadMapper {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * 创建 MarketingMonitorWebhookPayloadMapper 实例并注入 domain.monitoring 场景依赖。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingMonitorWebhookPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * toIngestCommand 校验或转换 domain.monitoring 场景的数据。
     * @param source source 参数，用于 toIngestCommand 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回组装或转换后的结果对象。
     */
    public MarketingMonitorItemIngestCommand toIngestCommand(MarketingMonitorSourceDO source,
                                                             Map<String, Object> payload) {
        if (source == null || source.getId() == null) {
            throw new IllegalArgumentException("monitoring source is required");
        }
        if (payload == null) {
            throw new IllegalArgumentException("monitoring webhook payload is required");
        }
        Map<String, Object> metadata = sourceMetadata(source);
        // Webhook providers use different field names; resolve aliases here so ingest commands stay provider-neutral.
        return new MarketingMonitorItemIngestCommand(
                source.getId(),
                required(firstText(payload, "externalItemId", "external_item_id", "id", "eventId"), "externalItemId"),
                firstText(payload, "sourceUrl", "source_url", "url", "permalink"),
                authorKey(payload),
                firstNonBlank(firstText(payload, "brandKey", "brand_key", "brand"),
                        text(metadata.get("defaultBrandKey"))),
                required(firstText(payload, "text", "textContent", "message", "content"), "text"),
                firstText(payload, "language", "lang"),
                publishedAt(payload),
                competitors(payload.containsKey("competitors") ? payload.get("competitors") : metadata.get("competitors")),
                new LinkedHashMap<>(payload));
    }

    /**
     * 执行 sourceMetadata 流程，围绕 source metadata 完成校验、计算或结果组装。
     *
     * @param source source 参数，用于 sourceMetadata 流程中的校验、计算或对象转换。
     * @return 返回 sourceMetadata 流程生成的业务结果。
     */
    private Map<String, Object> sourceMetadata(MarketingMonitorSourceDO source) {
        if (source.getMetadataJson() == null || source.getMetadataJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(source.getMetadataJson(), OBJECT_MAP);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            // Invalid metadata should not reject a valid webhook; it only removes optional defaults.
            return Map.of();
        }
    }

    /**
     * 执行 authorKey 流程，围绕 author key 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 authorKey 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 author key 生成的文本或业务键。
     */
    private String authorKey(Map<String, Object> payload) {
        String direct = firstText(payload, "authorKey", "author_key", "authorId", "author_id");
        if (direct != null) {
            return direct;
        }
        Object author = payload.get("author");
        if (author instanceof Map<?, ?> map) {
            return firstText(map, "id", "key", "username", "name");
        }
        return text(author);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param String string 参数，用于 publishedAt 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回流程执行后的业务结果。
     */
    private LocalDateTime publishedAt(Map<String, Object> payload) {
        Object value = firstValue(payload, "publishedAt", "published_at", "createdAt", "created_at", "timestamp");
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            long raw = number.longValue();
            // Accept both epoch seconds and milliseconds because providers differ on timestamp precision.
            long epochMillis = raw > 9_999_999_999L ? raw : raw * 1000L;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        }
        String text = text(value);
        if (text == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(text);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (Exception ex) {
                throw new IllegalArgumentException("publishedAt is invalid");
            }
        }
    }

    /**
     * 执行 competitors 流程，围绕 competitors 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 competitors 汇总后的集合、分页或映射视图。
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<String>> competitors(Object value) {
        if (value == null) {
            return Map.of();
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = normalizeKey(text(entry.getKey()));
                List<String> terms = stringList(entry.getValue());
                if (key != null && !terms.isEmpty()) {
                    // Preserve provider order for deterministic evidence rendering in monitoring reports.
                    result.put(key, terms);
                }
            }
            return result;
        }
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                String key = normalizeKey(firstText(map, "key", "competitorKey", "competitor_key", "name"));
                Object termsValue = map.containsKey("terms") ? map.get("terms") : map.get("matchedTerms");
                List<String> terms = stringList(termsValue);
                if (terms.isEmpty() && key != null) {
                    terms = List.of(key);
                }
                if (key != null && !terms.isEmpty()) {
                    result.put(key, terms);
                }
            }
            return result;
        }
        Map<String, Object> converted = objectMapper.convertValue(value, OBJECT_MAP);
        return competitors(converted);
    }

    /**
     * 执行 stringList 流程，围绕 string list 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string list 汇总后的集合、分页或映射视图。
     */
    private List<String> stringList(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> terms = new ArrayList<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (Object item : list) {
                String text = text(item);
                if (text != null) {
                    terms.add(text);
                }
            }
            return List.copyOf(terms);
        }
        String text = text(value);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return text == null ? List.of() : List.of(text);
    }

    /**
     * 执行 firstText 流程，围绕 first text 完成校验、计算或结果组装。
     *
     * @param map map 参数，用于 firstText 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstText 流程中的校验、计算或对象转换。
     * @return 返回 first text 生成的文本或业务键。
     */
    private String firstText(Map<?, ?> map, String... keys) {
        Object value = firstValue(map, keys);
        return text(value);
    }

    /**
     * 执行 firstValue 流程，围绕 first value 完成校验、计算或结果组装。
     *
     * @param map map 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @return 返回 firstValue 流程生成的业务结果。
     */
    private Object firstValue(Map<?, ?> map, String... keys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (map == null) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 执行 firstNonBlank 流程，围绕 first non blank 完成校验、计算或结果组装。
     *
     * @param first first 参数，用于 firstNonBlank 流程中的校验、计算或对象转换。
     * @param second second 参数，用于 firstNonBlank 流程中的校验、计算或对象转换。
     * @return 返回 first non blank 生成的文本或业务键。
     */
    private String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
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
    private String normalizeKey(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
