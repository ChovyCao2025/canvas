package org.chovy.canvas.marketing.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.chovy.canvas.marketing.api.MarketingPreferenceFacade;

public class MarketingPreferenceCatalog {

    private static final String OPT_IN = "OPT_IN";
    private static final String OPT_OUT = "OPT_OUT";

    private final List<ConsentState> consents = new ArrayList<>();
    private final List<ChannelState> channels = new ArrayList<>();
    private final List<SuppressionState> suppressions = new ArrayList<>();
    private long nextSuppressionId = 1L;

    public synchronized MarketingPreferenceFacade.PreferenceReport report(Long tenantId, String userId) {
        List<MarketingPreferenceFacade.ConsentRow> consentRows = consents.stream()
                .filter(row -> row.matches(tenantId, userId))
                .sorted(Comparator.comparing(row -> row.channel))
                .map(MarketingPreferenceCatalog::toConsentRow)
                .toList();
        List<MarketingPreferenceFacade.ChannelRow> channelRows = channels.stream()
                .filter(row -> row.matches(tenantId, userId))
                .sorted(Comparator.comparing(row -> row.channel))
                .map(MarketingPreferenceCatalog::toChannelRow)
                .toList();
        List<MarketingPreferenceFacade.SuppressionRow> suppressionRows = suppressions.stream()
                .filter(row -> row.matches(tenantId, userId))
                .sorted(Comparator.comparing((SuppressionState row) -> row.createdAt).reversed())
                .map(MarketingPreferenceCatalog::toSuppressionRow)
                .toList();
        MarketingPreferenceFacade.PreferenceSummary summary = new MarketingPreferenceFacade.PreferenceSummary(
                channelRows.size(),
                (int) consentRows.stream().filter(row -> OPT_IN.equalsIgnoreCase(row.consentStatus())).count(),
                (int) consentRows.stream().filter(row -> OPT_OUT.equalsIgnoreCase(row.consentStatus())).count(),
                (int) suppressionRows.stream().filter(row -> "ACTIVE".equals(row.state())).count(),
                (int) channelRows.stream().filter(MarketingPreferenceFacade.ChannelRow::reachable).count());
        return new MarketingPreferenceFacade.PreferenceReport(userId, consentRows, channelRows, suppressionRows,
                summary);
    }

    public synchronized MarketingPreferenceFacade.ConsentRow updateConsent(
            Long tenantId,
            String userId,
            MarketingPreferenceFacade.ConsentUpdateCommand command) {
        String channel = normalize(command.channel());
        ConsentState row = consents.stream()
                .filter(candidate -> candidate.matches(tenantId, userId, channel))
                .findFirst()
                .orElseGet(() -> {
                    ConsentState created = new ConsentState(tenantId, userId, channel);
                    consents.add(created);
                    return created;
                });
        row.consentStatus = normalizeConsentStatus(command.consentStatus());
        row.source = defaultString(command.source(), row.source == null ? "operator" : row.source);
        row.updatedAt = LocalDateTime.now();
        return toConsentRow(row);
    }

    public synchronized MarketingPreferenceFacade.ChannelRow updateChannel(
            Long tenantId,
            String userId,
            MarketingPreferenceFacade.ChannelUpdateCommand command) {
        String channel = normalize(command.channel());
        ChannelState row = channels.stream()
                .filter(candidate -> candidate.matches(tenantId, userId, channel))
                .findFirst()
                .orElseGet(() -> {
                    ChannelState created = new ChannelState(tenantId, userId, channel);
                    channels.add(created);
                    return created;
                });
        row.address = defaultString(command.address(), "");
        row.enabled = !Boolean.FALSE.equals(command.enabled());
        row.verified = Boolean.TRUE.equals(command.verified());
        row.metadata = command.metadata();
        row.updatedAt = LocalDateTime.now();
        return toChannelRow(row);
    }

