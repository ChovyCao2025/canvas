package org.chovy.canvas.engine.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.chovy.canvas.engine.channel.ChannelConnector;
import org.chovy.canvas.engine.channel.ChannelConnectorRegistry;
import org.chovy.canvas.engine.channel.DownstreamBulkheadRegistry;
import org.chovy.canvas.engine.policy.MarketingPolicyService;
import org.chovy.canvas.engine.policy.MarketingPolicyService.PolicyDecision;
import org.chovy.canvas.infrastructure.http.ExternalHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
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
    /** Optional provider bulkhead guard for downstream channel systems. */
    private final DownstreamBulkheadRegistry bulkheadRegistry;

    /** 初始化触达投递依赖，并按配置创建外部触达平台客户端。 */
    @Autowired
    public ReachDeliveryService(
            MessageSendRecordMapper recordMapper,
            ObjectMapper objectMapper,
            ExternalHttpClient externalHttpClient,
            ObjectProvider<DeliveryOutboxService> outboxServiceProvider,
            ObjectProvider<MarketingPolicyService> policyServiceProvider,
            ObjectProvider<ChannelConnectorRegistry> connectorRegistryProvider,
            ObjectProvider<DownstreamBulkheadRegistry> bulkheadRegistryProvider
    ) {
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
        this.externalHttpClient = externalHttpClient;
        this.outboxService = outboxServiceProvider == null ? null : outboxServiceProvider.getIfAvailable();
        this.policyService = policyServiceProvider == null ? null : policyServiceProvider.getIfAvailable();
        this.connectorRegistry = connectorRegistryProvider == null ? null : connectorRegistryProvider.getIfAvailable();
        this.bulkheadRegistry = bulkheadRegistryProvider == null ? null : bulkheadRegistryProvider.getIfAvailable();
    }

    /** 初始化触达投递依赖，并按配置创建外部触达平台客户端。 */
    public ReachDeliveryService(
            MessageSendRecordMapper recordMapper,
            ObjectMapper objectMapper,
            ExternalHttpClient externalHttpClient
    ) {
        this(recordMapper, objectMapper, externalHttpClient, null, null, null, null);
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
        this.bulkheadRegistry = null;
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
        this.bulkheadRegistry = null;
    }

    /** 初始化触达投递依赖，并显式传入 bulkhead registry，便于 focused tests 覆盖 provider 保护。 */
    public ReachDeliveryService(
            MessageSendRecordMapper recordMapper,
            ObjectMapper objectMapper,
            ExternalHttpClient externalHttpClient,
            ChannelConnectorRegistry connectorRegistry,
            DownstreamBulkheadRegistry bulkheadRegistry
    ) {
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
        this.externalHttpClient = externalHttpClient;
        this.outboxService = null;
        this.policyService = null;
        this.connectorRegistry = connectorRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
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
        assertProviderBulkheadPermits(outbox);
        Map<String, Object> payload = payloadAsMap(outbox);
        ChannelConnector connector = resolveConnector(outbox);
        if (connector != null) {
            return Mono.fromCallable(() -> dispatchToConnector(outbox, payload, connector))
                    .subscribeOn(Schedulers.boundedElastic());
        }
        return callReachPlatform(payload);
    }

    private void assertProviderBulkheadPermits(DeliveryOutboxDO outbox) {
        if (bulkheadRegistry == null || outbox == null) {
            return;
        }
        DownstreamBulkheadRegistry.Decision decision = bulkheadRegistry.permit(
                outbox.getTenantId(),
                outbox.getProvider(),
                outbox.getChannel(),
                Instant.now());
        if (!decision.permitted()) {
            throw new IllegalStateException("provider bulkhead open: " + decision.reason());
        }
    }

    /**
     * 根据 outbox 中的租户、渠道和供应商解析可用渠道连接器。
     *
     * @param outbox 触达 outbox 记录
     * @return 可用连接器，未配置或禁用时返回 null
     */
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

    /**
     * 通过渠道连接器执行发送。
     *
     * @param outbox 触达 outbox 记录
     * @param payload 标准化发送载荷
     * @param connector 渠道连接器
     * @return 连接器标准响应
     */
    private Map<String, Object> dispatchToConnector(DeliveryOutboxDO outbox,
                                                    Map<String, Object> payload,
                                                    ChannelConnector connector) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ChannelConnector.ConnectorSendResult result = connector.send(new ChannelConnector.ConnectorSendRequest(
                outbox.getTenantId(),
                outbox.getChannel(),
                outbox.getProvider(),
                outbox.getUserId(),
                payload));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return response;
    }

    /**
     * 调用默认触达平台发送消息。
     *
     * @param payload 标准化发送载荷
     * @return 触达平台响应
     */
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

    /**
     * 按同意、抑制、渠道可达、静默时段和频控顺序评估触达策略。
     *
     * @param request 触达请求
     * @return 策略评估结果
     */
    private PolicyDecision evaluatePolicy(DeliveryRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (policyService == null || request.policyOptions() == null) {
            return PolicyDecision.allow();
        }
        PolicyOptions policy = request.policyOptions();
        PolicyDecision decision = policyService.consentAllowed(
                request.tenantId(), request.userId(), request.channel(), policy.requireExplicitConsent());
        if (!decision.allowed()) return decision;

        decision = policyService.suppressionAllowed(request.tenantId(), request.userId(), request.channel());
        if (!decision.allowed()) return decision;

        decision = policyService.channelAvailable(request.tenantId(), request.userId(), request.channel());
        if (!decision.allowed()) return decision;

        decision = policyService.quietHoursAllowed(
                request.userId(), policy.quietStart(), policy.quietEnd(), policy.quietTimezone());
        if (!decision.allowed()) return decision;

        // 汇总前面计算出的状态和明细，返回给调用方。
        return policyService.consumeFrequency(
                request.userId(),
                request.canvasId(),
                request.nodeId(),
                policy.frequencyScope(),
                request.channel(),
                policy.frequencyMax(),
                Duration.ofSeconds(policy.frequencyWindowSeconds()));
    }

    /**
     * 将发送记录标记为策略跳过。
     *
     * @param record 发送记录
     * @param decision 策略拒绝结果
     * @return 跳过状态的触达结果
     */
    private DeliveryResult markSkipped(MessageSendRecordDO record, PolicyDecision decision) {
        String reason = decision.reasonCode() + ": " + decision.reasonMessage();
        record.setStatus(MessageSendRecordDO.STATUS_SKIPPED);
        record.setErrorMessage(reason.substring(0, Math.min(500, reason.length())));
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.updateById(record);
        return new DeliveryResult(false, false, record.getId(), null, record.getErrorMessage());
    }

    /**
     * 构造使用默认供应商和默认策略的标准化触达请求。
     *
     * <p>该方法只组装 payload 和默认策略，不写发送流水、不写 outbox、也不调用外部渠道；返回对象通常传给
     * {@link #send(DeliveryRequest)} 执行后续幂等投递。
     *
     * @param tenantId 租户 ID
     * @param executionId 画布执行实例 ID
     * @param canvasId 画布 ID
     * @param userId 接收用户 ID
     * @param nodeId 触达节点 ID
     * @param channel 触达渠道
     * @param templateId 模板 ID
     * @param content 模板内容或已渲染内容
     * @param variables 渲染变量和上下文变量
     * @param idempotencyKey 触达幂等键
     * @return 标准化触达请求
     */
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

    /**
     * 构造使用默认供应商的标准化触达请求。
     *
     * <p>该方法保留调用方传入的营销策略选项，并把供应商默认成 REACH；只返回请求对象，不产生数据库、Redis 或外部调用副作用。
     *
     * @param tenantId 租户 ID
     * @param executionId 画布执行实例 ID
     * @param canvasId 画布 ID
     * @param userId 接收用户 ID
     * @param nodeId 触达节点 ID
     * @param channel 触达渠道
     * @param templateId 模板 ID
     * @param content 内容字段
     * @param variables 模板变量
     * @param idempotencyKey 触达幂等键
     * @param policyOptions 触达前策略选项
     * @return 标准化触达请求
     */
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

    /**
     * 构造标准化触达投递请求，并保留实际渠道供应商。
     *
     * <p>方法将渠道、模板、用户、内容、变量、幂等键和供应商写入稳定顺序的 payload；该 payload 后续会落入发送流水/outbox，
     * 并用于渠道连接器调用、回执关联和审计。方法本身只构造对象，不执行外部调用。
     *
     * @param tenantId 租户 ID
     * @param executionId 画布执行实例 ID
     * @param canvasId 画布 ID
     * @param userId 接收用户 ID
     * @param nodeId 触达节点 ID
     * @param channel 触达渠道
     * @param provider 实际渠道供应商
     * @param templateId 模板 ID
     * @param content 内容字段
     * @param variables 模板变量
     * @param idempotencyKey 触达幂等键
     * @param policyOptions 触达前策略选项
     * @return 标准化触达请求
     */
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

    /**
     * 构造不显式传入租户的标准化触达请求。
     *
     * <p>该重载使用默认租户语义，并复用完整重载构造 payload；不会写数据库或调用外部渠道，只返回可交给
     * {@link #send(DeliveryRequest)} 或 outbox 的请求对象。
     *
     * @param executionId 画布执行实例 ID
     * @param canvasId 画布 ID
     * @param userId 接收用户 ID
     * @param nodeId 触达节点 ID
     * @param channel 触达渠道
     * @param templateId 模板 ID
     * @param content 模板内容或已渲染内容
     * @param variables 渲染变量和上下文变量
     * @param idempotencyKey 触达幂等键
     * @return 标准化触达请求
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("触达请求序列化失败", e);
        }
    }

    /**
     * 从 outbox 记录读取标准化 payload。
     *
     * @param outbox 触达 outbox 记录
     * @return 解析后的 payload Map
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalArgumentException("delivery payload JSON is invalid", ex);
        }
    }

    /**
     * 规范化渠道供应商。
     *
     * @param provider 原始供应商
     * @return 大写供应商，缺失时返回 REACH
     */
    private String normalizeProvider(String provider) {
        return provider == null || provider.isBlank() ? "REACH" : provider.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 发送前记录准备结果，标识本次是否命中幂等重复请求。
     *
     * @param record 已落库或已命中的发送记录.
     * @param duplicate 是否命中幂等重复请求.
     */
    private record PreparedRecord(
        MessageSendRecordDO record,
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
        /**
         * 返回触达前策略的默认选项。
         *
         * <p>默认要求显式同意、启用夜间静默和旅程级频控；用于发送节点未配置策略时保持保守触达语义。
         *
         * @return 默认策略选项
         */
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
