package org.chovy.canvas.marketing.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 定义MarketingPreferenceFacade的营销上下文访问契约。
 */
public interface MarketingPreferenceFacade {

    /**
     * 执行report业务操作。
     */
    PreferenceReport report(Long tenantId, String userId);

    /**
     * 更新consent业务对象。
     */
    ConsentRow updateConsent(Long tenantId, String userId, ConsentUpdateCommand command);

    /**
     * 更新channel业务对象。
     */
    ChannelRow updateChannel(Long tenantId, String userId, ChannelUpdateCommand command);

    /**
     * 执行addSuppression业务操作。
     */
    SuppressionRow addSuppression(Long tenantId, String userId, SuppressionCreateCommand command);

    /**
     * 执行deactivateSuppression业务操作。
     */
    void deactivateSuppression(Long tenantId, Long suppressionId);

    /**
     * 表示PreferenceReport的数据结构。
     */
    static final class PreferenceReport {

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * consents 字段值。
         */
        private final List<ConsentRow> consents;

        /**
         * channels 字段值。
         */
        private final List<ChannelRow> channels;

        /**
         * suppressions 字段值。
         */
        private final List<SuppressionRow> suppressions;

        /**
         * summary 字段值。
         */
        private final PreferenceSummary summary;

        /**
         * 创建PreferenceReport实例。
         */
        public PreferenceReport(String userId, List<ConsentRow> consents, List<ChannelRow> channels, List<SuppressionRow> suppressions, PreferenceSummary summary) {
            this.userId = userId;
            this.consents = consents;
            this.channels = channels;
            this.suppressions = suppressions;
            this.summary = summary;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回consents 字段值。
         */
        public List<ConsentRow> consents() {
            return consents;
        }

        /**
         * 返回channels 字段值。
         */
        public List<ChannelRow> channels() {
            return channels;
        }

        /**
         * 返回suppressions 字段值。
         */
        public List<SuppressionRow> suppressions() {
            return suppressions;
        }

        /**
         * 返回summary 字段值。
         */
        public PreferenceSummary summary() {
            return summary;
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
            PreferenceReport that = (PreferenceReport) o;
            return                     Objects.equals(userId, that.userId) &&
                    Objects.equals(consents, that.consents) &&
                    Objects.equals(channels, that.channels) &&
                    Objects.equals(suppressions, that.suppressions) &&
                    Objects.equals(summary, that.summary);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(userId, consents, channels, suppressions, summary);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "PreferenceReport[userId=" + userId + ", consents=" + consents + ", channels=" + channels + ", suppressions=" + suppressions + ", summary=" + summary + "]";
        }
    }

    /**
     * 保存ConsentRow的内存行数据。
     */
    static final class ConsentRow {

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * 同意状态。
         */
        private final String consentStatus;

        /**
         * 数据来源。
         */
        private final String source;

        /**
         * 最后更新时间。
         */
        private final LocalDateTime updatedAt;

        /**
         * 创建ConsentRow实例。
         */
        public ConsentRow(String channel, String consentStatus, String source, LocalDateTime updatedAt) {
            this.channel = channel;
            this.consentStatus = consentStatus;
            this.source = source;
            this.updatedAt = updatedAt;
        }

        /**
         * 返回渠道标识。
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回同意状态。
         */
        public String consentStatus() {
            return consentStatus;
        }

        /**
         * 返回数据来源。
         */
        public String source() {
            return source;
        }

