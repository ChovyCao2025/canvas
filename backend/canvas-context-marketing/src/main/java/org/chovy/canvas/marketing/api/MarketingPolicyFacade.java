package org.chovy.canvas.marketing.api;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketingPolicyFacade {

    PolicyState policyState(Long tenantId, String userId, String channel);

    ConsentView upsertConsent(Long tenantId, ConsentCommand command);

    SuppressionView upsertSuppression(Long tenantId, SuppressionCommand command);

    ChannelView upsertChannel(Long tenantId, ChannelCommand command);

    record ConsentCommand(String userId, String channel, String consentStatus, String source) {
    }

    record SuppressionCommand(String userId, String channel, String reason, Boolean active, LocalDateTime expiresAt) {
    }

    record ChannelCommand(
            String userId,
            String channel,
            String address,
            Integer enabled,
            Integer verified,
            String metadata) {
    }

    record PolicyState(
            String userId,
            String channel,
            ConsentView consent,
            List<SuppressionView> suppressions,
            ChannelView customerChannel) {
    }

    record ConsentView(
            Long id,
            Long tenantId,
            String userId,
            String channel,
            String consentStatus,
            String source) {
    }

    record SuppressionView(
            Long id,
            Long tenantId,
            String userId,
            String channel,
            String reason,
            int active,
            LocalDateTime expiresAt) {
    }

    record ChannelView(
            Long id,
            Long tenantId,
            String userId,
            String channel,
            String address,
            int enabled,
            int verified,
            String metadata) {
    }
}
