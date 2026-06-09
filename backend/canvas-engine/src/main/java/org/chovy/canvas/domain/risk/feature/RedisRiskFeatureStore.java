package org.chovy.canvas.domain.risk.feature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Redis 风控特征存储，按租户、特征键和主体哈希隔离预计算在线特征。
 */
public class RedisRiskFeatureStore implements RiskFeatureStore {

    private static final String KEY_PREFIX = "risk:feature:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * 创建 Redis 特征存储，未传入 ObjectMapper 时使用默认 JSON 映射器。
     */
    public RedisRiskFeatureStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 写入指定主体的在线特征值，并设置过期时间。
     */
    @Override
    public void set(Long tenantId, String featureKey, String subjectHash, Object value, Duration ttl) {
        String payload = serialize(value);
        redis.opsForValue().set(key(tenantId, featureKey, subjectHash), payload, ttl);
    }

    /**
     * 读取指定主体的在线特征值，载荷损坏时删除缓存并返回空。
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            // 损坏的特征载荷会被清理，后续请求可由上游链路重新写入。
            redis.delete(key);
            return Optional.empty();
        }
    }

    /**
     * 将运行时值包装为带类型信封的 JSON 字符串。
     */
    private String serialize(Object value) {
        // Redis 只能存字符串，类型信封用于保留规则评估依赖的数值、布尔和字符串语义。
        String type = value instanceof Number ? "NUMBER" : value instanceof Boolean ? "BOOLEAN" : "STRING";
        try {
            return objectMapper.writeValueAsString(new StoredFeature(type, value));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("risk feature value is not JSON serializable", e);
        }
    }

    /**
     * 解析缓存载荷，并按类型信封恢复原始值。
     */
    private Object parse(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText("").toUpperCase(Locale.ROOT);
            JsonNode value = root.path("value");
            return switch (type) {
                case "NUMBER" -> numberValue(value);
                case "BOOLEAN" -> value.asBoolean();
                case "STRING" -> value.asText();
                default -> null;
            };
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("risk feature value is corrupt", e);
        }
    }

    /**
     * 构造 Redis 键，键中的主体部分必须已经是哈希值。
     */
    private String key(Long tenantId, String featureKey, String subjectHash) {
        // 主体组件在进入特征存储前必须已完成哈希，避免 Redis 键暴露原始标识。
        return KEY_PREFIX + tenantId + ":" + featureKey + ":" + subjectHash;
    }

    /**
     * 将 JSON 数值节点恢复为整数、长整数或浮点数。
     */
    private Object numberValue(JsonNode value) {
        if (!value.isNumber()) {
            return null;
        }
        String text = value.asText();
        // 整数值优先恢复为 Integer/Long，保证等值规则比较结果可预测。
        if (!text.contains(".") && !text.contains("e") && !text.contains("E")) {
            try {
                return Integer.parseInt(text);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                return Long.parseLong(text);
            }
        }
        return value.asDouble();
    }

    /**
     * Redis 中保存的特征载荷信封。
     */
    private record StoredFeature(String type, Object value) {
    }
}
