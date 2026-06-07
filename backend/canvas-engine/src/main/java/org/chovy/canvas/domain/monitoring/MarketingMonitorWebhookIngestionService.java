package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Service
public class MarketingMonitorWebhookIngestionService {

    private static final int SECRET_BYTES = 32;
    private static final int SECRET_PREFIX_LENGTH = 12;
    private static final int DEFAULT_TOLERANCE_SECONDS = 300;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final MarketingMonitorSourceMapper sourceMapper;
    private final MarketingMonitoringService monitoringService;
    private final MarketingMonitorWebhookPayloadMapper payloadMapper;
    private final MarketingMonitorWebhookSignatureService signatureService;
    private final SecretCipher secretCipher;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public MarketingMonitorWebhookIngestionService(MarketingMonitorSourceMapper sourceMapper,
                                                   MarketingMonitoringService monitoringService,
                                                   MarketingMonitorWebhookPayloadMapper payloadMapper,
                                                   MarketingMonitorWebhookSignatureService signatureService,
                                                   SecretCipher secretCipher,
                                                   BCryptPasswordEncoder passwordEncoder,
                                                   ObjectMapper objectMapper) {
        this(sourceMapper, monitoringService, payloadMapper, signatureService, secretCipher,
                passwordEncoder, objectMapper, Clock.systemDefaultZone());
    }

    MarketingMonitorWebhookIngestionService(MarketingMonitorSourceMapper sourceMapper,
                                            MarketingMonitoringService monitoringService,
                                            MarketingMonitorWebhookPayloadMapper payloadMapper,
                                            MarketingMonitorWebhookSignatureService signatureService,
                                            SecretCipher secretCipher,
                                            BCryptPasswordEncoder passwordEncoder,
                                            ObjectMapper objectMapper,
                                            Clock clock) {
        this.sourceMapper = sourceMapper;
        this.monitoringService = monitoringService;
        this.payloadMapper = payloadMapper;
        this.signatureService = signatureService;
        this.secretCipher = secretCipher;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public MarketingMonitorWebhookSecretView rotateSecret(Long tenantId, Long sourceId, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (sourceId == null) {
            throw new IllegalArgumentException("sourceId is required");
        }
        MarketingMonitorSourceDO source = sourceMapper.selectById(sourceId);
        if (source == null || !scopedTenantId.equals(source.getTenantId())) {
            throw new IllegalArgumentException("monitor source is not found");
        }
        String rawSecret = generateSecret();
        LocalDateTime rotatedAt = now();
        int tolerance = tolerance(source.getWebhookSignatureToleranceSeconds());
        source.setWebhookEnabled(1);
        source.setWebhookSecretPrefix(rawSecret.substring(0, Math.min(SECRET_PREFIX_LENGTH, rawSecret.length())));
        source.setWebhookSecretHash(passwordEncoder.encode(rawSecret));
        source.setWebhookSecretCiphertext(secretCipher.encrypt(rawSecret));
        source.setWebhookSignatureToleranceSeconds(tolerance);
        source.setUpdatedAt(rotatedAt);
        sourceMapper.updateById(source);
        return new MarketingMonitorWebhookSecretView(
                source.getId(),
                scopedTenantId,
                source.getSourceKey(),
                source.getWebhookSecretPrefix(),
                rawSecret,
                endpointPath(scopedTenantId, source.getSourceKey()),
                tolerance,
                defaultActor(actor),
                rotatedAt);
    }

    public MarketingMonitorWebhookIngestView ingestWebhook(Long tenantId,
                                                           String sourceKey,
                                                           String timestamp,
                                                           String signature,
                                                           String rawBody) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String normalizedSourceKey = normalizeKey(sourceKey);
        MarketingMonitorSourceDO source = sourceMapper.selectOne(
                new LambdaQueryWrapper<MarketingMonitorSourceDO>()
                        .eq(MarketingMonitorSourceDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorSourceDO::getSourceKey, normalizedSourceKey)
                        .last("LIMIT 1"));
        validateWebhookSource(scopedTenantId, source);
        String secret = secretCipher.decrypt(source.getWebhookSecretCiphertext());
        signatureService.verifyOrThrow(secret, timestamp, rawBody, signature,
                tolerance(source.getWebhookSignatureToleranceSeconds()));
        MarketingMonitorItemIngestCommand command = payloadMapper.toIngestCommand(source, parsePayload(rawBody));
        MarketingMonitorIngestResult result = monitoringService.ingestItem(
                scopedTenantId,
                command,
                "monitor-webhook:" + source.getSourceKey());
        return new MarketingMonitorWebhookIngestView(
                scopedTenantId,
                source.getId(),
                source.getSourceKey(),
                result);
    }

    private void validateWebhookSource(Long tenantId, MarketingMonitorSourceDO source) {
        if (source == null || !tenantId.equals(source.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "monitor source is not found");
        }
        if (!enabled(source.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "monitor source is disabled");
        }
        if (!enabled(source.getWebhookEnabled())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "monitoring webhook is not enabled");
        }
        if (isBlank(source.getWebhookSecretCiphertext())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "monitoring webhook secret is not configured");
        }
    }

    private Map<String, Object> parsePayload(String rawBody) {
        if (isBlank(rawBody)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monitoring webhook payload is required");
        }
        try {
            return objectMapper.readValue(rawBody, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monitoring webhook payload must be JSON", ex);
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return "monwhsec_" + HexFormat.of().formatHex(bytes);
    }

    private String endpointPath(Long tenantId, String sourceKey) {
        return "/public/marketing-monitoring/webhooks/" + tenantId + "/" + sourceKey;
    }

    private int tolerance(Integer value) {
        return value == null || value <= 0 ? DEFAULT_TOLERANCE_SECONDS : value;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeKey(String value) {
        if (isBlank(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceKey is required");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean enabled(Integer value) {
        return value != null && value == 1;
    }

    private String defaultActor(String actor) {
        return isBlank(actor) ? "system" : actor.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
