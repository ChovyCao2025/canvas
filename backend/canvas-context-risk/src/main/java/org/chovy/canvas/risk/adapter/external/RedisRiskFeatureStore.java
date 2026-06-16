package org.chovy.canvas.risk.adapter.external;

import org.chovy.canvas.risk.domain.dsl.RiskRuleJsonCodec;
import org.chovy.canvas.risk.domain.dsl.RiskRuleJsonNode;
import org.chovy.canvas.risk.domain.runtime.RiskFeatureStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;

/**
 * Redis-backed online feature store for precomputed risk features.
 */
@Component
public class RedisRiskFeatureStore implements RiskFeatureStore {

    /**
     * 保存 KEY_PREFIX 对应的风控状态或配置。
     */
    private static final String KEY_PREFIX = "risk:feature:";


    /**
     * 保存 redis 对应的风控状态或配置。
     */
    private final StringRedisTemplate redis;

    /**
     * 保存 jsonCodec 对应的风控状态或配置。
     */
    private final RiskRuleJsonCodec jsonCodec;

    public RedisRiskFeatureStore(StringRedisTemplate redis, RiskRuleJsonCodec jsonCodec) {
        this.redis = redis;
        this.jsonCodec = jsonCodec;
    }

    /**
     * 执行 set 相关的风控处理逻辑。
     */
    @Override
    public void set(Long tenantId, String featureKey, String subjectHash, Object value, Duration ttl) {
        redis.opsForValue().set(key(tenantId, featureKey, subjectHash), serialize(value), ttl);
    }

    /**
     * 执行 get 相关的风控处理逻辑。
     */
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

    /**
     * 执行 serialize 相关的风控处理逻辑。
     */
    private String serialize(Object value) {
        String type = value instanceof Number ? "NUMBER" : value instanceof Boolean ? "BOOLEAN" : "STRING";
        return jsonCodec.writeCanonical(new StoredFeature(type, value));
    }

    /**
     * 执行 parse 相关的风控处理逻辑。
     */
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

    /**
     * 执行 key 相关的风控处理逻辑。
     */
    private String key(Long tenantId, String featureKey, String subjectHash) {
        return KEY_PREFIX + tenantId + ":" + featureKey + ":" + subjectHash;
    }

    /**
     * 执行 numberValue 相关的风控处理逻辑。
     */
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

    /**
     * 定义 StoredFeature 的风控模块职责和数据契约。
     */
    private static final class StoredFeature {

        /**
         * StoredFeature 的 type 字段。
         */
        private final String type;


        /**
         * StoredFeature 的 value 字段。
         */
        private final Object value;


        /**
         * 创建 StoredFeature。
         *
         * @param type StoredFeature 的 type 字段
         * @param value StoredFeature 的 value 字段
         */
        public StoredFeature(String type, Object value) {
            this.type = type;
            this.value = value;
        }

        /**
         * 返回 StoredFeature 的 type 字段。
         *
         * @return type 字段值
         */
        public String type() {
            return type;
        }

        /**
         * 返回 Jackson Bean 序列化使用的类型字段。
         *
         * @return 特征值类型
         */
        public String getType() {
            return type;
        }

        /**
         * 返回 StoredFeature 的 value 字段。
         *
         * @return value 字段值
         */
        public Object value() {
            return value;
        }

        /**
         * 返回 Jackson Bean 序列化使用的特征值字段。
         *
         * @return 特征值
         */
        public Object getValue() {
            return value;
        }

        /**
         * 比较当前 StoredFeature 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof StoredFeature other)) {
                return false;
            }
            return Objects.equals(type, other.type)
                    && Objects.equals(value, other.value);
        }

        /**
         * 计算 StoredFeature 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }

        /**
         * 返回 StoredFeature 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "StoredFeature[type=" + type + ", value=" + value + "]";
        }
    }
}
