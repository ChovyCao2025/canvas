package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.policy.MarketingPreferenceCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/canvas/marketing-preferences")
/**
 * MarketingPreferenceCenterController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class MarketingPreferenceCenterController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingPreferenceCenterService service;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 初始化 MarketingPreferenceCenterController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingPreferenceCenterController(MarketingPreferenceCenterService service) {
        this(service, null);
    }

    @Autowired
    /**
     * 初始化 MarketingPreferenceCenterController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingPreferenceCenterController(MarketingPreferenceCenterService service,
                                               TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/users/{userId}")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 report 流程生成的业务结果。
     */
    public Mono<R<MarketingPreferenceCenterService.PreferenceReport>> report(@PathVariable String userId) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.report(tenantId, userId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/users/{userId}/consents/{channel}")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 updateConsent 流程中的校验、计算或对象转换。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<MarketingPreferenceCenterService.ConsentRow>> updateConsent(
            @PathVariable String userId,
            @PathVariable String channel,
            @RequestBody ConsentUpdateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.upsertConsent(
                tenantId,
                userId,
                new MarketingPreferenceCenterService.ConsentUpdateCommand(channel, req.consentStatus(), req.source()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/users/{userId}/channels/{channel}")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 updateChannel 流程中的校验、计算或对象转换。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<MarketingPreferenceCenterService.ChannelRow>> updateChannel(
            @PathVariable String userId,
            @PathVariable String channel,
            @RequestBody ChannelUpdateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.upsertChannel(
                tenantId,
                userId,
                new MarketingPreferenceCenterService.ChannelUpdateCommand(
                        channel, req.address(), req.enabled(), req.verified(), req.metadata()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/users/{userId}/suppressions")
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 addSuppression 流程生成的业务结果。
     */
    public Mono<R<MarketingPreferenceCenterService.SuppressionRow>> addSuppression(
            @PathVariable String userId,
            @RequestBody SuppressionCreateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.addSuppression(
                tenantId,
                userId,
                new MarketingPreferenceCenterService.SuppressionCreateCommand(
                        req.channel(), req.reason(), req.active(), req.expiresAt()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/suppressions/{id}/deactivate")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 deactivateSuppression 流程生成的业务结果。
     */
    public Mono<R<Void>> deactivateSuppression(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
            service.deactivateSuppression(tenantId, id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 current tenant id 计算得到的数量、金额或指标值。
     */
    private Mono<Long> currentTenantId() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tenantContextResolver.current()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    /**
     * ConsentUpdateReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class ConsentUpdateReq {

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
         * 创建 ConsentUpdateReq 实例。
         *
         * @param consentStatus consentStatus 字段值
         * @param source source 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ConsentUpdateReq(@com.fasterxml.jackson.annotation.JsonProperty("consentStatus") String consentStatus, @com.fasterxml.jackson.annotation.JsonProperty("source") String source) {
            this.consentStatus = consentStatus;
            this.source = source;
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
         * 判断两个 ConsentUpdateReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConsentUpdateReq that)) {
                return false;
            }
            return java.util.Objects.equals(consentStatus, that.consentStatus) && java.util.Objects.equals(source, that.source);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(consentStatus, source);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ConsentUpdateReq[" + "consentStatus=" + consentStatus + ", " + "source=" + source + "]";
        }
    }

    /**
     * ChannelUpdateReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class ChannelUpdateReq {

        /**
         * address 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("address")
        private final String address;

        /**
         * 启用状态。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("enabled")
        private final Boolean enabled;

        /**
         * verified 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("verified")
        private final Boolean verified;

        /**
         * metadata 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("metadata")
        private final String metadata;

        /**
         * 创建 ChannelUpdateReq 实例。
         *
         * @param address address 字段值
         * @param enabled 启用状态
         * @param verified verified 字段值
         * @param metadata metadata 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ChannelUpdateReq(@com.fasterxml.jackson.annotation.JsonProperty("address") String address, @com.fasterxml.jackson.annotation.JsonProperty("enabled") Boolean enabled, @com.fasterxml.jackson.annotation.JsonProperty("verified") Boolean verified, @com.fasterxml.jackson.annotation.JsonProperty("metadata") String metadata) {
            this.address = address;
            this.enabled = enabled;
            this.verified = verified;
            this.metadata = metadata;
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
        public Boolean enabled() {
            return enabled;
        }

        /**
         * 返回verified 字段值。
         *
         * @return verified 字段值
         */
        public Boolean verified() {
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
         * 判断两个 ChannelUpdateReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ChannelUpdateReq that)) {
                return false;
            }
            return java.util.Objects.equals(address, that.address) && java.util.Objects.equals(enabled, that.enabled) && java.util.Objects.equals(verified, that.verified) && java.util.Objects.equals(metadata, that.metadata);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(address, enabled, verified, metadata);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ChannelUpdateReq[" + "address=" + address + ", " + "enabled=" + enabled + ", " + "verified=" + verified + ", " + "metadata=" + metadata + "]";
        }
    }

    /**
     * SuppressionCreateReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class SuppressionCreateReq {

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
         * 创建 SuppressionCreateReq 实例。
         *
         * @param channel 渠道
         * @param reason 原因
         * @param active 启用状态
         * @param expiresAt expiresAt 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public SuppressionCreateReq(@com.fasterxml.jackson.annotation.JsonProperty("channel") String channel, @com.fasterxml.jackson.annotation.JsonProperty("reason") String reason, @com.fasterxml.jackson.annotation.JsonProperty("active") Boolean active, @com.fasterxml.jackson.annotation.JsonProperty("expiresAt") LocalDateTime expiresAt) {
            this.channel = channel;
            this.reason = reason;
            this.active = active;
            this.expiresAt = expiresAt;
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
         * 判断两个 SuppressionCreateReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SuppressionCreateReq that)) {
                return false;
            }
            return java.util.Objects.equals(channel, that.channel) && java.util.Objects.equals(reason, that.reason) && java.util.Objects.equals(active, that.active) && java.util.Objects.equals(expiresAt, that.expiresAt);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(channel, reason, active, expiresAt);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "SuppressionCreateReq[" + "channel=" + channel + ", " + "reason=" + reason + ", " + "active=" + active + ", " + "expiresAt=" + expiresAt + "]";
        }
    }
}
