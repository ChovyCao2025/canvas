package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDeliveryDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertDeliveryMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.domain.cdp.WebhookRetryPolicy;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * MarketingMonitorAlertFanoutService 编排 domain.monitoring 场景的领域业务规则。
 */
@Slf4j
@Service
public class MarketingMonitorAlertFanoutService {

    private static final String EVENT_ALERT_OPENED = "marketing.monitor.alert.opened";
    private static final String STATUS_OPEN = "OPEN";
    private static final int SECRET_PREFIX_LENGTH = 12;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final MarketingMonitorAlertChannelMapper channelMapper;
    private final MarketingMonitorAlertDeliveryMapper deliveryMapper;
    private final MarketingMonitorAlertMapper alertMapper;
    private final WebhookRetryPolicy retryPolicy;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final SecretCipher secretCipher;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Clock clock;

    /**
     * 创建 MarketingMonitorAlertFanoutService 实例并注入 domain.monitoring 场景依赖。
     * @param channelMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param retryPolicy retry policy 参数，用于 MarketingMonitorAlertFanoutService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param webClientBuilder 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 MarketingMonitorAlertFanoutService 流程中的校验、计算或对象转换。
     * @param passwordEncoder password encoder 参数，用于 MarketingMonitorAlertFanoutService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public MarketingMonitorAlertFanoutService(MarketingMonitorAlertChannelMapper channelMapper,
                                              MarketingMonitorAlertDeliveryMapper deliveryMapper,
                                              MarketingMonitorAlertMapper alertMapper,
                                              WebhookRetryPolicy retryPolicy,
                                              ObjectMapper objectMapper,
                                              WebClient.Builder webClientBuilder,
                                              SecretCipher secretCipher,
                                              BCryptPasswordEncoder passwordEncoder) {
        this(channelMapper, deliveryMapper, alertMapper, retryPolicy, objectMapper, webClientBuilder,
                secretCipher, passwordEncoder, Clock.systemDefaultZone());
    }

    /**
     * 执行 MarketingMonitorAlertFanoutService 流程，围绕 marketing monitor alert fanout service 完成校验、计算或结果组装。
     *
     * @param channelMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param retryPolicy retry policy 参数，用于 MarketingMonitorAlertFanoutService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param webClientBuilder 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 MarketingMonitorAlertFanoutService 流程中的校验、计算或对象转换。
     * @param passwordEncoder password encoder 参数，用于 MarketingMonitorAlertFanoutService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingMonitorAlertFanoutService(MarketingMonitorAlertChannelMapper channelMapper,
                                       MarketingMonitorAlertDeliveryMapper deliveryMapper,
                                       MarketingMonitorAlertMapper alertMapper,
                                       WebhookRetryPolicy retryPolicy,
                                       ObjectMapper objectMapper,
                                       WebClient.Builder webClientBuilder,
                                       SecretCipher secretCipher,
                                       BCryptPasswordEncoder passwordEncoder,
                                       Clock clock) {
        this.channelMapper = channelMapper;
        this.deliveryMapper = deliveryMapper;
        this.alertMapper = alertMapper;
        this.retryPolicy = retryPolicy == null ? new WebhookRetryPolicy() : retryPolicy;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.webClientBuilder = webClientBuilder == null ? WebClient.builder() : webClientBuilder;
        this.secretCipher = secretCipher;
        this.passwordEncoder = passwordEncoder == null ? new BCryptPasswordEncoder() : passwordEncoder;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建或更新业务记录，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingMonitorAlertChannelView upsertChannel(Long tenantId,
                                                          MarketingMonitorAlertChannelCommand command,
                                                          String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("alert channel command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String channelKey = normalizeKey(command.channelKey(), "channelKey");
        String channelType = normalizeType(command.channelType(), "channelType");
        String endpointUrl = validateUrl(command.endpointUrl());
        String signingMode = normalizeSigningMode(command.signingMode());
        String rawSecret = trimToNull(command.secret());
        if (requiresSecret(signingMode) && rawSecret == null) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            MarketingMonitorAlertChannelDO existing = channelMapper.selectOne(
                    new LambdaQueryWrapper<MarketingMonitorAlertChannelDO>()
                            .eq(MarketingMonitorAlertChannelDO::getTenantId, scopedTenantId)
                            .eq(MarketingMonitorAlertChannelDO::getChannelKey, channelKey)
                            .last("LIMIT 1"));
            if (existing == null || isBlank(existing.getSecretCiphertext())) {
                throw new IllegalArgumentException("secret is required for signing mode " + signingMode);
            }
            return updateExisting(existing, command, actor, endpointUrl, channelType, signingMode, null);
        }
        MarketingMonitorAlertChannelDO row = channelMapper.selectOne(
                new LambdaQueryWrapper<MarketingMonitorAlertChannelDO>()
                        .eq(MarketingMonitorAlertChannelDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorAlertChannelDO::getChannelKey, channelKey)
                        .last("LIMIT 1"));
        if (row == null) {
            row = new MarketingMonitorAlertChannelDO();
            row.setTenantId(scopedTenantId);
            row.setChannelKey(channelKey);
            row.setCreatedBy(defaultActor(actor));
            row.setCreatedAt(now());
        }
        return updateExisting(row, command, actor, endpointUrl, channelType, signingMode, rawSecret);
    }

    /**
     * 执行业务操作 dispatchAlert，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param alertId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingMonitorAlertDispatchView dispatchAlert(Long tenantId, Long alertId, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (alertId == null) {
            throw new IllegalArgumentException("alertId is required");
        }
        MarketingMonitorAlertDO alert = alertMapper.selectById(alertId);
        if (alert == null || !scopedTenantId.equals(alert.getTenantId())) {
            throw new IllegalArgumentException("alert is not found");
        }
        return dispatchAlert(scopedTenantId, alert, actor);
    }

    /**
     * 执行业务操作 dispatchAlert，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param alert alert 参数，用于 dispatchAlert 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingMonitorAlertDispatchView dispatchAlert(Long tenantId,
                                                           MarketingMonitorAlertDO alert,
                                                           String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (alert == null || !scopedTenantId.equals(alert.getTenantId())) {
            throw new IllegalArgumentException("alert is not found");
        }
        if (!STATUS_OPEN.equals(normalizeOptionalUpper(alert.getStatus()))) {
            return dispatchView(scopedTenantId, alert.getId(), List.of());
        }
        List<MarketingMonitorAlertDeliveryDO> deliveries = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (MarketingMonitorAlertChannelDO channel : enabledChannels(scopedTenantId)) {
            if (!matches(channel, alert)) {
                continue;
            }
            deliveries.add(sendOnce(channel, alert, defaultActor(actor), 1));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return dispatchView(scopedTenantId, alert.getId(), deliveries);
    }

    /**
     * 执行业务操作 deliveries，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param alertId 目标业务记录 ID，需与租户边界匹配
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingMonitorAlertDeliveryView> deliveries(Long tenantId,
                                                              Long alertId,
                                                              String status,
                                                              int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int boundedLimit = boundedLimit(limit);
        String normalizedStatus = normalizeOptionalUpper(status);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(deliveryMapper.selectList(new LambdaQueryWrapper<MarketingMonitorAlertDeliveryDO>()
                        .eq(MarketingMonitorAlertDeliveryDO::getTenantId, scopedTenantId)
                        .eq(alertId != null, MarketingMonitorAlertDeliveryDO::getAlertId, alertId)
                        .eq(normalizedStatus != null, MarketingMonitorAlertDeliveryDO::getStatus, normalizedStatus)
                        .orderByDesc(MarketingMonitorAlertDeliveryDO::getCreatedAt)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .last("LIMIT " + boundedLimit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> alertId == null || alertId.equals(row.getAlertId()))
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .limit(boundedLimit)
                .map(this::toDeliveryView)
                .toList();
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param endpointUrl endpoint url 参数，用于 updateExisting 流程中的校验、计算或对象转换。
     * @param channelType 类型标识，用于选择对应处理分支。
     * @param signingMode signing mode 参数，用于 updateExisting 流程中的校验、计算或对象转换。
     * @param rawSecret raw secret 参数，用于 updateExisting 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private MarketingMonitorAlertChannelView updateExisting(MarketingMonitorAlertChannelDO row,
                                                            MarketingMonitorAlertChannelCommand command,
                                                            String actor,
                                                            String endpointUrl,
                                                            String channelType,
                                                            String signingMode,
                                                            String rawSecret) {
        LocalDateTime changedAt = now();
        row.setChannelType(channelType);
        row.setDisplayName(defaultString(command.displayName(), row.getChannelKey()));
        row.setEndpointUrl(endpointUrl);
        row.setEnabled(Boolean.FALSE.equals(command.enabled()) ? 0 : 1);
        row.setMinSeverity(normalizeSeverity(defaultString(command.minSeverity(), "LOW")));
        row.setAlertTypesJson(json(normalizedAlertTypes(command.alertTypes())));
        row.setSigningMode(signingMode);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (rawSecret != null) {
            row.setSecretPrefix(rawSecret.substring(0, Math.min(SECRET_PREFIX_LENGTH, rawSecret.length())));
            row.setSecretHash(passwordEncoder.encode(rawSecret));
            row.setSecretCiphertext(secretCipher.encrypt(rawSecret));
        }
        row.setMetadataJson(json(command.metadata()));
        row.setMaxAttempts(maxAttempts(command.maxAttempts()));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            channelMapper.insert(row);
        } else {
            channelMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toChannelView(row);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param channel channel 参数，用于 sendOnce 流程中的校验、计算或对象转换。
     * @param alert alert 参数，用于 sendOnce 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param attempt attempt 参数，用于 sendOnce 流程中的校验、计算或对象转换。
     * @return 返回 sendOnce 流程生成的业务结果。
     */
    private MarketingMonitorAlertDeliveryDO sendOnce(MarketingMonitorAlertChannelDO channel,
                                                     MarketingMonitorAlertDO alert,
                                                     String actor,
                                                     int attempt) {
        String deliveryId = UUID.randomUUID().toString();
        String timestamp = String.valueOf(clock.instant().getEpochSecond());
        String rawPayload = writeJson(payload(channel, alert, deliveryId, actor, timestamp));
        MarketingMonitorAlertDeliveryDO delivery = newDelivery(channel, alert, deliveryId, attempt, rawPayload, actor);
        try {
            WebClient.RequestBodySpec request = webClientBuilder.build()
                    .post()
                    .uri(channel.getEndpointUrl())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Canvas-Event", EVENT_ALERT_OPENED)
                    .header("X-Canvas-Delivery", deliveryId)
                    .header("X-Canvas-Timestamp", timestamp);
            String secret = secret(channel);
            if ("CANVAS_HMAC".equals(normalizeSigningMode(channel.getSigningMode())) && !isBlank(secret)) {
                request.header("X-Canvas-Signature", canvasSignature(secret, timestamp, rawPayload));
            }
            ResponseEntity<String> response = request.bodyValue(rawPayload)
                    .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
                    .block(Duration.ofSeconds(10));
            Integer status = response == null ? null : response.getStatusCode().value();
            delivery.setHttpStatus(status);
            delivery.setResponseBody(response == null ? null : trimLength(response.getBody(), 2000));
            applyDecision(delivery, retryPolicy.classify(status, false, attempt, maxAttempts(channel)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            delivery.setErrorMessage(trimLength(ex.getMessage(), 1000));
            applyDecision(delivery, retryPolicy.classify(null, true, attempt, maxAttempts(channel)));
            log.warn("[MONITORING] alert fanout failed channel={} alert={} error={}",
                    channel.getChannelKey(), alert.getId(), ex.getMessage());
        }
        deliveryMapper.insert(delivery);
        return delivery;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param channel channel 参数，用于 newDelivery 流程中的校验、计算或对象转换。
     * @param alert alert 参数，用于 newDelivery 流程中的校验、计算或对象转换。
     * @param deliveryId 业务对象 ID，用于定位具体记录。
     * @param attempt attempt 参数，用于 newDelivery 流程中的校验、计算或对象转换。
     * @param rawPayload raw payload 参数，用于 newDelivery 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 newDelivery 流程生成的业务结果。
     */
    private MarketingMonitorAlertDeliveryDO newDelivery(MarketingMonitorAlertChannelDO channel,
                                                        MarketingMonitorAlertDO alert,
                                                        String deliveryId,
                                                        int attempt,
                                                        String rawPayload,
                                                        String actor) {
        LocalDateTime createdAt = now();
        MarketingMonitorAlertDeliveryDO row = new MarketingMonitorAlertDeliveryDO();
        row.setTenantId(alert.getTenantId());
        row.setAlertId(alert.getId());
        row.setChannelId(channel.getId());
        row.setChannelKey(channel.getChannelKey());
        row.setChannelType(channel.getChannelType());
        row.setDeliveryId(deliveryId);
        row.setAttempt(attempt);
        row.setRequestPayload(rawPayload);
        row.setStatus(MarketingMonitorAlertDeliveryDO.RETRYING);
        row.setCreatedBy(actor);
        row.setCreatedAt(createdAt);
        row.setUpdatedAt(createdAt);
        return row;
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param delivery delivery 参数，用于 applyDecision 流程中的校验、计算或对象转换。
     * @param decision decision 参数，用于 applyDecision 流程中的校验、计算或对象转换。
     */
    private void applyDecision(MarketingMonitorAlertDeliveryDO delivery,
                               WebhookRetryPolicy.Decision decision) {
        delivery.setStatus(decision.status());
        delivery.setNextRetryAt(decision.nextRetryAt());
        delivery.setTerminalReason(decision.terminalReason());
        delivery.setUpdatedAt(now());
    }

    /**
     * 执行 payload 流程，围绕 payload 完成校验、计算或结果组装。
     *
     * @param channel channel 参数，用于 payload 流程中的校验、计算或对象转换。
     * @param alert alert 参数，用于 payload 流程中的校验、计算或对象转换。
     * @param deliveryId 业务对象 ID，用于定位具体记录。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 payload 流程生成的业务结果。
     */
    private Map<String, Object> payload(MarketingMonitorAlertChannelDO channel,
                                        MarketingMonitorAlertDO alert,
                                        String deliveryId,
                                        String actor,
                                        String timestamp) {
        return switch (normalizeChannelType(channel.getChannelType())) {
            case "SLACK" -> slackPayload(alert);
            case "FEISHU" -> feishuPayload(channel, alert, timestamp);
            case "TEAMS" -> teamsPayload(alert);
            default -> genericPayload(alert, deliveryId, actor);
        };
    }

    /**
     * 执行 genericPayload 流程，围绕 generic payload 完成校验、计算或结果组装。
     *
     * @param alert alert 参数，用于 genericPayload 流程中的校验、计算或对象转换。
     * @param deliveryId 业务对象 ID，用于定位具体记录。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 genericPayload 流程生成的业务结果。
     */
    private Map<String, Object> genericPayload(MarketingMonitorAlertDO alert,
                                               String deliveryId,
                                               String actor) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", EVENT_ALERT_OPENED);
        payload.put("deliveryId", deliveryId);
        payload.put("tenantId", alert.getTenantId());
        payload.put("actor", actor);
        payload.put("alert", alertPayload(alert));
        return payload;
    }

    /**
     * 执行 slackPayload 流程，围绕 slack payload 完成校验、计算或结果组装。
     *
     * @param alert alert 参数，用于 slackPayload 流程中的校验、计算或对象转换。
     * @return 返回 slackPayload 流程生成的业务结果。
     */
    private Map<String, Object> slackPayload(MarketingMonitorAlertDO alert) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", alertText(alert));
        payload.put("blocks", List.of(
                Map.of(
                        "type", "section",
                        "text", Map.of(
                                "type", "mrkdwn",
                                /**
                                 * 按默认值规则处理输入值。
                                 *
                                 * @return 返回 defaultString 流程生成的业务结果。
                                 */
                                "text", "*" + defaultString(alert.getTitle(), "Monitoring alert") + "*\n"
                                        /**
                                         * 执行 alertText 流程，围绕 alert text 完成校验、计算或结果组装。
                                         *
                                         * @return 返回 alertText 流程生成的业务结果。
                                         */
                                        + alertText(alert)))));
        return payload;
    }

    /**
     * 执行 feishuPayload 流程，围绕 feishu payload 完成校验、计算或结果组装。
     *
     * @param channel channel 参数，用于 feishuPayload 流程中的校验、计算或对象转换。
     * @param alert alert 参数，用于 feishuPayload 流程中的校验、计算或对象转换。
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 feishuPayload 流程生成的业务结果。
     */
    private Map<String, Object> feishuPayload(MarketingMonitorAlertChannelDO channel,
                                              MarketingMonitorAlertDO alert,
                                              String timestamp) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String secret = secret(channel);
        if (!isBlank(secret)) {
            payload.put("timestamp", timestamp);
            payload.put("sign", feishuSign(timestamp, secret));
        }
        payload.put("msg_type", "text");
        payload.put("content", Map.of("text", alertText(alert)));
        return payload;
    }

    /**
     * 执行 teamsPayload 流程，围绕 teams payload 完成校验、计算或结果组装。
     *
     * @param alert alert 参数，用于 teamsPayload 流程中的校验、计算或对象转换。
     * @return 返回 teamsPayload 流程生成的业务结果。
     */
    private Map<String, Object> teamsPayload(MarketingMonitorAlertDO alert) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("@type", "MessageCard");
        payload.put("@context", "https://schema.org/extensions");
        payload.put("summary", defaultString(alert.getTitle(), "Monitoring alert"));
        payload.put("themeColor", "D83B01");
        payload.put("title", defaultString(alert.getTitle(), "Monitoring alert"));
        payload.put("text", alertText(alert));
        return payload;
    }

