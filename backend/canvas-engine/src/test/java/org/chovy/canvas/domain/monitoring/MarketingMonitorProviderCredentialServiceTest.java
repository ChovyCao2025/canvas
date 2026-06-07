package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialEventDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialEventMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.security.SecretCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitorProviderCredentialServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T00:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final String CIPHER_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void upsertEncryptsSecretsReturnsSanitizedViewAndWritesEvent() {
        Fixture fixture = fixture(new FakeTransport(200, "{}"));
        when(fixture.credentialMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<MarketingMonitorProviderCredentialDO>getArgument(0).setId(100L);
            return 1;
        }).when(fixture.credentialMapper).insert(any(MarketingMonitorProviderCredentialDO.class));
        ArgumentCaptor<MarketingMonitorProviderCredentialDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialDO.class);
        ArgumentCaptor<MarketingMonitorProviderCredentialEventDO> eventCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialEventDO.class);

        MarketingMonitorProviderCredentialView view = fixture.service.upsert(7L, command(), "operator-1");

        assertThat(view.id()).isEqualTo(100L);
        assertThat(view.credentialKey()).isEqualTo("x-prod");
        assertThat(view.providerType()).isEqualTo("X_RECENT_SEARCH");
        assertThat(view.authType()).isEqualTo("OAUTH2_BEARER");
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.accessTokenPrefix()).isEqualTo("xoxb-secret-");
        assertThat(view.refreshTokenPrefix()).isEqualTo("refresh-secr");
        assertThat(view.apiKeyPrefix()).isNull();
        assertThat(view.metadata()).containsEntry("owner", "brand-team");
        assertThat(view.toString()).doesNotContain("xoxb-secret-token", "refresh-secret-token", "client-secret");
        verify(fixture.credentialMapper).insert(rowCaptor.capture());
        MarketingMonitorProviderCredentialDO row = rowCaptor.getValue();
        assertThat(row.getAccessTokenCiphertext()).startsWith("v1:");
        assertThat(row.getRefreshTokenCiphertext()).startsWith("v1:");
        assertThat(row.getClientIdCiphertext()).startsWith("v1:");
        assertThat(row.getClientSecretCiphertext()).startsWith("v1:");
        assertThat(row.getAccessTokenCiphertext()).doesNotContain("xoxb-secret-token");
        assertThat(fixture.cipher.decrypt(row.getAccessTokenCiphertext())).isEqualTo("xoxb-secret-token");
        verify(fixture.eventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("UPSERTED");
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void upsertWithoutNewSecretsPreservesExistingEncryptedValues() {
        Fixture fixture = fixture(new FakeTransport(200, "{}"));
        MarketingMonitorProviderCredentialDO existing = credential(7L, 100L);
        when(fixture.credentialMapper.selectOne(any())).thenReturn(existing);
        ArgumentCaptor<MarketingMonitorProviderCredentialDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialDO.class);

        MarketingMonitorProviderCredentialView view = fixture.service.upsert(7L,
                new MarketingMonitorProviderCredentialCommand(
                        "x-prod",
                        "x_recent_search",
                        "oauth2_bearer",
                        "Renamed X",
                        true,
                        null,
                        null,
                        null,
                        "Bearer",
                        List.of("tweet.read"),
                        now().plusHours(2),
                        now().plusDays(10),
                        "https://api.x.com/2/oauth2/token",
                        null,
                        null,
                        null,
                        Map.of("owner", "ops-team")),
                "operator-2");

        verify(fixture.credentialMapper).updateById(rowCaptor.capture());
        MarketingMonitorProviderCredentialDO row = rowCaptor.getValue();
        assertThat(row.getAccessTokenCiphertext()).isEqualTo(existing.getAccessTokenCiphertext());
        assertThat(row.getRefreshTokenCiphertext()).isEqualTo(existing.getRefreshTokenCiphertext());
        assertThat(row.getClientSecretCiphertext()).isEqualTo(existing.getClientSecretCiphertext());
        assertThat(view.displayName()).isEqualTo("Renamed X");
        assertThat(view.metadata()).containsEntry("owner", "ops-team");
    }

    @Test
    void disablePreventsResolutionAndWritesEvent() {
        Fixture fixture = fixture(new FakeTransport(200, "{}"));
        MarketingMonitorProviderCredentialDO existing = credential(7L, 100L);
        when(fixture.credentialMapper.selectOne(any())).thenReturn(existing);
        when(fixture.credentialMapper.selectById(100L)).thenReturn(existing);
        ArgumentCaptor<MarketingMonitorProviderCredentialDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialDO.class);

        MarketingMonitorProviderCredentialView view = fixture.service.disable(7L, "x-prod", "operator-1");

        assertThat(view.status()).isEqualTo("DISABLED");
        verify(fixture.credentialMapper).updateById(rowCaptor.capture());
        assertThat(rowCaptor.getValue().getStatus()).isEqualTo("DISABLED");
        assertThatThrownBy(() -> fixture.service.resolveValue(7L, "credential:x-prod:access_token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void refreshCredentialUpdatesAccessTokenExpiryAndEventWithoutLeakingSecrets() {
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
        MarketingMonitorProviderCredentialDO existing = credential(7L, 100L);
        when(fixture.credentialMapper.selectOne(any())).thenReturn(existing);
        ArgumentCaptor<MarketingMonitorProviderCredentialDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialDO.class);
        ArgumentCaptor<MarketingMonitorProviderCredentialEventDO> eventCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialEventDO.class);

        MarketingMonitorProviderCredentialView view = fixture.service.refresh(7L, "x-prod",
                new MarketingMonitorProviderCredentialRefreshCommand(true), "operator-1");

        assertThat(transport.lastRequest().method()).isEqualTo("POST");
        assertThat(transport.lastRequest().uri().toString()).isEqualTo("https://api.x.com/2/oauth2/token");
        Map<String, String> body = form(transport.lastRequest().body());
        assertThat(body).containsEntry("grant_type", "refresh_token")
                .containsEntry("refresh_token", "refresh-secret-token")
                .containsEntry("client_id", "client-id")
                .containsEntry("client_secret", "client-secret");
        verify(fixture.credentialMapper).updateById(rowCaptor.capture());
        MarketingMonitorProviderCredentialDO row = rowCaptor.getValue();
        assertThat(fixture.cipher.decrypt(row.getAccessTokenCiphertext())).isEqualTo("fresh-access-token");
        assertThat(fixture.cipher.decrypt(row.getRefreshTokenCiphertext())).isEqualTo("fresh-refresh-token");
        assertThat(row.getExpiresAt()).isEqualTo(now().plusHours(1));
        assertThat(row.getLastRefreshStatus()).isEqualTo("SUCCESS");
        assertThat(view.accessTokenPrefix()).isEqualTo("fresh-access");
        assertThat(view.toString()).doesNotContain("fresh-access-token", "fresh-refresh-token", "client-secret");
        verify(fixture.eventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("REFRESHED");
        assertThat(eventCaptor.getValue().getMetadataJson()).doesNotContain("fresh-access-token", "fresh-refresh-token");
    }

    @Test
    void refreshFailurePreservesExistingTokensAndWritesFailureEvent() {
        FakeTransport transport = new FakeTransport(500, "{\"error\":\"temporarily_unavailable\"}");
        Fixture fixture = fixture(transport);
        MarketingMonitorProviderCredentialDO existing = credential(7L, 100L);
        when(fixture.credentialMapper.selectOne(any())).thenReturn(existing);
        ArgumentCaptor<MarketingMonitorProviderCredentialDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialDO.class);
        ArgumentCaptor<MarketingMonitorProviderCredentialEventDO> eventCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialEventDO.class);

        MarketingMonitorProviderCredentialView view = fixture.service.refresh(7L, "x-prod",
                new MarketingMonitorProviderCredentialRefreshCommand(true), "operator-1");

        verify(fixture.credentialMapper).updateById(rowCaptor.capture());
        MarketingMonitorProviderCredentialDO row = rowCaptor.getValue();
        assertThat(row.getAccessTokenCiphertext()).isEqualTo(existing.getAccessTokenCiphertext());
        assertThat(row.getRefreshTokenCiphertext()).isEqualTo(existing.getRefreshTokenCiphertext());
        assertThat(row.getLastRefreshStatus()).isEqualTo("FAILED");
        assertThat(row.getLastRefreshError()).contains("500");
        assertThat(view.lastRefreshStatus()).isEqualTo("FAILED");
        verify(fixture.eventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("REFRESH_FAILED");
        assertThat(eventCaptor.getValue().getErrorMessage()).contains("500");
    }

    @Test
    void refreshDueRefreshesOnlyCredentialsExpiringWithinWindow() {
        FakeTransport transport = new FakeTransport(200, """
                {
                  "access_token": "fresh-access-token",
                  "token_type": "bearer",
                  "expires_in": 7200
                }
                """);
        Fixture fixture = fixture(transport);
        MarketingMonitorProviderCredentialDO due = credential(7L, 100L);
        MarketingMonitorProviderCredentialDO future = credential(7L, 101L);
        future.setCredentialKey("future-prod");
        future.setExpiresAt(now().plusHours(2));
        when(fixture.credentialMapper.selectList(any())).thenReturn(List.of(due, future));
        when(fixture.credentialMapper.selectOne(any())).thenReturn(due);
        ArgumentCaptor<MarketingMonitorProviderCredentialDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialDO.class);

        MarketingMonitorProviderCredentialDueRefreshResult result = fixture.service.refreshDue(7L,
                new MarketingMonitorProviderCredentialDueRefreshCommand(10, 10), "monitoring-token-scheduler");

        assertThat(result.tenantId()).isEqualTo(7L);
        assertThat(result.candidateCount()).isEqualTo(2);
        assertThat(result.dueCount()).isEqualTo(1);
        assertThat(result.refreshedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.cutoffAt()).isEqualTo(now().plusMinutes(10));
        assertThat(result.credentials()).singleElement()
                .satisfies(view -> assertThat(view.credentialKey()).isEqualTo("x-prod"));
        verify(fixture.credentialMapper).updateById(rowCaptor.capture());
        assertThat(fixture.cipher.decrypt(rowCaptor.getValue().getAccessTokenCiphertext()))
                .isEqualTo("fresh-access-token");
    }

    @Test
    void revokeCredentialPostsRefreshTokenDisablesLocalCredentialAndWritesEvent() {
        FakeTransport transport = new FakeTransport(200, "{}");
        Fixture fixture = fixture(transport);
        MarketingMonitorProviderCredentialDO existing = credential(7L, 100L);
        existing.setRevokeEndpoint("https://api.x.com/2/oauth2/revoke");
        when(fixture.credentialMapper.selectOne(any())).thenReturn(existing);
        ArgumentCaptor<MarketingMonitorProviderCredentialDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialDO.class);
        ArgumentCaptor<MarketingMonitorProviderCredentialEventDO> eventCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialEventDO.class);

        MarketingMonitorProviderCredentialView view = fixture.service.revoke(7L, "x-prod",
                new MarketingMonitorProviderCredentialRevokeCommand(
                        null,
                        null,
                        true,
                        null,
                        Map.of("ticket", "SR-1")),
                "operator-1");

        assertThat(transport.lastRequest().method()).isEqualTo("POST");
        assertThat(transport.lastRequest().uri().toString()).isEqualTo("https://api.x.com/2/oauth2/revoke");
        Map<String, String> body = form(transport.lastRequest().body());
        assertThat(body).containsEntry("token", "refresh-secret-token")
                .containsEntry("token_type_hint", "refresh_token")
                .containsEntry("client_id", "client-id")
                .containsEntry("client_secret", "client-secret");
        verify(fixture.credentialMapper).updateById(rowCaptor.capture());
        MarketingMonitorProviderCredentialDO row = rowCaptor.getValue();
        assertThat(row.getStatus()).isEqualTo("DISABLED");
        assertThat(row.getRevokedAt()).isEqualTo(now());
        assertThat(row.getLastRevokeStatus()).isEqualTo("SUCCESS");
        assertThat(row.getLastRevokeError()).isNull();
        assertThat(view.status()).isEqualTo("DISABLED");
        assertThat(view.lastRevokeStatus()).isEqualTo("SUCCESS");
        assertThat(view.toString()).doesNotContain("refresh-secret-token", "client-secret");
        verify(fixture.eventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("REVOKED");
        assertThat(eventCaptor.getValue().getMetadataJson())
                .contains("\"httpStatus\":200")
                .doesNotContain("refresh-secret-token", "client-secret");
    }

    @Test
    void revokeFailurePreservesLocalCredentialAndWritesFailureEvent() {
        FakeTransport transport = new FakeTransport(500, "{\"error\":\"temporarily_unavailable\"}");
        Fixture fixture = fixture(transport);
        MarketingMonitorProviderCredentialDO existing = credential(7L, 100L);
        existing.setRevokeEndpoint("https://api.x.com/2/oauth2/revoke");
        when(fixture.credentialMapper.selectOne(any())).thenReturn(existing);
        ArgumentCaptor<MarketingMonitorProviderCredentialDO> rowCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialDO.class);
        ArgumentCaptor<MarketingMonitorProviderCredentialEventDO> eventCaptor =
                ArgumentCaptor.forClass(MarketingMonitorProviderCredentialEventDO.class);

        MarketingMonitorProviderCredentialView view = fixture.service.revoke(7L, "x-prod",
                new MarketingMonitorProviderCredentialRevokeCommand(null, "refresh_token", true, true, Map.of()),
                "operator-1");

        verify(fixture.credentialMapper).updateById(rowCaptor.capture());
        MarketingMonitorProviderCredentialDO row = rowCaptor.getValue();
        assertThat(row.getStatus()).isEqualTo("ACTIVE");
        assertThat(row.getRevokedAt()).isNull();
        assertThat(row.getLastRevokeStatus()).isEqualTo("FAILED");
        assertThat(row.getLastRevokeError()).contains("500");
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.lastRevokeStatus()).isEqualTo("FAILED");
        verify(fixture.eventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("REVOKE_FAILED");
        assertThat(eventCaptor.getValue().getErrorMessage()).contains("500");
    }

    private Fixture fixture(FakeTransport transport) {
        MarketingMonitorProviderCredentialMapper credentialMapper =
                mock(MarketingMonitorProviderCredentialMapper.class);
        MarketingMonitorProviderCredentialEventMapper eventMapper =
                mock(MarketingMonitorProviderCredentialEventMapper.class);
        SecretCipher cipher = SecretCipher.fromBase64Key(CIPHER_KEY);
        return new Fixture(
                credentialMapper,
                eventMapper,
                cipher,
                new MarketingMonitorProviderCredentialService(
                        credentialMapper,
                        eventMapper,
                        transport,
                        new ObjectMapper(),
                        cipher,
                        new BCryptPasswordEncoder(),
                        CLOCK));
    }

    private MarketingMonitorProviderCredentialCommand command() {
        return new MarketingMonitorProviderCredentialCommand(
                " X-Prod ",
                "x_recent_search",
                "oauth2_bearer",
                "X Production",
                true,
                "xoxb-secret-token",
                "refresh-secret-token",
                null,
                "Bearer",
                List.of("tweet.read", "users.read"),
                now().plusHours(1),
                now().plusDays(30),
                "https://api.x.com/2/oauth2/token",
                "https://api.x.com/2/oauth2/revoke",
                "client-id",
                "client-secret",
                Map.of("owner", "brand-team"));
    }

    private MarketingMonitorProviderCredentialDO credential(Long tenantId, Long id) {
        SecretCipher cipher = SecretCipher.fromBase64Key(CIPHER_KEY);
        MarketingMonitorProviderCredentialDO row = new MarketingMonitorProviderCredentialDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setCredentialKey("x-prod");
        row.setProviderType("X_RECENT_SEARCH");
        row.setAuthType("OAUTH2_BEARER");
        row.setDisplayName("X Production");
        row.setStatus("ACTIVE");
        row.setTokenType("Bearer");
        row.setScopesJson("[\"tweet.read\",\"users.read\"]");
        row.setAccessTokenPrefix("xoxb-secret-");
        row.setAccessTokenHash("hash");
        row.setAccessTokenCiphertext(cipher.encrypt("xoxb-secret-token"));
        row.setRefreshTokenPrefix("refresh-secr");
        row.setRefreshTokenHash("hash");
        row.setRefreshTokenCiphertext(cipher.encrypt("refresh-secret-token"));
        row.setClientIdCiphertext(cipher.encrypt("client-id"));
        row.setClientSecretCiphertext(cipher.encrypt("client-secret"));
        row.setRefreshEndpoint("https://api.x.com/2/oauth2/token");
        row.setRevokeEndpoint("https://api.x.com/2/oauth2/revoke");
        row.setExpiresAt(now().plusMinutes(5));
        row.setRefreshTokenExpiresAt(now().plusDays(30));
        row.setRefreshAttemptCount(0);
        row.setMetadataJson("{\"owner\":\"brand-team\"}");
        row.setCreatedBy("operator-1");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
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
            return new MarketingMonitorProviderHttpResponse(statusCode, body, Map.of());
        }

        private MarketingMonitorProviderHttpRequest lastRequest() {
            return lastRequest;
        }
    }

    private record Fixture(MarketingMonitorProviderCredentialMapper credentialMapper,
                           MarketingMonitorProviderCredentialEventMapper eventMapper,
                           SecretCipher cipher,
                           MarketingMonitorProviderCredentialService service) {
    }
}
