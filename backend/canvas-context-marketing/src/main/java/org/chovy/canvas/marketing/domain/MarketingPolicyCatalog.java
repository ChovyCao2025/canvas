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

/**
 * 维护MarketingPolicy相关的内存业务目录。
 */
public class MarketingPolicyCatalog {

    /**
     * 保存ALL_CHANNELS字段值。
     */
    private static final String ALL_CHANNELS = "ALL";

    private final Map<PolicyChannelKey, ConsentView> consents = new LinkedHashMap<>();
    private final Map<SuppressionKey, SuppressionView> suppressions = new LinkedHashMap<>();
    private final Map<PolicyChannelKey, ChannelView> channels = new LinkedHashMap<>();

    /**
     * 保存consentIds字段值。
     */
    private long consentIds;

    /**
     * 保存suppressionIds字段值。
     */
    private long suppressionIds;

    /**
     * 保存channelIds字段值。
     */
    private long channelIds;

    /**
     * 执行policyState业务操作。
     */
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

    /**
     * 执行upsertConsent业务操作。
     */
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

    /**
     * 执行upsertSuppression业务操作。
     */
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

    /**
     * 执行upsertChannel业务操作。
     */
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

    /**
     * 规范化tenant输入值。
     */
    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 规范化channelOrAll输入值。
     */
    private static String normalizeChannelOrAll(String value) {
        return value == null || value.isBlank() ? ALL_CHANNELS : normalizeRequired(value, "channel");
    }

    /**
     * 规范化required输入值。
     */
    private static String normalizeRequired(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    /**
     * 校验并返回d必填值。
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 执行optional业务操作。
     */
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 表示PolicyChannelKey使用的稳定匹配键。
     */
    private static final class PolicyChannelKey {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * 创建PolicyChannelKey实例。
         */
        public PolicyChannelKey(Long tenantId, String userId, String channel) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回渠道标识。
         */
        public String channel() {
            return channel;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PolicyChannelKey that = (PolicyChannelKey) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(channel, that.channel);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, userId, channel);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "PolicyChannelKey[tenantId=" + tenantId + ", userId=" + userId + ", channel=" + channel + "]";
        }
    }

    /**
     * 表示SuppressionKey使用的稳定匹配键。
     */
    private static final class SuppressionKey {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * 问题原因。
         */
        private final String reason;

        /**
         * 创建SuppressionKey实例。
         */
        public SuppressionKey(Long tenantId, String userId, String channel, String reason) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
            this.reason = reason;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回渠道标识。
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回问题原因。
         */
        public String reason() {
            return reason;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SuppressionKey that = (SuppressionKey) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(reason, that.reason);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, userId, channel, reason);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "SuppressionKey[tenantId=" + tenantId + ", userId=" + userId + ", channel=" + channel + ", reason=" + reason + "]";
        }
    }
}