    public synchronized MarketingPreferenceFacade.SuppressionRow addSuppression(
            Long tenantId,
            String userId,
            MarketingPreferenceFacade.SuppressionCreateCommand command) {
        SuppressionState row = new SuppressionState(
                nextSuppressionId++,
                tenantId,
                userId,
                normalize(command.channel()),
                defaultString(command.reason(), "operator"),
                !Boolean.FALSE.equals(command.active()),
                command.expiresAt(),
                LocalDateTime.now(),
                LocalDateTime.now());
        suppressions.add(row);
        return toSuppressionRow(row);
    }

    public synchronized void deactivateSuppression(Long tenantId, Long suppressionId) {
        suppressions.stream()
                .filter(row -> Objects.equals(row.tenantId, tenantId))
                .filter(row -> Objects.equals(row.id, suppressionId))
                .findFirst()
                .ifPresent(row -> {
                    row.active = false;
                    row.updatedAt = LocalDateTime.now();
                });
    }

    private static MarketingPreferenceFacade.ConsentRow toConsentRow(ConsentState row) {
        return new MarketingPreferenceFacade.ConsentRow(row.channel, row.consentStatus, row.source, row.updatedAt);
    }

    private static MarketingPreferenceFacade.ChannelRow toChannelRow(ChannelState row) {
        boolean reachable = row.enabled && row.address != null && !row.address.isBlank();
        return new MarketingPreferenceFacade.ChannelRow(row.channel, row.address, row.enabled, row.verified,
                reachable, row.metadata, row.updatedAt);
    }

    private static MarketingPreferenceFacade.SuppressionRow toSuppressionRow(SuppressionState row) {
        return new MarketingPreferenceFacade.SuppressionRow(row.id, row.channel, row.reason, row.active,
                suppressionState(row), row.expiresAt, row.createdAt, row.updatedAt);
    }

    private static String suppressionState(SuppressionState row) {
        if (!row.active) {
            return "INACTIVE";
        }
        if (row.expiresAt != null && !row.expiresAt.isAfter(LocalDateTime.now())) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    private static String normalizeConsentStatus(String status) {
        String normalized = defaultString(status, OPT_OUT).toUpperCase(Locale.ROOT);
        if ("OPTIN".equals(normalized)) {
            return OPT_IN;
        }
        if ("OPTOUT".equals(normalized)) {
            return OPT_OUT;
        }
        if (OPT_IN.equals(normalized) || OPT_OUT.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported consent status: " + status);
    }

    private static String normalize(String channel) {
        return defaultString(channel, "ALL").toUpperCase(Locale.ROOT);
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static final class ConsentState {
        private final Long tenantId;
        private final String userId;
        private final String channel;
        private String consentStatus;
        private String source;
        private LocalDateTime updatedAt;

        private ConsentState(Long tenantId, String userId, String channel) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
        }

        private boolean matches(Long tenantId, String userId) {
            return Objects.equals(this.tenantId, tenantId) && Objects.equals(this.userId, userId);
        }

        private boolean matches(Long tenantId, String userId, String channel) {
            return matches(tenantId, userId) && Objects.equals(this.channel, channel);
        }
    }

    private static final class ChannelState {
        private final Long tenantId;
        private final String userId;
        private final String channel;
        private String address;
        private boolean enabled;
        private boolean verified;
        private String metadata;
        private LocalDateTime updatedAt;

        private ChannelState(Long tenantId, String userId, String channel) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
        }

        private boolean matches(Long tenantId, String userId) {
            return Objects.equals(this.tenantId, tenantId) && Objects.equals(this.userId, userId);
        }

        private boolean matches(Long tenantId, String userId, String channel) {
            return matches(tenantId, userId) && Objects.equals(this.channel, channel);
        }
    }

    private static final class SuppressionState {
        private final Long id;
        private final Long tenantId;
        private final String userId;
        private final String channel;
        private final String reason;
        private boolean active;
        private final LocalDateTime expiresAt;
        private final LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private SuppressionState(
                Long id,
                Long tenantId,
                String userId,
                String channel,
                String reason,
                boolean active,
                LocalDateTime expiresAt,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
            this.reason = reason;
            this.active = active;
            this.expiresAt = expiresAt;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        private boolean matches(Long tenantId, String userId) {
            return Objects.equals(this.tenantId, tenantId) && Objects.equals(this.userId, userId);
        }
    }
}
