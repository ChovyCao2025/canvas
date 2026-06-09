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
     * 创建仅使用旧版触达服务的发送处理器。
     *
     * @param deliveryService 触达发送服务
     */
    AbstractSendMessageHandler(ReachDeliveryService deliveryService) {
        this(deliveryService, (ChannelConnectorRegistry) null);
    }

    /**
     * 创建带渠道连接器注册表的发送处理器。
     *
     * @param deliveryService 触达发送服务
     * @param connectorRegistry 渠道连接器注册表
     */
    AbstractSendMessageHandler(ReachDeliveryService deliveryService, ChannelConnectorRegistry connectorRegistry) {
        this(deliveryService, connectorRegistry, null, null, null);
    }

    /**
     * 创建带内容发布解析服务的发送处理器。
     *
     * @param deliveryService 触达发送服务
     * @param contentReleaseService 内容发布解析服务
     */
    AbstractSendMessageHandler(ReachDeliveryService deliveryService,
                               MarketingContentReleaseService contentReleaseService) {
        this(deliveryService, null, null, null, null, contentReleaseService);
    }

    /**
     * 创建带渠道控制能力的发送处理器。
     *
     * @param deliveryService 触达发送服务
     * @param connectorRegistry 渠道连接器注册表
     * @param backpressureService 供应商背压服务
     * @param fallbackService 渠道降级服务
     * @param dedupeService 渠道去重服务
     */
    AbstractSendMessageHandler(ReachDeliveryService deliveryService,
                               ChannelConnectorRegistry connectorRegistry,
                               ProviderBackpressureService backpressureService,
                               ChannelFallbackService fallbackService,
                               ChannelDedupeService dedupeService) {
        this(deliveryService, connectorRegistry, backpressureService, fallbackService, dedupeService, null);
    }

    /**
     * 创建完整依赖的发送处理器。
     *
     * @param deliveryService 触达发送服务
     * @param connectorRegistry 渠道连接器注册表
     * @param backpressureService 供应商背压服务
     * @param fallbackService 渠道降级服务
     * @param dedupeService 渠道去重服务
     * @param contentReleaseService 内容发布解析服务
     */
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
     * channel 处理 engine.handlers 场景的业务逻辑。
     * @return 返回 channel 生成的文本或业务键。
     */
    protected abstract String channel();

    /**
     * 根据节点配置解析本次触达渠道。
     *
     * <p>默认返回子类固定渠道；子类可覆盖以支持配置化渠道。返回值会参与连接器选择、渠道去重、供应商限流、触达请求
     * payload 和成功/失败路由输出。
     *
     * @param config 当前触达节点配置，可包含渠道覆写参数
     * @return 标准化前的渠道标识
     */
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
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
     * isReachNode 校验或转换 engine.handlers 场景的数据。
     * @return 返回布尔判断结果。
     */
    @Override
    public boolean isReachNode() {
        return true;
    }

    /**
     * 声明发送类节点会产生触达副作用，需要调度层进行节点级幂等保护。
     *
     * <p>触达可能写发送流水、outbox、渠道去重记录，并调用外部供应商；返回 {@code true} 后重复执行会优先使用
     * {@link #completedSideEffectResult(Map, ExecutionContext, Map)} 恢复已完成输出。
     *
     * @param config 当前触达节点配置，包含模板、渠道、供应商和幂等键
     * @param ctx 画布执行上下文，提供租户、执行实例和用户信息
     * @return 始终为 {@code true}
     */
    @Override
    public boolean requiresSideEffectIdempotency(Map<String, Object> config, ExecutionContext ctx) {
        return true;
    }

    /**
     * 构造发送类节点的副作用操作键。
     *
     * <p>优先使用显式幂等键；未配置时按用户、渠道和模板生成稳定键。该键影响重复触达时是否跳过 outbox/外部渠道调用并
     * 复用上下文输出。
     *
     * @param config 当前触达节点配置，读取 {@code idempotencyKey}、渠道和模板
     * @param ctx 画布执行上下文，读取用户 ID
     * @return 用于节点副作用幂等表的业务操作键
     */
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

    /**
     * 将已完成的触达副作用缓存输出还原为节点结果。
     *
     * <p>调度层命中节点副作用幂等记录时调用本方法；不会重新写 outbox、Redis 或调用外部渠道，只按缓存输出恢复成功
     * 下一跳和上下文字段，保证重复执行的路由语义与首次发送一致。
     *
     * @param config 当前触达节点配置，用于读取成功下一跳
     * @param ctx 画布执行上下文，本方法不修改上下文
     * @param cachedOutput 首次执行完成时保存的节点输出
     * @return 路由到成功分支的节点结果
     */
    @Override
    public NodeResult completedSideEffectResult(Map<String, Object> config,
                                                ExecutionContext ctx,
                                                Map<String, Object> cachedOutput) {
        String successNodeId = string(config, "successNodeId", string(config, "nextNodeId", null));
        return NodeResult.routed("success", successNodeId, cachedOutput);
    }

    /**
     * 从节点配置中提取触达内容字段。
     *
     * @param config 节点配置
     * @return 触达内容 Map
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

    /**
     * 将内容发布版本渲染结果合并到触达内容中。
     *
     * @param content 原始内容
     * @param release 已解析的内容发布版本
     * @return 合并后的内容
     */
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

    /**
     * 转换内容发布版本关联素材为节点输出结构。
     *
     * @param assets 已解析素材列表
     * @return 可序列化的素材输出列表
     */
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

    /**
     * 构造内容发布版本相关节点输出。
     *
     * @param release 已解析内容发布版本
     * @return 内容发布版本输出字段
     */
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

    /**
     * 构造渠道连接器发送载荷。
     *
     * @param channel 渠道标识
     * @param templateId 模板 ID
     * @param userId 目标用户 ID
     * @param content 内容字段
     * @param variables 模板变量
     * @param idempotencyKey 幂等键
     * @return 连接器发送载荷
     */
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

    /**
     * 构造连接器调用结果输出。
     *
     * @param mode 连接器模式
     * @param provider 供应商
     * @param externalMessageId 外部消息 ID
     * @param status 连接器状态
     * @param reason 失败或跳过原因
     * @return 节点输出字段
     */
    private Map<String, Object> connectorOutput(ChannelConnector.ConnectorMode mode,
                                                String provider,
                                                String externalMessageId,
                                                String status,
                                                String reason) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("connectorMode", mode.name());
        output.put("connectorProvider", provider);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return output;
    }

    /**
     * 判断两条触达路由是否指向相同渠道和供应商。
     *
     * @param channel 当前渠道
     * @param provider 当前供应商
     * @param nextChannel 下一路由渠道
     * @param nextProvider 下一路由供应商
     * @return true 表示路由一致
     */
    private boolean sameRoute(String channel, String provider, String nextChannel, String nextProvider) {
        return normalizeProvider(provider).equals(normalizeProvider(nextProvider))
                /**
                 * 规范化输入值。
                 *
                 * @return 返回解析、归一化或安全处理后的值。
                 */
                && normalizeChannel(channel).equals(normalizeChannel(nextChannel));
    }

    /**
     * 解析节点配置中的模板变量映射。
     *
     * @param config 节点配置
     * @param ctx 执行上下文
     * @return 已从上下文取值后的变量映射
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
     * 构造触达执行结果输出。
     *
     * @param result 触达服务返回结果
     * @return 节点输出字段
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
     * 从源 Map 拷贝指定 key 到目标 Map。
     *
     * @param source 源 Map
     * @param target 目标 Map
     * @param key 字段 key
     */
    private void copy(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    /**
     * 读取字符串配置。
     *
     * @param config 节点配置
     * @param key 配置 key
     * @param fallback 默认值
     * @return 字符串配置值
     */
    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }

    /**
     * 规范化供应商标识。
     *
     * @param provider 原始供应商
     * @return 大写供应商，缺失时返回 DEFAULT
     */
    private String normalizeProvider(String provider) {
        return provider == null || provider.isBlank() ? "DEFAULT" : provider.trim().toUpperCase();
    }

    /**
     * 规范化渠道标识。
     *
     * @param channel 原始渠道
     * @return 大写渠道，缺失时返回 UNKNOWN
     */
    private String normalizeChannel(String channel) {
        return channel == null || channel.isBlank() ? "UNKNOWN" : channel.trim().toUpperCase();
    }

    /**
     * 读取布尔配置。
     *
     * @param config 节点配置
     * @param key 配置 key
     * @param fallback 默认值
     * @return 布尔配置值
     */
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

    /**
     * 读取整数配置。
     *
     * @param config 节点配置
     * @param key 配置 key
     * @param fallback 默认值
     * @return 整数配置值
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
