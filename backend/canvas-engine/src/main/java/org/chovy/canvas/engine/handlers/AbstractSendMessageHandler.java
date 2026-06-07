package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.domain.content.MarketingContentReleaseService;
import org.chovy.canvas.engine.channel.ChannelConnector;
import org.chovy.canvas.engine.channel.ChannelDedupeService;
import org.chovy.canvas.engine.channel.ChannelFallbackService;
import org.chovy.canvas.engine.channel.ChannelConnectorRegistry;
import org.chovy.canvas.engine.channel.ProviderBackpressureService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发送类节点处理器抽象基类。
 *
 * <p>封装短信、邮件、Push、站内信、微信等触达节点共同的参数解析、策略校验和发送记录写入流程。
 * <p>子类只需要声明具体渠道类型和渠道特有字段，跨节点路由、重试和状态持久化仍由执行引擎统一管理。
 */
abstract class AbstractSendMessageHandler implements NodeHandler {

    /** 触达发送服务，负责统一落库和调用具体渠道。 */
    private final ReachDeliveryService deliveryService;
    /** 渠道连接器注册表；为空时保持旧版直发路径，便于存量测试和渐进接入。 */
    private final ChannelConnectorRegistry connectorRegistry;
    private final ProviderBackpressureService backpressureService;
    private final ChannelFallbackService fallbackService;
    private final ChannelDedupeService dedupeService;
    private final MarketingContentReleaseService contentReleaseService;

    /**
     * 构造 AbstractSendMessageHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param deliveryService deliveryService 方法执行所需的业务参数
     */
    AbstractSendMessageHandler(ReachDeliveryService deliveryService) {
        this(deliveryService, (ChannelConnectorRegistry) null);
    }

    AbstractSendMessageHandler(ReachDeliveryService deliveryService, ChannelConnectorRegistry connectorRegistry) {
        this(deliveryService, connectorRegistry, null, null, null);
    }

    AbstractSendMessageHandler(ReachDeliveryService deliveryService,
                               MarketingContentReleaseService contentReleaseService) {
        this(deliveryService, null, null, null, null, contentReleaseService);
    }

    AbstractSendMessageHandler(ReachDeliveryService deliveryService,
                               ChannelConnectorRegistry connectorRegistry,
                               ProviderBackpressureService backpressureService,
                               ChannelFallbackService fallbackService,
                               ChannelDedupeService dedupeService) {
        this(deliveryService, connectorRegistry, backpressureService, fallbackService, dedupeService, null);
    }

    AbstractSendMessageHandler(ReachDeliveryService deliveryService,
                               ChannelConnectorRegistry connectorRegistry,
                               ProviderBackpressureService backpressureService,
                               ChannelFallbackService fallbackService,
                               ChannelDedupeService dedupeService,
                               MarketingContentReleaseService contentReleaseService) {
        this.deliveryService = deliveryService;
        this.connectorRegistry = connectorRegistry;
        this.backpressureService = backpressureService;
        this.fallbackService = fallbackService;
        this.dedupeService = dedupeService;
        this.contentReleaseService = contentReleaseService;
    }

    /**
     * 执行 channel 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 转换或查询得到的字符串结果
     */
    protected abstract String channel();

