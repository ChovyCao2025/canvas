package org.chovy.canvas.marketing.api;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketingPreferenceFacade {

    PreferenceReport report(Long tenantId, String userId);

    ConsentRow updateConsent(Long tenantId, String userId, ConsentUpdateCommand command);

    ChannelRow updateChannel(Long tenantId, String userId, ChannelUpdateCommand command);

    SuppressionRow addSuppression(Long tenantId, String userId, SuppressionCreateCommand command);

    void deactivateSuppression(Long tenantId, Long suppressionId);

    record PreferenceReport(
            String userId,
            List<ConsentRow> consents,
            List<ChannelRow> channels,
            List<SuppressionRow> suppressions,
            PreferenceSummary summary) {
    }

    record ConsentRow(String channel, String consentStatus, String source, LocalDateTime updatedAt) {
    }

    record ChannelRow(
            String channel,
            String address,
            boolean enabled,
            boolean verified,
            boolean reachable,
            String metadata,
            LocalDateTime updatedAt) {
    }

    record SuppressionRow(
            Long id,
            String channel,
            String reason,
            boolean active,
            String state,
            LocalDateTime expiresAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    record PreferenceSummary(
            int totalChannels,
            int optInCount,
            int optOutCount,
            int activeSuppressionCount,
            int reachableChannelCount) {
    }

    record ConsentUpdateCommand(String channel, String consentStatus, String source) {
    }

    record ChannelUpdateCommand(String channel, String address, Boolean enabled, Boolean verified, String metadata) {
    }

    record SuppressionCreateCommand(String channel, String reason, Boolean active, LocalDateTime expiresAt) {
    }
}
