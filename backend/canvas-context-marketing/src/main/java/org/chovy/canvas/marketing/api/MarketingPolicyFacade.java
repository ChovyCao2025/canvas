package org.chovy.canvas.marketing.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 定义MarketingPolicyFacade的营销上下文访问契约。
 */
public interface MarketingPolicyFacade {

    /**
     * 执行policyState业务操作。
     */
    PolicyState policyState(Long tenantId, String userId, String channel);

    /**
     * 执行upsertConsent业务操作。
     */
    ConsentView upsertConsent(Long tenantId, ConsentCommand command);

    /**
     * 执行upsertSuppression业务操作。
     */
    SuppressionView upsertSuppression(Long tenantId, SuppressionCommand command);

    /**
     * 执行upsertChannel业务操作。
     */
    ChannelView upsertChannel(Long tenantId, ChannelCommand command);

    /**
     * 承载ConsentCommand调用所需的输入参数。
     */
    static final class ConsentCommand {

        /**
         * 用户标识。
         */
        private final String userId;

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
         * 创建ConsentCommand实例。
         */
        public ConsentCommand(String userId, String channel, String consentStatus, String source) {
            this.userId = userId;
            this.channel = channel;
            this.consentStatus = consentStatus;
            this.source = source;
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
            ConsentCommand that = (ConsentCommand) o;
            return                     Objects.equals(userId, that.userId) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(consentStatus, that.consentStatus) &&
                    Objects.equals(source, that.source);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(userId, channel, consentStatus, source);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ConsentCommand[userId=" + userId + ", channel=" + channel + ", consentStatus=" + consentStatus + ", source=" + source + "]";
        }
    }

    /**
     * 承载SuppressionCommand调用所需的输入参数。
     */
    static final class SuppressionCommand {

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
         * active 字段值。
         */
        private final Boolean active;

        /**
         * 过期时间。
         */
        private final LocalDateTime expiresAt;

