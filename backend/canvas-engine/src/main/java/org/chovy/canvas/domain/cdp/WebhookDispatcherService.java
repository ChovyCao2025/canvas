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
        } catch (RuntimeException ex) {
            log.setErrorMessage(ex.getMessage());
            applyDecision(log, retryPolicy.classify(null, true, attempt, maxAttempts(sub)), null, ex.getMessage());
        }
        deliveryLogMapper.insert(log);
    }

    public boolean matches(String eventTypesJson, String eventType) {
        try {
            List<String> eventTypes = objectMapper.readValue(eventTypesJson, STRING_LIST);
            return eventTypes.contains(eventType);
        } catch (Exception e) {
            log.warn("[WEBHOOK] invalid event_types json: {}", e.getMessage());
            return false;
        }
    }

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

    private void applyDecision(WebhookDeliveryLogDO log, WebhookRetryPolicy.Decision decision,
                               Integer httpStatus, String errorMessage) {
        log.setHttpStatus(httpStatus);
        log.setStatus(decision.status());
        log.setNextRetryAt(decision.nextRetryAt());
        log.setTerminalReason(decision.terminalReason());
        log.setErrorMessage(errorMessage);
    }

    private int maxAttempts(WebhookSubscriptionDO sub) {
        return sub.getMaxAttempts() == null || sub.getMaxAttempts() <= 0 ? 3 : sub.getMaxAttempts();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("webhook payload is not JSON serializable", e);
        }
    }

    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }
}
