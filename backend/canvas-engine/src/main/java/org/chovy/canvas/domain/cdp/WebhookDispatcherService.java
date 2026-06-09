package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.WebhookDeliveryLogDO;
import org.chovy.canvas.dal.dataobject.WebhookSubscriptionDO;
import org.chovy.canvas.dal.mapper.WebhookDeliveryLogMapper;
import org.chovy.canvas.dal.mapper.WebhookSubscriptionMapper;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WebhookDispatcherService 编排 domain.cdp 场景的领域业务规则。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDispatcherService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final WebhookSubscriptionMapper subscriptionMapper;
    private final WebhookDeliveryLogMapper deliveryLogMapper;
    private final WebhookSignatureService signatureService;
    private final WebhookRetryPolicy retryPolicy;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final SecretCipher secretCipher;

    /**
     * 向订阅端分发业务 Webhook，作为CDP 客户数据的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     */
    public void dispatch(Long tenantId, String eventType, Map<String, Object> payload) {
        List<WebhookSubscriptionDO> subs = subscriptionMapper.selectList(new LambdaQueryWrapper<WebhookSubscriptionDO>()
                .eq(WebhookSubscriptionDO::getTenantId, normalizeTenantId(tenantId))
                .eq(WebhookSubscriptionDO::getStatus, WebhookSubscriptionDO.ACTIVE));
        for (WebhookSubscriptionDO sub : subs) {
            if (matches(sub.getEventTypes(), eventType)) {
                sendOnce(sub, eventType, payload, UUID.randomUUID().toString(), 1);
            }
        }
    }

    /**
     * 向单个订阅端发送一次 Webhook 投递，作为CDP 客户数据的服务入口。
     * <p>该方法不接收显式租户参数时，会依赖输入对象、密钥或已有记录携带的租户信息维持隔离。
     * 会通过 Mapper 写入、更新或关闭持久化记录；可能与外部供应商、Webhook 或上传交接端点交互。
     * @param sub sub 参数，用于 sendOnce 流程中的校验、计算或对象转换。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param deliveryId 目标业务记录 ID，需与租户边界匹配
     * @param attempt attempt 参数，用于 sendOnce 流程中的校验、计算或对象转换。
     */
    public void sendOnce(WebhookSubscriptionDO sub, String eventType, Map<String, Object> payload,
                         String deliveryId, int attempt) {
        String rawPayload = writeJson(WebhookDeliveryPayload.of(eventType, deliveryId, payload));
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = signatureService.sign(secretCipher.decrypt(sub.getSecretCiphertext()), timestamp, rawPayload);
        WebhookDeliveryLogDO log = newLog(sub, eventType, rawPayload, deliveryId, attempt);
        try {
            ResponseEntity<Void> response = webClientBuilder.build()
                    .post()
                    .uri(sub.getCallbackUrl())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Canvas-Event", eventType)
                    .header("X-Canvas-Delivery", deliveryId)
                    .header("X-Canvas-Timestamp", timestamp)
                    .header("X-Canvas-Signature", signature)
                    .bodyValue(rawPayload)
                    .exchangeToMono(clientResponse -> clientResponse.toBodilessEntity())
                    .block(Duration.ofSeconds(10));
            Integer status = response == null ? null : response.getStatusCode().value();
            applyDecision(log, retryPolicy.classify(status, false, attempt, maxAttempts(sub)), status, null);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            log.setErrorMessage(ex.getMessage());
            applyDecision(log, retryPolicy.classify(null, true, attempt, maxAttempts(sub)), null, ex.getMessage());
        }
        deliveryLogMapper.insert(log);
    }

    /**
     * 判断输入是否满足业务规则，作为CDP 客户数据的服务入口。
     * <p>该方法不接收显式租户参数时，会依赖输入对象、密钥或已有记录携带的租户信息维持隔离。
     * 可能与外部供应商、Webhook 或上传交接端点交互。
     * @param eventTypesJson JSON 字符串，承载结构化配置或明细。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @return 如果目标记录在租户边界内被成功更新或规则匹配则返回 true，否则返回 false
     */
    public boolean matches(String eventTypesJson, String eventType) {
        try {
            List<String> eventTypes = objectMapper.readValue(eventTypesJson, STRING_LIST);
            return eventTypes.contains(eventType);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[WEBHOOK] invalid event_types json: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param sub sub 参数，用于 newLog 流程中的校验、计算或对象转换。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param deliveryId 业务对象 ID，用于定位具体记录。
     * @param attempt attempt 参数，用于 newLog 流程中的校验、计算或对象转换。
     * @return 返回 newLog 流程生成的业务结果。
     */
    private WebhookDeliveryLogDO newLog(WebhookSubscriptionDO sub, String eventType, String payload,
                                        String deliveryId, int attempt) {
        WebhookDeliveryLogDO log = new WebhookDeliveryLogDO();
        log.setTenantId(sub.getTenantId());
        log.setSubscriptionId(sub.getId());
        log.setDeliveryId(deliveryId);
        log.setEventType(eventType);
        log.setPayload(payload);
        log.setAttempt(attempt);
        log.setStatus(WebhookDeliveryLogDO.PENDING);
        return log;
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param log log 参数，用于 applyDecision 流程中的校验、计算或对象转换。
     * @param decision decision 参数，用于 applyDecision 流程中的校验、计算或对象转换。
     * @param httpStatus 业务状态，用于筛选或推进状态流转。
     * @param errorMessage error message 参数，用于 applyDecision 流程中的校验、计算或对象转换。
     */
    private void applyDecision(WebhookDeliveryLogDO log, WebhookRetryPolicy.Decision decision,
                               Integer httpStatus, String errorMessage) {
        log.setHttpStatus(httpStatus);
        log.setStatus(decision.status());
        log.setNextRetryAt(decision.nextRetryAt());
        log.setTerminalReason(decision.terminalReason());
        log.setErrorMessage(errorMessage);
    }

    /**
     * 执行 maxAttempts 流程，围绕 max attempts 完成校验、计算或结果组装。
     *
     * @param sub sub 参数，用于 maxAttempts 流程中的校验、计算或对象转换。
     * @return 返回 max attempts 计算得到的数量、金额或指标值。
     */
    private int maxAttempts(WebhookSubscriptionDO sub) {
        return sub.getMaxAttempts() == null || sub.getMaxAttempts() <= 0 ? 3 : sub.getMaxAttempts();
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 write json 生成的文本或业务键。
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("webhook payload is not JSON serializable", e);
        }
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }
}