    protected String channel(Map<String, Object> config) {
        return channel();
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String channel = channel(config);
        String provider = normalizeProvider(string(config, "provider", "DEFAULT"));
        String nodeId = string(config, "__nodeId", channel.toLowerCase() + "-send");
        String templateId = string(config, "templateId", string(config, "template_id", null));
        String successNodeId = string(config, "successNodeId", string(config, "nextNodeId", null));
        String failNodeId = string(config, "failNodeId", null);
        String idempotencyKey = string(config, "idempotencyKey",
                ctx.getExecutionId() + ":" + nodeId + ":" + channel);
        Map<String, Object> content = content(config);
        Map<String, Object> variables = variables(config, ctx);
        MarketingContentReleaseService.ResolvedRelease contentRelease = null;
        String contentReleaseKey = string(config, "contentReleaseKey", string(config, "releaseKey", null));
        if (contentReleaseKey != null && !contentReleaseKey.isBlank()) {
            if (contentReleaseService == null) {
                return Mono.just(NodeResult.fail("content release resolver unavailable: " + contentReleaseKey));
            }
            try {
                contentRelease = contentReleaseService.resolve(
                        new TenantContext(ctx.getTenantId(), null, ctx.getUserId()),
                        contentReleaseKey.trim(),
                        variables);
                content = mergeContentRelease(content, contentRelease);
                if (templateId == null || templateId.isBlank()) {
                    templateId = contentRelease.releaseKey();
                }
            } catch (RuntimeException ex) {
                String message = ex.getMessage() == null ? "content release resolve failed" : ex.getMessage();
                return Mono.just(NodeResult.fail("content release resolve failed: " + message));
            }
        }
        Map<String, Object> contentReleaseOutput = contentReleaseOutput(contentRelease);
        Map<String, Object> connectorPayload = connectorPayload(channel, templateId, ctx.getUserId(), content, variables, idempotencyKey);
        ReachDeliveryService.PolicyOptions policy = new ReachDeliveryService.PolicyOptions(
                bool(config, "requireExplicitConsent", true),
                string(config, "quietStart", "22:00"),
                string(config, "quietEnd", "08:00"),
                string(config, "quietTimezone", "USER_LOCAL"),
                string(config, "frequencyScope", "JOURNEY"),
                integer(config, "frequencyMax", 1),
                integer(config, "frequencyWindowSeconds", 86400));

        if (connectorRegistry != null) {
            ChannelConnector connector = connectorRegistry.resolve(ctx.getTenantId(), channel, provider);
            ChannelConnector.ConnectorMode mode = connector.mode();
            if (mode == ChannelConnector.ConnectorMode.DISABLED) {
                String reason = connector.health().message();
                return Mono.just(NodeResult.fail("connector disabled: " + reason,
                        connectorOutput(mode, provider, null, "DISABLED", reason)));
            }

            String dedupeGroup = string(config, "dedupeGroup", null);
            if (dedupeService != null && dedupeGroup != null && !dedupeGroup.isBlank()) {
                ChannelDedupeService.Decision decision = dedupeService.reservePayload(
                        ctx.getTenantId(),
                        dedupeGroup,
                        channel,
                        ctx.getUserId(),
                        templateId,
                        connectorPayload,
                        Duration.ofSeconds(integer(config, "dedupeWindowSeconds", 86_400)));
                if ("DUPLICATE".equals(decision.status())) {
                    Map<String, Object> output = connectorOutput(mode, provider, null, "DUPLICATE", "duplicate channel content");
                    output.put("dedupeGroup", dedupeGroup);
                    output.put("dedupeHash", decision.contentHash());
                    return Mono.just(NodeResult.suppressed(
                            string(config, "dedupeNodeId", string(config, "skipNodeId", null)),
                            "CHANNEL_DEDUPE",
                            "duplicate channel content").withOutput(output));
                }
            }

            Map<String, Object> fallbackOutput = new LinkedHashMap<>();
            if (backpressureService != null) {
                ProviderBackpressureService.Decision decision = backpressureService.decide(
                        ctx.getTenantId(), channel, provider, "SEND", mode == ChannelConnector.ConnectorMode.SANDBOX);
                if (!"ALLOWED".equals(decision.status())) {
                    ChannelFallbackService.FallbackDecision fallback = fallbackService == null
                            ? null
                            : fallbackService.resolve(ctx.getTenantId(), ctx.getExecutionId(), nodeId, channel, provider);
                    if (fallback == null || sameRoute(channel, provider, fallback.finalChannel(), fallback.finalProvider())) {
                        Map<String, Object> output = connectorOutput(mode, provider, null, decision.status(), decision.reason());
                        return Mono.just(NodeResult.fail("provider blocked: " + decision.reason(), output));
                    }
                    fallbackOutput.put("fallbackOriginalChannel", fallback.originalChannel());
                    fallbackOutput.put("fallbackOriginalProvider", fallback.originalProvider());
                    fallbackOutput.put("fallbackFinalChannel", fallback.finalChannel());
                    fallbackOutput.put("fallbackFinalProvider", fallback.finalProvider());
                    fallbackOutput.put("fallbackReason", fallback.reason());
                    channel = fallback.finalChannel();
                    provider = fallback.finalProvider();
                    connectorPayload = connectorPayload(channel, templateId, ctx.getUserId(), content, variables, idempotencyKey);
                    connector = connectorRegistry.resolve(ctx.getTenantId(), channel, provider);
                    mode = connector.mode();
                    if (mode == ChannelConnector.ConnectorMode.DISABLED) {
                        String reason = connector.health().message();
                        Map<String, Object> output = connectorOutput(mode, provider, null, "DISABLED", reason);
                        output.putAll(fallbackOutput);
                        return Mono.just(NodeResult.fail("connector disabled: " + reason, output));
                    }
                }
            }

            if (mode == ChannelConnector.ConnectorMode.SANDBOX) {
                ChannelConnector.ConnectorSendResult result = connector.send(new ChannelConnector.ConnectorSendRequest(
                        ctx.getTenantId(),
                        channel,
                        provider,
                        ctx.getUserId(),
                        connectorPayload));
                Map<String, Object> output = connectorOutput(mode, provider,
                        result.externalMessageId(), result.status(), result.reason());
                output.putAll(fallbackOutput);
                output.putAll(contentReleaseOutput);
                if (result.accepted()) {
                    return Mono.just(NodeResult.routed("success", successNodeId, output));
                }
                if (failNodeId != null && !failNodeId.isBlank()) {
                    return Mono.just(NodeResult.routed("fail", failNodeId, output));
                }
                return Mono.just(NodeResult.fail("connector sandbox send failed: " + result.reason(), output));
            }
        }

        // 发送请求里固定带执行、画布、用户、节点和渠道信息，便于触达记录做幂等与审计。
        ReachDeliveryService.DeliveryRequest request = deliveryService.request(
                ctx.getTenantId(),
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                ctx.getUserId(),
                nodeId,
                channel,
                provider,
                templateId,
                content,
                variables,
                idempotencyKey,
                policy
        );

        String deliveryProvider = provider;
        return deliveryService.send(request)
                .map(result -> {
                    Map<String, Object> output = output(result);
                    output.putAll(contentReleaseOutput);
                    if (connectorRegistry != null) {
                        output.put("connectorMode", ChannelConnector.ConnectorMode.REAL.name());
                        output.put("connectorProvider", deliveryProvider);
                    }
                    if (result.sent()) {
                        return NodeResult.routed("success", successNodeId, output);
                    }
                    if (failNodeId != null && !failNodeId.isBlank()) {
                        // 配置了失败分支时，将发送失败作为可路由业务结果，而不是直接中断旅程。
                        return NodeResult.routed("fail", failNodeId, output);
                    }
                    return NodeResult.fail("触达发送失败: " + result.errorMessage());
                });
    }

