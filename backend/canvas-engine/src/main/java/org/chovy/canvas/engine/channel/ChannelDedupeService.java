package org.chovy.canvas.engine.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
/**
 * ChannelDedupeService 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class ChannelDedupeService {

    private final Repository repository;
    private final JsonMapper jsonMapper;

    /**
     * 初始化 ChannelDedupeService 实例。
     *
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ChannelDedupeService(Repository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.jsonMapper = JsonMapper.builder()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dedupeGroup dedupe group 参数，用于 reserve 流程中的校验、计算或对象转换。
     * @param contentHash content hash 参数，用于 reserve 流程中的校验、计算或对象转换。
     * @param channel channel 参数，用于 reserve 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param ttl ttl 参数，用于 reserve 流程中的校验、计算或对象转换。
     * @return 返回 reserve 流程生成的业务结果。
     */
    public Decision reserve(Long tenantId,
                            String dedupeGroup,
                            String contentHash,
                            String channel,
                            String userId,
                            Duration ttl) {
        boolean reserved = repository.reserve(
                ChannelConnectorRegistry.tenant(tenantId),
                normalizeGroup(dedupeGroup),
                contentHash,
                ChannelConnectorRegistry.normalize(channel),
                normalizeUser(userId),
                ttl == null ? Duration.ofHours(24) : ttl);
        return new Decision(reserved ? "RESERVED" : "DUPLICATE", contentHash);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dedupeGroup dedupe group 参数，用于 reservePayload 流程中的校验、计算或对象转换。
     * @param channel channel 参数，用于 reservePayload 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param templateId 业务对象 ID，用于定位具体记录。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param ttl ttl 参数，用于 reservePayload 流程中的校验、计算或对象转换。
     * @return 返回 reservePayload 流程生成的业务结果。
     */
    public Decision reservePayload(Long tenantId,
                                   String dedupeGroup,
                                   String channel,
                                   String userId,
                                   String templateId,
                                   Map<String, Object> payload,
                                   Duration ttl) {
        return reserve(tenantId, dedupeGroup, hashPayload(channel, templateId, payload), channel, userId, ttl);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param channel channel 参数，用于 hashPayload 流程中的校验、计算或对象转换。
     * @param templateId 业务对象 ID，用于定位具体记录。
     * @param MapString map string 参数，用于 hashPayload 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回布尔判断结果。
     */
    public String hashPayload(String channel, String templateId, Map<String, Object> payload) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("channel", ChannelConnectorRegistry.normalize(channel));
        canonical.put("templateId", templateId == null ? "" : templateId);
        canonical.put("payload", payload == null ? Map.of() : payload);
        try {
            return sha256(jsonMapper.writeValueAsString(canonical));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to hash channel payload", ex);
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeGroup(String value) {
        return value == null || value.isBlank() ? "default" : value.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeUser(String value) {
        return value == null || value.isBlank() ? "anonymous" : value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sha256 生成的文本或业务键。
     */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /**
     * Repository 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public interface Repository {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param dedupeGroup dedupe group 参数，用于 reserve 流程中的校验、计算或对象转换。
         * @param contentHash content hash 参数，用于 reserve 流程中的校验、计算或对象转换。
         * @param channel channel 参数，用于 reserve 流程中的校验、计算或对象转换。
         * @param userId 业务对象 ID，用于定位具体记录。
         * @param ttl ttl 参数，用于 reserve 流程中的校验、计算或对象转换。
         * @return 返回 reserve 的布尔判断结果。
         */
        boolean reserve(Long tenantId, String dedupeGroup, String contentHash, String channel, String userId, Duration ttl);
    }

    /**
     * Decision 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record Decision(String status, String contentHash) {
    }
}
