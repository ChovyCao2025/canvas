package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderOAuthAuthorizationDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderOAuthAuthorizationEventDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderOAuthAuthorizationEventMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderOAuthAuthorizationMapper;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MarketingMonitorProviderOAuthAuthorizationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_EXCHANGED = "EXCHANGED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String CODE_CHALLENGE_METHOD = "S256";
    private static final Set<String> RESERVED_AUTHORIZE_PARAMS = Set.of(
            "response_type",
            "client_id",
            "redirect_uri",
            "scope",
            "state",
            "code_challenge",
            "code_challenge_method");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final MarketingMonitorProviderOAuthAuthorizationMapper authorizationMapper;
    private final MarketingMonitorProviderOAuthAuthorizationEventMapper eventMapper;
    private final MarketingMonitorProviderCredentialService credentialService;
    private final MarketingMonitorProviderHttpTransport tokenTransport;
    private final ObjectMapper objectMapper;
    private final SecretCipher secretCipher;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public MarketingMonitorProviderOAuthAuthorizationService(
            MarketingMonitorProviderOAuthAuthorizationMapper authorizationMapper,
            MarketingMonitorProviderOAuthAuthorizationEventMapper eventMapper,
            MarketingMonitorProviderCredentialService credentialService,
            MarketingMonitorProviderHttpTransport tokenTransport,
            ObjectMapper objectMapper,
            SecretCipher secretCipher) {
        this(authorizationMapper, eventMapper, credentialService, tokenTransport, objectMapper, secretCipher,
                Clock.systemDefaultZone());
    }

    MarketingMonitorProviderOAuthAuthorizationService(
            MarketingMonitorProviderOAuthAuthorizationMapper authorizationMapper,
            MarketingMonitorProviderOAuthAuthorizationEventMapper eventMapper,
            MarketingMonitorProviderCredentialService credentialService,
            MarketingMonitorProviderHttpTransport tokenTransport,
            ObjectMapper objectMapper,
            SecretCipher secretCipher,
            Clock clock) {
        this.authorizationMapper = authorizationMapper;
        this.eventMapper = eventMapper;
        this.credentialService = credentialService;
        this.tokenTransport = tokenTransport;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.secretCipher = secretCipher;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderOAuthAuthorizationView startAuthorization(
            Long tenantId,
            MarketingMonitorProviderOAuthAuthorizationCommand command,
            String actor) {
        if (command == null) {
            throw new IllegalArgumentException("oauth authorization command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String state = randomToken();
        String verifier = randomToken();
        String challenge = s256(verifier);
        LocalDateTime changedAt = now();
        MarketingMonitorProviderOAuthAuthorizationDO row = new MarketingMonitorProviderOAuthAuthorizationDO();
        row.setTenantId(scopedTenantId);
        row.setAuthState(state);
        row.setCredentialKey(normalizeKey(command.credentialKey(), "credentialKey"));
        row.setProviderType(normalizeType(command.providerType(), "providerType"));
        row.setAuthType(normalizeType(command.authType(), "authType"));
        row.setDisplayName(defaultString(command.displayName(), row.getCredentialKey()));
        row.setStatus(STATUS_PENDING);
        row.setAuthorizeEndpoint(validateHttpUrl(command.authorizeEndpoint(), "authorizeEndpoint"));
        row.setTokenEndpoint(validateHttpUrl(command.tokenEndpoint(), "tokenEndpoint"));
        row.setRevokeEndpoint(validateOptionalHttpUrl(command.revokeEndpoint(), "revokeEndpoint"));
        row.setRedirectUri(validateHttpUrl(command.redirectUri(), "redirectUri"));
        row.setClientIdCiphertext(secretCipher.encrypt(required(command.clientId(), "clientId")));
        row.setClientSecretCiphertext(hasText(command.clientSecret()) ? secretCipher.encrypt(command.clientSecret().trim()) : null);
        row.setScopesJson(json(normalizedScopes(command.scopes())));
        row.setCodeVerifierCiphertext(secretCipher.encrypt(verifier));
        row.setCodeChallenge(challenge);
        row.setCodeChallengeMethod(CODE_CHALLENGE_METHOD);
        row.setAuthorizeParamsJson(json(validatedAuthorizeParams(command.authorizeParams())));
        row.setExpiresAt(changedAt.plusMinutes(boundedExpires(command.expiresInMinutes())));
        row.setMetadataJson(json(safeMap(command.metadata())));
        row.setCreatedBy(defaultActor(actor));
        row.setUpdatedBy(defaultActor(actor));
        row.setCreatedAt(changedAt);
        row.setUpdatedAt(changedAt);
        authorizationMapper.insert(row);
        writeEvent(scopedTenantId, row, "STARTED", STATUS_PENDING, Map.of(
                "providerType", row.getProviderType(),
                "authType", row.getAuthType(),
                "expiresAt", string(row.getExpiresAt())), null, actor);
        return toView(row, authorizationUrl(row));
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderOAuthAuthorizationView completeAuthorization(
            Long tenantId,
            MarketingMonitorProviderOAuthCallbackCommand command,
            String actor) {
        if (command == null) {
            throw new IllegalArgumentException("oauth callback command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderOAuthAuthorizationDO row =
                requiredAuthorization(scopedTenantId, command.state());
        if (!STATUS_PENDING.equals(row.getStatus())) {
            row.setLastErrorMessage("oauth authorization state is not pending");
            return fail(row, scopedTenantId, "DUPLICATE_CALLBACK", STATUS_FAILED, row.getLastErrorMessage(),
                    safeMap(command.metadata()), actor);
        }
        if (row.getExpiresAt() != null && row.getExpiresAt().isBefore(now())) {
            return fail(row, scopedTenantId, "EXPIRED", STATUS_EXPIRED,
                    "oauth authorization state expired", safeMap(command.metadata()), actor);
        }
        if (hasText(command.error())) {
            row.setProviderError(command.error().trim());
            row.setProviderErrorDescription(trimLength(command.errorDescription(), 1000));
            return fail(row, scopedTenantId, "CALLBACK_FAILED", STATUS_FAILED,
                    defaultString(command.errorDescription(), command.error()),
                    safeMap(command.metadata()), actor);
        }
        String code = required(command.code(), "code");
        try {
            MarketingMonitorProviderHttpResponse response = tokenTransport.execute(tokenRequest(row, code));
            row.setLastHttpStatus(response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fail(row, scopedTenantId, "TOKEN_EXCHANGE_FAILED", STATUS_FAILED,
                        "provider token exchange failed with status " + response.statusCode(),
                        eventMetadata(command.metadata(), response, null), actor);
            }
            JsonNode body = jsonNode(response.body());
            String accessToken = text(body, "access_token");
            if (!hasText(accessToken)) {
                return fail(row, scopedTenantId, "TOKEN_EXCHANGE_FAILED", STATUS_FAILED,
                        "provider token response did not include access_token",
                        eventMetadata(command.metadata(), response, null), actor);
            }
            MarketingMonitorProviderCredentialCommand credentialCommand =
                    credentialCommand(row, body, accessToken, command.metadata());
            MarketingMonitorProviderCredentialView credential =
                    credentialService.upsert(scopedTenantId, credentialCommand, actor);
            LocalDateTime changedAt = now();
            row.setStatus(STATUS_EXCHANGED);
            row.setCredentialId(credential.id());
            row.setTokenType(credentialCommand.tokenType());
            row.setLastErrorMessage(null);
            row.setCompletedAt(changedAt);
            row.setUpdatedBy(defaultActor(actor));
            row.setUpdatedAt(changedAt);
            authorizationMapper.updateById(row);
            writeEvent(scopedTenantId, row, "EXCHANGED", "SUCCESS",
                    eventMetadata(command.metadata(), response, credential.id()), null, actor);
            return toView(row, null);
        } catch (RuntimeException ex) {
            return fail(row, scopedTenantId, "TOKEN_EXCHANGE_FAILED", STATUS_FAILED,
                    message(ex), safeMap(command.metadata()), actor);
        }
    }

    public List<MarketingMonitorProviderOAuthAuthorizationView> list(
            Long tenantId,
            MarketingMonitorProviderOAuthAuthorizationQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderOAuthAuthorizationQuery effectiveQuery = query == null
                ? new MarketingMonitorProviderOAuthAuthorizationQuery(null, null, null, 50)
                : query;
        int limit = boundedLimit(effectiveQuery.limit());
        String credentialKey = normalizeOptionalKey(effectiveQuery.credentialKey());
        String providerType = normalizeOptionalUpper(effectiveQuery.providerType());
        String status = normalizeOptionalUpper(effectiveQuery.status());
        return safeList(authorizationMapper.selectList(new LambdaQueryWrapper<MarketingMonitorProviderOAuthAuthorizationDO>()
                        .eq(MarketingMonitorProviderOAuthAuthorizationDO::getTenantId, scopedTenantId)
                        .eq(credentialKey != null,
                                MarketingMonitorProviderOAuthAuthorizationDO::getCredentialKey, credentialKey)
                        .eq(providerType != null,
                                MarketingMonitorProviderOAuthAuthorizationDO::getProviderType, providerType)
                        .eq(status != null, MarketingMonitorProviderOAuthAuthorizationDO::getStatus, status)
                        .orderByDesc(MarketingMonitorProviderOAuthAuthorizationDO::getCreatedAt)
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> credentialKey == null || credentialKey.equals(row.getCredentialKey()))
                .filter(row -> providerType == null || providerType.equals(row.getProviderType()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(row -> toView(row, null))
                .toList();
    }

    public List<MarketingMonitorProviderOAuthAuthorizationEventView> events(
            Long tenantId,
            MarketingMonitorProviderOAuthAuthorizationEventQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderOAuthAuthorizationEventQuery effectiveQuery = query == null
                ? new MarketingMonitorProviderOAuthAuthorizationEventQuery(null, null, null, null, 50)
                : query;
        int limit = boundedLimit(effectiveQuery.limit());
        String authState = trimToNull(effectiveQuery.authState());
        String credentialKey = normalizeOptionalKey(effectiveQuery.credentialKey());
        String eventType = normalizeOptionalUpper(effectiveQuery.eventType());
        String status = normalizeOptionalUpper(effectiveQuery.status());
        return safeList(eventMapper.selectList(new LambdaQueryWrapper<MarketingMonitorProviderOAuthAuthorizationEventDO>()
                        .eq(MarketingMonitorProviderOAuthAuthorizationEventDO::getTenantId, scopedTenantId)
                        .eq(authState != null,
                                MarketingMonitorProviderOAuthAuthorizationEventDO::getAuthState, authState)
                        .eq(credentialKey != null,
                                MarketingMonitorProviderOAuthAuthorizationEventDO::getCredentialKey, credentialKey)
                        .eq(eventType != null,
                                MarketingMonitorProviderOAuthAuthorizationEventDO::getEventType, eventType)
                        .eq(status != null,
                                MarketingMonitorProviderOAuthAuthorizationEventDO::getStatus, status)
                        .orderByDesc(MarketingMonitorProviderOAuthAuthorizationEventDO::getCreatedAt)
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> authState == null || authState.equals(row.getAuthState()))
                .filter(row -> credentialKey == null || credentialKey.equals(row.getCredentialKey()))
                .filter(row -> eventType == null || eventType.equals(row.getEventType()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toEventView)
                .toList();
    }

    private MarketingMonitorProviderOAuthAuthorizationView fail(
            MarketingMonitorProviderOAuthAuthorizationDO row,
            Long tenantId,
            String eventType,
            String status,
            String error,
            Map<String, Object> metadata,
            String actor) {
        LocalDateTime changedAt = now();
        row.setStatus(status);
        row.setLastErrorMessage(trimLength(error, 1000));
        row.setCompletedAt(changedAt);
        row.setUpdatedBy(defaultActor(actor));
        row.setUpdatedAt(changedAt);
        authorizationMapper.updateById(row);
        writeEvent(tenantId, row, eventType, status, metadata, row.getLastErrorMessage(), actor);
        return toView(row, null);
    }

    private MarketingMonitorProviderCredentialCommand credentialCommand(
            MarketingMonitorProviderOAuthAuthorizationDO row,
            JsonNode body,
            String accessToken,
            Map<String, Object> callbackMetadata) {
        List<String> scopes = tokenScopes(body);
        if (scopes.isEmpty()) {
            scopes = stringList(row.getScopesJson());
        }
        Long expiresIn = longValue(body, "expires_in");
        Map<String, Object> metadata = new LinkedHashMap<>(map(row.getMetadataJson()));
        metadata.put("oauthAuthorizationId", row.getId());
        metadata.put("oauthState", row.getAuthState());
        safeMap(callbackMetadata).forEach((key, value) -> metadata.put("callback_" + key, value));
        return new MarketingMonitorProviderCredentialCommand(
                row.getCredentialKey(),
                row.getProviderType(),
                row.getAuthType(),
                row.getDisplayName(),
                true,
                accessToken,
                text(body, "refresh_token"),
                null,
                normalizeTokenType(defaultString(text(body, "token_type"), "Bearer")),
                scopes,
                expiresIn == null || expiresIn <= 0 ? null : now().plusSeconds(expiresIn),
                null,
                row.getTokenEndpoint(),
                row.getRevokeEndpoint(),
                secretCipher.decrypt(row.getClientIdCiphertext()),
                secretCipher.decrypt(row.getClientSecretCiphertext()),
                metadata);
    }

    private MarketingMonitorProviderHttpRequest tokenRequest(
            MarketingMonitorProviderOAuthAuthorizationDO row,
            String code) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", row.getRedirectUri());
        form.put("client_id", secretCipher.decrypt(row.getClientIdCiphertext()));
        form.put("code_verifier", secretCipher.decrypt(row.getCodeVerifierCiphertext()));
        String clientSecret = secretCipher.decrypt(row.getClientSecretCiphertext());
        if (hasText(clientSecret)) {
            form.put("client_secret", clientSecret);
        }
        return new MarketingMonitorProviderHttpRequest(
                "POST",
                URI.create(row.getTokenEndpoint()),
                Map.of(
                        "Content-Type", "application/x-www-form-urlencoded",
                        "Accept", "application/json"),
                form(form));
    }

    private MarketingMonitorProviderOAuthAuthorizationDO requiredAuthorization(Long tenantId, String state) {
        String authState = required(state, "state");
        MarketingMonitorProviderOAuthAuthorizationDO row = authorizationMapper.selectOne(
                new LambdaQueryWrapper<MarketingMonitorProviderOAuthAuthorizationDO>()
                        .eq(MarketingMonitorProviderOAuthAuthorizationDO::getTenantId, tenantId)
                        .eq(MarketingMonitorProviderOAuthAuthorizationDO::getAuthState, authState)
                        .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("oauth authorization state is not found");
        }
        return row;
    }

    private String authorizationUrl(MarketingMonitorProviderOAuthAuthorizationDO row) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", secretCipher.decrypt(row.getClientIdCiphertext()));
        params.put("redirect_uri", row.getRedirectUri());
        String scope = String.join(" ", stringList(row.getScopesJson()));
        if (hasText(scope)) {
            params.put("scope", scope);
        }
        params.put("state", row.getAuthState());
        params.put("code_challenge", row.getCodeChallenge());
        params.put("code_challenge_method", CODE_CHALLENGE_METHOD);
        map(row.getAuthorizeParamsJson()).forEach((key, value) -> {
            if (value != null && hasText(String.valueOf(value))) {
                params.put(key, String.valueOf(value));
            }
        });
        String separator = row.getAuthorizeEndpoint().contains("?") ? "&" : "?";
        return row.getAuthorizeEndpoint() + separator + form(params);
    }

    private void writeEvent(Long tenantId,
                            MarketingMonitorProviderOAuthAuthorizationDO row,
                            String eventType,
                            String status,
                            Map<String, Object> metadata,
                            String error,
                            String actor) {
        MarketingMonitorProviderOAuthAuthorizationEventDO event =
                new MarketingMonitorProviderOAuthAuthorizationEventDO();
        event.setTenantId(tenantId);
        event.setAuthorizationId(row.getId());
        event.setAuthState(row.getAuthState());
        event.setCredentialKey(row.getCredentialKey());
        event.setEventType(eventType);
        event.setStatus(status);
        event.setMetadataJson(json(safeMap(metadata)));
        event.setErrorMessage(trimLength(error, 1000));
        event.setCreatedBy(defaultActor(actor));
        event.setCreatedAt(now());
        eventMapper.insert(event);
    }

    private MarketingMonitorProviderOAuthAuthorizationView toView(
            MarketingMonitorProviderOAuthAuthorizationDO row,
            String authorizationUrl) {
        return new MarketingMonitorProviderOAuthAuthorizationView(
                row.getId(),
                row.getTenantId(),
                row.getAuthState(),
                row.getCredentialKey(),
                row.getProviderType(),
                row.getAuthType(),
                row.getDisplayName(),
                row.getStatus(),
                authorizationUrl,
                row.getAuthorizeEndpoint(),
                row.getTokenEndpoint(),
                row.getRedirectUri(),
                stringList(row.getScopesJson()),
                row.getCodeChallengeMethod(),
                row.getCredentialId(),
                row.getProviderError(),
                row.getProviderErrorDescription(),
                row.getLastHttpStatus(),
                row.getLastErrorMessage(),
                row.getExpiresAt(),
                row.getCompletedAt(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private MarketingMonitorProviderOAuthAuthorizationEventView toEventView(
            MarketingMonitorProviderOAuthAuthorizationEventDO row) {
        return new MarketingMonitorProviderOAuthAuthorizationEventView(
                row.getId(),
                row.getTenantId(),
                row.getAuthorizationId(),
                row.getAuthState(),
                row.getCredentialKey(),
                row.getEventType(),
                row.getStatus(),
                map(row.getMetadataJson()),
                row.getErrorMessage(),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    private Map<String, Object> eventMetadata(Map<String, Object> callbackMetadata,
                                              MarketingMonitorProviderHttpResponse response,
                                              Long credentialId) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (response != null) {
            result.put("httpStatus", response.statusCode());
            String requestId = response.headers().get("x-request-id");
            if (hasText(requestId)) {
                result.put("providerRequestId", requestId);
            }
        }
        if (credentialId != null) {
            result.put("credentialId", credentialId);
        }
        safeMap(callbackMetadata).forEach((key, value) -> result.put("callback_" + key, value));
        return result;
    }

    private Map<String, Object> validatedAuthorizeParams(Map<String, Object> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        safeMap(values).forEach((key, value) -> {
            String normalized = normalizeKey(key, "authorizeParam").replace('-', '_');
            if (RESERVED_AUTHORIZE_PARAMS.contains(normalized)) {
                throw new IllegalArgumentException("authorizeParams cannot override reserved OAuth param: " + key);
            }
            if (value != null && hasText(String.valueOf(value))) {
                result.put(key.trim(), String.valueOf(value).trim());
            }
        });
        return result;
    }

    private List<String> tokenScopes(JsonNode body) {
        String scope = text(body, "scope");
        if (!hasText(scope)) {
            return List.of();
        }
        List<String> scopes = new ArrayList<>();
        for (String value : scope.trim().split("\\s+")) {
            if (hasText(value)) {
                scopes.add(value.trim());
            }
        }
        return scopes;
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

    private JsonNode jsonNode(String body) {
        try {
            return objectMapper.readTree(hasText(body) ? body : "{}");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("provider token response JSON parse failed", ex);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("oauth authorization JSON serialization failed", ex);
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

    private String s256(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to generate PKCE challenge", ex);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asLong() : null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String validateHttpUrl(String value, String field) {
        String trimmed = required(value, field);
        URI uri = URI.create(trimmed);
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(field + " must be an HTTP URL");
        }
        return trimmed;
    }

    private String validateOptionalHttpUrl(String value, String field) {
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

    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String trimLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String defaultActor(String actor) {
        return hasText(actor) ? actor.trim() : "system";
    }

    private int boundedExpires(Integer value) {
        int minutes = value == null ? 15 : value;
        if (minutes < 5) {
            return 5;
        }
        return Math.min(minutes, 60);
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
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
}
