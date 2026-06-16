package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * MarketingPolicyAdminController 业务组件。
 */
@RestController
@RequestMapping("/canvas/policies")
@RequiredArgsConstructor
public class MarketingPolicyAdminController {

    /**
     * default租户标识常量，用于保持控制器内部规则一致。
     */
    private static final Long DEFAULT_TENANT_ID = 0L;
    /**
     * allchannels常量，用于保持控制器内部规则一致。
     */
    private static final String ALL_CHANNELS = "ALL";

    /**
     * consent数据访问组件，用于访问和持久化对应数据。
     */
    private final MarketingConsentMapper consentMapper;
    /**
     * suppression数据访问组件，用于访问和持久化对应数据。
     */
    private final MarketingSuppressionMapper suppressionMapper;
    /**
     * 渠道数据访问组件，用于访问和持久化对应数据。
     */
    private final CustomerChannelMapper channelMapper;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * ConsentReq 数据记录。
     */
    public static final class ConsentReq {

        /**
         * 用户标识。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("userId")
        private final String userId;

        /**
         * 渠道。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("channel")
        private final String channel;

        /**
         * consentStatus 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("consentStatus")
        private final String consentStatus;

        /**
         * source 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("source")
        private final String source;

        /**
         * 创建 ConsentReq 实例。
         *
         * @param userId 用户标识
         * @param channel 渠道
         * @param consentStatus consentStatus 字段值
         * @param source source 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ConsentReq(@com.fasterxml.jackson.annotation.JsonProperty("userId") String userId, @com.fasterxml.jackson.annotation.JsonProperty("channel") String channel, @com.fasterxml.jackson.annotation.JsonProperty("consentStatus") String consentStatus, @com.fasterxml.jackson.annotation.JsonProperty("source") String source) {
            this.userId = userId;
            this.channel = channel;
            this.consentStatus = consentStatus;
            this.source = source;
        }

        /**
         * 返回用户标识。
         *
         * @return 用户标识
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回渠道。
         *
         * @return 渠道
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回consentStatus 字段值。
         *
         * @return consentStatus 字段值
         */
        public String consentStatus() {
            return consentStatus;
        }

        /**
         * 返回source 字段值。
         *
         * @return source 字段值
         */
        public String source() {
            return source;
        }

        /**
         * 判断两个 ConsentReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConsentReq that)) {
                return false;
            }
            return java.util.Objects.equals(userId, that.userId) && java.util.Objects.equals(channel, that.channel) && java.util.Objects.equals(consentStatus, that.consentStatus) && java.util.Objects.equals(source, that.source);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, channel, consentStatus, source);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ConsentReq[" + "userId=" + userId + ", " + "channel=" + channel + ", " + "consentStatus=" + consentStatus + ", " + "source=" + source + "]";
        }
    }

    /**
     * SuppressionReq 数据记录。
     */
    public static final class SuppressionReq {

        /**
         * 用户标识。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("userId")
        private final String userId;

        /**
         * 渠道。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("channel")
        private final String channel;

        /**
         * 原因。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("reason")
        private final String reason;

        /**
         * 启用状态。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("active")
        private final Boolean active;

        /**
         * expiresAt 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("expiresAt")
        private final LocalDateTime expiresAt;

        /**
         * 创建 SuppressionReq 实例。
         *
         * @param userId 用户标识
         * @param channel 渠道
         * @param reason 原因
         * @param active 启用状态
         * @param expiresAt expiresAt 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public SuppressionReq(@com.fasterxml.jackson.annotation.JsonProperty("userId") String userId, @com.fasterxml.jackson.annotation.JsonProperty("channel") String channel, @com.fasterxml.jackson.annotation.JsonProperty("reason") String reason, @com.fasterxml.jackson.annotation.JsonProperty("active") Boolean active, @com.fasterxml.jackson.annotation.JsonProperty("expiresAt") LocalDateTime expiresAt) {
            this.userId = userId;
            this.channel = channel;
            this.reason = reason;
            this.active = active;
            this.expiresAt = expiresAt;
        }

        /**
         * 返回用户标识。
         *
         * @return 用户标识
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回渠道。
         *
         * @return 渠道
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回原因。
         *
         * @return 原因
         */
        public String reason() {
            return reason;
        }

