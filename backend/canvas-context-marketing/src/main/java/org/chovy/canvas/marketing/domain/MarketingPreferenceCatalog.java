package org.chovy.canvas.marketing.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.chovy.canvas.marketing.api.MarketingPreferenceFacade;

/**
 * 维护MarketingPreference相关的内存业务目录。
 */
public class MarketingPreferenceCatalog {

    /**
     * 保存OPT_IN字段值。
     */
    private static final String OPT_IN = "OPT_IN";

    /**
     * 保存OPT_OUT字段值。
     */
    private static final String OPT_OUT = "OPT_OUT";

    private final List<ConsentState> consents = new ArrayList<>();
    private final List<ChannelState> channels = new ArrayList<>();
    private final List<SuppressionState> suppressions = new ArrayList<>();

    /**
     * 保存nextSuppressionId字段值。
     */
    private long nextSuppressionId = 1L;

    /**
     * 执行report业务操作。
     */
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

    /**
     * 更新consent业务对象。
     */
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

    /**
     * 更新channel业务对象。
     */
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

    /**
     * 执行addSuppression业务操作。
     */
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

    /**
     * 执行deactivateSuppression业务操作。
     */
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

    /**
     * 转换为consentRow对象。
     */
    private static MarketingPreferenceFacade.ConsentRow toConsentRow(ConsentState row) {
        return new MarketingPreferenceFacade.ConsentRow(row.channel, row.consentStatus, row.source, row.updatedAt);
    }

    /**
     * 转换为channelRow对象。
     */
    private static MarketingPreferenceFacade.ChannelRow toChannelRow(ChannelState row) {
        boolean reachable = row.enabled && row.address != null && !row.address.isBlank();
        return new MarketingPreferenceFacade.ChannelRow(row.channel, row.address, row.enabled, row.verified,
                reachable, row.metadata, row.updatedAt);
    }

    /**
     * 转换为suppressionRow对象。
     */
    private static MarketingPreferenceFacade.SuppressionRow toSuppressionRow(SuppressionState row) {
        return new MarketingPreferenceFacade.SuppressionRow(row.id, row.channel, row.reason, row.active,
                suppressionState(row), row.expiresAt, row.createdAt, row.updatedAt);
    }

    /**
     * 执行suppressionState业务操作。
     */
    private static String suppressionState(SuppressionState row) {
        if (!row.active) {
            return "INACTIVE";
        }
        if (row.expiresAt != null && !row.expiresAt.isAfter(LocalDateTime.now())) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    /**
     * 规范化consentStatus输入值。
     */
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

    /**
     * 规范化输入值。
     */
    private static String normalize(String channel) {
        return defaultString(channel, "ALL").toUpperCase(Locale.ROOT);
    }

    /**
     * 执行defaultString业务操作。
     */
    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 提供ConsentState的业务能力。
     */
    private static final class ConsentState {
        /**
         * 保存tenantId字段值。
         */
        private final Long tenantId;

        /**
         * 保存userId字段值。
         */
        private final String userId;

        /**
         * 保存channel字段值。
         */
        private final String channel;

        /**
         * 保存consentStatus字段值。
         */
        private String consentStatus;

        /**
         * 保存source字段值。
         */
        private String source;

        /**
         * 保存updatedAt字段值。
         */
        private LocalDateTime updatedAt;

        /**
         * 创建ConsentState实例。
         */
        private ConsentState(Long tenantId, String userId, String channel) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
        }

        /**
         * 执行matches业务操作。
         */
        private boolean matches(Long tenantId, String userId) {
            return Objects.equals(this.tenantId, tenantId) && Objects.equals(this.userId, userId);
        }

        /**
         * 执行matches业务操作。
         */
        private boolean matches(Long tenantId, String userId, String channel) {
            return matches(tenantId, userId) && Objects.equals(this.channel, channel);
        }
    }

    /**
     * 提供ChannelState的业务能力。
     */
    private static final class ChannelState {
        /**
         * 保存tenantId字段值。
         */
        private final Long tenantId;

        /**
         * 保存userId字段值。
         */
        private final String userId;

        /**
         * 保存channel字段值。
         */
        private final String channel;

        /**
         * 保存address字段值。
         */
        private String address;

        /**
         * 保存enabled字段值。
         */
        private boolean enabled;

        /**
         * 保存verified字段值。
         */
        private boolean verified;

        /**
         * 保存metadata字段值。
         */
        private String metadata;

        /**
         * 保存updatedAt字段值。
         */
        private LocalDateTime updatedAt;

        /**
         * 创建ChannelState实例。
         */
        private ChannelState(Long tenantId, String userId, String channel) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
        }

        /**
         * 执行matches业务操作。
         */
        private boolean matches(Long tenantId, String userId) {
            return Objects.equals(this.tenantId, tenantId) && Objects.equals(this.userId, userId);
        }

        /**
         * 执行matches业务操作。
         */
        private boolean matches(Long tenantId, String userId, String channel) {
            return matches(tenantId, userId) && Objects.equals(this.channel, channel);
        }
    }

    /**
     * 提供SuppressionState的业务能力。
     */
    private static final class SuppressionState {
        /**
         * 保存id字段值。
         */
        private final Long id;

        /**
         * 保存tenantId字段值。
         */
        private final Long tenantId;

        /**
         * 保存userId字段值。
         */
        private final String userId;

        /**
         * 保存channel字段值。
         */
        private final String channel;

        /**
         * 保存reason字段值。
         */
        private final String reason;

        /**
         * 保存active字段值。
         */
        private boolean active;

        /**
         * 保存expiresAt字段值。
         */
        private final LocalDateTime expiresAt;

        /**
         * 保存createdAt字段值。
         */
        private final LocalDateTime createdAt;

        /**
         * 保存updatedAt字段值。
         */
        private LocalDateTime updatedAt;

        /**
         * 创建SuppressionState实例。
         */
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

        /**
         * 执行matches业务操作。
         */
        private boolean matches(Long tenantId, String userId) {
            return Objects.equals(this.tenantId, tenantId) && Objects.equals(this.userId, userId);
        }
    }
}
