package org.chovy.canvas.risk.adapter.external;

import org.chovy.canvas.risk.domain.dsl.RiskRuleJsonCodec;
import org.chovy.canvas.risk.domain.dsl.RiskRuleJsonNode;
import org.chovy.canvas.risk.domain.runtime.RiskFeatureStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Redis-backed online feature store for precomputed risk features.
 */
@Component
public class RedisRiskFeatureStore implements RiskFeatureStore {

    private static final String KEY_PREFIX = "risk:feature:";

    private final StringRedisTemplate redis;
    private final RiskRuleJsonCodec jsonCodec;

    public RedisRiskFeatureStore(StringRedisTemplate redis, RiskRuleJsonCodec jsonCodec) {
        this.redis = redis;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void set(Long tenantId, String featureKey, String subjectHash, Object value, Duration ttl) {
        redis.opsForValue().set(key(tenantId, featureKey, subjectHash), serialize(value), ttl);
    }

    @Override
    public Optional<Object> get(Long tenantId, String featureKey, String subjectHash) {
        String key = key(tenantId, featureKey, subjectHash);
        String payload = redis.opsForValue().get(key);
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(parse(payload));
        } catch (RuntimeException ex) {
            redis.delete(key);
            return Optional.empty();
        }
    }

    private String serialize(Object value) {
        String type = value instanceof Number ? "NUMBER" : value instanceof Boolean ? "BOOLEAN" : "STRING";
        return jsonCodec.writeCanonical(new StoredFeature(type, value));
    }

    private Object parse(String payload) {
        RiskRuleJsonNode root = jsonCodec.readTree(payload);
        String type = Optional.ofNullable(root.path("type").textValue()).orElse("").toUpperCase(Locale.ROOT);
        RiskRuleJsonNode value = root.path("value");
        return switch (type) {
            case "NUMBER" -> numberValue(value);
            case "BOOLEAN" -> value.isBoolean() ? value.booleanValue() : null;
            case "STRING" -> value.textValue();
            default -> null;
        };
    }

    private String key(Long tenantId, String featureKey, String subjectHash) {
        return KEY_PREFIX + tenantId + ":" + featureKey + ":" + subjectHash;
    }

    private Object numberValue(RiskRuleJsonNode value) {
        if (value.isIntegralNumber()) {
            long number = value.longValue();
            if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
                return (int) number;
            }
            return number;
        }
        if (value.isDecimalNumber()) {
            return value.decimalValue().doubleValue();
        }
        return null;
    }

    private record StoredFeature(String type, Object value) {
    }
}
