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

    public MarketingMonitorAlertChannelView upsertChannel(Long tenantId,
                                                          MarketingMonitorAlertChannelCommand command,
                                                          String actor) {
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

    public MarketingMonitorAlertDispatchView dispatchAlert(Long tenantId,
                                                           MarketingMonitorAlertDO alert,
                                                           String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (alert == null || !scopedTenantId.equals(alert.getTenantId())) {
            throw new IllegalArgumentException("alert is not found");
        }
        if (!STATUS_OPEN.equals(normalizeOptionalUpper(alert.getStatus()))) {
            return dispatchView(scopedTenantId, alert.getId(), List.of());
        }
        List<MarketingMonitorAlertDeliveryDO> deliveries = new ArrayList<>();
        for (MarketingMonitorAlertChannelDO channel : enabledChannels(scopedTenantId)) {
            if (!matches(channel, alert)) {
                continue;
            }
            deliveries.add(sendOnce(channel, alert, defaultActor(actor), 1));
        }
        return dispatchView(scopedTenantId, alert.getId(), deliveries);
    }

    public List<MarketingMonitorAlertDeliveryView> deliveries(Long tenantId,
                                                              Long alertId,
                                                              String status,
                                                              int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int boundedLimit = boundedLimit(limit);
        String normalizedStatus = normalizeOptionalUpper(status);
        return safeList(deliveryMapper.selectList(new LambdaQueryWrapper<MarketingMonitorAlertDeliveryDO>()
                        .eq(MarketingMonitorAlertDeliveryDO::getTenantId, scopedTenantId)
                        .eq(alertId != null, MarketingMonitorAlertDeliveryDO::getAlertId, alertId)
                        .eq(normalizedStatus != null, MarketingMonitorAlertDeliveryDO::getStatus, normalizedStatus)
                        .orderByDesc(MarketingMonitorAlertDeliveryDO::getCreatedAt)
                        .last("LIMIT " + boundedLimit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> alertId == null || alertId.equals(row.getAlertId()))
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .limit(boundedLimit)
                .map(this::toDeliveryView)
                .toList();
    }

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
        if (rawSecret != null) {
            row.setSecretPrefix(rawSecret.substring(0, Math.min(SECRET_PREFIX_LENGTH, rawSecret.length())));
            row.setSecretHash(passwordEncoder.encode(rawSecret));
            row.setSecretCiphertext(secretCipher.encrypt(rawSecret));
        }
        row.setMetadataJson(json(command.metadata()));
        row.setMaxAttempts(maxAttempts(command.maxAttempts()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            channelMapper.insert(row);
        } else {
            channelMapper.updateById(row);
        }
        return toChannelView(row);
    }

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
        } catch (RuntimeException ex) {
            delivery.setErrorMessage(trimLength(ex.getMessage(), 1000));
            applyDecision(delivery, retryPolicy.classify(null, true, attempt, maxAttempts(channel)));
            log.warn("[MONITORING] alert fanout failed channel={} alert={} error={}",
                    channel.getChannelKey(), alert.getId(), ex.getMessage());
        }
        deliveryMapper.insert(delivery);
        return delivery;
    }

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

    private void applyDecision(MarketingMonitorAlertDeliveryDO delivery,
                               WebhookRetryPolicy.Decision decision) {
        delivery.setStatus(decision.status());
        delivery.setNextRetryAt(decision.nextRetryAt());
        delivery.setTerminalReason(decision.terminalReason());
        delivery.setUpdatedAt(now());
    }

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

    private Map<String, Object> slackPayload(MarketingMonitorAlertDO alert) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", alertText(alert));
        payload.put("blocks", List.of(
                Map.of(
                        "type", "section",
                        "text", Map.of(
                                "type", "mrkdwn",
                                "text", "*" + defaultString(alert.getTitle(), "Monitoring alert") + "*\n"
                                        + alertText(alert)))));
        return payload;
    }

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

    private String alertText(MarketingMonitorAlertDO alert) {
        return "[" + defaultString(alert.getSeverity(), "UNKNOWN") + "] "
                + defaultString(alert.getTitle(), "Monitoring alert")
                + " | type=" + defaultString(alert.getAlertType(), "UNKNOWN")
                + " | scope=" + defaultString(alert.getScopeKey(), "-")
                + " | reason=" + defaultString(alert.getReason(), "-");
    }

    private MarketingMonitorAlertDispatchView dispatchView(Long tenantId,
                                                           Long alertId,
                                                           List<MarketingMonitorAlertDeliveryDO> deliveries) {
        List<MarketingMonitorAlertDeliveryView> views = safeList(deliveries).stream()
                .map(this::toDeliveryView)
                .toList();
        int delivered = (int) views.stream()
                .filter(view -> MarketingMonitorAlertDeliveryDO.SUCCESS.equals(view.status()))
                .count();
        int failed = views.size() - delivered;
        return new MarketingMonitorAlertDispatchView(tenantId, alertId, views.size(), delivered, failed, views);
    }

    private List<MarketingMonitorAlertChannelDO> enabledChannels(Long tenantId) {
        return safeList(channelMapper.selectList(new LambdaQueryWrapper<MarketingMonitorAlertChannelDO>()
                        .eq(MarketingMonitorAlertChannelDO::getTenantId, tenantId)
                        .eq(MarketingMonitorAlertChannelDO::getEnabled, 1)
                        .orderByAsc(MarketingMonitorAlertChannelDO::getId))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> enabled(row.getEnabled()))
                .toList();
    }

    private boolean matches(MarketingMonitorAlertChannelDO channel, MarketingMonitorAlertDO alert) {
        if (severityRank(alert.getSeverity()) < severityRank(channel.getMinSeverity())) {
            return false;
        }
        List<String> alertTypes = stringList(channel.getAlertTypesJson()).stream()
                .map(this::normalizeOptionalUpper)
                .filter(value -> value != null)
                .toList();
        return alertTypes.isEmpty() || alertTypes.contains(normalizeOptionalUpper(alert.getAlertType()));
    }

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

    private List<String> normalizedAlertTypes(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::normalizeOptionalUpper)
                .filter(value -> value != null)
                .distinct()
                .toList();
    }

    private String canvasSignature(String secret, String timestamp, String rawPayload) {
        String signedPayload = timestamp + "\n" + defaultString(rawPayload, "");
        return "sha256=" + hmacSha256Hex(secret, signedPayload);
    }

    private String feishuSign(String timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign Feishu alert payload", ex);
        }
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign monitoring alert payload", ex);
        }
    }

    private String secret(MarketingMonitorAlertChannelDO channel) {
        return isBlank(channel.getSecretCiphertext()) || secretCipher == null
                ? null
                : secretCipher.decrypt(channel.getSecretCiphertext());
    }

    private int maxAttempts(MarketingMonitorAlertChannelDO row) {
        return maxAttempts(row == null ? null : row.getMaxAttempts());
    }

    private int maxAttempts(Integer value) {
        if (value == null || value <= 0) {
            return 3;
        }
        return Math.min(value, 10);
    }

    private int severityRank(String severity) {
        return switch (normalizeSeverity(defaultString(severity, "LOW"))) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private String normalizeSeverity(String severity) {
        String normalized = normalizeOptionalUpper(severity);
        return normalized == null ? "LOW" : normalized;
    }

    private String normalizeChannelType(String value) {
        String normalized = normalizeType(value, "channelType");
        return switch (normalized) {
            case "SLACK", "FEISHU", "TEAMS" -> normalized;
            default -> "WEBHOOK";
        };
    }

    private String normalizeSigningMode(String value) {
        String normalized = normalizeOptionalUpper(value);
        return switch (normalized == null ? "NONE" : normalized) {
            case "CANVAS_HMAC", "FEISHU_BOT" -> normalized;
            default -> "NONE";
        };
    }

    private boolean requiresSecret(String signingMode) {
        return "CANVAS_HMAC".equals(signingMode) || "FEISHU_BOT".equals(signingMode);
    }

    private String validateUrl(String value) {
        String url = required(value, "endpointUrl");
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("endpointUrl must be http or https");
        }
        return url;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("monitoring alert fanout JSON serialization failed", ex);
        }
    }

    private String writeJson(Object value) {
        return json(value);
    }

    private List<String> stringList(String json) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private Map<String, Object> map(String json) {
        if (isBlank(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeKey(String value, String field) {
        return required(value, field).toLowerCase(Locale.ROOT);
    }

    private String normalizeType(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String required(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String defaultString(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String trimLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String defaultActor(String actor) {
        return isBlank(actor) ? "system" : actor.trim();
    }

    private String dateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private boolean enabled(Integer value) {
        return value != null && value == 1;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