        /**
         * 返回最后更新时间。
         */
        public LocalDateTime updatedAt() {
            return updatedAt;
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
            ConsentRow that = (ConsentRow) o;
            return                     Objects.equals(channel, that.channel) &&
                    Objects.equals(consentStatus, that.consentStatus) &&
                    Objects.equals(source, that.source) &&
                    Objects.equals(updatedAt, that.updatedAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(channel, consentStatus, source, updatedAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ConsentRow[channel=" + channel + ", consentStatus=" + consentStatus + ", source=" + source + ", updatedAt=" + updatedAt + "]";
        }
    }

    /**
     * 保存ChannelRow的内存行数据。
     */
    static final class ChannelRow {

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * 渠道地址。
         */
        private final String address;

        /**
         * 是否启用。
         */
        private final boolean enabled;

        /**
         * 是否已验证。
         */
        private final boolean verified;

        /**
         * reachable 字段值。
         */
        private final boolean reachable;

        /**
         * 扩展元数据。
         */
        private final String metadata;

        /**
         * 最后更新时间。
         */
        private final LocalDateTime updatedAt;

        /**
         * 创建ChannelRow实例。
         */
        public ChannelRow(String channel, String address, boolean enabled, boolean verified, boolean reachable, String metadata, LocalDateTime updatedAt) {
            this.channel = channel;
            this.address = address;
            this.enabled = enabled;
            this.verified = verified;
            this.reachable = reachable;
            this.metadata = metadata;
            this.updatedAt = updatedAt;
        }

        /**
         * 返回渠道标识。
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回渠道地址。
         */
        public String address() {
            return address;
        }

        /**
         * 返回是否启用。
         */
        public boolean enabled() {
            return enabled;
        }

        /**
         * 返回是否已验证。
         */
        public boolean verified() {
            return verified;
        }

        /**
         * 返回reachable 字段值。
         */
        public boolean reachable() {
            return reachable;
        }

        /**
         * 返回扩展元数据。
         */
        public String metadata() {
            return metadata;
        }

        /**
         * 返回最后更新时间。
         */
        public LocalDateTime updatedAt() {
            return updatedAt;
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
            ChannelRow that = (ChannelRow) o;
            return                     Objects.equals(channel, that.channel) &&
                    Objects.equals(address, that.address) &&
                    enabled == that.enabled &&
                    verified == that.verified &&
                    reachable == that.reachable &&
                    Objects.equals(metadata, that.metadata) &&
                    Objects.equals(updatedAt, that.updatedAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(channel, address, enabled, verified, reachable, metadata, updatedAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ChannelRow[channel=" + channel + ", address=" + address + ", enabled=" + enabled + ", verified=" + verified + ", reachable=" + reachable + ", metadata=" + metadata + ", updatedAt=" + updatedAt + "]";
        }
    }

    /**
     * 保存SuppressionRow的内存行数据。
     */
    static final class SuppressionRow {

        /**
         * 记录的唯一标识。
         */
        private final Long id;

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * 问题原因。
         */
        private final String reason;

        /**
         * active 字段值。
         */
        private final boolean active;

        /**
         * state 字段值。
         */
        private final String state;

        /**
         * 过期时间。
         */
        private final LocalDateTime expiresAt;

        /**
         * 创建时间。
         */
        private final LocalDateTime createdAt;

        /**
         * 最后更新时间。
         */
        private final LocalDateTime updatedAt;

        /**
         * 创建SuppressionRow实例。
         */
        public SuppressionRow(Long id, String channel, String reason, boolean active, String state, LocalDateTime expiresAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.channel = channel;
            this.reason = reason;
            this.active = active;
            this.state = state;
            this.expiresAt = expiresAt;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        /**
         * 返回记录的唯一标识。
         */
        public Long id() {
            return id;
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
         * 返回active 字段值。
         */
        public boolean active() {
            return active;
        }

        /**
         * 返回state 字段值。
         */
        public String state() {
            return state;
        }

        /**
         * 返回过期时间。
         */
        public LocalDateTime expiresAt() {
            return expiresAt;
        }

        /**
         * 返回创建时间。
         */
        public LocalDateTime createdAt() {
            return createdAt;
        }

        /**
         * 返回最后更新时间。
         */
        public LocalDateTime updatedAt() {
            return updatedAt;
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
            SuppressionRow that = (SuppressionRow) o;
            return                     Objects.equals(id, that.id) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(reason, that.reason) &&
                    active == that.active &&
                    Objects.equals(state, that.state) &&
                    Objects.equals(expiresAt, that.expiresAt) &&
                    Objects.equals(createdAt, that.createdAt) &&
                    Objects.equals(updatedAt, that.updatedAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, channel, reason, active, state, expiresAt, createdAt, updatedAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "SuppressionRow[id=" + id + ", channel=" + channel + ", reason=" + reason + ", active=" + active + ", state=" + state + ", expiresAt=" + expiresAt + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
        }
    }

    /**
     * 表示PreferenceSummary的数据结构。
     */
    static final class PreferenceSummary {

        /**
         * totalChannels 字段值。
         */
        private final int totalChannels;

        /**
         * optInCount 字段值。
         */
        private final int optInCount;

        /**
         * optOutCount 字段值。
         */
        private final int optOutCount;

        /**
         * activeSuppressionCount 字段值。
         */
        private final int activeSuppressionCount;

        /**
         * reachableChannelCount 字段值。
         */
        private final int reachableChannelCount;

        /**
         * 创建PreferenceSummary实例。
         */
        public PreferenceSummary(int totalChannels, int optInCount, int optOutCount, int activeSuppressionCount, int reachableChannelCount) {
            this.totalChannels = totalChannels;
            this.optInCount = optInCount;
            this.optOutCount = optOutCount;
            this.activeSuppressionCount = activeSuppressionCount;
            this.reachableChannelCount = reachableChannelCount;
        }

        /**
         * 返回totalChannels 字段值。
         */
        public int totalChannels() {
            return totalChannels;
        }

        /**
         * 返回optInCount 字段值。
         */
        public int optInCount() {
            return optInCount;
        }

        /**
         * 返回optOutCount 字段值。
         */
        public int optOutCount() {
            return optOutCount;
        }

        /**
         * 返回activeSuppressionCount 字段值。
         */
        public int activeSuppressionCount() {
            return activeSuppressionCount;
        }

        /**
         * 返回reachableChannelCount 字段值。
         */
        public int reachableChannelCount() {
            return reachableChannelCount;
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
            PreferenceSummary that = (PreferenceSummary) o;
            return                     totalChannels == that.totalChannels &&
                    optInCount == that.optInCount &&
                    optOutCount == that.optOutCount &&
                    activeSuppressionCount == that.activeSuppressionCount &&
                    reachableChannelCount == that.reachableChannelCount;
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(totalChannels, optInCount, optOutCount, activeSuppressionCount, reachableChannelCount);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "PreferenceSummary[totalChannels=" + totalChannels + ", optInCount=" + optInCount + ", optOutCount=" + optOutCount + ", activeSuppressionCount=" + activeSuppressionCount + ", reachableChannelCount=" + reachableChannelCount + "]";
        }
    }

    /**
     * 承载ConsentUpdateCommand调用所需的输入参数。
     */
    static final class ConsentUpdateCommand {

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * 同意状态。
         */
        private final String consentStatus;

        /**
         * 数据来源。
         */
        private final String source;

        /**
         * 创建ConsentUpdateCommand实例。
         */
        public ConsentUpdateCommand(String channel, String consentStatus, String source) {
            this.channel = channel;
            this.consentStatus = consentStatus;
            this.source = source;
        }

        /**
         * 返回渠道标识。
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回同意状态。
         */
        public String consentStatus() {
            return consentStatus;
        }

        /**
         * 返回数据来源。
         */
        public String source() {
            return source;
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
            ConsentUpdateCommand that = (ConsentUpdateCommand) o;
            return                     Objects.equals(channel, that.channel) &&
                    Objects.equals(consentStatus, that.consentStatus) &&
                    Objects.equals(source, that.source);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(channel, consentStatus, source);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ConsentUpdateCommand[channel=" + channel + ", consentStatus=" + consentStatus + ", source=" + source + "]";
        }
    }

    /**
     * 承载ChannelUpdateCommand调用所需的输入参数。
     */
    static final class ChannelUpdateCommand {

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * 渠道地址。
         */
        private final String address;

        /**
         * 是否启用。
         */
        private final Boolean enabled;

        /**
         * 是否已验证。
         */
        private final Boolean verified;

        /**
         * 扩展元数据。
         */
        private final String metadata;

        /**
         * 创建ChannelUpdateCommand实例。
         */
        public ChannelUpdateCommand(String channel, String address, Boolean enabled, Boolean verified, String metadata) {
            this.channel = channel;
            this.address = address;
            this.enabled = enabled;
            this.verified = verified;
            this.metadata = metadata;
        }

        /**
         * 返回渠道标识。
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回渠道地址。
         */
        public String address() {
            return address;
        }

        /**
         * 返回是否启用。
         */
        public Boolean enabled() {
            return enabled;
        }

        /**
         * 返回是否已验证。
         */
        public Boolean verified() {
            return verified;
        }

        /**
         * 返回扩展元数据。
         */
        public String metadata() {
            return metadata;
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
            ChannelUpdateCommand that = (ChannelUpdateCommand) o;
            return                     Objects.equals(channel, that.channel) &&
                    Objects.equals(address, that.address) &&
                    Objects.equals(enabled, that.enabled) &&
                    Objects.equals(verified, that.verified) &&
                    Objects.equals(metadata, that.metadata);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(channel, address, enabled, verified, metadata);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ChannelUpdateCommand[channel=" + channel + ", address=" + address + ", enabled=" + enabled + ", verified=" + verified + ", metadata=" + metadata + "]";
        }
    }

    /**
     * 承载SuppressionCreateCommand调用所需的输入参数。
     */
    static final class SuppressionCreateCommand {

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * 问题原因。
         */
        private final String reason;

        /**
         * active 字段值。
         */
        private final Boolean active;

        /**
         * 过期时间。
         */
        private final LocalDateTime expiresAt;

        /**
         * 创建SuppressionCreateCommand实例。
         */
        public SuppressionCreateCommand(String channel, String reason, Boolean active, LocalDateTime expiresAt) {
            this.channel = channel;
            this.reason = reason;
            this.active = active;
            this.expiresAt = expiresAt;
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
         * 返回active 字段值。
         */
        public Boolean active() {
            return active;
        }

        /**
         * 返回过期时间。
         */
        public LocalDateTime expiresAt() {
            return expiresAt;
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
            SuppressionCreateCommand that = (SuppressionCreateCommand) o;
            return                     Objects.equals(channel, that.channel) &&
                    Objects.equals(reason, that.reason) &&
                    Objects.equals(active, that.active) &&
                    Objects.equals(expiresAt, that.expiresAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(channel, reason, active, expiresAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "SuppressionCreateCommand[channel=" + channel + ", reason=" + reason + ", active=" + active + ", expiresAt=" + expiresAt + "]";
        }
    }
}
