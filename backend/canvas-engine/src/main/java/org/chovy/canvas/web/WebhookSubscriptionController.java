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

    @GetMapping
    public Mono<R<List<WebhookSubscriptionDTO>>> list() {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(subscriptionMapper.selectList(
                                        new LambdaQueryWrapper<WebhookSubscriptionDO>()
                                                .eq(WebhookSubscriptionDO::getTenantId, tenantId(ctx))
                                                .orderByDesc(WebhookSubscriptionDO::getId))
                                .stream()
                                .map(this::toDto)
                                .toList()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

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

    @PutMapping("/{id}/pause")
    public Mono<R<Void>> pause(@PathVariable Long id) {
        return updateStatus(id, WebhookSubscriptionDO.PAUSED);
    }

    @PutMapping("/{id}/resume")
    public Mono<R<Void>> resume(@PathVariable Long id) {
        return updateStatus(id, WebhookSubscriptionDO.ACTIVE);
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return updateStatus(id, WebhookSubscriptionDO.DISABLED);
    }

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

    @GetMapping("/{id}/deliveries")
    public Mono<R<List<WebhookDeliveryDTO>>> deliveries(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    WebhookSubscriptionDO row = requireTenantRow(ctx, id);
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

    private Mono<R<Void>> updateStatus(Long id, String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    WebhookSubscriptionDO row = requireTenantRow(ctx, id);
                    row.setStatus(status);
                    subscriptionMapper.updateById(row);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    private WebhookSubscriptionDO requireTenantRow(TenantContext ctx, Long id) {
        WebhookSubscriptionDO row = subscriptionMapper.selectById(id);
        Long normalizedTenantId = tenantId(ctx);
        if (row == null || !normalizedTenantId.equals(row.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook subscription not found");
        }
        return row;
    }

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

    private String applyNewSecret(WebhookSubscriptionDO row) {
        String rawSecret = generateSecret();
        row.setSecretPrefix(rawSecret.substring(0, Math.min(rawSecret.length(), SECRET_PREFIX_LENGTH)));
        row.setSecretHash(passwordEncoder.encode(rawSecret));
        row.setSecretCiphertext(secretCipher.encrypt(rawSecret));
        return rawSecret;
    }

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return "whsec_" + HexFormat.of().formatHex(bytes);
    }

    private String writeJson(List<String> eventTypes) {
        try {
            return objectMapper.writeValueAsString(eventTypes);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("eventTypes is not JSON serializable", e);
        }
    }

    private List<String> readEventTypes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> normalizeEventTypes(List<String> eventTypes) {
        return eventTypes == null ? List.of() : eventTypes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private Integer normalizeMaxAttempts(Integer value) {
        return value == null || value <= 0 ? 3 : value;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }
}