    /**
     * 执行 alertPayload 流程，围绕 alert payload 完成校验、计算或结果组装。
     *
     * @param alert alert 参数，用于 alertPayload 流程中的校验、计算或对象转换。
     * @return 返回 alertPayload 流程生成的业务结果。
     */
    private Map<String, Object> alertPayload(MarketingMonitorAlertDO alert) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", alert.getId());
        payload.put("alertType", alert.getAlertType());
        payload.put("severity", alert.getSeverity());
        payload.put("status", alert.getStatus());
        payload.put("scopeKey", alert.getScopeKey());
        payload.put("title", alert.getTitle());
        payload.put("reason", alert.getReason());
        payload.put("itemCount", alert.getItemCount());
        payload.put("windowStart", dateTime(alert.getWindowStart()));
        payload.put("windowEnd", dateTime(alert.getWindowEnd()));
        payload.put("metadata", map(alert.getMetadataJson()));
        payload.put("createdAt", dateTime(alert.getCreatedAt()));
        return payload;
    }

    /**
     * 执行 alertText 流程，围绕 alert text 完成校验、计算或结果组装。
     *
     * @param alert alert 参数，用于 alertText 流程中的校验、计算或对象转换。
     * @return 返回 alert text 生成的文本或业务键。
     */
    private String alertText(MarketingMonitorAlertDO alert) {
        return "[" + defaultString(alert.getSeverity(), "UNKNOWN") + "] "
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @return 返回 defaultString 流程生成的业务结果。
                 */
                + defaultString(alert.getTitle(), "Monitoring alert")
                + " | type=" + defaultString(alert.getAlertType(), "UNKNOWN")
                + " | scope=" + defaultString(alert.getScopeKey(), "-")
                + " | reason=" + defaultString(alert.getReason(), "-");
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param alertId 业务对象 ID，用于定位具体记录。
     * @param deliveries deliveries 参数，用于 dispatchView 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private MarketingMonitorAlertDispatchView dispatchView(Long tenantId,
                                                           Long alertId,
                                                           List<MarketingMonitorAlertDeliveryDO> deliveries) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<MarketingMonitorAlertDeliveryView> views = safeList(deliveries).stream()
                .map(this::toDeliveryView)
                .toList();
        int delivered = (int) views.stream()
                .filter(view -> MarketingMonitorAlertDeliveryDO.SUCCESS.equals(view.status()))
                .count();
        int failed = views.size() - delivered;
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorAlertDispatchView(tenantId, alertId, views.size(), delivered, failed, views);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 enabled channels 汇总后的集合、分页或映射视图。
     */
    private List<MarketingMonitorAlertChannelDO> enabledChannels(Long tenantId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(channelMapper.selectList(new LambdaQueryWrapper<MarketingMonitorAlertChannelDO>()
                        .eq(MarketingMonitorAlertChannelDO::getTenantId, tenantId)
                        .eq(MarketingMonitorAlertChannelDO::getEnabled, 1)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .orderByAsc(MarketingMonitorAlertChannelDO::getId))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> enabled(row.getEnabled()))
                .toList();
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param channel channel 参数，用于 matches 流程中的校验、计算或对象转换。
     * @param alert alert 参数，用于 matches 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean matches(MarketingMonitorAlertChannelDO channel, MarketingMonitorAlertDO alert) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (severityRank(alert.getSeverity()) < severityRank(channel.getMinSeverity())) {
            return false;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<String> alertTypes = stringList(channel.getAlertTypesJson()).stream()
                .map(this::normalizeOptionalUpper)
                .filter(value -> value != null)
                .toList();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return alertTypes.isEmpty() || alertTypes.contains(normalizeOptionalUpper(alert.getAlertType()));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorAlertChannelView toChannelView(MarketingMonitorAlertChannelDO row) {
        return new MarketingMonitorAlertChannelView(
                row.getId(),
                row.getTenantId(),
                row.getChannelKey(),
                row.getChannelType(),
                row.getDisplayName(),
                row.getEndpointUrl(),
                enabled(row.getEnabled()),
                row.getMinSeverity(),
                stringList(row.getAlertTypesJson()),
                row.getSigningMode(),
                row.getSecretPrefix(),
                map(row.getMetadataJson()),
                maxAttempts(row),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorAlertDeliveryView toDeliveryView(MarketingMonitorAlertDeliveryDO row) {
        return new MarketingMonitorAlertDeliveryView(
                row.getId(),
                row.getTenantId(),
                row.getAlertId(),
                row.getChannelId(),
                row.getChannelKey(),
                row.getChannelType(),
                row.getDeliveryId(),
                row.getAttempt() == null ? 0 : row.getAttempt(),
                row.getHttpStatus(),
                row.getStatus(),
                row.getNextRetryAt(),
                row.getErrorMessage(),
                row.getTerminalReason(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 规范化输入值。
     *
     * @param values values 参数，用于 normalizedAlertTypes 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> normalizedAlertTypes(List<String> values) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (values == null) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return values.stream()
                .map(this::normalizeOptionalUpper)
                .filter(value -> value != null)
                .distinct()
                .toList();
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param secret secret 参数，用于 canvasSignature 流程中的校验、计算或对象转换。
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @param rawPayload raw payload 参数，用于 canvasSignature 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private String canvasSignature(String secret, String timestamp, String rawPayload) {
        String signedPayload = timestamp + "\n" + defaultString(rawPayload, "");
        return "sha256=" + hmacSha256Hex(secret, signedPayload);
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @param secret secret 参数，用于 feishuSign 流程中的校验、计算或对象转换。
     * @return 返回 feishu sign 生成的文本或业务键。
     */
    private String feishuSign(String timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign Feishu alert payload", ex);
        }
    }

    /**
     * 执行 hmacSha256Hex 流程，围绕 hmac sha256 hex 完成校验、计算或结果组装。
     *
     * @param secret secret 参数，用于 hmacSha256Hex 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 hmac sha256 hex 生成的文本或业务键。
     */
    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign monitoring alert payload", ex);
        }
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param channel channel 参数，用于 secret 流程中的校验、计算或对象转换。
     * @return 返回 secret 生成的文本或业务键。
     */
    private String secret(MarketingMonitorAlertChannelDO channel) {
        return isBlank(channel.getSecretCiphertext()) || secretCipher == null
                ? null
                : secretCipher.decrypt(channel.getSecretCiphertext());
    }

    /**
     * 执行 maxAttempts 流程，围绕 max attempts 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 max attempts 计算得到的数量、金额或指标值。
     */
    private int maxAttempts(MarketingMonitorAlertChannelDO row) {
        return maxAttempts(row == null ? null : row.getMaxAttempts());
    }

    /**
     * 执行 maxAttempts 流程，围绕 max attempts 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 max attempts 计算得到的数量、金额或指标值。
     */
    private int maxAttempts(Integer value) {
        if (value == null || value <= 0) {
            return 3;
        }
        return Math.min(value, 10);
    }

    /**
     * 执行 severityRank 流程，围绕 severity rank 完成校验、计算或结果组装。
     *
     * @param severity severity 参数，用于 severityRank 流程中的校验、计算或对象转换。
     * @return 返回 severity rank 计算得到的数量、金额或指标值。
     */
    private int severityRank(String severity) {
        return switch (normalizeSeverity(defaultString(severity, "LOW"))) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    /**
     * 规范化输入值。
     *
     * @param severity severity 参数，用于 normalizeSeverity 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeSeverity(String severity) {
        String normalized = normalizeOptionalUpper(severity);
        return normalized == null ? "LOW" : normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeChannelType(String value) {
        String normalized = normalizeType(value, "channelType");
        return switch (normalized) {
            case "SLACK", "FEISHU", "TEAMS" -> normalized;
            default -> "WEBHOOK";
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeSigningMode(String value) {
        String normalized = normalizeOptionalUpper(value);
        return switch (normalized == null ? "NONE" : normalized) {
            case "CANVAS_HMAC", "FEISHU_BOT" -> normalized;
            default -> "NONE";
        };
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param signingMode signing mode 参数，用于 requiresSecret 流程中的校验、计算或对象转换。
     * @return 返回 requires secret 的布尔判断结果。
     */
    private boolean requiresSecret(String signingMode) {
        return "CANVAS_HMAC".equals(signingMode) || "FEISHU_BOT".equals(signingMode);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private String validateUrl(String value) {
        String url = required(value, "endpointUrl");
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("endpointUrl must be http or https");
        }
        return url;
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("monitoring alert fanout JSON serialization failed", ex);
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 write json 生成的文本或业务键。
     */
    private String writeJson(Object value) {
        return json(value);
    }

    /**
     * 执行 stringList 流程，围绕 string list 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 string list 汇总后的集合、分页或映射视图。
     */
    private List<String> stringList(String json) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (isBlank(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param values values 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeKey(String value, String field) {
        return required(value, field).toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeType(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param maxLength max length 参数，用于 trimLength 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 default actor 生成的文本或业务键。
     */
    private String defaultActor(String actor) {
        return isBlank(actor) ? "system" : actor.trim();
    }

    /**
     * 执行 dateTime 流程，围绕 date time 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 date time 生成的文本或业务键。
     */
    private String dateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 enabled 的布尔判断结果。
     */
    private boolean enabled(Integer value) {
        return value != null && value == 1;
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
