package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.ChannelConnectorDO;
import org.chovy.canvas.dal.dataobject.ChannelDedupeRecordDO;
import org.chovy.canvas.dal.dataobject.ChannelFallbackDecisionDO;
import org.chovy.canvas.dal.dataobject.ChannelProviderLimitDO;
import org.chovy.canvas.dal.mapper.ChannelConnectorMapper;
import org.chovy.canvas.dal.mapper.ChannelDedupeRecordMapper;
import org.chovy.canvas.dal.mapper.ChannelFallbackDecisionMapper;
import org.chovy.canvas.dal.mapper.ChannelProviderLimitMapper;
import org.chovy.canvas.engine.channel.ChannelConnector;
import org.chovy.canvas.engine.channel.ChannelConnectorRegistry;
import org.chovy.canvas.engine.channel.ChannelFallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/channels/connectors")
/**
 * ChannelConnectorController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class ChannelConnectorController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final Service service;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 初始化 ChannelConnectorController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ChannelConnectorController(Service service) {
        this(service, null);
    }

    @Autowired
    /**
     * 初始化 ChannelConnectorController 实例。
     *
     * @param connectorMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param limitMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param decisionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dedupeRecordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fallbackService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public ChannelConnectorController(ChannelConnectorMapper connectorMapper,
                                      ChannelProviderLimitMapper limitMapper,
                                      ChannelFallbackDecisionMapper decisionMapper,
                                      ChannelDedupeRecordMapper dedupeRecordMapper,
                                      ChannelFallbackService fallbackService,
                                      TenantContextResolver tenantContextResolver) {
        this(new MapperBackedService(connectorMapper, limitMapper, decisionMapper, dedupeRecordMapper, fallbackService),
                tenantContextResolver);
    }

    /**
     * 初始化 ChannelConnectorController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    private ChannelConnectorController(Service service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<ConnectorRow>>> list() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.list(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/limits")
    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @return 返回解析、归一化或安全处理后的值。
     */
    public Mono<R<List<LimitRow>>> limits() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listLimits(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/mode")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<Void>> updateMode(@PathVariable Long id, @RequestBody ModeUpdateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
            service.updateMode(tenantId, id, req.mode(), req.reason());
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/health-test")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 testHealth 流程生成的业务结果。
     */
    public Mono<R<HealthResult>> testHealth(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.testHealth(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/fallback/validate")
    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回布尔判断结果。
     */
    public Mono<R<ValidationResult>> validateFallback(@RequestBody FallbackPolicyReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.validateFallback(tenantId, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/fallback/decisions")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 decisions 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<FallbackDecisionRow>>> decisions() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listDecisions(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/dedupe-records")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 dedupe records 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<DedupeRecordRow>>> dedupeRecords() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listDedupeRecords(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
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
     * Service 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public interface Service {
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        List<ConnectorRow> list(Long tenantId);

        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        List<LimitRow> listLimits(Long tenantId);

        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param id 业务对象 ID，用于定位具体记录。
         * @param mode mode 参数，用于 updateMode 流程中的校验、计算或对象转换。
         * @param reason 原因说明，用于记录状态变化的业务依据。
         */
        void updateMode(Long tenantId, Long id, String mode, String reason);

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param id 业务对象 ID，用于定位具体记录。
         * @return 返回 testHealth 流程生成的业务结果。
         */
        HealthResult testHealth(Long tenantId, Long id);

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param req 请求对象，承载本次操作的输入参数。
         * @return 返回布尔判断结果。
         */
        ValidationResult validateFallback(Long tenantId, FallbackPolicyReq req);

        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        List<FallbackDecisionRow> listDecisions(Long tenantId);

        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        List<DedupeRecordRow> listDedupeRecords(Long tenantId);
    }

    /**
     * ConnectorRow 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class ConnectorRow {

        /**
         * id 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("id")
        private final Long id;

        /**
         * connectorKey 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("connectorKey")
        private final String connectorKey;

        /**
         * 渠道。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("channel")
        private final String channel;

        /**
         * 提供方。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("provider")
        private final String provider;

        /**
         * mode 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("mode")
        private final String mode;

        /**
         * healthStatus 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("healthStatus")
        private final String healthStatus;

        /**
         * healthMessage 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("healthMessage")
        private final String healthMessage;

        /**
         * 创建 ConnectorRow 实例。
         *
         * @param id id 字段值
         * @param connectorKey connectorKey 字段值
         * @param channel 渠道
         * @param provider 提供方
         * @param mode mode 字段值
         * @param healthStatus healthStatus 字段值
         * @param healthMessage healthMessage 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ConnectorRow(@com.fasterxml.jackson.annotation.JsonProperty("id") Long id, @com.fasterxml.jackson.annotation.JsonProperty("connectorKey") String connectorKey, @com.fasterxml.jackson.annotation.JsonProperty("channel") String channel, @com.fasterxml.jackson.annotation.JsonProperty("provider") String provider, @com.fasterxml.jackson.annotation.JsonProperty("mode") String mode, @com.fasterxml.jackson.annotation.JsonProperty("healthStatus") String healthStatus, @com.fasterxml.jackson.annotation.JsonProperty("healthMessage") String healthMessage) {
            this.id = id;
            this.connectorKey = connectorKey;
            this.channel = channel;
            this.provider = provider;
            this.mode = mode;
            this.healthStatus = healthStatus;
            this.healthMessage = healthMessage;
        }

        /**
         * 返回id 字段值。
         *
         * @return id 字段值
         */
        public Long id() {
            return id;
        }

        /**
         * 返回connectorKey 字段值。
         *
         * @return connectorKey 字段值
         */
        public String connectorKey() {
            return connectorKey;
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
         * 返回提供方。
         *
         * @return 提供方
         */
        public String provider() {
            return provider;
        }

        /**
         * 返回mode 字段值。
         *
         * @return mode 字段值
         */
        public String mode() {
            return mode;
        }

        /**
         * 返回healthStatus 字段值。
         *
         * @return healthStatus 字段值
         */
        public String healthStatus() {
            return healthStatus;
        }

        /**
         * 返回healthMessage 字段值。
         *
         * @return healthMessage 字段值
         */
        public String healthMessage() {
            return healthMessage;
        }

        /**
         * 判断两个 ConnectorRow 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConnectorRow that)) {
                return false;
            }
            return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(connectorKey, that.connectorKey) && java.util.Objects.equals(channel, that.channel) && java.util.Objects.equals(provider, that.provider) && java.util.Objects.equals(mode, that.mode) && java.util.Objects.equals(healthStatus, that.healthStatus) && java.util.Objects.equals(healthMessage, that.healthMessage);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, connectorKey, channel, provider, mode, healthStatus, healthMessage);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ConnectorRow[" + "id=" + id + ", " + "connectorKey=" + connectorKey + ", " + "channel=" + channel + ", " + "provider=" + provider + ", " + "mode=" + mode + ", " + "healthStatus=" + healthStatus + ", " + "healthMessage=" + healthMessage + "]";
        }
    }

    /**
     * LimitRow 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class LimitRow {

        /**
         * 渠道。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("channel")
        private final String channel;

        /**
         * 提供方。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("provider")
        private final String provider;

        /**
         * operation 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("operation")
        private final String operation;

        /**
         * perSecondLimit 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("perSecondLimit")
        private final Integer perSecondLimit;

        /**
         * dailyLimit 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("dailyLimit")
        private final Long dailyLimit;

        /**
         * failClosed 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("failClosed")
        private final boolean failClosed;

        /**
         * updatedAt 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("updatedAt")
        private final String updatedAt;

        /**
         * 创建 LimitRow 实例。
         *
         * @param channel 渠道
         * @param provider 提供方
         * @param operation operation 字段值
         * @param perSecondLimit perSecondLimit 字段值
         * @param dailyLimit dailyLimit 字段值
         * @param failClosed failClosed 字段值
         * @param updatedAt updatedAt 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public LimitRow(@com.fasterxml.jackson.annotation.JsonProperty("channel") String channel, @com.fasterxml.jackson.annotation.JsonProperty("provider") String provider, @com.fasterxml.jackson.annotation.JsonProperty("operation") String operation, @com.fasterxml.jackson.annotation.JsonProperty("perSecondLimit") Integer perSecondLimit, @com.fasterxml.jackson.annotation.JsonProperty("dailyLimit") Long dailyLimit, @com.fasterxml.jackson.annotation.JsonProperty("failClosed") boolean failClosed, @com.fasterxml.jackson.annotation.JsonProperty("updatedAt") String updatedAt) {
            this.channel = channel;
            this.provider = provider;
            this.operation = operation;
            this.perSecondLimit = perSecondLimit;
            this.dailyLimit = dailyLimit;
            this.failClosed = failClosed;
            this.updatedAt = updatedAt;
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
         * 返回提供方。
         *
         * @return 提供方
         */
        public String provider() {
            return provider;
        }

        /**
         * 返回operation 字段值。
         *
         * @return operation 字段值
         */
        public String operation() {
            return operation;
        }

        /**
         * 返回perSecondLimit 字段值。
         *
         * @return perSecondLimit 字段值
         */
        public Integer perSecondLimit() {
            return perSecondLimit;
        }

        /**
         * 返回dailyLimit 字段值。
         *
         * @return dailyLimit 字段值
         */
        public Long dailyLimit() {
            return dailyLimit;
        }

        /**
         * 返回failClosed 字段值。
         *
         * @return failClosed 字段值
         */
        public boolean failClosed() {
            return failClosed;
        }

        /**
         * 返回updatedAt 字段值。
         *
         * @return updatedAt 字段值
         */
        public String updatedAt() {
            return updatedAt;
        }

        /**
         * 判断两个 LimitRow 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LimitRow that)) {
                return false;
            }
            return java.util.Objects.equals(channel, that.channel) && java.util.Objects.equals(provider, that.provider) && java.util.Objects.equals(operation, that.operation) && java.util.Objects.equals(perSecondLimit, that.perSecondLimit) && java.util.Objects.equals(dailyLimit, that.dailyLimit) && java.util.Objects.equals(failClosed, that.failClosed) && java.util.Objects.equals(updatedAt, that.updatedAt);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(channel, provider, operation, perSecondLimit, dailyLimit, failClosed, updatedAt);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "LimitRow[" + "channel=" + channel + ", " + "provider=" + provider + ", " + "operation=" + operation + ", " + "perSecondLimit=" + perSecondLimit + ", " + "dailyLimit=" + dailyLimit + ", " + "failClosed=" + failClosed + ", " + "updatedAt=" + updatedAt + "]";
        }
    }

    /**
     * ModeUpdateReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class ModeUpdateReq {

        /**
         * mode 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("mode")
        private final String mode;

        /**
         * 原因。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("reason")
        private final String reason;

        /**
         * 创建 ModeUpdateReq 实例。
         *
         * @param mode mode 字段值
         * @param reason 原因
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ModeUpdateReq(@com.fasterxml.jackson.annotation.JsonProperty("mode") String mode, @com.fasterxml.jackson.annotation.JsonProperty("reason") String reason) {
            this.mode = mode;
            this.reason = reason;
        }

        /**
         * 返回mode 字段值。
         *
         * @return mode 字段值
         */
        public String mode() {
            return mode;
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
         * 判断两个 ModeUpdateReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ModeUpdateReq that)) {
                return false;
            }
            return java.util.Objects.equals(mode, that.mode) && java.util.Objects.equals(reason, that.reason);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(mode, reason);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ModeUpdateReq[" + "mode=" + mode + ", " + "reason=" + reason + "]";
        }
    }

    /**
     * HealthResult 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class HealthResult {

        /**
         * 状态。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("status")
        private final String status;

        /**
         * 消息。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("message")
        private final String message;

        /**
         * 创建 HealthResult 实例。
         *
         * @param status 状态
         * @param message 消息
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public HealthResult(@com.fasterxml.jackson.annotation.JsonProperty("status") String status, @com.fasterxml.jackson.annotation.JsonProperty("message") String message) {
            this.status = status;
            this.message = message;
        }

        /**
         * 返回状态。
         *
         * @return 状态
         */
        public String status() {
            return status;
        }

        /**
         * 返回消息。
         *
         * @return 消息
         */
        public String message() {
            return message;
        }

        /**
         * 判断两个 HealthResult 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof HealthResult that)) {
                return false;
            }
            return java.util.Objects.equals(status, that.status) && java.util.Objects.equals(message, that.message);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(status, message);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "HealthResult[" + "status=" + status + ", " + "message=" + message + "]";
        }
    }

    /**
     * FallbackPolicyReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class FallbackPolicyReq {

        /**
         * 渠道。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("channel")
        private final String channel;

        /**
         * 提供方。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("provider")
        private final String provider;

        /**
         * fallbackChannel 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("fallbackChannel")
        private final String fallbackChannel;

        /**
         * fallbackProvider 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("fallbackProvider")
        private final String fallbackProvider;

        /**
         * 创建 FallbackPolicyReq 实例。
         *
         * @param channel 渠道
         * @param provider 提供方
         * @param fallbackChannel fallbackChannel 字段值
         * @param fallbackProvider fallbackProvider 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public FallbackPolicyReq(@com.fasterxml.jackson.annotation.JsonProperty("channel") String channel, @com.fasterxml.jackson.annotation.JsonProperty("provider") String provider, @com.fasterxml.jackson.annotation.JsonProperty("fallbackChannel") String fallbackChannel, @com.fasterxml.jackson.annotation.JsonProperty("fallbackProvider") String fallbackProvider) {
            this.channel = channel;
            this.provider = provider;
            this.fallbackChannel = fallbackChannel;
            this.fallbackProvider = fallbackProvider;
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
         * 返回提供方。
         *
         * @return 提供方
         */
        public String provider() {
            return provider;
        }

        /**
         * 返回fallbackChannel 字段值。
         *
         * @return fallbackChannel 字段值
         */
        public String fallbackChannel() {
            return fallbackChannel;
        }

        /**
         * 返回fallbackProvider 字段值。
         *
         * @return fallbackProvider 字段值
         */
        public String fallbackProvider() {
            return fallbackProvider;
        }

        /**
         * 判断两个 FallbackPolicyReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FallbackPolicyReq that)) {
                return false;
            }
            return java.util.Objects.equals(channel, that.channel) && java.util.Objects.equals(provider, that.provider) && java.util.Objects.equals(fallbackChannel, that.fallbackChannel) && java.util.Objects.equals(fallbackProvider, that.fallbackProvider);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(channel, provider, fallbackChannel, fallbackProvider);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "FallbackPolicyReq[" + "channel=" + channel + ", " + "provider=" + provider + ", " + "fallbackChannel=" + fallbackChannel + ", " + "fallbackProvider=" + fallbackProvider + "]";
        }
    }

    /**
     * ValidationResult 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class ValidationResult {

        /**
         * valid 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("valid")
        private final boolean valid;

        /**
         * 消息。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("message")
        private final String message;

        /**
         * 创建 ValidationResult 实例。
         *
         * @param valid valid 字段值
         * @param message 消息
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ValidationResult(@com.fasterxml.jackson.annotation.JsonProperty("valid") boolean valid, @com.fasterxml.jackson.annotation.JsonProperty("message") String message) {
            this.valid = valid;
            this.message = message;
        }

        /**
         * 返回valid 字段值。
         *
         * @return valid 字段值
         */
        public boolean valid() {
            return valid;
        }

        /**
         * 返回消息。
         *
         * @return 消息
         */
        public String message() {
            return message;
        }

        /**
         * 判断两个 ValidationResult 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ValidationResult that)) {
                return false;
            }
            return java.util.Objects.equals(valid, that.valid) && java.util.Objects.equals(message, that.message);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(valid, message);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ValidationResult[" + "valid=" + valid + ", " + "message=" + message + "]";
        }
    }

    /**
     * FallbackDecisionRow 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class FallbackDecisionRow {

        /**
         * originalChannel 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("originalChannel")
        private final String originalChannel;

        /**
         * originalProvider 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("originalProvider")
        private final String originalProvider;

        /**
         * finalChannel 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("finalChannel")
        private final String finalChannel;

        /**
         * finalProvider 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("finalProvider")
        private final String finalProvider;

        /**
         * decisionReason 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("decisionReason")
        private final String decisionReason;

        /**
         * createdAt 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("createdAt")
        private final String createdAt;

        /**
         * 创建 FallbackDecisionRow 实例。
         *
         * @param originalChannel originalChannel 字段值
         * @param originalProvider originalProvider 字段值
         * @param finalChannel finalChannel 字段值
         * @param finalProvider finalProvider 字段值
         * @param decisionReason decisionReason 字段值
         * @param createdAt createdAt 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public FallbackDecisionRow(@com.fasterxml.jackson.annotation.JsonProperty("originalChannel") String originalChannel, @com.fasterxml.jackson.annotation.JsonProperty("originalProvider") String originalProvider, @com.fasterxml.jackson.annotation.JsonProperty("finalChannel") String finalChannel, @com.fasterxml.jackson.annotation.JsonProperty("finalProvider") String finalProvider, @com.fasterxml.jackson.annotation.JsonProperty("decisionReason") String decisionReason, @com.fasterxml.jackson.annotation.JsonProperty("createdAt") String createdAt) {
            this.originalChannel = originalChannel;
            this.originalProvider = originalProvider;
            this.finalChannel = finalChannel;
            this.finalProvider = finalProvider;
            this.decisionReason = decisionReason;
            this.createdAt = createdAt;
        }

        /**
         * 返回originalChannel 字段值。
         *
         * @return originalChannel 字段值
         */
        public String originalChannel() {
            return originalChannel;
        }

        /**
         * 返回originalProvider 字段值。
         *
         * @return originalProvider 字段值
         */
        public String originalProvider() {
            return originalProvider;
        }

        /**
         * 返回finalChannel 字段值。
         *
         * @return finalChannel 字段值
         */
        public String finalChannel() {
            return finalChannel;
        }

        /**
         * 返回finalProvider 字段值。
         *
         * @return finalProvider 字段值
         */
        public String finalProvider() {
            return finalProvider;
        }

        /**
         * 返回decisionReason 字段值。
         *
         * @return decisionReason 字段值
         */
        public String decisionReason() {
            return decisionReason;
        }

        /**
         * 返回createdAt 字段值。
         *
         * @return createdAt 字段值
         */
        public String createdAt() {
            return createdAt;
        }

        /**
         * 判断两个 FallbackDecisionRow 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FallbackDecisionRow that)) {
                return false;
            }
            return java.util.Objects.equals(originalChannel, that.originalChannel) && java.util.Objects.equals(originalProvider, that.originalProvider) && java.util.Objects.equals(finalChannel, that.finalChannel) && java.util.Objects.equals(finalProvider, that.finalProvider) && java.util.Objects.equals(decisionReason, that.decisionReason) && java.util.Objects.equals(createdAt, that.createdAt);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(originalChannel, originalProvider, finalChannel, finalProvider, decisionReason, createdAt);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "FallbackDecisionRow[" + "originalChannel=" + originalChannel + ", " + "originalProvider=" + originalProvider + ", " + "finalChannel=" + finalChannel + ", " + "finalProvider=" + finalProvider + ", " + "decisionReason=" + decisionReason + ", " + "createdAt=" + createdAt + "]";
        }
    }

    /**
     * DedupeRecordRow 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class DedupeRecordRow {

        /**
         * dedupeGroup 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("dedupeGroup")
        private final String dedupeGroup;

        /**
         * contentHash 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("contentHash")
        private final String contentHash;

        /**
         * 渠道。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("channel")
        private final String channel;

        /**
         * 用户标识。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("userId")
        private final String userId;

        /**
         * expiresAt 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("expiresAt")
        private final String expiresAt;

        /**
         * 创建 DedupeRecordRow 实例。
         *
         * @param dedupeGroup dedupeGroup 字段值
         * @param contentHash contentHash 字段值
         * @param channel 渠道
         * @param userId 用户标识
         * @param expiresAt expiresAt 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public DedupeRecordRow(@com.fasterxml.jackson.annotation.JsonProperty("dedupeGroup") String dedupeGroup, @com.fasterxml.jackson.annotation.JsonProperty("contentHash") String contentHash, @com.fasterxml.jackson.annotation.JsonProperty("channel") String channel, @com.fasterxml.jackson.annotation.JsonProperty("userId") String userId, @com.fasterxml.jackson.annotation.JsonProperty("expiresAt") String expiresAt) {
            this.dedupeGroup = dedupeGroup;
            this.contentHash = contentHash;
            this.channel = channel;
            this.userId = userId;
            this.expiresAt = expiresAt;
        }

        /**
         * 返回dedupeGroup 字段值。
         *
         * @return dedupeGroup 字段值
         */
        public String dedupeGroup() {
            return dedupeGroup;
        }

        /**
         * 返回contentHash 字段值。
         *
         * @return contentHash 字段值
         */
        public String contentHash() {
            return contentHash;
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
         * 返回用户标识。
         *
         * @return 用户标识
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回expiresAt 字段值。
         *
         * @return expiresAt 字段值
         */
        public String expiresAt() {
            return expiresAt;
        }

        /**
         * 判断两个 DedupeRecordRow 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DedupeRecordRow that)) {
                return false;
            }
            return java.util.Objects.equals(dedupeGroup, that.dedupeGroup) && java.util.Objects.equals(contentHash, that.contentHash) && java.util.Objects.equals(channel, that.channel) && java.util.Objects.equals(userId, that.userId) && java.util.Objects.equals(expiresAt, that.expiresAt);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(dedupeGroup, contentHash, channel, userId, expiresAt);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "DedupeRecordRow[" + "dedupeGroup=" + dedupeGroup + ", " + "contentHash=" + contentHash + ", " + "channel=" + channel + ", " + "userId=" + userId + ", " + "expiresAt=" + expiresAt + "]";
        }
    }

    /**
     * MapperBackedService 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    private static class MapperBackedService implements Service {

        /**
         * connector数据访问组件，用于访问和持久化对应数据。
         */
        private final ChannelConnectorMapper connectorMapper;
        /**
         * 限制数据访问组件，用于访问和持久化对应数据。
         */
        private final ChannelProviderLimitMapper limitMapper;
        /**
         * 决策数据访问组件，用于访问和持久化对应数据。
         */
        private final ChannelFallbackDecisionMapper decisionMapper;
        /**
         * deduperecord数据访问组件，用于访问和持久化对应数据。
         */
        private final ChannelDedupeRecordMapper dedupeRecordMapper;
        /**
         * fallback服务，用于承接对应业务能力和领域编排。
         */
        private final ChannelFallbackService fallbackService;

        /**
         * 初始化 MapperBackedService 实例。
         *
         * @param connectorMapper 依赖组件，用于完成数据访问或外部能力调用。
         * @param limitMapper 依赖组件，用于完成数据访问或外部能力调用。
         * @param decisionMapper 依赖组件，用于完成数据访问或外部能力调用。
         * @param dedupeRecordMapper 依赖组件，用于完成数据访问或外部能力调用。
         * @param fallbackService 依赖组件，用于完成数据访问或外部能力调用。
         */
        private MapperBackedService(ChannelConnectorMapper connectorMapper,
                                    ChannelProviderLimitMapper limitMapper,
                                    ChannelFallbackDecisionMapper decisionMapper,
                                    ChannelDedupeRecordMapper dedupeRecordMapper,
                                    ChannelFallbackService fallbackService) {
            this.connectorMapper = connectorMapper;
            this.limitMapper = limitMapper;
            this.decisionMapper = decisionMapper;
            this.dedupeRecordMapper = dedupeRecordMapper;
            this.fallbackService = fallbackService;
        }

        @Override
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        public List<ConnectorRow> list(Long tenantId) {
            return connectorMapper.selectList(new LambdaQueryWrapper<ChannelConnectorDO>()
                            .in(tenantId != null && tenantId != 0L, ChannelConnectorDO::getTenantId, List.of(0L, tenantId))
                            .eq(tenantId == null || tenantId == 0L, ChannelConnectorDO::getTenantId, 0L)
                            .orderByAsc(ChannelConnectorDO::getChannel, ChannelConnectorDO::getProvider))
                    .stream()
                    .map(row -> new ConnectorRow(
                            row.getId(),
                            row.getConnectorKey(),
                            row.getChannel(),
                            row.getProvider(),
                            row.getMode(),
                            row.getHealthStatus(),
                            row.getHealthMessage()))
                    .toList();
        }

        @Override
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        public List<LimitRow> listLimits(Long tenantId) {
            return limitMapper.selectList(new LambdaQueryWrapper<ChannelProviderLimitDO>()
                            .in(tenantId != null && tenantId != 0L, ChannelProviderLimitDO::getTenantId, List.of(0L, tenantId))
                            .eq(tenantId == null || tenantId == 0L, ChannelProviderLimitDO::getTenantId, 0L)
                            .orderByAsc(ChannelProviderLimitDO::getChannel, ChannelProviderLimitDO::getProvider))
                    .stream()
                    .map(row -> new LimitRow(
                            row.getChannel(),
                            row.getProvider(),
                            row.getOperation(),
                            row.getPerSecondLimit(),
                            row.getDailyLimit(),
                            row.getFailClosed() == null || row.getFailClosed() == 1,
                            format(row.getUpdatedAt())))
                    .toList();
        }

        @Override
        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param id 业务对象 ID，用于定位具体记录。
         * @param mode mode 参数，用于 updateMode 流程中的校验、计算或对象转换。
         * @param reason 原因说明，用于记录状态变化的业务依据。
         */
        public void updateMode(Long tenantId, Long id, String mode, String reason) {
            ChannelConnectorDO row = requireConnector(tenantId, id);
            ChannelConnector.ConnectorMode parsedMode = parseMode(mode);
            row.setMode(parsedMode.name());
            row.setDisabledReason(parsedMode == ChannelConnector.ConnectorMode.DISABLED ? reason : null);
            connectorMapper.updateById(row);
        }

        @Override
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param id 业务对象 ID，用于定位具体记录。
         * @return 返回 testHealth 流程生成的业务结果。
         */
        public HealthResult testHealth(Long tenantId, Long id) {
            ChannelConnectorDO row = requireConnector(tenantId, id);
            ChannelConnector.ConnectorMode mode = parseMode(row.getMode());
            String status = switch (mode) {
                case SANDBOX -> "UP";
                case DISABLED -> "DISABLED";
                case REAL -> row.getHealthStatus() == null ? "UNKNOWN" : row.getHealthStatus();
            };
            String message = switch (mode) {
                case SANDBOX -> "sandbox connector ready";
                case DISABLED -> row.getDisabledReason();
                case REAL -> row.getHealthMessage();
            };
            row.setHealthStatus(status);
            row.setHealthMessage(message);
            row.setLastCheckedAt(LocalDateTime.now());
            connectorMapper.updateById(row);
            return new HealthResult(status, message);
        }

        @Override
        /**
         * 校验输入、权限或业务前置条件。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param req 请求对象，承载本次操作的输入参数。
         * @return 返回布尔判断结果。
         */
        public ValidationResult validateFallback(Long tenantId, FallbackPolicyReq req) {
            try {
                fallbackService.validateCandidate(tenantId, req.channel(), req.provider(), req.fallbackChannel(), req.fallbackProvider());
                return new ValidationResult(true, "ok");
            } catch (IllegalArgumentException ex) {
                return new ValidationResult(false, ex.getMessage());
            }
        }

        @Override
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        public List<FallbackDecisionRow> listDecisions(Long tenantId) {
            return decisionMapper.selectList(new LambdaQueryWrapper<ChannelFallbackDecisionDO>()
                            .in(tenantId != null && tenantId != 0L, ChannelFallbackDecisionDO::getTenantId, List.of(0L, tenantId))
                            .eq(tenantId == null || tenantId == 0L, ChannelFallbackDecisionDO::getTenantId, 0L)
                            .orderByDesc(ChannelFallbackDecisionDO::getCreatedAt)
                            .last("LIMIT 50"))
                    .stream()
                    .map(row -> new FallbackDecisionRow(
                            row.getOriginalChannel(),
                            row.getOriginalProvider(),
                            row.getFinalChannel(),
                            row.getFinalProvider(),
                            row.getDecisionReason(),
                            format(row.getCreatedAt())))
                    .toList();
        }

        @Override
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        public List<DedupeRecordRow> listDedupeRecords(Long tenantId) {
            return dedupeRecordMapper.selectList(new LambdaQueryWrapper<ChannelDedupeRecordDO>()
                            .in(tenantId != null && tenantId != 0L, ChannelDedupeRecordDO::getTenantId, List.of(0L, tenantId))
                            .eq(tenantId == null || tenantId == 0L, ChannelDedupeRecordDO::getTenantId, 0L)
                            .orderByDesc(ChannelDedupeRecordDO::getCreatedAt)
                            .last("LIMIT 50"))
                    .stream()
                    .map(row -> new DedupeRecordRow(
                            row.getDedupeGroup(),
                            row.getContentHash(),
                            row.getChannel(),
                            row.getUserId(),
                            format(row.getExpiresAt())))
                    .toList();
        }

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param id 业务对象 ID，用于定位具体记录。
         * @return 返回 requireConnector 流程生成的业务结果。
         */
        private ChannelConnectorDO requireConnector(Long tenantId, Long id) {
            ChannelConnectorDO row = connectorMapper.selectById(id);
            if (row == null) {
                /**
                 * 执行 illegalargumentexception 对应的内部处理流程。
                 *
                 * @param id 标识，由调用方提供
                 * @return 返回内部处理结果
                 */
                throw new IllegalArgumentException("Channel connector not found: " + id);
            }
            Long effectiveTenant = tenantId == null ? 0L : tenantId;
            if (effectiveTenant != 0L && row.getTenantId() != null && !row.getTenantId().equals(effectiveTenant)) {
                /**
                 * 执行 illegalargumentexception 对应的内部处理流程。
                 *
                 * @param id 标识，由调用方提供
                 * @return 返回内部处理结果
                 */
                throw new IllegalArgumentException("Channel connector tenant mismatch: " + id);
            }
            return row;
        }

        /**
         * 解析、归一化或保护输入值，生成安全可用的中间结果。
         *
         * @param mode mode 参数，用于 parseMode 流程中的校验、计算或对象转换。
         * @return 返回解析、归一化或安全处理后的值。
         */
        private static ChannelConnector.ConnectorMode parseMode(String mode) {
            if (mode == null || mode.isBlank()) {
                return ChannelConnector.ConnectorMode.DISABLED;
            }
            return ChannelConnector.ConnectorMode.valueOf(mode.trim().toUpperCase());
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param value 待处理值，用于规则计算或转换。
         * @return 返回 format 生成的文本或业务键。
         */
        private static String format(LocalDateTime value) {
            return value == null ? null : value.toString();
        }
    }
}