    /**
     * 判断 is Reach Node 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    @Override
    public boolean isReachNode() {
        return true;
    }

    @Override
    public boolean requiresSideEffectIdempotency(Map<String, Object> config, ExecutionContext ctx) {
        return true;
    }

    @Override
    public String sideEffectOperationKey(Map<String, Object> config, ExecutionContext ctx) {
        String explicit = string(config, MapFieldKeys.IDEMPOTENCY_KEY, null);
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        String templateId = string(config, "templateId", string(config, "template_id",
                string(config, "contentReleaseKey", string(config, "releaseKey", ""))));
        return ctx.getUserId() + ":reach:" + channel(config) + ":" + templateId;
    }

    @Override
    public NodeResult completedSideEffectResult(Map<String, Object> config,
                                                ExecutionContext ctx,
                                                Map<String, Object> cachedOutput) {
        String successNodeId = string(config, "successNodeId", string(config, "nextNodeId", null));
        return NodeResult.routed("success", successNodeId, cachedOutput);
    }

    /**
     * 执行 content 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> content(Map<String, Object> config) {
        Map<String, Object> content = new LinkedHashMap<>();
        copy(config, content, "subject");
        copy(config, content, "previewText");
        copy(config, content, "title");
        copy(config, content, "body");
        copy(config, content, "content");
        copy(config, content, "imageUrl");
        copy(config, content, "clickUrl");
        copy(config, content, "fromName");
        copy(config, content, "fromEmail");
        return content;
    }

    private Map<String, Object> mergeContentRelease(Map<String, Object> content,
                                                    MarketingContentReleaseService.ResolvedRelease release) {
        Map<String, Object> merged = new LinkedHashMap<>(content);
        if (release.renderedSubject() != null) {
            merged.put("subject", release.renderedSubject());
        }
        if (release.renderedBody() != null) {
            merged.put("body", release.renderedBody());
        }
        merged.put("contentReleaseKey", release.releaseKey());
        merged.put("contentReleaseVersion", release.sourceVersion());
        merged.put("contentReleaseStatus", release.status());
        merged.put("contentReleaseSourceType", release.sourceType());
        merged.put("contentReleaseSourceKey", release.sourceKey());
        merged.put("contentSnapshotJson", release.snapshotJson());
        merged.put("assets", releaseAssets(release.assets()));
        return merged;
    }

    private List<Map<String, Object>> releaseAssets(List<MarketingContentReleaseService.ResolvedAsset> assets) {
        return assets.stream()
                .map(asset -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("assetKey", asset.assetKey());
                    item.put("status", asset.status());
                    item.put("snapshotJson", asset.snapshotJson());
                    return item;
                })
                .toList();
    }

    private Map<String, Object> contentReleaseOutput(MarketingContentReleaseService.ResolvedRelease release) {
        if (release == null) {
            return Map.of();
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("contentReleaseKey", release.releaseKey());
        output.put("contentReleaseVersion", release.sourceVersion());
        output.put("contentReleaseStatus", release.status());
        return output;
    }

    private Map<String, Object> connectorPayload(String channel,
                                                 String templateId,
                                                 String userId,
                                                 Map<String, Object> content,
                                                 Map<String, Object> variables,
                                                 String idempotencyKey) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(MapFieldKeys.CHANNEL, channel);
        payload.put(MapFieldKeys.TEMPLATE_ID, templateId);
        payload.put(MapFieldKeys.USER_ID, userId);
        payload.put(MapFieldKeys.CONTENT, content == null ? Map.of() : content);
        payload.put(MapFieldKeys.VARIABLES, variables == null ? Map.of() : variables);
        payload.put(MapFieldKeys.IDEMPOTENCY_KEY, idempotencyKey);
        return payload;
    }

    private Map<String, Object> connectorOutput(ChannelConnector.ConnectorMode mode,
                                                String provider,
                                                String externalMessageId,
                                                String status,
                                                String reason) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("connectorMode", mode.name());
        output.put("connectorProvider", provider);
        if (status != null) {
            output.put("connectorStatus", status);
        }
        if (externalMessageId != null) {
            output.put("externalMessageId", externalMessageId);
        }
        if (reason != null) {
            output.put("connectorReason", reason);
            output.put("errorMessage", reason);
        }
        return output;
    }

    private boolean sameRoute(String channel, String provider, String nextChannel, String nextProvider) {
        return normalizeProvider(provider).equals(normalizeProvider(nextProvider))
                && normalizeChannel(channel).equals(normalizeChannel(nextChannel));
    }

    /**
     * 执行 variables 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 按业务键组织的映射结果
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> variables(Map<String, Object> config, ExecutionContext ctx) {
        Object raw = config.getOrDefault("variables", config.get("variablesMapping"));
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        ((Map<Object, Object>) map).forEach((key, value) -> {
            if (key == null) return;
            // 变量值支持 $field / $.field 从上下文取值，用于模板个性化渲染。
            variables.put(key.toString(), resolve(value, ctx));
        });
        return variables;
    }

    /**
     * 构建、解析或转换 resolve 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    private Object resolve(Object value, ExecutionContext ctx) {
        if (value instanceof String text && text.startsWith("$")) {
            String field = text.startsWith("$.") ? text.substring(2) : text.substring(1);
            Object contextValue = ctx.getContextValue(field);
            return contextValue == null ? value : contextValue;
        }
        return value;
    }

    /**
     * 执行 output 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param result result 方法执行所需的业务参数
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> output(ReachDeliveryService.DeliveryResult result) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("sendRecordId", result.recordId());
        output.put("sendStatus", result.sent()
                ? (result.externalMessageId() == null ? "SUBMITTED" : "SENT")
                : "FAILED");
        output.put("duplicate", result.duplicate());
        if (result.externalMessageId() != null) {
            output.put("externalMessageId", result.externalMessageId());
        }
        if (result.errorMessage() != null) {
            output.put("errorMessage", result.errorMessage());
        }
        return output;
    }

    /**
     * 执行 copy 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param source source 方法执行所需的业务参数
     * @param target target 方法执行所需的业务参数
     * @param key key 对应的缓存键、配置键或业务键
     */
    private void copy(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    /**
     * 执行 string 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param key key 对应的缓存键、配置键或业务键
     * @param fallback fallback 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }

    private String normalizeProvider(String provider) {
        return provider == null || provider.isBlank() ? "DEFAULT" : provider.trim().toUpperCase();
    }

    private String normalizeChannel(String channel) {
        return channel == null || channel.isBlank() ? "UNKNOWN" : channel.trim().toUpperCase();
    }

    private boolean bool(Map<String, Object> config, String key, boolean fallback) {
        Object value = config.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private int integer(Map<String, Object> config, String key, int fallback) {
        Object value = config.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
