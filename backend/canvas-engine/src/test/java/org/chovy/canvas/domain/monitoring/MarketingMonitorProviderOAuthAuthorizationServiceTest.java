package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderOAuthAuthorizationDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderOAuthAuthorizationEventDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderOAuthAuthorizationEventMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderOAuthAuthorizationMapper;
import org.chovy.canvas.security.SecretCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitorProviderOAuthAuthorizationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T00:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final String CIPHER_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void startAuthorizationBuildsPkceUrlStoresEncryptedStateAndWritesEvent() {
        Fixture fixture = fixture(new FakeTransport(200, "{}"));
        doAnswer(invocation -> {
            invocation.<MarketingMonitorProviderOAuthAuthorizationDO>getArgument(0).setId(200L);
            return 1;
        }).when(fixture.authorizationMapper).insert(any(MarketingMonitorProviderOAuthAuthorizationDO.class));
        ArgumentCaptor<MarketingMonitorProviderOAuthAuthorizationDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderOAuthAuthorizationDO.class);
        ArgumentCaptor<MarketingMonitorProviderOAuthAuthorizationEventDO> eventCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderOAuthAuthorizationEventDO.class);

        MarketingMonitorProviderOAuthAuthorizationView view =
                fixture.service.startAuthorization(7L, command(), "operator-1");

        assertThat(view.id()).isEqualTo(200L);
        assertThat(view.credentialKey()).isEqualTo("x-prod");
        assertThat(view.providerType()).isEqualTo("X_RECENT_SEARCH");
        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.authState()).isNotBlank();
        assertThat(view.authorizationUrl()).startsWith("https://provider.example.test/oauth/authorize?");
        Map<String, String> query = query(view.authorizationUrl());
        assertThat(query).containsEntry("response_type", "code")
                .containsEntry("client_id", "client-id")
                .containsEntry("redirect_uri", "https://canvas.example.test/oauth/callback")
                .containsEntry("scope", "tweet.read users.read")
                .containsEntry("state", view.authState())
                .containsEntry("code_challenge_method", "S256")
                .containsEntry("access_type", "offline")
                .containsEntry("prompt", "consent");
        verify(fixture.authorizationMapper).insert(rowCaptor.capture());
        MarketingMonitorProviderOAuthAuthorizationDO row = rowCaptor.getValue();
        assertThat(row.getAuthState()).isEqualTo(view.authState());
        assertThat(fixture.cipher.decrypt(row.getClientIdCiphertext())).isEqualTo("client-id");
        assertThat(fixture.cipher.decrypt(row.getClientSecretCiphertext())).isEqualTo("client-secret");
        String verifier = fixture.cipher.decrypt(row.getCodeVerifierCiphertext());
        assertThat(verifier).hasSizeGreaterThanOrEqualTo(43);
        assertThat(row.getCodeChallenge()).isEqualTo(s256(verifier));
        assertThat(row.getExpiresAt()).isEqualTo(now().plusMinutes(20));
        assertThat(view.toString()).doesNotContain("client-secret", verifier);
        verify(fixture.eventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("STARTED");
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void completeAuthorizationExchangesCodeUpsertsCredentialAndMarksCompleted() {
        FakeTransport transport = new FakeTransport(200, """
                {
                  "access_token": "fresh-access-token",
                  "refresh_token": "fresh-refresh-token",
                  "token_type": "bearer",
                  "expires_in": 3600,
                  "scope": "tweet.read users.read"
                }
                """);
        Fixture fixture = fixture(transport);
        MarketingMonitorProviderOAuthAuthorizationDO existing = authorization("state-1");
        when(fixture.authorizationMapper.selectOne(any())).thenReturn(existing);
        when(fixture.credentialService.upsert(eq(7L), any(MarketingMonitorProviderCredentialCommand.class),
                eq("operator-1"))).thenReturn(credentialView());
        ArgumentCaptor<MarketingMonitorProviderCredentialCommand> credentialCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialCommand.class);
        ArgumentCaptor<MarketingMonitorProviderOAuthAuthorizationDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderOAuthAuthorizationDO.class);
        ArgumentCaptor<MarketingMonitorProviderOAuthAuthorizationEventDO> eventCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderOAuthAuthorizationEventDO.class);

        MarketingMonitorProviderOAuthAuthorizationView view = fixture.service.completeAuthorization(7L,
                new MarketingMonitorProviderOAuthCallbackCommand(
                        "state-1",
                        "auth-code",
                        null,
                        null,
                        Map.of("providerRequestId", "cb-1")),
                "operator-1");

        assertThat(transport.lastRequest().method()).isEqualTo("POST");
        assertThat(transport.lastRequest().uri().toString()).isEqualTo("https://provider.example.test/oauth/token");
        Map<String, String> body = form(transport.lastRequest().body());
        assertThat(body).containsEntry("grant_type", "authorization_code")
                .containsEntry("code", "auth-code")
                .containsEntry("redirect_uri", "https://canvas.example.test/oauth/callback")
                .containsEntry("client_id", "client-id")
                .containsEntry("client_secret", "client-secret")
                .containsEntry("code_verifier", "test-code-verifier");
        verify(fixture.credentialService).upsert(eq(7L), credentialCaptor.capture(), eq("operator-1"));
        MarketingMonitorProviderCredentialCommand credential = credentialCaptor.getValue();
        assertThat(credential.credentialKey()).isEqualTo("x-prod");
        assertThat(credential.accessToken()).isEqualTo("fresh-access-token");
        assertThat(credential.refreshToken()).isEqualTo("fresh-refresh-token");
        assertThat(credential.tokenType()).isEqualTo("Bearer");
        assertThat(credential.scopes()).containsExactly("tweet.read", "users.read");
        assertThat(credential.expiresAt()).isEqualTo(now().plusHours(1));
        assertThat(credential.refreshEndpoint()).isEqualTo("https://provider.example.test/oauth/token");
        assertThat(credential.metadata()).containsEntry("oauthAuthorizationId", 200L);
        verify(fixture.authorizationMapper).updateById(rowCaptor.capture());
        MarketingMonitorProviderOAuthAuthorizationDO row = rowCaptor.getValue();
        assertThat(row.getStatus()).isEqualTo("EXCHANGED");
        assertThat(row.getCredentialId()).isEqualTo(100L);
        assertThat(row.getCompletedAt()).isEqualTo(now());
        assertThat(row.getLastHttpStatus()).isEqualTo(200);
        assertThat(view.status()).isEqualTo("EXCHANGED");
        assertThat(view.credentialId()).isEqualTo(100L);
        assertThat(view.toString()).doesNotContain("fresh-access-token", "fresh-refresh-token", "auth-code");
        verify(fixture.eventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("EXCHANGED");
        assertThat(eventCaptor.getValue().getMetadataJson()).doesNotContain("fresh-access-token", "auth-code");
    }

    @Test
    void completeAuthorizationProviderErrorMarksFailedWithoutTokenExchange() {
        FakeTransport transport = new FakeTransport(200, "{}");
        Fixture fixture = fixture(transport);
        MarketingMonitorProviderOAuthAuthorizationDO existing = authorization("state-1");
        when(fixture.authorizationMapper.selectOne(any())).thenReturn(existing);
        ArgumentCaptor<MarketingMonitorProviderOAuthAuthorizationDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderOAuthAuthorizationDO.class);

        MarketingMonitorProviderOAuthAuthorizationView view = fixture.service.completeAuthorization(7L,
                new MarketingMonitorProviderOAuthCallbackCommand(
                        "state-1",
                        null,
                        "access_denied",
                        "operator denied consent",
                        Map.of()),
                "operator-1");

        assertThat(transport.lastRequest()).isNull();
        verify(fixture.credentialService, never()).upsert(any(), any(), any());
        verify(fixture.authorizationMapper).updateById(rowCaptor.capture());
        assertThat(rowCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(rowCaptor.getValue().getProviderError()).isEqualTo("access_denied");
        assertThat(rowCaptor.getValue().getProviderErrorDescription()).isEqualTo("operator denied consent");
        assertThat(view.status()).isEqualTo("FAILED");
    }

    @Test
    void completeAuthorizationExpiredStateMarksExpiredWithoutTokenExchange() {
        FakeTransport transport = new FakeTransport(200, "{}");
        Fixture fixture = fixture(transport);
        MarketingMonitorProviderOAuthAuthorizationDO existing = authorization("state-1");
        existing.setExpiresAt(now().minusMinutes(1));
        when(fixture.authorizationMapper.selectOne(any())).thenReturn(existing);
        ArgumentCaptor<MarketingMonitorProviderOAuthAuthorizationDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderOAuthAuthorizationDO.class);

        MarketingMonitorProviderOAuthAuthorizationView view = fixture.service.completeAuthorization(7L,
                new MarketingMonitorProviderOAuthCallbackCommand(
                        "state-1",
                        "auth-code",
                        null,
                        null,
                        Map.of()),
                "operator-1");

        assertThat(transport.lastRequest()).isNull();
        verify(fixture.credentialService, never()).upsert(any(), any(), any());
        verify(fixture.authorizationMapper).updateById(rowCaptor.capture());
        assertThat(rowCaptor.getValue().getStatus()).isEqualTo("EXPIRED");
        assertThat(rowCaptor.getValue().getLastErrorMessage()).contains("expired");
        assertThat(view.status()).isEqualTo("EXPIRED");
    }

    private Fixture fixture(FakeTransport transport) {
        MarketingMonitorProviderOAuthAuthorizationMapper authorizationMapper =
                mock(MarketingMonitorProviderOAuthAuthorizationMapper.class);
        MarketingMonitorProviderOAuthAuthorizationEventMapper eventMapper =
                mock(MarketingMonitorProviderOAuthAuthorizationEventMapper.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        SecretCipher cipher = SecretCipher.fromBase64Key(CIPHER_KEY);
        return new Fixture(
                authorizationMapper,
                eventMapper,
                credentialService,
                cipher,
                new MarketingMonitorProviderOAuthAuthorizationService(
                        authorizationMapper,
                        eventMapper,
                        credentialService,
                        transport,
                        new ObjectMapper(),
                        cipher,
                        CLOCK));
    }

    private MarketingMonitorProviderOAuthAuthorizationCommand command() {
        return new MarketingMonitorProviderOAuthAuthorizationCommand(
                " X-Prod ",
                "x_recent_search",
                "oauth2_bearer",
                "X Production",
                "https://provider.example.test/oauth/authorize",
                "https://provider.example.test/oauth/token",
                "https://provider.example.test/oauth/revoke",
                "https://canvas.example.test/oauth/callback",
                "client-id",
                "client-secret",
                List.of("tweet.read", "users.read"),
                Map.of("access_type", "offline", "prompt", "consent"),
                20,
                Map.of("owner", "brand-team"));
    }

    private MarketingMonitorProviderOAuthAuthorizationDO authorization(String state) {
        SecretCipher cipher = SecretCipher.fromBase64Key(CIPHER_KEY);
        MarketingMonitorProviderOAuthAuthorizationDO row = new MarketingMonitorProviderOAuthAuthorizationDO();
        row.setId(200L);
        row.setTenantId(7L);
        row.setAuthState(state);
        row.setCredentialKey("x-prod");
        row.setProviderType("X_RECENT_SEARCH");
        row.setAuthType("OAUTH2_BEARER");
        row.setDisplayName("X Production");
        row.setStatus("PENDING");
        row.setAuthorizeEndpoint("https://provider.example.test/oauth/authorize");
        row.setTokenEndpoint("https://provider.example.test/oauth/token");
        row.setRevokeEndpoint("https://provider.example.test/oauth/revoke");
        row.setRedirectUri("https://canvas.example.test/oauth/callback");
        row.setClientIdCiphertext(cipher.encrypt("client-id"));
        row.setClientSecretCiphertext(cipher.encrypt("client-secret"));
        row.setScopesJson("[\"tweet.read\",\"users.read\"]");
        row.setCodeVerifierCiphertext(cipher.encrypt("test-code-verifier"));
        row.setCodeChallenge(s256("test-code-verifier"));
        row.setCodeChallengeMethod("S256");
        row.setAuthorizeParamsJson("{\"access_type\":\"offline\"}");
        row.setExpiresAt(now().plusMinutes(5));
        row.setMetadataJson("{\"owner\":\"brand-team\"}");
        row.setCreatedBy("operator-1");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private MarketingMonitorProviderCredentialView credentialView() {
        return new MarketingMonitorProviderCredentialView(
                100L,
                7L,
                "x-prod",
                "X_RECENT_SEARCH",
                "OAUTH2_BEARER",
                "X Production",
                "ACTIVE",
                "Bearer",
                List.of("tweet.read", "users.read"),
                "fresh-access",
                "fresh-refresh",
                null,
                "https://provider.example.test/oauth/token",
                "https://provider.example.test/oauth/revoke",
                now().plusHours(1),
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                Map.of("owner", "brand-team"),
                "operator-1",
                "operator-1",
                now(),
                now());
    }

    private Map<String, String> query(String url) {
        return form(url.substring(url.indexOf('?') + 1));
    }

    private Map<String, String> form(String body) {
        Map<String, String> values = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts.length > 1 ? parts[1] : "", StandardCharsets.UTF_8));
        }
        return values;
    }

    private static String s256(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 8, 0);
    }

    private static class FakeTransport implements MarketingMonitorProviderHttpTransport {
        private final int statusCode;
        private final String body;
        private MarketingMonitorProviderHttpRequest lastRequest;

        private FakeTransport(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public MarketingMonitorProviderHttpResponse execute(MarketingMonitorProviderHttpRequest request) {
            this.lastRequest = request;
            return new MarketingMonitorProviderHttpResponse(statusCode, body, Map.of("x-request-id", "req-1"));
        }

        private MarketingMonitorProviderHttpRequest lastRequest() {
            return lastRequest;
        }
    }

    private record Fixture(MarketingMonitorProviderOAuthAuthorizationMapper authorizationMapper,
                           MarketingMonitorProviderOAuthAuthorizationEventMapper eventMapper,
                           MarketingMonitorProviderCredentialService credentialService,
                           SecretCipher cipher,
                           MarketingMonitorProviderOAuthAuthorizationService service) {
    }
}