        /**
         * 返回启用状态。
         *
         * @return 启用状态
         */
        public Boolean active() {
            return active;
        }

        /**
         * 返回expiresAt 字段值。
         *
         * @return expiresAt 字段值
         */
        public LocalDateTime expiresAt() {
            return expiresAt;
        }

        /**
         * 判断两个 SuppressionReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SuppressionReq that)) {
                return false;
            }
            return java.util.Objects.equals(userId, that.userId) && java.util.Objects.equals(channel, that.channel) && java.util.Objects.equals(reason, that.reason) && java.util.Objects.equals(active, that.active) && java.util.Objects.equals(expiresAt, that.expiresAt);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, channel, reason, active, expiresAt);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "SuppressionReq[" + "userId=" + userId + ", " + "channel=" + channel + ", " + "reason=" + reason + ", " + "active=" + active + ", " + "expiresAt=" + expiresAt + "]";
        }
    }

    /**
     * ChannelReq 数据记录。
     */
    public static final class ChannelReq {

        /**
         * 用户标识。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("userId")
        private final String userId;

        /**
         * 渠道。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("channel")
        private final String channel;

        /**
         * address 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("address")
        private final String address;

        /**
         * 启用状态。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("enabled")
        private final Integer enabled;

        /**
         * verified 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("verified")
        private final Integer verified;

        /**
         * metadata 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("metadata")
        private final String metadata;

        /**
         * 创建 ChannelReq 实例。
         *
         * @param userId 用户标识
         * @param channel 渠道
         * @param address address 字段值
         * @param enabled 启用状态
         * @param verified verified 字段值
         * @param metadata metadata 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ChannelReq(@com.fasterxml.jackson.annotation.JsonProperty("userId") String userId, @com.fasterxml.jackson.annotation.JsonProperty("channel") String channel, @com.fasterxml.jackson.annotation.JsonProperty("address") String address, @com.fasterxml.jackson.annotation.JsonProperty("enabled") Integer enabled, @com.fasterxml.jackson.annotation.JsonProperty("verified") Integer verified, @com.fasterxml.jackson.annotation.JsonProperty("metadata") String metadata) {
            this.userId = userId;
            this.channel = channel;
            this.address = address;
            this.enabled = enabled;
            this.verified = verified;
            this.metadata = metadata;
        }

        /**
         * 返回用户标识。
         *
         * @return 用户标识
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回渠道。
         *
         * @return 渠道
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回address 字段值。
         *
         * @return address 字段值
         */
        public String address() {
            return address;
        }

        /**
         * 返回启用状态。
         *
         * @return 启用状态
         */
        public Integer enabled() {
            return enabled;
        }

        /**
         * 返回verified 字段值。
         *
         * @return verified 字段值
         */
        public Integer verified() {
            return verified;
        }

        /**
         * 返回metadata 字段值。
         *
         * @return metadata 字段值
         */
        public String metadata() {
            return metadata;
        }

        /**
         * 判断两个 ChannelReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ChannelReq that)) {
                return false;
            }
            return java.util.Objects.equals(userId, that.userId) && java.util.Objects.equals(channel, that.channel) && java.util.Objects.equals(address, that.address) && java.util.Objects.equals(enabled, that.enabled) && java.util.Objects.equals(verified, that.verified) && java.util.Objects.equals(metadata, that.metadata);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, channel, address, enabled, verified, metadata);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ChannelReq[" + "userId=" + userId + ", " + "channel=" + channel + ", " + "address=" + address + ", " + "enabled=" + enabled + ", " + "verified=" + verified + ", " + "metadata=" + metadata + "]";
        }
    }

    /**
     * PolicyState 数据记录。
     */
    public static final class PolicyState {

