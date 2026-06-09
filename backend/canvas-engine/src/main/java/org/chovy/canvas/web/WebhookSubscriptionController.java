package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.WebhookDeliveryLogDO;
import org.chovy.canvas.dal.dataobject.WebhookSubscriptionDO;
import org.chovy.canvas.dal.mapper.WebhookDeliveryLogMapper;
import org.chovy.canvas.dal.mapper.WebhookSubscriptionMapper;
import org.chovy.canvas.domain.cdp.WebhookDispatcherService;
import org.chovy.canvas.domain.cdp.WebhookSubscriptionValidator;
import org.chovy.canvas.dto.webhook.WebhookDeliveryDTO;
import org.chovy.canvas.dto.webhook.WebhookRotateSecretResp;
import org.chovy.canvas.dto.webhook.WebhookSubscriptionDTO;
import org.chovy.canvas.dto.webhook.WebhookSubscriptionReq;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WebhookSubscriptionController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/cdp/webhooks")
@RequiredArgsConstructor
public class WebhookSubscriptionController {
    private static final int SECRET_BYTES = 24;
    private static final int SECRET_PREFIX_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final TenantContextResolver tenantContextResolver;
    private final WebhookSubscriptionMapper subscriptionMapper;
    private final WebhookDeliveryLogMapper deliveryLogMapper;
    private final WebhookSubscriptionValidator validator;
    private final WebhookDispatcherService dispatcher;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecretCipher secretCipher;
    /**
     * 查询当前租户的 Webhook 订阅配置。
     *
     * <p>接口只返回当前租户下的订阅记录，并按最近创建顺序展示。返回 DTO 会隐藏完整签名密钥，
     * 仅暴露密钥前缀等可识别信息，避免管理页泄露回调验签凭据。</p>
     *
     * @return 异步返回当前租户的 Webhook 订阅列表。
     */
    @GetMapping
    public Mono<R<List<WebhookSubscriptionDTO>>> list() {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tenantContextResolver.currentOrError()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(subscriptionMapper.selectList(
                                        new LambdaQueryWrapper<WebhookSubscriptionDO>()
                                                .eq(WebhookSubscriptionDO::getTenantId, tenantId(ctx))
                                                .orderByDesc(WebhookSubscriptionDO::getId))
                                .stream()
                                .map(this::toDto)
                                .toList()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建当前租户的 Webhook 订阅。
     *
     * <p>创建前会校验回调地址和事件类型，服务端生成新的签名密钥并保存哈希/密文，
     * 订阅默认进入 ACTIVE 状态。租户 ID 与创建人来自当前登录上下文，不接受客户端覆盖。</p>
     *
     * @param req Webhook 订阅请求，包含名称、回调地址、事件类型和最大重试次数。
     * @return 异步返回已创建的订阅配置，不包含完整明文密钥。
     */
    @PostMapping
    public Mono<R<WebhookSubscriptionDTO>> create(@RequestBody WebhookSubscriptionReq req) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    validator.validate(req.callbackUrl(), req.eventTypes());
                    WebhookSubscriptionDO row = new WebhookSubscriptionDO();
                    row.setTenantId(tenantId(ctx));
                    row.setName(requireText(req.name(), "name"));
                    row.setCallbackUrl(req.callbackUrl().trim());
                    applyNewSecret(row);
                    row.setEventTypes(writeJson(normalizeEventTypes(req.eventTypes())));
                    row.setStatus(WebhookSubscriptionDO.ACTIVE);
                    row.setMaxAttempts(normalizeMaxAttempts(req.maxAttempts()));
                    row.setCreatedBy(ctx.username());
                    subscriptionMapper.insert(row);
                    return R.ok(toDto(row));
                }).subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 更新当前租户下的 Webhook 订阅配置。
     *
     * <p>接口会先按租户加载订阅，找不到或不属于当前租户时返回 404。
     * 更新只修改名称、回调地址、事件类型和重试次数，不会自动轮换签名密钥。</p>
     *
     * @param id Webhook 订阅 ID。
     * @param req 新的订阅配置。
     * @return 异步返回更新后的订阅配置。
     */
    @PutMapping("/{id}")
    public Mono<R<WebhookSubscriptionDTO>> update(@PathVariable Long id, @RequestBody WebhookSubscriptionReq req) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    validator.validate(req.callbackUrl(), req.eventTypes());
                    WebhookSubscriptionDO row = requireTenantRow(ctx, id);
                    row.setName(requireText(req.name(), "name"));
                    row.setCallbackUrl(req.callbackUrl().trim());
                    row.setEventTypes(writeJson(normalizeEventTypes(req.eventTypes())));
                    row.setMaxAttempts(normalizeMaxAttempts(req.maxAttempts()));
                    subscriptionMapper.updateById(row);
                    return R.ok(toDto(row));
                }).subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 暂停当前租户下的 Webhook 订阅。
     *
     * <p>暂停后调度器不再向该订阅投递事件，但保留配置和历史投递记录，便于后续恢复或排查。
     * 状态变更前会校验订阅归属，防止跨租户操作。</p>
     *
     * @param id Webhook 订阅 ID。
     * @return 异步返回空响应，表示状态已切换为 PAUSED。
     */
    @PutMapping("/{id}/pause")
    public Mono<R<Void>> pause(@PathVariable Long id) {
        return updateStatus(id, WebhookSubscriptionDO.PAUSED);
    }
    /**
     * 恢复当前租户下已暂停的 Webhook 订阅。
     *
     * <p>恢复只修改订阅状态为 ACTIVE，不会补偿暂停期间未投递的历史事件。
     * 调用方需要通过投递日志确认恢复后的新事件是否正常送达。</p>
     *
     * @param id Webhook 订阅 ID。
     * @return 异步返回空响应，表示状态已切换为 ACTIVE。
     */
    @PutMapping("/{id}/resume")
    public Mono<R<Void>> resume(@PathVariable Long id) {
        return updateStatus(id, WebhookSubscriptionDO.ACTIVE);
    }
    /**
     * 停用当前租户下的 Webhook 订阅。
     *
     * <p>接口采用软停用语义，将订阅状态置为 DISABLED，而不是删除记录。
     * 这样可以保留密钥前缀、投递日志和审计线索，便于后续安全追溯。</p>
     *
     * @param id Webhook 订阅 ID。
     * @return 异步返回空响应，表示订阅已停用。
     */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return updateStatus(id, WebhookSubscriptionDO.DISABLED);
    }
    /**
     * 为当前租户的 Webhook 订阅轮换签名密钥。
     *
     * <p>接口会生成新的随机密钥，更新哈希值、密文和可展示前缀，并且只在本次响应中返回明文密钥。
     * 客户端需要立即保存并同步到接收端，后续列表和详情接口不会再次返回明文。</p>
     *
     * @param id Webhook 订阅 ID。
     * @return 异步返回新密钥明文和密钥前缀。
     */
    @PostMapping("/{id}/rotate-secret")
    public Mono<R<WebhookRotateSecretResp>> rotateSecret(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    WebhookSubscriptionDO row = requireTenantRow(ctx, id);
                    String rawSecret = applyNewSecret(row);
                    subscriptionMapper.updateById(row);
                    return R.ok(new WebhookRotateSecretResp(row.getId(), rawSecret, row.getSecretPrefix()));
                }).subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 向当前租户的 Webhook 订阅发送测试事件。
     *
     * <p>测试事件使用 {@code webhook.test} 类型和随机幂等键，只用于验证目标地址、签名密钥和重试配置。
     * 该接口会真实调用投递服务并产生投递日志，但不会代表任何业务事件。</p>
     *
     * @param id Webhook 订阅 ID。
     * @return 异步返回空响应，表示测试事件已交给投递服务。
     */
    @PostMapping("/{id}/test")
    public Mono<R<Void>> testDelivery(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    WebhookSubscriptionDO row = requireTenantRow(ctx, id);
                    dispatcher.sendOnce(
                            row,
                            "webhook.test",
                            Map.of("subscriptionId", row.getId(), "test", true),
                            UUID.randomUUID().toString(),
                            1);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户某个 Webhook 订阅的最近投递日志。
     *
     * <p>接口先校验订阅归属，再按租户和订阅 ID 读取最近 100 条投递记录。
     * 返回结果用于排查回调失败、重试次数、响应码和签名验证问题，不会触发重放。</p>
     *
     * @param id Webhook 订阅 ID。
     * @return 异步返回最近投递日志列表。
     */
    @GetMapping("/{id}/deliveries")
    public Mono<R<List<WebhookDeliveryDTO>>> deliveries(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    WebhookSubscriptionDO row = requireTenantRow(ctx, id);
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    return R.ok(deliveryLogMapper.selectList(new LambdaQueryWrapper<WebhookDeliveryLogDO>()
                                    .eq(WebhookDeliveryLogDO::getTenantId, tenantId(ctx))
                                    .eq(WebhookDeliveryLogDO::getSubscriptionId, row.getId())
                                    .orderByDesc(WebhookDeliveryLogDO::getCreatedAt)
                                    .last("LIMIT 100"))
                            .stream()
                            .map(this::toDeliveryDto)
                            .toList());
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回流程执行后的业务结果。
     */
    private Mono<R<Void>> updateStatus(Long id, String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    WebhookSubscriptionDO row = requireTenantRow(ctx, id);
                    row.setStatus(status);
                    subscriptionMapper.updateById(row);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param ctx ctx 参数，用于 requireTenantRow 流程中的校验、计算或对象转换。
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 requireTenantRow 流程生成的业务结果。
     */
    private WebhookSubscriptionDO requireTenantRow(TenantContext ctx, Long id) {
        WebhookSubscriptionDO row = subscriptionMapper.selectById(id);
        Long normalizedTenantId = tenantId(ctx);
        if (row == null || !normalizedTenantId.equals(row.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook subscription not found");
        }
        return row;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private WebhookSubscriptionDTO toDto(WebhookSubscriptionDO row) {
        return new WebhookSubscriptionDTO(
                row.getId(),
                row.getName(),
                row.getCallbackUrl(),
                row.getSecretPrefix(),
                readEventTypes(row.getEventTypes()),
                row.getStatus(),
                row.getMaxAttempts(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private WebhookDeliveryDTO toDeliveryDto(WebhookDeliveryLogDO row) {
        return new WebhookDeliveryDTO(
                row.getId(),
                row.getDeliveryId(),
                row.getEventType(),
                row.getAttempt(),
                row.getHttpStatus(),
                row.getStatus(),
                row.getNextRetryAt(),
                row.getErrorMessage(),
                row.getTerminalReason(),
                row.getCreatedAt());
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 apply new secret 生成的文本或业务键。
     */
    private String applyNewSecret(WebhookSubscriptionDO row) {
        String rawSecret = generateSecret();
        row.setSecretPrefix(rawSecret.substring(0, Math.min(rawSecret.length(), SECRET_PREFIX_LENGTH)));
        row.setSecretHash(passwordEncoder.encode(rawSecret));
        row.setSecretCiphertext(secretCipher.encrypt(rawSecret));
        return rawSecret;
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @return 返回 generate secret 生成的文本或业务键。
     */
    private String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return "whsec_" + HexFormat.of().formatHex(bytes);
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param eventTypes event types 参数，用于 writeJson 流程中的校验、计算或对象转换。
     * @return 返回 write json 生成的文本或业务键。
     */
    private String writeJson(List<String> eventTypes) {
        try {
            return objectMapper.writeValueAsString(eventTypes);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("eventTypes is not JSON serializable", e);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 read event types 汇总后的集合、分页或映射视图。
     */
    private List<String> readEventTypes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 规范化输入值。
     *
     * @param eventTypes event types 参数，用于 normalizeEventTypes 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> normalizeEventTypes(List<String> eventTypes) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return eventTypes == null ? List.of() : eventTypes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Integer normalizeMaxAttempts(Integer value) {
        return value == null || value <= 0 ? 3 : value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param ctx ctx 参数，用于 tenantId 流程中的校验、计算或对象转换。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }
}
