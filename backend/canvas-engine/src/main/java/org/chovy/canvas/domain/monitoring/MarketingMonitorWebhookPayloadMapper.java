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

@Service
public class MarketingMonitorWebhookPayloadMapper {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public MarketingMonitorWebhookPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public MarketingMonitorItemIngestCommand toIngestCommand(MarketingMonitorSourceDO source,
                                                             Map<String, Object> payload) {
        if (source == null || source.getId() == null) {
            throw new IllegalArgumentException("monitoring source is required");
        }
        if (payload == null) {
            throw new IllegalArgumentException("monitoring webhook payload is required");
        }
        Map<String, Object> metadata = sourceMetadata(source);
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

    private Map<String, Object> sourceMetadata(MarketingMonitorSourceDO source) {
        if (source.getMetadataJson() == null || source.getMetadataJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(source.getMetadataJson(), OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

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

    private LocalDateTime publishedAt(Map<String, Object> payload) {
        Object value = firstValue(payload, "publishedAt", "published_at", "createdAt", "created_at", "timestamp");
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            long raw = number.longValue();
            long epochMillis = raw > 9_999_999_999L ? raw : raw * 1000L;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        }
        String text = text(value);
        if (text == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(text);
            } catch (Exception ex) {
                throw new IllegalArgumentException("publishedAt is invalid");
            }
        }
    }

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

    private List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> terms = new ArrayList<>();
            for (Object item : list) {
                String text = text(item);
                if (text != null) {
                    terms.add(text);
                }
            }
            return List.copyOf(terms);
        }
        String text = text(value);
        return text == null ? List.of() : List.of(text);
    }

    private String firstText(Map<?, ?> map, String... keys) {
        Object value = firstValue(map, keys);
        return text(value);
    }

    private Object firstValue(Map<?, ?> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }

    private String required(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String normalizeKey(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