        /**
         * 用户标识。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("userId")
        private final String userId;

        /**
         * 渠道。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("channel")
        private final String channel;

        /**
         * consent 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("consent")
        private final MarketingConsentDO consent;

        /**
         * suppressions 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("suppressions")
        private final List<MarketingSuppressionDO> suppressions;

        /**
         * customerChannel 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("customerChannel")
        private final CustomerChannelDO customerChannel;

        /**
         * 创建 PolicyState 实例。
         *
         * @param userId 用户标识
         * @param channel 渠道
         * @param consent consent 字段值
         * @param suppressions suppressions 字段值
         * @param customerChannel customerChannel 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public PolicyState(@com.fasterxml.jackson.annotation.JsonProperty("userId") String userId, @com.fasterxml.jackson.annotation.JsonProperty("channel") String channel, @com.fasterxml.jackson.annotation.JsonProperty("consent") MarketingConsentDO consent, @com.fasterxml.jackson.annotation.JsonProperty("suppressions") List<MarketingSuppressionDO> suppressions, @com.fasterxml.jackson.annotation.JsonProperty("customerChannel") CustomerChannelDO customerChannel) {
            this.userId = userId;
            this.channel = channel;
            this.consent = consent;
            this.suppressions = suppressions;
            this.customerChannel = customerChannel;
        }

        /**
         * 返回用户标识。
         *
         * @return 用户标识
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回渠道。
         *
         * @return 渠道
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回consent 字段值。
         *
         * @return consent 字段值
         */
        public MarketingConsentDO consent() {
            return consent;
        }

        /**
         * 返回suppressions 字段值。
         *
         * @return suppressions 字段值
         */
        public List<MarketingSuppressionDO> suppressions() {
            return suppressions;
        }

        /**
         * 返回customerChannel 字段值。
         *
         * @return customerChannel 字段值
         */
        public CustomerChannelDO customerChannel() {
            return customerChannel;
        }

