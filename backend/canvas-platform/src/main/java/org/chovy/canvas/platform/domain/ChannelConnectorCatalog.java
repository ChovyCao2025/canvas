package org.chovy.canvas.platform.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 渠道连接器目录，保存演示连接器、限流、健康检查和回退数据。
 */
public class ChannelConnectorCatalog {

    /**
     * 内存中的渠道连接器记录。
     */
    private final List<Map<String, Object>> connectors = new ArrayList<>();

    /**
     * 创建渠道连接器目录并写入固定演示连接器。
     */
    public ChannelConnectorCatalog() {
        connectors.add(connector(1001L, 42L, "email-sendgrid", "EMAIL", "SENDGRID", "SANDBOX",
                "UNKNOWN", "sandbox connector ready"));
        connectors.add(connector(1002L, 42L, "sms-twilio", "SMS", "TWILIO", "SANDBOX",
                "UNKNOWN", "sandbox connector ready"));
    }

    /**
     * 查询租户下的连接器。
     *
     * @param tenantId 租户标识
     * @return 连接器列表
     */
    public List<Map<String, Object>> connectors(Long tenantId) {
        return connectors.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .map(ChannelConnectorCatalog::copy)
                .toList();
    }

    /**
     * 查询租户的渠道限流配置。
     *
     * @param tenantId 租户标识
     * @return 限流配置列表
     */
    public List<Map<String, Object>> limits(Long tenantId) {
        return List.of(
                ordered(
                        "channel", "EMAIL",
                        "provider", "SENDGRID",
                        "operation", "SEND",
                        "perSecondLimit", 50,
                        "dailyLimit", 100000L,
                        "failClosed", true,
                        "updatedAt", Instant.EPOCH.toString()),
                ordered(
                        "channel", "SMS",
                        "provider", "TWILIO",
                        "operation", "SEND",
                        "perSecondLimit", 10,
                        "dailyLimit", 20000L,
                        "failClosed", true,
                        "updatedAt", Instant.EPOCH.toString()));
    }

    /**
     * 更新连接器运行模式。
     *
     * @param tenantId 租户标识
     * @param connectorId 连接器标识
     * @param mode 目标模式
     * @param reason 变更原因
     * @param actor 操作者
     * @return 更新后的连接器记录
     */
    public Map<String, Object> updateMode(Long tenantId, Long connectorId, String mode, String reason, String actor) {
        Map<String, Object> connector = requireConnector(tenantId, connectorId);
        String normalizedMode = normalizeMode(mode);
        connector.put("mode", normalizedMode);
        connector.put("disabledReason", "DISABLED".equals(normalizedMode) ? requireReason(reason) : null);
        connector.put("operator", actor);
        return copy(connector);
    }

    /**
     * 执行连接器健康检查。
     *
     * @param tenantId 租户标识
     * @param connectorId 连接器标识
     * @return 健康检查结果
     */
    public Map<String, Object> healthTest(Long tenantId, Long connectorId) {
        Map<String, Object> connector = requireConnector(tenantId, connectorId);
        String mode = String.valueOf(connector.get("mode"));
        String status = switch (mode) {
            case "SANDBOX", "REAL" -> "UP";
            case "DISABLED" -> "DISABLED";
            default -> "UNKNOWN";
        };
        String message = switch (mode) {
            case "SANDBOX" -> "sandbox connector ready";
            case "REAL" -> "provider health probe accepted";
            case "DISABLED" -> String.valueOf(connector.getOrDefault("disabledReason", "connector disabled"));
            default -> "unknown connector mode";
        };
        connector.put("healthStatus", status);
        connector.put("healthMessage", message);
        // 健康检查时间固定为 epoch，保证演示响应和测试断言稳定。
        connector.put("lastCheckedAt", Instant.EPOCH.toString());
        return ordered(
                "tenantId", tenantId,
                "id", connectorId,
                "status", status,
                "message", message,
                "checkedAt", Instant.EPOCH.toString());
    }

