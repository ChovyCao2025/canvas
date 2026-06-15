package org.chovy.canvas.marketing.domain;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.chovy.canvas.marketing.api.MarketingPolicyFacade.ChannelCommand;
import org.chovy.canvas.marketing.api.MarketingPolicyFacade.ChannelView;
import org.chovy.canvas.marketing.api.MarketingPolicyFacade.ConsentCommand;
import org.chovy.canvas.marketing.api.MarketingPolicyFacade.ConsentView;
import org.chovy.canvas.marketing.api.MarketingPolicyFacade.PolicyState;
import org.chovy.canvas.marketing.api.MarketingPolicyFacade.SuppressionCommand;
import org.chovy.canvas.marketing.api.MarketingPolicyFacade.SuppressionView;

public class MarketingPolicyCatalog {

    private static final String ALL_CHANNELS = "ALL";

    private final Map<PolicyChannelKey, ConsentView> consents = new LinkedHashMap<>();
    private final Map<SuppressionKey, SuppressionView> suppressions = new LinkedHashMap<>();
    private final Map<PolicyChannelKey, ChannelView> channels = new LinkedHashMap<>();
    private long consentIds;
    private long suppressionIds;
    private long channelIds;

    public synchronized PolicyState policyState(Long tenantId, String userId, String channel) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedUserId = required(userId, "userId");
        String scopedChannel = normalizeRequired(channel, "channel");
        ConsentView consent = consents.get(new PolicyChannelKey(scopedTenantId, scopedUserId, scopedChannel));
        List<SuppressionView> matchingSuppressions = suppressions.values().stream()
                .filter(row -> Objects.equals(row.tenantId(), scopedTenantId))
                .filter(row -> Objects.equals(row.userId(), scopedUserId))
                .filter(row -> Objects.equals(row.channel(), scopedChannel) || Objects.equals(row.channel(), ALL_CHANNELS))
                .sorted(Comparator.comparing(SuppressionView::id))
                .toList();
        ChannelView customerChannel = channels.get(new PolicyChannelKey(scopedTenantId, scopedUserId, scopedChannel));
        return new PolicyState(scopedUserId, scopedChannel, consent, matchingSuppressions, customerChannel);
    }

    public synchronized ConsentView upsertConsent(Long tenantId, ConsentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("consent request is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String userId = required(command.userId(), "userId");
        String channel = normalizeRequired(command.channel(), "channel");
        String consentStatus = normalizeRequired(command.consentStatus(), "consentStatus");
        PolicyChannelKey key = new PolicyChannelKey(scopedTenantId, userId, channel);
        ConsentView existing = consents.get(key);
        ConsentView view = new ConsentView(existing == null ? ++consentIds : existing.id(), scopedTenantId, userId,
                channel, consentStatus, optional(command.source()));
        consents.put(key, view);
        return view;
    }

    public synchronized SuppressionView upsertSuppression(Long tenantId, SuppressionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("suppression request is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String userId = required(command.userId(), "userId");
        String channel = normalizeChannelOrAll(command.channel());
        String reason = required(command.reason(), "reason");
        SuppressionKey key = new SuppressionKey(scopedTenantId, userId, channel, reason);
        SuppressionView existing = suppressions.get(key);
        SuppressionView view = new SuppressionView(existing == null ? ++suppressionIds : existing.id(),
                scopedTenantId, userId, channel, reason, Boolean.FALSE.equals(command.active()) ? 0 : 1,
                command.expiresAt());
        suppressions.put(key, view);
        return view;
    }

    public synchronized ChannelView upsertChannel(Long tenantId, ChannelCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("channel request is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String userId = required(command.userId(), "userId");
        String channel = normalizeRequired(command.channel(), "channel");
        PolicyChannelKey key = new PolicyChannelKey(scopedTenantId, userId, channel);
        ChannelView existing = channels.get(key);
        ChannelView view = new ChannelView(existing == null ? ++channelIds : existing.id(), scopedTenantId, userId,
                channel, optional(command.address()), command.enabled() == null ? 1 : command.enabled(),
                command.verified() == null ? 0 : command.verified(), optional(command.metadata()));
        channels.put(key, view);
        return view;
    }

    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String normalizeChannelOrAll(String value) {
        return value == null || value.isBlank() ? ALL_CHANNELS : normalizeRequired(value, "channel");
    }

    private static String normalizeRequired(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record PolicyChannelKey(Long tenantId, String userId, String channel) {
    }

    private record SuppressionKey(Long tenantId, String userId, String channel, String reason) {
    }
}
