package org.chovy.canvas.engine.channel;

import java.util.Map;

public class DisabledChannelConnector implements ChannelConnector {

    private final String reason;

    public DisabledChannelConnector(String reason) {
        this.reason = reason == null || reason.isBlank() ? "connector disabled" : reason;
    }

    @Override
    public ConnectorMode mode() {
        return ConnectorMode.DISABLED;
    }

    @Override
    public ConnectorHealth health() {
        return new ConnectorHealth("DISABLED", reason);
    }

    @Override
    public ConnectorCapabilities capabilities() {
        return new ConnectorCapabilities(false, false, Map.of());
    }

    @Override
    public ConnectorSendResult send(ConnectorSendRequest request) {
        return new ConnectorSendResult(false, null, "DISABLED", reason);
    }

    @Override
    public ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload) {
        return new ConnectorReceiptResult(null, "UNSUPPORTED", Map.of());
    }
}