    /**
     * 校验主连接器和回退连接器是否不同。
     *
     * @param tenantId 租户标识
     * @param channel 主通道
     * @param provider 主供应方
     * @param fallbackChannel 回退通道
     * @param fallbackProvider 回退供应方
     * @return 校验结果
     */
    public Map<String, Object> validateFallback(Long tenantId, String channel, String provider,
                                                String fallbackChannel, String fallbackProvider) {
        String normalizedChannel = requireCode(channel, "channel is required");
        String normalizedProvider = requireCode(provider, "provider is required");
        String normalizedFallbackChannel = requireCode(fallbackChannel, "fallbackChannel is required");
        String normalizedFallbackProvider = requireCode(fallbackProvider, "fallbackProvider is required");
        if (normalizedChannel.equals(normalizedFallbackChannel)
                && normalizedProvider.equals(normalizedFallbackProvider)) {
            return ordered(
                    "tenantId", tenantId,
                    "valid", false,
                    "message", "fallback connector must differ from primary connector",
                    "channel", normalizedChannel,
                    "provider", normalizedProvider,
                    "fallbackChannel", normalizedFallbackChannel,
                    "fallbackProvider", normalizedFallbackProvider);
        }
        return ordered(
                "tenantId", tenantId,
                "valid", true,
                "message", "ok",
                "channel", normalizedChannel,
                "provider", normalizedProvider,
                "fallbackChannel", normalizedFallbackChannel,
                "fallbackProvider", normalizedFallbackProvider);
    }

    /**
     * 查询回退决策演示记录。
     *
     * @param tenantId 租户标识
     * @return 回退决策列表
     */
    public List<Map<String, Object>> fallbackDecisions(Long tenantId) {
        return List.of(ordered(
                "tenantId", tenantId,
                "originalChannel", "EMAIL",
                "originalProvider", "SENDGRID",
                "finalChannel", "SMS",
                "finalProvider", "TWILIO",
                "decisionReason", "provider limit",
                "createdAt", Instant.EPOCH.toString()));
    }

    /**
     * 查询去重演示记录。
     *
     * @param tenantId 租户标识
     * @return 去重记录列表
     */
    public List<Map<String, Object>> dedupeRecords(Long tenantId) {
        return List.of(ordered(
                "tenantId", tenantId,
                "dedupeGroup", "campaign-1",
                "contentHash", "hash-email-001",
                "channel", "EMAIL",
                "userId", "user-1",
                "expiresAt", Instant.EPOCH.plusSeconds(3600).toString()));
    }

    /**
     * 查询并校验租户内连接器。
     *
     * @param tenantId 租户标识
     * @param connectorId 连接器标识
     * @return 内部连接器记录
     */
    private Map<String, Object> requireConnector(Long tenantId, Long connectorId) {
        if (connectorId == null || connectorId <= 0) {
            throw new IllegalArgumentException("channel connector id is required");
        }
        Map<String, Object> connector = connectors.stream()
                .filter(row -> Objects.equals(row.get("id"), connectorId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Channel connector not found: " + connectorId));
        if (!Objects.equals(connector.get("tenantId"), tenantId)) {
            throw new IllegalArgumentException("channel connector tenant mismatch: " + connectorId);
        }
        return connector;
    }

    /**
     * 构造连接器记录。
     *
     * @param id 连接器标识
     * @param tenantId 租户标识
     * @param connectorKey 连接器稳定键
     * @param channel 通道
     * @param provider 供应方
     * @param mode 运行模式
     * @param healthStatus 健康状态
     * @param healthMessage 健康说明
     * @return 连接器记录
     */
    private static Map<String, Object> connector(Long id, Long tenantId, String connectorKey, String channel,
                                                 String provider, String mode, String healthStatus,
                                                 String healthMessage) {
        return ordered(
                "id", id,
                "tenantId", tenantId,
                "connectorKey", connectorKey,
                "channel", channel,
                "provider", provider,
                "mode", mode,
                "healthStatus", healthStatus,
                "healthMessage", healthMessage);
    }

    /**
     * 标准化连接器运行模式。
     *
     * @param mode 原始模式
     * @return 标准化模式
     */
    private static String normalizeMode(String mode) {
        String value = requireCode(mode, "mode is required");
        if (!List.of("SANDBOX", "REAL", "DISABLED").contains(value)) {
            throw new IllegalArgumentException("mode must be one of SANDBOX, REAL, DISABLED");
        }
        return value;
    }

    /**
     * 校验禁用原因。
     *
     * @param reason 原始原因
     * @return 修剪后的原因
     */
    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required when disabling a connector");
        }
        return reason.trim();
    }

    /**
     * 校验并标准化代码类文本。
     *
     * @param value 原始文本
     * @param message 校验失败时使用的异常消息
     * @return 大写代码值
     */
    private static String requireCode(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 复制记录，避免调用方直接修改内部状态。
     *
     * @param source 原始记录
     * @return 复制后的记录
     */
    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    /**
     * 按参数顺序构造有序 Map。
     *
     * @param pairs 键值交替排列的参数
     * @return 有序 Map
     */
    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
