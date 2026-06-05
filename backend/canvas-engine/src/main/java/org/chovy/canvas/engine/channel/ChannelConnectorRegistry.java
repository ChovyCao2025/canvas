package org.chovy.canvas.engine.channel;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class ChannelConnectorRegistry {

    private final Repository repository;
    private final Map<String, ChannelConnector> realConnectors;

    public ChannelConnectorRegistry(Repository repository, Map<String, ChannelConnector> realConnectors) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.realConnectors = realConnectors == null ? Map.of() : realConnectors;
    }

    public ChannelConnector resolve(Long tenantId, String channel, String provider) {
        String normalizedChannel = normalize(channel);
        String normalizedProvider = normalizeProvider(provider);
        ConnectorConfig config = repository.find(tenant(tenantId), normalizedChannel, normalizedProvider);
        if (config == null) {
            return new DisabledChannelConnector(normalizedChannel + "/" + normalizedProvider + " connector not configured");
        }
        if (config.mode() == ChannelConnector.ConnectorMode.DISABLED) {
            return new DisabledChannelConnector(config.disabledReason());
        }
        if (config.mode() == ChannelConnector.ConnectorMode.SANDBOX) {
            return new SandboxConnector(config);
        }
        ChannelConnector connector = realConnectors.get(config.connectorKey());
        if (connector == null) {
            connector = realConnectors.get(config.channel() + ":" + config.provider());
        }
        if (connector == null) {
            return new DisabledChannelConnector("real connector not registered: " + config.connectorKey());
        }
        return connector;
    }

    public interface Repository {
        ConnectorConfig find(Long tenantId, String channel, String provider);
    }

    public record ConnectorConfig(
            String connectorKey,
            String channel,
            String provider,
            ChannelConnector.ConnectorMode mode,
            String disabledReason,
            String healthStatus,
            String healthMessage,
            String capabilitiesJson) {

        public ConnectorConfig(String connectorKey,
                               String channel,
                               String provider,
                               ChannelConnector.ConnectorMode mode,
                               String disabledReason) {
            this(connectorKey, channel, provider, mode, disabledReason, "UNKNOWN", null, null);
        }
    }

    static ChannelConnector.ConnectorMode parseMode(String value) {
        if (value == null || value.isBlank()) {
            return ChannelConnector.ConnectorMode.DISABLED;
        }
        try {
            return ChannelConnector.ConnectorMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ChannelConnector.ConnectorMode.DISABLED;
        }
    }

    static String normalize(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.trim().toUpperCase(Locale.ROOT);
    }

    static String normalizeProvider(String value) {
        return value == null || value.isBlank() ? "DEFAULT" : normalize(value);
    }

    static Long tenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static class SandboxConnector implements ChannelConnector {

        private final ConnectorConfig config;

        private SandboxConnector(ConnectorConfig config) {
            this.config = config;
        }

        @Override
        public ConnectorMode mode() {
            return ConnectorMode.SANDBOX;
        }

        @Override
        public ConnectorHealth health() {
            return new ConnectorHealth(
                    config.healthStatus() == null ? "UP" : config.healthStatus(),
                    config.healthMessage() == null ? "sandbox connector ready" : config.healthMessage());
        }

        @Override
        public ConnectorCapabilities capabilities() {
            return new ConnectorCapabilities(true, false, Map.of());
        }

        @Override
        public ConnectorSendResult send(ConnectorSendRequest request) {
            String userId = request == null || request.userId() == null ? "anonymous" : request.userId();
            return new ConnectorSendResult(
                    true,
                    "sandbox-" + config.channel() + "-" + userId,
                    "ACCEPTED",
                    null);
        }

        @Override
        public ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload) {
            return new ConnectorReceiptResult(null, "UNSUPPORTED", Map.of());
        }
    }
}