        /**
         * 判断两个 PolicyState 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PolicyState that)) {
                return false;
            }
            return java.util.Objects.equals(userId, that.userId) && java.util.Objects.equals(channel, that.channel) && java.util.Objects.equals(consent, that.consent) && java.util.Objects.equals(suppressions, that.suppressions) && java.util.Objects.equals(customerChannel, that.customerChannel);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, channel, consent, suppressions, customerChannel);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "PolicyState[" + "userId=" + userId + ", " + "channel=" + channel + ", " + "consent=" + consent + ", " + "suppressions=" + suppressions + ", " + "customerChannel=" + customerChannel + "]";
        }
    }

    /**
     * 执行 policyState 流程，围绕 policy state 完成校验、计算或结果组装。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 policyState 流程中的校验、计算或对象转换。
     * @return 返回 policyState 流程生成的业务结果。
     */
    @GetMapping("/state")
    public Mono<R<PolicyState>> policyState(@RequestParam String userId, @RequestParam String channel) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            Long tenantId = tenantId(context);
            String normalizedUserId = required(userId, "userId");
            String normalizedChannel = normalizeRequired(channel, "channel");
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            MarketingConsentDO consent = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                    .eq(MarketingConsentDO::getTenantId, tenantId)
                    .eq(MarketingConsentDO::getUserId, normalizedUserId)
                    .eq(MarketingConsentDO::getChannel, normalizedChannel)
                    .last("LIMIT 1"));
            List<MarketingSuppressionDO> suppressions = suppressionMapper.selectList(
                    new LambdaQueryWrapper<MarketingSuppressionDO>()
                            .eq(MarketingSuppressionDO::getTenantId, tenantId)
                            .eq(MarketingSuppressionDO::getUserId, normalizedUserId)
                            .and(q -> q.eq(MarketingSuppressionDO::getChannel, normalizedChannel)
                                    .or()
                                    .eq(MarketingSuppressionDO::getChannel, ALL_CHANNELS)
                                    .or()
                                    .isNull(MarketingSuppressionDO::getChannel)));
            CustomerChannelDO customerChannel = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                    .eq(CustomerChannelDO::getTenantId, tenantId)
                    .eq(CustomerChannelDO::getUserId, normalizedUserId)
                    .eq(CustomerChannelDO::getChannel, normalizedChannel)
                    .last("LIMIT 1"));
            // 汇总前面计算出的状态和明细，返回给调用方。
            return R.ok(new PolicyState(normalizedUserId, normalizedChannel,
                    consent, suppressions == null ? List.of() : suppressions, customerChannel));
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/consent")
    public Mono<R<MarketingConsentDO>> upsertConsent(@RequestBody ConsentReq req) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (req == null) {
                /**
                 * 执行 illegalargumentexception 对应的内部处理流程。
                 *
                 * @param required" required"，由调用方提供
                 * @return 返回内部处理结果
                 */
                throw new IllegalArgumentException("consent request is required");
            }
            Long tenantId = tenantId(context);
            String userId = required(req.userId(), "userId");
            String channel = normalizeRequired(req.channel(), "channel");
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            MarketingConsentDO row = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                    .eq(MarketingConsentDO::getTenantId, tenantId)
                    .eq(MarketingConsentDO::getUserId, userId)
                    .eq(MarketingConsentDO::getChannel, channel)
                    .last("LIMIT 1"));
            boolean insert = row == null;
            if (insert) {
                row = new MarketingConsentDO();
            }
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setChannel(channel);
            row.setConsentStatus(normalizeRequired(req.consentStatus(), "consentStatus"));
            row.setSource(optional(req.source()));
            if (insert) {
                consentMapper.insert(row);
            } else {
                consentMapper.updateById(row);
            }
            return R.ok(row);
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/suppression")
    public Mono<R<MarketingSuppressionDO>> upsertSuppression(@RequestBody SuppressionReq req) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (req == null) {
                /**
                 * 执行 illegalargumentexception 对应的内部处理流程。
                 *
                 * @param required" required"，由调用方提供
                 * @return 返回内部处理结果
                 */
                throw new IllegalArgumentException("suppression request is required");
            }
            Long tenantId = tenantId(context);
            String userId = required(req.userId(), "userId");
            String channel = normalizeChannelOrAll(req.channel());
            String reason = required(req.reason(), "reason");
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            MarketingSuppressionDO row = suppressionMapper.selectOne(new LambdaQueryWrapper<MarketingSuppressionDO>()
                    .eq(MarketingSuppressionDO::getTenantId, tenantId)
                    .eq(MarketingSuppressionDO::getUserId, userId)
                    .eq(MarketingSuppressionDO::getChannel, channel)
                    .eq(MarketingSuppressionDO::getReason, reason)
                    .last("LIMIT 1"));
            boolean insert = row == null;
            if (insert) {
                row = new MarketingSuppressionDO();
            }
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setChannel(channel);
            row.setReason(reason);
            row.setActive(Boolean.FALSE.equals(req.active()) ? 0 : 1);
            row.setExpiresAt(req.expiresAt());
            if (insert) {
                suppressionMapper.insert(row);
            } else {
                suppressionMapper.updateById(row);
            }
            return R.ok(row);
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/channel")
    public Mono<R<CustomerChannelDO>> upsertChannel(@RequestBody ChannelReq req) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (req == null) {
                /**
                 * 执行 illegalargumentexception 对应的内部处理流程。
                 *
                 * @param required" required"，由调用方提供
                 * @return 返回内部处理结果
                 */
                throw new IllegalArgumentException("channel request is required");
            }
            Long tenantId = tenantId(context);
            String userId = required(req.userId(), "userId");
            String channel = normalizeRequired(req.channel(), "channel");
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            CustomerChannelDO row = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                    .eq(CustomerChannelDO::getTenantId, tenantId)
                    .eq(CustomerChannelDO::getUserId, userId)
                    .eq(CustomerChannelDO::getChannel, channel)
                    .last("LIMIT 1"));
            boolean insert = row == null;
            if (insert) {
                row = new CustomerChannelDO();
            }
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setChannel(channel);
            row.setAddress(optional(req.address()));
            row.setEnabled(req.enabled() == null ? 1 : req.enabled());
            row.setVerified(req.verified() == null ? 0 : req.verified());
            row.setMetadata(optional(req.metadata()));
            if (insert) {
                channelMapper.insert(row);
            } else {
                channelMapper.updateById(row);
            }
            return R.ok(row);
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? DEFAULT_TENANT_ID : context.tenantId();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeChannelOrAll(String value) {
        return value == null || value.isBlank() ? ALL_CHANNELS : normalizeRequired(value, "channel");
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeRequired(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            /**
             * 执行 illegalargumentexception 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 执行 optional 流程，围绕 optional 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 optional 生成的文本或业务键。
     */
    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
