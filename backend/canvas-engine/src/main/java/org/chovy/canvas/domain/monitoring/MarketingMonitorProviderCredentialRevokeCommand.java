package org.chovy.canvas.domain.monitoring;

import java.util.Map;

public record MarketingMonitorProviderCredentialRevokeCommand(
        String revokeEndpoint,
        String tokenTypeHint,
        Boolean revokeRefreshToken,
        Boolean disableAfterRevoke,
        Map<String, Object> metadata) {
}
