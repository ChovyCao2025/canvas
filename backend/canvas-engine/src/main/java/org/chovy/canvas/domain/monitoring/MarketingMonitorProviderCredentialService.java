package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialEventDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialEventMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MarketingMonitorProviderCredentialService {

    private static final int SECRET_PREFIX_LENGTH = 12;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final MarketingMonitorProviderCredentialMapper credentialMapper;
    private final MarketingMonitorProviderCredentialEventMapper eventMapper;
    private final MarketingMonitorProviderHttpTransport refreshTransport;
    private final ObjectMapper objectMapper;
    private final SecretCipher secretCipher;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public MarketingMonitorProviderCredentialService(
            MarketingMonitorProviderCredentialMapper credentialMapper,
            MarketingMonitorProviderCredentialEventMapper eventMapper,
            MarketingMonitorProviderHttpTransport refreshTransport,
            ObjectMapper objectMapper,
            SecretCipher secretCipher,
            BCryptPasswordEncoder passwordEncoder) {
        this(credentialMapper, eventMapper, refreshTransport, objectMapper, secretCipher, passwordEncoder,
                Clock.systemDefaultZone());
    }

    MarketingMonitorProviderCredentialService(
            MarketingMonitorProviderCredentialMapper credentialMapper,
            MarketingMonitorProviderCredentialEventMapper eventMapper,
            MarketingMonitorProviderHttpTransport refreshTransport,
            ObjectMapper objectMapper,
            SecretCipher secretCipher,
            BCryptPasswordEncoder passwordEncoder,
            Clock clock) {
        this.credentialMapper = credentialMapper;
        this.eventMapper = eventMapper;
        this.refreshTransport = refreshTransport;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.secretCipher = secretCipher;
        this.passwordEncoder = passwordEncoder == null ? new BCryptPasswordEncoder() : passwordEncoder;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderCredentialView upsert(Long tenantId,
                                                         MarketingMonitorProviderCredentialCommand command,
                                                         String actor) {
        if (command == null) {
            throw new IllegalArgumentException("provider credential command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String credentialKey = normalizeKey(command.credentialKey(), "credentialKey");
        MarketingMonitorProviderCredentialDO row = findCredential(scopedTenantId, credentialKey);
        LocalDateTime changedAt = now();
        if (row == null) {
            row = new MarketingMonitorProviderCredentialDO();
            row.setTenantId(scopedTenantId);
            row.setCredentialKey(credentialKey);
            row.setCreatedBy(defaultActor(actor));
            row.setCreatedAt(changedAt);
            row.setRefreshAttemptCount(0);
        }
        row.setProviderType(normalizeType(command.providerType(), "providerType"));
        row.setAuthType(normalizeType(command.authType(), "authType"));
        row.setDisplayName(defaultString(command.displayName(), credentialKey));
        row.setStatus(Boolean.FALSE.equals(command.enabled()) ? STATUS_DISABLED : STATUS_ACTIVE);
        row.setTokenType(trimToNull(command.tokenType()));
        row.setScopesJson(json(normalizedScopes(command.scopes())));
        applySecret(command.accessToken(), row::setAccessTokenPrefix, row::setAccessTokenHash,
                row::setAccessTokenCiphertext);
        applySecret(command.refreshToken(), row::setRefreshTokenPrefix, row::setRefreshTokenHash,
                row::setRefreshTokenCiphertext);
        applySecret(command.apiKey(), row::setApiKeyPrefix, row::setApiKeyHash, row::setApiKeyCiphertext);
        if (hasText(command.clientId())) {
            row.setClientIdCiphertext(secretCipher.encrypt(command.clientId().trim()));
        }
        if (hasText(command.clientSecret())) {
            row.setClientSecretCiphertext(secretCipher.encrypt(command.clientSecret().trim()));
        }
        row.setRefreshEndpoint(validateOptionalUri(command.refreshEndpoint(), "refreshEndpoint"));
        row.setRevokeEndpoint(validateOptionalUri(command.revokeEndpoint(), "revokeEndpoint"));
        row.setExpiresAt(command.expiresAt());
        row.setRefreshTokenExpiresAt(command.refreshTokenExpiresAt());
        row.setMetadataJson(json(safeMap(command.metadata())));
        row.setUpdatedBy(defaultActor(actor));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            credentialMapper.insert(row);
        } else {
            credentialMapper.updateById(row);
        }
        writeEvent(scopedTenantId, row, "UPSERTED", "SUCCESS", Map.of(
                "providerType", row.getProviderType(),
                "authType", row.getAuthType(),
                "status", row.getStatus()), null, actor);
        return toView(row);
    }

    public List<MarketingMonitorProviderCredentialView> list(Long tenantId,
                                                             MarketingMonitorProviderCredentialQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialQuery effectiveQuery = query == null
                ? new MarketingMonitorProviderCredentialQuery(null, null, null, 50)
                : query;
        int limit = boundedLimit(effectiveQuery.limit());
        String providerType = normalizeOptionalUpper(effectiveQuery.providerType());
        String authType = normalizeOptionalUpper(effectiveQuery.authType());
        String status = normalizeOptionalUpper(effectiveQuery.status());
        return safeList(credentialMapper.selectList(new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                        .eq(MarketingMonitorProviderCredentialDO::getTenantId, scopedTenantId)
                        .eq(providerType != null, MarketingMonitorProviderCredentialDO::getProviderType, providerType)
                        .eq(authType != null, MarketingMonitorProviderCredentialDO::getAuthType, authType)
                        .eq(status != null, MarketingMonitorProviderCredentialDO::getStatus, status)
                        .orderByDesc(MarketingMonitorProviderCredentialDO::getUpdatedAt)
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> providerType == null || providerType.equals(row.getProviderType()))
                .filter(row -> authType == null || authType.equals(row.getAuthType()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderCredentialView disable(Long tenantId, String credentialKey, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialDO row = requiredCredential(scopedTenantId, credentialKey);
        row.setStatus(STATUS_DISABLED);
        row.setUpdatedBy(defaultActor(actor));
        row.setUpdatedAt(now());
        credentialMapper.updateById(row);
        writeEvent(scopedTenantId, row, "DISABLED", "SUCCESS", Map.of("status", STATUS_DISABLED), null, actor);
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderCredentialView refresh(Long tenantId,
                                                          String credentialKey,
                                                          MarketingMonitorProviderCredentialRefreshCommand command,
                                                          String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialDO row = requiredCredential(scopedTenantId, credentialKey);
        ensureEnabled(row);
        int nextAttempt = defaultInt(row.getRefreshAttemptCount()) + 1;
        try {
            MarketingMonitorProviderHttpResponse response = refreshTransport.execute(refreshRequest(row));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return refreshFailed(scopedTenantId, row, nextAttempt,
                        "provider refresh failed with status " + response.statusCode(), actor,
                        Map.of("httpStatus", response.statusCode()));
            }
            JsonNode body = jsonNode(response.body());
            String accessToken = text(body, "access_token");
            if (!hasText(accessToken)) {
                return refreshFailed(scopedTenantId, row, nextAttempt,
                        "provider refresh response did not include access_token", actor,
                        Map.of("httpStatus", response.statusCode()));
            }
            LocalDateTime changedAt = now();
            applySecret(accessToken, row::setAccessTokenPrefix, row::setAccessTokenHash,
                    row::setAccessTokenCiphertext);
            String refreshToken = text(body, "refresh_token");
            if (hasText(refreshToken)) {
                applySecret(refreshToken, row::setRefreshTokenPrefix, row::setRefreshTokenHash,
                        row::setRefreshTokenCiphertext);
            }
            String tokenType = text(body, "token_type");
            if (hasText(tokenType)) {
                row.setTokenType(normalizeTokenType(tokenType));
            }
            List<String> scopes = refreshScopes(body);
            if (!scopes.isEmpty()) {
                row.setScopesJson(json(scopes));
            }
            Long expiresIn = longValue(body, "expires_in");
            if (expiresIn != null && expiresIn > 0) {
                row.setExpiresAt(changedAt.plusSeconds(expiresIn));
            }
            row.setLastRefreshedAt(changedAt);
            row.setRefreshAttemptCount(nextAttempt);
            row.setLastRefreshStatus("SUCCESS");
            row.setLastRefreshError(null);
            row.setUpdatedBy(defaultActor(actor));
            row.setUpdatedAt(changedAt);
            credentialMapper.updateById(row);
            writeEvent(scopedTenantId, row, "REFRESHED", "SUCCESS", Map.of(
                    "httpStatus", response.statusCode(),
                    "tokenType", row.getTokenType(),
                    "expiresAt", string(row.getExpiresAt())), null, actor);
            return toView(row);
        } catch (RuntimeException ex) {
            return refreshFailed(scopedTenantId, row, nextAttempt, message(ex), actor, Map.of());
        }
    }

    public MarketingMonitorProviderCredentialDueRefreshResult refreshDue(
            Long tenantId,
            MarketingMonitorProviderCredentialDueRefreshCommand command,
            String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialDueRefreshCommand effectiveCommand = command == null
                ? new MarketingMonitorProviderCredentialDueRefreshCommand(null, null)
                : command;
        int limit = boundedLimit(effectiveCommand.limit() == null ? 50 : effectiveCommand.limit());
        int windowMinutes = boundedWindowMinutes(effectiveCommand.windowMinutes());
        LocalDateTime evaluatedAt = now();
        LocalDateTime cutoffAt = evaluatedAt.plusMinutes(windowMinutes);
        List<MarketingMonitorProviderCredentialDO> candidates = safeList(credentialMapper.selectList(
                new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                        .eq(MarketingMonitorProviderCredentialDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorProviderCredentialDO::getStatus, STATUS_ACTIVE)
                        .isNotNull(MarketingMonitorProviderCredentialDO::getRefreshEndpoint)
                        .isNotNull(MarketingMonitorProviderCredentialDO::getRefreshTokenCiphertext)
                        .isNotNull(MarketingMonitorProviderCredentialDO::getExpiresAt)
                        .le(MarketingMonitorProviderCredentialDO::getExpiresAt, cutoffAt)
                        .orderByAsc(MarketingMonitorProviderCredentialDO::getExpiresAt)
                        .last("LIMIT " + limit)));
        List<MarketingMonitorProviderCredentialDO> due = candidates.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> STATUS_ACTIVE.equals(normalizeOptionalUpper(row.getStatus())))
                .filter(row -> hasText(row.getRefreshEndpoint()))
                .filter(row -> hasText(row.getRefreshTokenCiphertext()))
                .filter(row -> row.getExpiresAt() != null && !row.getExpiresAt().isAfter(cutoffAt))
                .sorted(Comparator.comparing(
                        MarketingMonitorProviderCredentialDO::getExpiresAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
        int refreshed = 0;
        int failed = 0;
        List<MarketingMonitorProviderCredentialView> credentials = new ArrayList<>();
        for (MarketingMonitorProviderCredentialDO row : due) {
            try {
                MarketingMonitorProviderCredentialView view = refresh(scopedTenantId, row.getCredentialKey(),
                        new MarketingMonitorProviderCredentialRefreshCommand(null), actor);
                credentials.add(view);
                if ("SUCCESS".equals(normalizeOptionalUpper(view.lastRefreshStatus()))) {
                    refreshed++;
                } else {
                    failed++;
                }
            } catch (RuntimeException ex) {
                failed++;
                credentials.add(refreshFailed(scopedTenantId, row,
                        defaultInt(row.getRefreshAttemptCount()) + 1,
                        message(ex),
                        actor,
                        Map.of("scheduled", true)));
            }
        }
        return new MarketingMonitorProviderCredentialDueRefreshResult(
                scopedTenantId,
                candidates.size(),
                due.size(),
                refreshed,
                failed,
                Math.max(0, candidates.size() - due.size()),
                cutoffAt,
                evaluatedAt,
                credentials);
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderCredentialView revoke(Long tenantId,
                                                         String credentialKey,
                                                         MarketingMonitorProviderCredentialRevokeCommand command,
                                                         String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialDO row = requiredCredential(scopedTenantId, credentialKey);
        MarketingMonitorProviderCredentialRevokeCommand effectiveCommand = command == null
                ? new MarketingMonitorProviderCredentialRevokeCommand(null, null, null, null, null)
                : command;
        try {
            String endpoint = validateOptionalUri(
                    defaultString(effectiveCommand.revokeEndpoint(), row.getRevokeEndpoint()), "revokeEndpoint");
            if (!hasText(endpoint)) {
                throw new IllegalStateException("revoke endpoint is required");
            }
            MarketingMonitorProviderHttpResponse response =
                    refreshTransport.execute(revokeRequest(row, endpoint, effectiveCommand));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return revokeFailed(scopedTenantId, row,
                        "provider revoke failed with status " + response.statusCode(),
                        actor,
                        revokeMetadata(effectiveCommand, response, null));
            }
            LocalDateTime changedAt = now();
            if (!Boolean.FALSE.equals(effectiveCommand.disableAfterRevoke())) {
                row.setStatus(STATUS_DISABLED);
            }
            row.setRevokeEndpoint(endpoint);
            row.setRevokedAt(changedAt);
            row.setLastRevokeStatus("SUCCESS");
            row.setLastRevokeError(null);
            row.setUpdatedBy(defaultActor(actor));
            row.setUpdatedAt(changedAt);
            credentialMapper.updateById(row);
            writeEvent(scopedTenantId, row, "REVOKED", "SUCCESS",
                    revokeMetadata(effectiveCommand, response, revokeTokenTypeHint(row, effectiveCommand)),
                    null,
                    actor);
            return toView(row);
        } catch (RuntimeException ex) {
            return revokeFailed(scopedTenantId, row, message(ex), actor,
                    revokeMetadata(effectiveCommand, null, null));
        }
    }

    public String resolveValue(Long tenantId, String reference) {
        CredentialReference parsed = parseReference(reference);
        MarketingMonitorProviderCredentialDO row = requiredCredential(normalizeTenant(tenantId), parsed.credentialKey());
        ensureEnabled(row);
        String value = switch (parsed.field()) {
            case "access_token" -> secretCipher.decrypt(row.getAccessTokenCiphertext());
            case "refresh_token" -> secretCipher.decrypt(row.getRefreshTokenCiphertext());
            case "api_key" -> secretCipher.decrypt(row.getApiKeyCiphertext());
            case "client_id" -> secretCipher.decrypt(row.getClientIdCiphertext());
            case "client_secret" -> secretCipher.decrypt(row.getClientSecretCiphertext());
            default -> throw new IllegalArgumentException("unsupported credential field: " + parsed.field());
        };
        if (!hasText(value)) {
            throw new IllegalStateException("credential value is not available: " + reference);
        }
        return value;
    }

    public List<MarketingMonitorProviderCredentialEventView> events(
            Long tenantId,
            MarketingMonitorProviderCredentialEventQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialEventQuery effectiveQuery = query == null
                ? new MarketingMonitorProviderCredentialEventQuery(null, null, null, 50)
                : query;
        int limit = boundedLimit(effectiveQuery.limit());
        String credentialKey = normalizeOptionalKey(effectiveQuery.credentialKey());
        String eventType = normalizeOptionalUpper(effectiveQuery.eventType());
        String status = normalizeOptionalUpper(effectiveQuery.status());
        return safeList(eventMapper.selectList(new LambdaQueryWrapper<MarketingMonitorProviderCredentialEventDO>()
                        .eq(MarketingMonitorProviderCredentialEventDO::getTenantId, scopedTenantId)
                        .eq(credentialKey != null,
                                MarketingMonitorProviderCredentialEventDO::getCredentialKey, credentialKey)
                        .eq(eventType != null, MarketingMonitorProviderCredentialEventDO::getEventType, eventType)
                        .eq(status != null, MarketingMonitorProviderCredentialEventDO::getStatus, status)
                        .orderByDesc(MarketingMonitorProviderCredentialEventDO::getCreatedAt)
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> credentialKey == null || credentialKey.equals(row.getCredentialKey()))
                .filter(row -> eventType == null || eventType.equals(row.getEventType()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toEventView)
                .toList();
    }

    private MarketingMonitorProviderHttpRequest refreshRequest(MarketingMonitorProviderCredentialDO row) {
        String endpoint = validateOptionalUri(row.getRefreshEndpoint(), "refreshEndpoint");
        if (!hasText(endpoint)) {
            throw new IllegalStateException("refresh endpoint is required");
        }
        String refreshToken = secretCipher.decrypt(row.getRefreshTokenCiphertext());
        String clientId = secretCipher.decrypt(row.getClientIdCiphertext());
        String clientSecret = secretCipher.decrypt(row.getClientSecretCiphertext());
        if (!hasText(refreshToken)) {
            throw new IllegalStateException("refresh token is required");
        }
        if (!hasText(clientId)) {
            throw new IllegalStateException("client id is required");
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        form.put("client_id", clientId);
        if (hasText(clientSecret)) {
            form.put("client_secret", clientSecret);
        }
        return new MarketingMonitorProviderHttpRequest(
                "POST",
                URI.create(endpoint),
                Map.of(
                        "Content-Type", "application/x-www-form-urlencoded",
                        "Accept", "application/json"),
                form(form));
    }

    private MarketingMonitorProviderHttpRequest revokeRequest(MarketingMonitorProviderCredentialDO row,
                                                              String endpoint,
                                                              MarketingMonitorProviderCredentialRevokeCommand command) {
        RevokeToken revokeToken = revokeToken(row, command);
        Map<String, String> form = new LinkedHashMap<>();
        form.put("token", revokeToken.token());
        form.put("token_type_hint", revokeToken.tokenTypeHint());
        String clientId = secretCipher.decrypt(row.getClientIdCiphertext());
        String clientSecret = secretCipher.decrypt(row.getClientSecretCiphertext());
        if (hasText(clientId)) {
            form.put("client_id", clientId);
        }
        if (hasText(clientSecret)) {
            form.put("client_secret", clientSecret);
        }
        return new MarketingMonitorProviderHttpRequest(
                "POST",
                URI.create(endpoint),
                Map.of(
                        "Content-Type", "application/x-www-form-urlencoded",
                        "Accept", "application/json"),
                form(form));
    }

    private MarketingMonitorProviderCredentialView refreshFailed(Long tenantId,
                                                                 MarketingMonitorProviderCredentialDO row,
                                                                 int attemptCount,
                                                                 String error,
                                                                 String actor,
                                                                 Map<String, Object> metadata) {
        LocalDateTime changedAt = now();
        row.setRefreshAttemptCount(attemptCount);
        row.setLastRefreshStatus("FAILED");
        row.setLastRefreshError(trimLength(error, 1000));
        row.setUpdatedBy(defaultActor(actor));
        row.setUpdatedAt(changedAt);
        credentialMapper.updateById(row);
        writeEvent(tenantId, row, "REFRESH_FAILED", "FAILED", metadata, row.getLastRefreshError(), actor);
        return toView(row);
    }

    private MarketingMonitorProviderCredentialView revokeFailed(Long tenantId,
                                                                MarketingMonitorProviderCredentialDO row,
                                                                String error,
                                                                String actor,
                                                                Map<String, Object> metadata) {
        LocalDateTime changedAt = now();
        row.setLastRevokeStatus("FAILED");
        row.setLastRevokeError(trimLength(error, 1000));
        row.setUpdatedBy(defaultActor(actor));
        row.setUpdatedAt(changedAt);
        credentialMapper.updateById(row);
        writeEvent(tenantId, row, "REVOKE_FAILED", "FAILED", metadata, row.getLastRevokeError(), actor);
        return toView(row);
    }

    private MarketingMonitorProviderCredentialDO requiredCredential(Long tenantId, String credentialKey) {
        String normalizedKey = normalizeKey(credentialKey, "credentialKey");
        MarketingMonitorProviderCredentialDO row = findCredential(tenantId, normalizedKey);
        if (row == null) {
            throw new IllegalArgumentException("provider credential is not found: " + normalizedKey);
        }
        return row;
    }

    private MarketingMonitorProviderCredentialDO findCredential(Long tenantId, String credentialKey) {
        return credentialMapper.selectOne(new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                .eq(MarketingMonitorProviderCredentialDO::getTenantId, tenantId)
                .eq(MarketingMonitorProviderCredentialDO::getCredentialKey, credentialKey)
                .last("LIMIT 1"));
    }

    private void writeEvent(Long tenantId,
                            MarketingMonitorProviderCredentialDO row,
                            String eventType,
                            String status,
                            Map<String, Object> metadata,
                            String error,
                            String actor) {
        MarketingMonitorProviderCredentialEventDO event = new MarketingMonitorProviderCredentialEventDO();
        event.setTenantId(tenantId);
        event.setCredentialId(row.getId());
        event.setCredentialKey(row.getCredentialKey());
        event.setEventType(eventType);
        event.setStatus(status);
        event.setMetadataJson(json(safeMap(metadata)));
        event.setErrorMessage(trimLength(error, 1000));
        event.setCreatedBy(defaultActor(actor));
        event.setCreatedAt(now());
        eventMapper.insert(event);
    }

    private void applySecret(String raw,
                             java.util.function.Consumer<String> prefixSetter,
                             java.util.function.Consumer<String> hashSetter,
                             java.util.function.Consumer<String> ciphertextSetter) {
        if (!hasText(raw)) {
            return;
        }
        String value = raw.trim();
        prefixSetter.accept(value.substring(0, Math.min(SECRET_PREFIX_LENGTH, value.length())));
        hashSetter.accept(passwordEncoder.encode(value));
        ciphertextSetter.accept(secretCipher.encrypt(value));
    }

    private MarketingMonitorProviderCredentialView toView(MarketingMonitorProviderCredentialDO row) {
        return new MarketingMonitorProviderCredentialView(
                row.getId(),
                row.getTenantId(),
                row.getCredentialKey(),
                row.getProviderType(),
                row.getAuthType(),
                row.getDisplayName(),
                row.getStatus(),
                row.getTokenType(),
                stringList(row.getScopesJson()),
                row.getAccessTokenPrefix(),
                row.getRefreshTokenPrefix(),
                row.getApiKeyPrefix(),
                row.getRefreshEndpoint(),
                row.getRevokeEndpoint(),
                row.getExpiresAt(),
                row.getRefreshTokenExpiresAt(),
                row.getRevokedAt(),
                row.getLastRefreshedAt(),
                defaultInt(row.getRefreshAttemptCount()),
                row.getLastRefreshStatus(),
                row.getLastRefreshError(),
                row.getLastRevokeStatus(),
                row.getLastRevokeError(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private MarketingMonitorProviderCredentialEventView toEventView(MarketingMonitorProviderCredentialEventDO row) {
        return new MarketingMonitorProviderCredentialEventView(
                row.getId(),
                row.getTenantId(),
                row.getCredentialId(),
                row.getCredentialKey(),
                row.getEventType(),
                row.getStatus(),
                map(row.getMetadataJson()),
                row.getErrorMessage(),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    private void ensureEnabled(MarketingMonitorProviderCredentialDO row) {
        if (STATUS_DISABLED.equals(normalizeOptionalUpper(row.getStatus()))) {
            throw new IllegalStateException("provider credential is disabled: " + row.getCredentialKey());
        }
    }

    private CredentialReference parseReference(String reference) {
        if (!hasText(reference)) {
            throw new IllegalArgumentException("credential reference is required");
        }
        String[] parts = reference.trim().split(":", 3);
        if (parts.length != 3 || !"credential".equals(parts[0])) {
            throw new IllegalArgumentException("credential reference must use credential:<key>:<field>");
        }
        return new CredentialReference(normalizeKey(parts[1], "credentialKey"),
                normalizeKey(parts[2], "field").replace('-', '_'));
    }

    private List<String> refreshScopes(JsonNode body) {
        String scope = text(body, "scope");
        if (hasText(scope)) {
            List<String> scopes = new ArrayList<>();
            for (String value : scope.trim().split("\\s+")) {
                if (hasText(value)) {
                    scopes.add(value.trim());
                }
            }
            return scopes;
        }
        JsonNode scopes = body.path("scopes");
        if (scopes.isArray()) {
            List<String> values = new ArrayList<>();
            scopes.forEach(node -> {
                if (hasText(node.asText())) {
                    values.add(node.asText().trim());
                }
            });
            return values;
        }
        return List.of();
    }

    private RevokeToken revokeToken(MarketingMonitorProviderCredentialDO row,
                                    MarketingMonitorProviderCredentialRevokeCommand command) {
        boolean preferRefreshToken = !Boolean.FALSE.equals(command.revokeRefreshToken());
        String token = preferRefreshToken ? secretCipher.decrypt(row.getRefreshTokenCiphertext()) : null;
        String defaultHint = "refresh_token";
        if (!hasText(token)) {
            token = secretCipher.decrypt(row.getAccessTokenCiphertext());
            defaultHint = "access_token";
        }
        if (!hasText(token)) {
            throw new IllegalStateException("credential token is required for revoke");
        }
        return new RevokeToken(token, defaultString(command.tokenTypeHint(), defaultHint));
    }

    private String revokeTokenTypeHint(MarketingMonitorProviderCredentialDO row,
                                       MarketingMonitorProviderCredentialRevokeCommand command) {
        return revokeToken(row, command).tokenTypeHint();
    }

    private Map<String, Object> revokeMetadata(MarketingMonitorProviderCredentialRevokeCommand command,
                                               MarketingMonitorProviderHttpResponse response,
                                               String tokenTypeHint) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (response != null) {
            metadata.put("httpStatus", response.statusCode());
            String requestId = response.headers().get("x-request-id");
            if (hasText(requestId)) {
                metadata.put("providerRequestId", requestId);
            }
        }
        if (hasText(tokenTypeHint)) {
            metadata.put("tokenTypeHint", tokenTypeHint);
        }
        sanitizedMetadata(command == null ? null : command.metadata())
                .forEach((key, value) -> metadata.put("request_" + key, value));
        return metadata;
    }

    private Map<String, Object> sanitizedMetadata(Map<String, Object> value) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        safeMap(value).forEach((key, metadataValue) -> {
            String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
            if (!normalized.contains("token")
                    && !normalized.contains("secret")
                    && !normalized.contains("password")
                    && !normalized.contains("api_key")) {
                metadata.put(key, metadataValue);
            }
        });
        return metadata;
    }

    private List<String> normalizedScopes(List<String> scopes) {
        if (scopes == null) {
            return List.of();
        }
        return scopes.stream()
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String form(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            first = false;
            builder.append(urlEncode(entry.getKey()))
                    .append("=")
                    .append(urlEncode(entry.getValue()));
        }
        return builder.toString();
    }

    private JsonNode jsonNode(String body) {
        try {
            return objectMapper.readTree(hasText(body) ? body : "{}");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("provider refresh response JSON parse failed", ex);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("provider credential JSON serialization failed", ex);
        }
    }

    private List<String> stringList(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asLong() : null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String validateOptionalUri(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        URI uri = URI.create(trimmed);
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(field + " must be an HTTP URL");
        }
        return trimmed;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeKey(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalKey(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeType(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeTokenType(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return "bearer".equalsIgnoreCase(trimmed) ? "Bearer" : trimmed;
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String defaultActor(String actor) {
        return hasText(actor) ? actor.trim() : "system";
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private int boundedWindowMinutes(Integer value) {
        int minutes = value == null ? 30 : value;
        if (minutes < 1) {
            return 1;
        }
        return Math.min(minutes, 24 * 60);
    }

    private String trimLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String message(RuntimeException ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CredentialReference(String credentialKey, String field) {
    }

    private record RevokeToken(String token, String tokenTypeHint) {
    }
}
