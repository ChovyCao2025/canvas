package org.chovy.canvas.domain.monitoring;

public record MarketingMonitorProviderCredentialDueRefreshCommand(
        Integer windowMinutes,
        Integer limit) {
}
