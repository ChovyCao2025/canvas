package org.chovy.canvas.engine.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.chovy.canvas.engine.channel.ChannelConnector;
import org.chovy.canvas.engine.channel.ChannelConnectorRegistry;
import org.chovy.canvas.engine.policy.MarketingPolicyService;
import org.chovy.canvas.engine.policy.MarketingPolicyService.PolicyDecision;
import org.chovy.canvas.infrastructure.http.ExternalHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reach Delivery 触达投递组件。
 *
 * <p>封装营销消息发送后的记录写入和投递状态维护，供短信、邮件、Push 等节点复用。
 * <p>外部渠道差异应在调用方或适配层处理，本类聚焦统一的投递流水语义。
 */
@Slf4j
@Service
public class ReachDeliveryService {

    /** 消息发送记录 Mapper，用于落库触达请求、状态和外部消息 ID。 */
    private final MessageSendRecordMapper recordMapper;
    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;
    /** 外部 HTTP 调用边界。 */
    private final ExternalHttpClient externalHttpClient;
    /** Crash-safe outbox boundary. Null only for legacy/unit-test construction. */
    private final DeliveryOutboxService outboxService;
    /** Marketing policy boundary. Null only for legacy/unit-test construction. */
    private final MarketingPolicyService policyService;
    /** Optional connector registry used by outbox dispatch to call provider-specific connectors. */
    private final ChannelConnectorRegistry connectorRegistry;

    /** 初始化触达投递依赖，并按配置创建外部触达平台客户端。 */
    @Autowired
    public ReachDeliveryService(
            MessageSendRecordMapper recordMapper,
            ObjectMapper objectMapper,
            ExternalHttpClient externalHttpClient,
            ObjectProvider<DeliveryOutboxService> outboxServiceProvider,
            ObjectProvider<MarketingPolicyService> policyServiceProvider,
            ObjectProvider<ChannelConnectorRegistry> connectorRegistryProvider
    ) {
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
        this.externalHttpClient = externalHttpClient;
        this.outboxService = outboxServiceProvider == null ? null : outboxServiceProvider.getIfAvailable();
        this.policyService = policyServiceProvider == null ? null : policyServiceProvider.getIfAvailable();
        this.connectorRegistry = connectorRegistryProvider == null ? null : connectorRegistryProvider.getIfAvailable();
    }

    /** 初始化触达投递依赖，并按配置创建外部触达平台客户端。 */
    public ReachDeliveryService(
            MessageSendRecordMapper recordMapper,
            ObjectMapper objectMapper,
            ExternalHttpClient externalHttpClient
    ) {
        this(recordMapper, objectMapper, externalHttpClient, null, null, null);
    }

    /** 初始化触达投递依赖，并显式传入策略服务，便于 focused tests 覆盖直发路径。 */
    public ReachDeliveryService(
            MessageSendRecordMapper recordMapper,
            ObjectMapper objectMapper,
            ExternalHttpClient externalHttpClient,
            MarketingPolicyService policyService
    ) {
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
        this.externalHttpClient = externalHttpClient;
        this.outboxService = null;
        this.policyService = policyService;
        this.connectorRegistry = null;
    }

    /** 初始化触达投递依赖，并显式传入连接器注册表，便于 outbox 派发 focused tests。 */
    public ReachDeliveryService(
            MessageSendRecordMapper recordMapper,
            ObjectMapper objectMapper,
            ExternalHttpClient externalHttpClient,
            ChannelConnectorRegistry connectorRegistry
    ) {
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
        this.externalHttpClient = externalHttpClient;
        this.outboxService = null;
        this.policyService = null;
        this.connectorRegistry = connectorRegistry;
    }

    /** 执行触达投递并写入发送记录。 */
    public Mono<DeliveryResult> send(DeliveryRequest request) {
        if (outboxService != null) {
            return Mono.fromCallable(() -> outboxService.enqueue(request))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(outbox -> new DeliveryResult(
                            true,
                            outbox.isDuplicate(),
                            outbox.getMessageSendRecordId(),
                            outbox.getProviderMessageId(),
                            null
                    ));
        }
        return sendDirect(request);
    }

