package org.chovy.canvas.engine.channel;

import java.util.Map;

public interface ChannelConnector {

    ConnectorMode mode();

    ConnectorHealth health();

    ConnectorCapabilities capabilities();

    ConnectorSendResult send(ConnectorSendRequest request);

    ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload);

    enum ConnectorMode {
        REAL,
        SANDBOX,
        DISABLED
    }

    record ConnectorHealth(String status, String message) {
    }

    record ConnectorCapabilities(boolean send, boolean receipt, Map<String, Object> attributes) {
    }

    record ConnectorSendRequest(
            Long tenantId,
            String channel,
            String provider,
            String userId,
            Map<String, Object> payload) {
    }

    record ConnectorSendResult(boolean accepted, String externalMessageId, String status, String reason) {
    }

    record ConnectorReceiptResult(String externalMessageId, String status, Map<String, Object> attributes) {
    }
}