        /**
         * 创建SuppressionCommand实例。
         */
        public SuppressionCommand(String userId, String channel, String reason, Boolean active, LocalDateTime expiresAt) {
            this.userId = userId;
            this.channel = channel;
            this.reason = reason;
            this.active = active;
            this.expiresAt = expiresAt;
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
            SuppressionCommand that = (SuppressionCommand) o;
            return                     Objects.equals(userId, that.userId) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(reason, that.reason) &&
                    Objects.equals(active, that.active) &&
                    Objects.equals(expiresAt, that.expiresAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(userId, channel, reason, active, expiresAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "SuppressionCommand[userId=" + userId + ", channel=" + channel + ", reason=" + reason + ", active=" + active + ", expiresAt=" + expiresAt + "]";
        }
    }

    /**
     * 承载ChannelCommand调用所需的输入参数。
     */
    static final class ChannelCommand {

        /**
         * 用户标识。
         */
        private final String userId;

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
        private final Integer enabled;

        /**
         * 是否已验证。
         */
        private final Integer verified;

        /**
         * 扩展元数据。
         */
        private final String metadata;

        /**
         * 创建ChannelCommand实例。
         */
        public ChannelCommand(String userId, String channel, String address, Integer enabled, Integer verified, String metadata) {
            this.userId = userId;
            this.channel = channel;
            this.address = address;
            this.enabled = enabled;
            this.verified = verified;
            this.metadata = metadata;
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
         * 返回渠道地址。
         */
        public String address() {
            return address;
        }

        /**
         * 返回是否启用。
         */
        public Integer enabled() {
            return enabled;
        }

        /**
         * 返回是否已验证。
         */
        public Integer verified() {
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
            ChannelCommand that = (ChannelCommand) o;
            return                     Objects.equals(userId, that.userId) &&
                    Objects.equals(channel, that.channel) &&
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
            return Objects.hash(userId, channel, address, enabled, verified, metadata);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ChannelCommand[userId=" + userId + ", channel=" + channel + ", address=" + address + ", enabled=" + enabled + ", verified=" + verified + ", metadata=" + metadata + "]";
        }
    }

    /**
     * 保存PolicyState的内存状态。
     */
    static final class PolicyState {

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * consent 字段值。
         */
        private final ConsentView consent;

        /**
         * suppressions 字段值。
         */
        private final List<SuppressionView> suppressions;

        /**
         * customerChannel 字段值。
         */
        private final ChannelView customerChannel;

        /**
         * 创建PolicyState实例。
         */
        public PolicyState(String userId, String channel, ConsentView consent, List<SuppressionView> suppressions, ChannelView customerChannel) {
            this.userId = userId;
            this.channel = channel;
            this.consent = consent;
            this.suppressions = suppressions;
            this.customerChannel = customerChannel;
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
         * 返回consent 字段值。
         */
        public ConsentView consent() {
            return consent;
        }

        /**
         * 返回suppressions 字段值。
         */
        public List<SuppressionView> suppressions() {
            return suppressions;
        }

        /**
         * 返回customerChannel 字段值。
         */
        public ChannelView customerChannel() {
            return customerChannel;
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
            PolicyState that = (PolicyState) o;
            return                     Objects.equals(userId, that.userId) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(consent, that.consent) &&
                    Objects.equals(suppressions, that.suppressions) &&
                    Objects.equals(customerChannel, that.customerChannel);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(userId, channel, consent, suppressions, customerChannel);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "PolicyState[userId=" + userId + ", channel=" + channel + ", consent=" + consent + ", suppressions=" + suppressions + ", customerChannel=" + customerChannel + "]";
        }
    }

    /**
     * 承载ConsentView返回给调用方的只读视图。
     */
    static final class ConsentView {

        /**
         * 记录的唯一标识。
         */
        private final Long id;

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
         * 同意状态。
         */
        private final String consentStatus;

        /**
         * 数据来源。
         */
        private final String source;

        /**
         * 创建ConsentView实例。
         */
        public ConsentView(Long id, Long tenantId, String userId, String channel, String consentStatus, String source) {
            this.id = id;
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
            this.consentStatus = consentStatus;
            this.source = source;
        }

        /**
         * 返回记录的唯一标识。
         */
        public Long id() {
            return id;
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
            ConsentView that = (ConsentView) o;
            return                     Objects.equals(id, that.id) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(consentStatus, that.consentStatus) &&
                    Objects.equals(source, that.source);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, tenantId, userId, channel, consentStatus, source);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ConsentView[id=" + id + ", tenantId=" + tenantId + ", userId=" + userId + ", channel=" + channel + ", consentStatus=" + consentStatus + ", source=" + source + "]";
        }
    }

    /**
     * 承载SuppressionView返回给调用方的只读视图。
     */
    static final class SuppressionView {

        /**
         * 记录的唯一标识。
         */
        private final Long id;

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
         * active 字段值。
         */
        private final int active;

        /**
         * 过期时间。
         */
        private final LocalDateTime expiresAt;

        /**
         * 创建SuppressionView实例。
         */
        public SuppressionView(Long id, Long tenantId, String userId, String channel, String reason, int active, LocalDateTime expiresAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
            this.reason = reason;
            this.active = active;
            this.expiresAt = expiresAt;
        }

        /**
         * 返回记录的唯一标识。
         */
        public Long id() {
            return id;
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
         * 返回active 字段值。
         */
        public int active() {
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
            SuppressionView that = (SuppressionView) o;
            return                     Objects.equals(id, that.id) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(reason, that.reason) &&
                    active == that.active &&
                    Objects.equals(expiresAt, that.expiresAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, tenantId, userId, channel, reason, active, expiresAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "SuppressionView[id=" + id + ", tenantId=" + tenantId + ", userId=" + userId + ", channel=" + channel + ", reason=" + reason + ", active=" + active + ", expiresAt=" + expiresAt + "]";
        }
    }

    /**
     * 承载ChannelView返回给调用方的只读视图。
     */
    static final class ChannelView {

        /**
         * 记录的唯一标识。
         */
        private final Long id;

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
         * 渠道地址。
         */
        private final String address;

        /**
         * 是否启用。
         */
        private final int enabled;

        /**
         * 是否已验证。
         */
        private final int verified;

        /**
         * 扩展元数据。
         */
        private final String metadata;

        /**
         * 创建ChannelView实例。
         */
        public ChannelView(Long id, Long tenantId, String userId, String channel, String address, int enabled, int verified, String metadata) {
            this.id = id;
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
            this.address = address;
            this.enabled = enabled;
            this.verified = verified;
            this.metadata = metadata;
        }

        /**
         * 返回记录的唯一标识。
         */
        public Long id() {
            return id;
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
         * 返回渠道地址。
         */
        public String address() {
            return address;
        }

        /**
         * 返回是否启用。
         */
        public int enabled() {
            return enabled;
        }

        /**
         * 返回是否已验证。
         */
        public int verified() {
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
            ChannelView that = (ChannelView) o;
            return                     Objects.equals(id, that.id) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(address, that.address) &&
                    enabled == that.enabled &&
                    verified == that.verified &&
                    Objects.equals(metadata, that.metadata);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, tenantId, userId, channel, address, enabled, verified, metadata);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ChannelView[id=" + id + ", tenantId=" + tenantId + ", userId=" + userId + ", channel=" + channel + ", address=" + address + ", enabled=" + enabled + ", verified=" + verified + ", metadata=" + metadata + "]";
        }
    }
}