    /** Legacy direct-send path retained for tests and explicit construction without an outbox bean. */
    Mono<DeliveryResult> sendDirect(DeliveryRequest request) {
        return Mono.fromCallable(() -> prepareRecord(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(prepared -> {
                    if (prepared.duplicate()) {
                        // 命中幂等键时直接返回已有流水状态，避免重复调用外部渠道造成二次触达。
                        boolean sent = MessageSendRecordDO.STATUS_SENT.equals(prepared.record().getStatus())
                                || MessageSendRecordDO.STATUS_PENDING.equals(prepared.record().getStatus());
                        return Mono.just(new DeliveryResult(
                                sent,
                                true,
                                prepared.record().getId(),
                                prepared.record().getExternalMessageId(),
                                prepared.record().getErrorMessage()
                        ));
                    }
                    PolicyDecision decision = evaluatePolicy(request);
                    if (!decision.allowed()) {
                        return Mono.just(markSkipped(prepared.record(), decision));
                    }
                    return callReachPlatform(request.payload())
                            .flatMap(response -> markSent(prepared.record(), response))
                            .onErrorResume(e -> markFailed(prepared.record(), e));
                });
    }

    /** 准备发送记录；命中幂等键时返回已有记录而不重复投递。 */
    private PreparedRecord prepareRecord(DeliveryRequest request) {
        MessageSendRecordDO existing = recordMapper.selectOne(new LambdaQueryWrapper<MessageSendRecordDO>()
                .eq(MessageSendRecordDO::getIdempotencyKey, request.idempotencyKey())
                .last("LIMIT 1"));
        if (existing != null) {
            return new PreparedRecord(existing, true);
        }

        // 先落 PENDING 流水再调用外部平台，失败时仍能回写错误并保留审计轨迹。
        MessageSendRecordDO record = new MessageSendRecordDO();
        record.setTenantId(request.tenantId());
        record.setExecutionId(request.executionId());
        record.setCanvasId(request.canvasId());
        record.setUserId(request.userId());
        record.setNodeId(request.nodeId());
        record.setChannel(request.channel());
        record.setTemplateId(request.templateId());
        record.setIdempotencyKey(request.idempotencyKey());
        record.setRequestPayload(toJson(request.payload()));
        record.setStatus(MessageSendRecordDO.STATUS_PENDING);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(record.getCreatedAt());
        recordMapper.insert(record);
        return new PreparedRecord(record, false);
    }

    /** 调用外部触达平台发送消息并返回渠道响应。 */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> dispatchToProvider(DeliveryOutboxDO outbox) {
        Map<String, Object> payload = payloadAsMap(outbox);
        ChannelConnector connector = resolveConnector(outbox);
        if (connector != null) {
            return Mono.fromCallable(() -> dispatchToConnector(outbox, payload, connector))
                    .subscribeOn(Schedulers.boundedElastic());
        }
        return callReachPlatform(payload);
    }

    private ChannelConnector resolveConnector(DeliveryOutboxDO outbox) {
        if (connectorRegistry == null || outbox == null) {
            return null;
        }
        ChannelConnector connector = connectorRegistry.resolve(outbox.getTenantId(), outbox.getChannel(), outbox.getProvider());
        if (connector == null || connector.mode() == ChannelConnector.ConnectorMode.DISABLED) {
            return null;
        }
        return connector;
    }

    private Map<String, Object> dispatchToConnector(DeliveryOutboxDO outbox,
                                                    Map<String, Object> payload,
                                                    ChannelConnector connector) {
        ChannelConnector.ConnectorSendResult result = connector.send(new ChannelConnector.ConnectorSendRequest(
                outbox.getTenantId(),
                outbox.getChannel(),
                outbox.getProvider(),
                outbox.getUserId(),
                payload));
        if (!result.accepted()) {
            String reason = result.reason() == null || result.reason().isBlank()
                    ? "connector send failed: " + result.status()
                    : result.reason();
            throw new IllegalStateException(reason);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        if (result.externalMessageId() != null) {
            response.put(MapFieldKeys.MESSAGE_ID, result.externalMessageId());
            response.put("externalMessageId", result.externalMessageId());
        }
        if (result.status() != null) {
            response.put("connectorStatus", result.status());
        }
        if (result.reason() != null) {
            response.put("connectorReason", result.reason());
        }
        return response;
    }

    private Mono<Map<String, Object>> callReachPlatform(Map<String, Object> payload) {
        // 渠道、模板、变量和幂等键统一放入 payload，具体渠道差异由触达平台适配。
        return externalHttpClient.postJson(ExternalHttpClient.REACH_PLATFORM, "/send", payload);
    }

    /** 将发送记录标记为成功并保存外部渠道消息 ID。 */
    private Mono<DeliveryResult> markSent(MessageSendRecordDO record, Map<String, Object> response) {
        return Mono.fromCallable(() -> {
                    record.setStatus(MessageSendRecordDO.STATUS_SENT);
                    Object messageId = response.getOrDefault(MapFieldKeys.MESSAGE_ID, response.get(MapFieldKeys.ID));
                    if (messageId != null) {
                        // 记录外部消息 ID，便于后续回执、排障和人工核对渠道侧状态。
                        record.setExternalMessageId(messageId.toString());
                    }
                    record.setUpdatedAt(LocalDateTime.now());
                    recordMapper.updateById(record);
                    return new DeliveryResult(true, false, record.getId(), record.getExternalMessageId(), null);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 将发送记录标记为失败并保存截断后的错误信息。 */
    private Mono<DeliveryResult> markFailed(MessageSendRecordDO record, Throwable error) {
        return Mono.fromCallable(() -> {
                    String message = error.getMessage() == null ? "delivery failed" : error.getMessage();
                    record.setStatus(MessageSendRecordDO.STATUS_FAILED);
                    record.setErrorMessage(message.substring(0, Math.min(500, message.length())));
                    record.setUpdatedAt(LocalDateTime.now());
                    recordMapper.updateById(record);
                    log.warn("[DELIVERY] 触达失败 recordId={} channel={} reason={}",
                            record.getId(), record.getChannel(), record.getErrorMessage());
                    return new DeliveryResult(false, false, record.getId(), null, record.getErrorMessage());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private PolicyDecision evaluatePolicy(DeliveryRequest request) {
        if (policyService == null || request.policyOptions() == null) {
            return PolicyDecision.allow();
        }
        PolicyOptions policy = request.policyOptions();
        PolicyDecision decision = policyService.consentAllowed(
                request.userId(), request.channel(), policy.requireExplicitConsent());
        if (!decision.allowed()) return decision;

        decision = policyService.suppressionAllowed(request.userId(), request.channel());
        if (!decision.allowed()) return decision;

        decision = policyService.channelAvailable(request.userId(), request.channel());
        if (!decision.allowed()) return decision;

        decision = policyService.quietHoursAllowed(
                request.userId(), policy.quietStart(), policy.quietEnd(), policy.quietTimezone());
        if (!decision.allowed()) return decision;

        return policyService.consumeFrequency(
                request.userId(),
                request.canvasId(),
                request.nodeId(),
                policy.frequencyScope(),
                request.channel(),
                policy.frequencyMax(),
                Duration.ofSeconds(policy.frequencyWindowSeconds()));
    }

    private DeliveryResult markSkipped(MessageSendRecordDO record, PolicyDecision decision) {
        String reason = decision.reasonCode() + ": " + decision.reasonMessage();
        record.setStatus(MessageSendRecordDO.STATUS_SKIPPED);
        record.setErrorMessage(reason.substring(0, Math.min(500, reason.length())));
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.updateById(record);
        return new DeliveryResult(false, false, record.getId(), null, record.getErrorMessage());
    }

    /** 构造标准化触达投递请求。 */
    public DeliveryRequest request(
            Long tenantId,
            String executionId,
            Long canvasId,
            String userId,
            String nodeId,
            String channel,
            String templateId,
            Map<String, Object> content,
            Map<String, Object> variables,
            String idempotencyKey
    ) {
        return request(tenantId, executionId, canvasId, userId, nodeId, channel, templateId,
                content, variables, idempotencyKey, PolicyOptions.defaults());
    }

    /** 构造标准化触达投递请求。 */
    public DeliveryRequest request(
            Long tenantId,
            String executionId,
            Long canvasId,
            String userId,
            String nodeId,
            String channel,
            String templateId,
            Map<String, Object> content,
            Map<String, Object> variables,
            String idempotencyKey,
            PolicyOptions policyOptions
    ) {
        return request(tenantId, executionId, canvasId, userId, nodeId, channel, "REACH",
                templateId, content, variables, idempotencyKey, policyOptions);
    }

    /** 构造标准化触达投递请求，并保留实际渠道供应商，供 outbox、回执和审计使用。 */
    public DeliveryRequest request(
            Long tenantId,
            String executionId,
            Long canvasId,
            String userId,
            String nodeId,
            String channel,
            String provider,
            String templateId,
            Map<String, Object> content,
            Map<String, Object> variables,
            String idempotencyKey,
            PolicyOptions policyOptions
    ) {
        String normalizedProvider = normalizeProvider(provider);
        Map<String, Object> payload = new LinkedHashMap<>();
        // LinkedHashMap 让序列化后的请求字段顺序稳定，便于日志比对和问题排查。
        payload.put(MapFieldKeys.CHANNEL, channel);
        payload.put(MapFieldKeys.TEMPLATE_ID, templateId);
        payload.put(MapFieldKeys.USER_ID, userId);
        payload.put(MapFieldKeys.CONTENT, content == null ? Map.of() : content);
        payload.put(MapFieldKeys.VARIABLES, variables == null ? Map.of() : variables);
        payload.put(MapFieldKeys.IDEMPOTENCY_KEY, idempotencyKey);
        payload.put("provider", normalizedProvider);
        return new DeliveryRequest(tenantId, executionId, canvasId, userId, nodeId, channel, normalizedProvider,
                templateId, payload, idempotencyKey, policyOptions);
    }

    public DeliveryRequest request(
            String executionId,
            Long canvasId,
            String userId,
            String nodeId,
            String channel,
            String templateId,
            Map<String, Object> content,
            Map<String, Object> variables,
            String idempotencyKey
    ) {
        return request(null, executionId, canvasId, userId, nodeId, channel, templateId,
                content, variables, idempotencyKey);
    }

    /** 将触达请求载荷序列化为发送记录中的 JSON 文本。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("触达请求序列化失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payloadAsMap(DeliveryOutboxDO outbox) {
        if (outboxService != null) {
            return outboxService.payloadAsMap(outbox);
        }
        if (outbox == null || outbox.getPayloadJson() == null || outbox.getPayloadJson().isBlank()) {
            return Map.of();
        }
        try {
            Object value = objectMapper.readValue(outbox.getPayloadJson(), Map.class);
            return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        } catch (Exception ex) {
            throw new IllegalArgumentException("delivery payload JSON is invalid", ex);
        }
    }

    private String normalizeProvider(String provider) {
        return provider == null || provider.isBlank() ? "REACH" : provider.trim().toUpperCase(Locale.ROOT);
    }

    /** 发送前记录准备结果，标识本次是否命中幂等重复请求。 */
    private record PreparedRecord(
            /** 已落库或已命中的发送记录。 */
            MessageSendRecordDO record,
            /** 是否命中幂等重复请求。 */
            boolean duplicate
    ) {
    }

    /** 标准化触达投递请求，封装节点执行产生的渠道、模板、变量和幂等键。 */
    public record DeliveryRequest(
            /* 租户 ID。 */
            Long tenantId,
            /* 画布执行实例 ID。 */
            String executionId,
            /* 画布 ID。 */
            Long canvasId,
            /* 接收触达的用户 ID。 */
            String userId,
            /* 发起触达的节点 ID。 */
            String nodeId,
            /* 触达渠道。 */
            String channel,
            /* 触达平台或渠道供应商。 */
            String provider,
            /* 渠道模板 ID。 */
            String templateId,
            /* 发送给触达平台的标准化载荷。 */
            Map<String, Object> payload,
            /* 幂等键，用于避免重复触达。 */
            String idempotencyKey,
            /* 触达前营销策略选项。 */
            PolicyOptions policyOptions
    ) {
    }

    /** 触达前营销策略选项。 */
    public record PolicyOptions(
            boolean requireExplicitConsent,
            String quietStart,
            String quietEnd,
            String quietTimezone,
            String frequencyScope,
            int frequencyMax,
            int frequencyWindowSeconds
    ) {
        public static PolicyOptions defaults() {
            return new PolicyOptions(true, "22:00", "08:00", "USER_LOCAL", "JOURNEY", 1, 86400);
        }
    }

    /** 触达投递结果，返回是否发送、是否命中幂等、记录 ID 和外部渠道回执。 */
    public record DeliveryResult(
            /* 是否已成功提交触达。 */
            boolean sent,
            /* 是否命中幂等重复请求。 */
            boolean duplicate,
            /* 对应的发送记录 ID。 */
            Long recordId,
            /* 外部渠道返回的消息 ID。 */
            String externalMessageId,
            /* 失败原因，成功时为空。 */
            String errorMessage
    ) {
    }
}
