package org.chovy.canvas.platform.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChannelConnectorCatalog {

    private final List<Map<String, Object>> connectors = new ArrayList<>();

    public ChannelConnectorCatalog() {
        connectors.add(connector(1001L, 42L, "email-sendgrid", "EMAIL", "SENDGRID", "SANDBOX",
                "UNKNOWN", "sandbox connector ready"));
        connectors.add(connector(1002L, 42L, "sms-twilio", "SMS", "TWILIO", "SANDBOX",
                "UNKNOWN", "sandbox connector ready"));
    }

    public List<Map<String, Object>> connectors(Long tenantId) {
        return connectors.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .map(ChannelConnectorCatalog::copy)
                .toList();
    }

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

    public Map<String, Object> updateMode(Long tenantId, Long connectorId, String mode, String reason, String actor) {
        Map<String, Object> connector = requireConnector(tenantId, connectorId);
        String normalizedMode = normalizeMode(mode);
        connector.put("mode", normalizedMode);
        connector.put("disabledReason", "DISABLED".equals(normalizedMode) ? requireReason(reason) : null);
        connector.put("operator", actor);
        return copy(connector);
    }

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
        connector.put("lastCheckedAt", Instant.EPOCH.toString());
        return ordered(
                "tenantId", tenantId,
                "id", connectorId,
                "status", status,
                "message", message,
                "checkedAt", Instant.EPOCH.toString());
    }

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

    public List<Map<String, Object>> dedupeRecords(Long tenantId) {
        return List.of(ordered(
                "tenantId", tenantId,
                "dedupeGroup", "campaign-1",
                "contentHash", "hash-email-001",
                "channel", "EMAIL",
                "userId", "user-1",
                "expiresAt", Instant.EPOCH.plusSeconds(3600).toString()));
    }

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

    private static String normalizeMode(String mode) {
        String value = requireCode(mode, "mode is required");
        if (!List.of("SANDBOX", "REAL", "DISABLED").contains(value)) {
            throw new IllegalArgumentException("mode must be one of SANDBOX, REAL, DISABLED");
        }
        return value;
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required when disabling a connector");
        }
        return reason.trim();
    }

    private static String requireCode(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
