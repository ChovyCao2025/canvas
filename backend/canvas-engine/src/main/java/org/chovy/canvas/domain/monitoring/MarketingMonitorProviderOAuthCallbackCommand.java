package org.chovy.canvas.domain.monitoring;

import java.util.Map;

public record MarketingMonitorProviderOAuthCallbackCommand(
        String state,
        String code,
        String error,
        String errorDescription,
        Map<String, Object> metadata) {
}
